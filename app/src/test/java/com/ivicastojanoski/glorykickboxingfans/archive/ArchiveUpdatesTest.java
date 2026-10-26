package com.ivicastojanoski.glorykickboxingfans.archive;

import org.junit.Test;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;
import static com.ivicastojanoski.glorykickboxingfans.archive.ArchiveUpdates.Outcome.*;

/** Pure JVM policy tests use a small semantic domain fixture and fake transport/store, not a JSON parser substitute. */
public class ArchiveUpdatesTest {
    private static Archive data(String id, int revision) {
        List<String> source=Collections.singletonList("demo-source-original");
        Archive.Bout bout=new Archive.Bout("demo-bout-test",1,"demo-fighter-a","demo-fighter-b","completed","draw",null,null,null,null,source);
        Archive.Event event=new Archive.Event("demo-event-test","Fictional event","2025-01-01","Demo hall","Demo city","partial-demo-card",Collections.emptyList(),source,Collections.singletonList(bout));
        return new Archive(id,revision,"Fictional archive","Fictional demo data","Included events only",
                Collections.singletonList(new Archive.Source("demo-source-original","Original synthetic data")),
                Arrays.asList(new Archive.Fighter("demo-fighter-a","Demo A",Collections.emptyList(),source),new Archive.Fighter("demo-fighter-b","Demo B",Collections.emptyList(),source)),
                Collections.singletonList(event),Collections.emptyList());
    }
    private static class Store implements ArchiveUpdates.Store {
        Archive value=data("demo-archive",2); int writes; boolean advanced; boolean fail;
        @Override public Archive current() { return value; }
        @Override public boolean activate(byte[] payload) throws IOException {
            if (fail) throw new IOException("Simulated interrupted write");
            if (advanced) { value=data("demo-archive",4); return false; }
            writes++; value=data("demo-archive",Integer.parseInt(new String(payload,StandardCharsets.UTF_8))); return true;
        }
    }
    private static ArchiveUpdates.Decoder decoder() { return stream -> {
        try { return data("demo-archive",Integer.parseInt(new String(ArchiveUpdates.readBounded(stream),StandardCharsets.UTF_8))); }
        catch (RuntimeException error) { throw new IOException("Bad fake fixture",error); }
    }; }
    private static ArchiveUpdates.Config enabled() { return ArchiveUpdates.Config.forApprovedEndpoint("https://example.invalid/archive.json"); }
    private static ArchiveUpdates updates(Store store, ArchiveUpdates.Transport transport) {
        return new ArchiveUpdates(enabled(),transport,decoder(),store,()->0);
    }
    private static ArchiveUpdates.Transport payload(String value) { return uri->new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
    @Test public void disabledTouchesNeitherStoreNorTransport() {
        assertEquals(DISABLED,new ArchiveUpdates(ArchiveUpdates.Config.disabled(),uri->{throw new AssertionError("Network touched");},decoder(),new ArchiveUpdates.Store() {
            public Archive current(){throw new AssertionError("Store touched");} public boolean activate(byte[] p){throw new AssertionError("Write touched");}
        },()->0).check());
    }
    @Test public void newerRevisionActivatesOnce() { Store s=new Store();assertEquals(ACTIVATED,updates(s,payload("3")).check());assertEquals(3,s.value.revision);assertEquals(1,s.writes); }
    @Test public void equalAndDowngradeNeverWrite() { for(String rev:new String[]{"2","1"}) { Store s=new Store();assertEquals(STALE,updates(s,payload(rev)).check());assertEquals(0,s.writes); } }
    @Test public void differentDatasetCannotReplaceArchive() {
        Store s=new Store();assertEquals(REJECTED,new ArchiveUpdates(enabled(),payload("3"),input->data("demo-other",3),s,()->0).check());assertEquals(0,s.writes);
    }
    @Test public void invalidCandidateDoesNotReachStore() { Store s=new Store();assertEquals(REJECTED,updates(s,payload("bad")).check());assertEquals(2,s.value.revision); }
    @Test public void oversizedStreamCannotActivateEvenWithoutLengthHeader() {
        Store s=new Store();assertEquals(UNAVAILABLE,updates(s,uri->new ByteArrayInputStream(new byte[ArchiveUpdates.MAX_BYTES+1])).check());assertEquals(0,s.writes);
    }
    @Test public void exactSizeLimitIsAcceptedByBoundedReader() throws Exception { assertEquals(ArchiveUpdates.MAX_BYTES,ArchiveUpdates.readBounded(new ByteArrayInputStream(new byte[ArchiveUpdates.MAX_BYTES])).length); }
    @Test public void interruptedTransferKeepsLastKnownGood() {
        Store s=new Store();assertEquals(UNAVAILABLE,updates(s,uri->new InputStream(){public int read() throws IOException{throw new InterruptedIOException("Simulated interruption");}}).check());assertEquals(2,s.value.revision);
    }
    @Test public void readTimeoutKeepsLastKnownGood() { Store s=new Store();assertEquals(UNAVAILABLE,updates(s,uri->{throw new SocketTimeoutException("Simulated timeout");}).check());assertEquals(0,s.writes); }
    @Test public void elapsedTransferBudgetStopsSlowProgress() {
        Store s=new Store();long[] time={0};assertEquals(UNAVAILABLE,new ArchiveUpdates(enabled(),payload("3"),decoder(),s,()->{time[0]+=6000;return time[0];}).check());assertEquals(0,s.writes);
    }
    @Test public void inputClosesWhenReadFails() {
        Store s=new Store();boolean[] closed={false};assertEquals(UNAVAILABLE,updates(s,uri->new InputStream(){public int read() throws IOException{throw new IOException("Failed");}public void close(){closed[0]=true;}}).check());assertTrue(closed[0]);
    }
    @Test public void cancellationBeforeIoPreservesInterruptFlag() {
        try { Thread.currentThread().interrupt();assertEquals(CANCELLED,updates(new Store(),uri->{throw new AssertionError("Network touched");}).check());assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }
    @Test public void cancellationAfterValidationCannotCommit() {
        Store s=new Store();try {assertEquals(CANCELLED,new ArchiveUpdates(enabled(),payload("3"),input->{Thread.currentThread().interrupt();return data("demo-archive",3);},s,()->0).check());assertEquals(0,s.writes);}
        finally{Thread.interrupted();}
    }
    @Test public void newerConcurrentCommitWinsStoreRevisionCheck() { Store s=new Store();s.advanced=true;assertEquals(STALE,updates(s,payload("3")).check());assertEquals(4,s.value.revision);assertEquals(0,s.writes); }
    @Test public void failedAtomicWriteIsNotReportedAsActivated() { Store s=new Store();s.fail=true;assertEquals(UNAVAILABLE,updates(s,payload("3")).check());assertEquals(2,s.value.revision); }
    @Test public void malformedUtf8CannotBeReplacedSilently() {
        Store s=new Store();boolean[] decoded={false};
        assertEquals(REJECTED,new ArchiveUpdates(enabled(),uri->new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}),input->{decoded[0]=true;return data("demo-archive",3);},s,()->0).check());
        assertFalse(decoded[0]);assertEquals(0,s.writes);
    }
    @Test(timeout=1000) public void noProgressStreamFailsClosed() { Store s=new Store();assertEquals(UNAVAILABLE,updates(s,uri->new InputStream(){public int read(){return 0;}public int read(byte[] b,int o,int n){return 0;}}).check());assertEquals(0,s.writes); }
    @Test public void unsafeEndpointShapesAreRejectedBeforeNetwork() {
        for(String url:Arrays.asList("http://example.invalid/a.json","https://user:pass@example.invalid/a.json","https://example.invalid/a.json?token=demo","https://example.invalid/a.json#fragment","https://example.invalid:8443/a.json","https://127.0.0.1/a.json","https://localhost/a.json","https://example.invalid/a/../b.json")) {
            try {ArchiveUpdates.Config.forApprovedEndpoint(url);fail(url);}catch(IllegalArgumentException expected){}
        }
    }
}
