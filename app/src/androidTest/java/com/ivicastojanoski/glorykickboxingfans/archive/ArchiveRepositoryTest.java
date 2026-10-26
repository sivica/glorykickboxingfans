package com.ivicastojanoski.glorykickboxingfans.archive;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AtomicFile;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Actual Android parser + AtomicFile, isolated files and synthetic payloads. No network or production-cache reset. */
@RunWith(AndroidJUnit4.class)
public class ArchiveRepositoryTest {
    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static byte[] bundled() throws Exception {
        try(InputStream in=context().getAssets().open("archive-demo-v1.json")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int count;
            while((count=in.read(bytes))!=-1)out.write(bytes,0,count);return out.toByteArray();
        }
    }
    private static byte[] revision(int value) throws Exception {return new JSONObject(new String(bundled(),StandardCharsets.UTF_8)).put("revision",value).toString().getBytes(StandardCharsets.UTF_8);}
    private static File file(){File dir=new File(context().getCacheDir(),"archive-update-test-"+UUID.randomUUID());assertTrue(dir.mkdir());return new File(dir,"dataset.json");}
    private static ArchiveRepository repo(File file,byte[] bundle){return new ArchiveRepository(()->new ByteArrayInputStream(bundle),file);}
    private static void refused(ArchiveRepository repo,byte[] bytes) throws Exception {try{repo.activate(bytes);fail("Invalid payload accepted");}catch(IOException expected){}assertEquals(3,repo.current().revision);}
    private static void refusedFromDownloadAndActivation(ArchiveRepository repo,String json) throws Exception {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);refused(repo,bytes);
        ArchiveUpdates check=new ArchiveUpdates(ArchiveUpdates.Config.forApprovedEndpoint("https://example.invalid/archive.json"),uri->new ByteArrayInputStream(bytes),ArchiveReader::read,repo,()->0);
        assertEquals(ArchiveUpdates.Outcome.REJECTED,check.check());assertEquals(3,repo.current().revision);
    }
    @Test public void productionDefaultsStayBundledDisabledAndWithoutNetworkPermission() throws Exception {
        assertFalse(ArchiveRepository.get(context()).load().cached);assertEquals(2,ArchiveRepository.get(context()).current().revision);
        assertEquals(ArchiveUpdates.Outcome.DISABLED,ArchiveRepository.get(context()).checkForUpdates());
        PackageInfo p=context().getPackageManager().getPackageInfo(context().getPackageName(),PackageManager.GET_PERMISSIONS);
        assertTrue(p.requestedPermissions==null || !Arrays.asList(p.requestedPermissions).contains("android.permission.INTERNET"));
    }
    @Test public void validUpdateSurvivesRepositoryRecreationOffline() throws Exception {
        File file=file();ArchiveRepository first=repo(file,bundled());assertFalse(first.load().cached);assertTrue(first.activate(revision(3)));
        ArchiveRepository restarted=repo(file,bundled());assertTrue(restarted.load().cached);assertEquals(3,restarted.current().revision);
        assertEquals("Aster Vale",restarted.current().fighter("demo-fighter-aster").name);
    }
    @Test public void staleDowngradeAndWrongIdentityCannotReplaceLastKnownGood() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));assertFalse(store.activate(revision(3)));assertFalse(store.activate(revision(2)));
        JSONObject other=new JSONObject(new String(revision(4),StandardCharsets.UTF_8));other.put("id","demo-other-archive");refused(store,other.toString().getBytes(StandardCharsets.UTF_8));
    }
    @Test public void invalidSchemaJsonAndIdentityCollisionKeepLastKnownGood() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));
        JSONObject future=new JSONObject(new String(revision(4),StandardCharsets.UTF_8)).put("schema_version",2);refused(store,future.toString().getBytes(StandardCharsets.UTF_8));
        JSONObject collision=new JSONObject(new String(revision(4),StandardCharsets.UTF_8));collision.getJSONArray("corrections").getJSONObject(0).put("id","demo-tournament-northern");refused(store,collision.toString().getBytes(StandardCharsets.UTF_8));
        refused(store,"{interrupted".getBytes(StandardCharsets.UTF_8));refused(store,new byte[ArchiveUpdates.MAX_BYTES+1]);
    }
    @Test public void malformedUtf8CannotEnterSavedOrBundledReader() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));
        byte[] invalid=revision(4);String text=new String(invalid,StandardCharsets.UTF_8);int index=text.indexOf("Fictional event archive");assertTrue(index>0);invalid[index]=(byte)0xff;refused(store,invalid);
        try {ArchiveReader.read(new ByteArrayInputStream(invalid));fail("Malformed UTF-8 accepted");}catch(IOException expected){}
    }
    @Test public void abandonedPartialWriteRecoversPreviouslyCommittedRevision() throws Exception {
        File file=file();ArchiveRepository store=repo(file,bundled());assertTrue(store.activate(revision(3)));
        FileOutputStream interrupted=new AtomicFile(file).startWrite();interrupted.write("{partial revision4".getBytes(StandardCharsets.UTF_8));
        // TEST ONLY: emulate descriptor closure on process death before finishWrite/failWrite.
        interrupted.close();
        ArchiveRepository restarted=repo(file,bundled());assertEquals(3,restarted.current().revision);assertTrue(restarted.load().cached);
    }
    @Test public void corruptCacheFallsBackToBundledWithoutInventingAnUpdate() throws Exception {
        File file=file();AtomicFile cache=new AtomicFile(file);FileOutputStream output=cache.startWrite();output.write("{corrupt".getBytes(StandardCharsets.UTF_8));cache.finishWrite(output);
        ArchiveRepository restarted=repo(file,bundled());assertEquals(2,restarted.current().revision);assertFalse(restarted.load().cached);
    }
    @Test public void injectedStartWriteFailureKeepsRealLastKnownGood() throws Exception {
        File file=file();byte[] baseline=bundled();ArchiveRepository store=repo(file,baseline);assertTrue(store.activate(revision(3)));
        ArchiveRepository failed=new ArchiveRepository(()->new ByteArrayInputStream(baseline),new AtomicFile(file){
            @Override public FileOutputStream startWrite() throws IOException {throw new IOException("Injected write-start failure");}
        });
        try{failed.activate(revision(4));fail("Write failure required");}catch(IOException expected){assertEquals("Injected write-start failure",expected.getMessage());}
        assertEquals(3,failed.current().revision);assertEquals(3,repo(file,baseline).current().revision);
    }
    @Test public void failedCommitRollsBackThroughRealPlatformAtomicFile() throws Exception {
        File file=file();byte[] baseline=bundled();ArchiveRepository store=repo(file,baseline);assertTrue(store.activate(revision(3)));
        ArchiveRepository failed=new ArchiveRepository(()->new ByteArrayInputStream(baseline),new AtomicFile(file){
            @Override public void finishWrite(FileOutputStream output){throw new IllegalStateException("Injected commit failure");}
        });
        try{failed.activate(revision(4));fail("Commit failure required");}catch(IllegalStateException expected){assertEquals("Injected commit failure",expected.getMessage());}
        assertEquals(3,failed.current().revision);assertEquals(3,repo(file,baseline).current().revision);
    }
    @Test public void newerBundledBaselineCannotBeDowngradedByOlderSavedFile() throws Exception {
        File file=file();ArchiveRepository old=repo(file,bundled());assertTrue(old.activate(revision(3)));
        ArchiveRepository newer=repo(file,revision(4));assertEquals(4,newer.current().revision);assertFalse(newer.load().cached);assertFalse(newer.activate(revision(3)));
    }
    @Test public void cancellationBeforePersistencePreservesSavedRevisionAndFlag() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));
        try {Thread.currentThread().interrupt();try{store.activate(revision(4));fail();}catch(InterruptedIOException expected){}assertTrue(Thread.currentThread().isInterrupted());}
        finally {Thread.interrupted();}
        assertEquals(3,store.current().revision);
    }
    @Test public void engineAndActualReaderStoreActivateOnlyValidNewerSyntheticPayload() throws Exception {
        ArchiveRepository store=repo(file(),bundled());byte[] bytes=revision(3);
        ArchiveUpdates updates=new ArchiveUpdates(ArchiveUpdates.Config.forApprovedEndpoint("https://example.invalid/archive.json"),uri->new ByteArrayInputStream(bytes),ArchiveReader::read,store,()->0);
        assertEquals(ArchiveUpdates.Outcome.ACTIVATED,updates.check());assertEquals(ArchiveUpdates.Outcome.STALE,updates.check());assertEquals(3,store.current().revision);
    }
    @Test public void fullDocumentAndUniqueKeysRequiredForActivation() throws Exception {
        File file=file();ArchiveRepository store=repo(file,bundled());assertTrue(store.activate(revision(3)));
        String json=new String(revision(4),StandardCharsets.UTF_8);
        for(String invalid:new String[]{json+" trailing garbage",json+" {}",json+" []",
                "{\"revision\":1,"+json.substring(1),"{\"\\u0072evision\":1,"+json.substring(1),
                json.replace("\"kind\":\"synthetic\"","\"kind\":\"invalid\",\"kind\":\"synthetic\"")}) {
            assertNotEquals(json,invalid);refusedFromDownloadAndActivation(store,invalid);
            File corrupt=file();AtomicFile cache=new AtomicFile(corrupt);FileOutputStream output=cache.startWrite();output.write(invalid.getBytes(StandardCharsets.UTF_8));cache.finishWrite(output);
            assertFalse(repo(corrupt,bundled()).load().cached);assertEquals(2,repo(corrupt,bundled()).current().revision);
        }
    }
    @Test public void lenientSyntaxCannotReplaceLastKnownGood() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));
        String json=new String(revision(4),StandardCharsets.UTF_8);
        for(String invalid:new String[]{"/* comment */"+json,json.replace("\"revision\":4","'revision':4"),
                json.replace("\"revision\":4","revision:4"),json.replace("\"revision\":4","\"revision\":+4"),
                json.replace("\"revision\":4","\"revision\":04"),json.replace("\"revision\":4","\"revision\":4.")}) {
            assertNotEquals(json,invalid);refusedFromDownloadAndActivation(store,invalid);
        }
    }
    @Test public void nestingLimitPrecedesAndroidParserAndProtectsSavedData() throws Exception {
        ArchiveRepository store=repo(file(),bundled());assertTrue(store.activate(revision(3)));
        StringBuilder nested=new StringBuilder();for(int i=0;i<4096;i++)nested.append('[');nested.append('0');for(int i=0;i<4096;i++)nested.append(']');
        String json="{\"unexpected\":"+nested+","+new String(revision(4),StandardCharsets.UTF_8).substring(1);
        try{ArchiveReader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));fail("Depth limit required");}
        catch(IOException expected){assertTrue(expected.getCause() instanceof IllegalArgumentException);assertEquals("JSON nesting limit exceeded",expected.getCause().getMessage());}
        refusedFromDownloadAndActivation(store,json);
        File file=file();AtomicFile cache=new AtomicFile(file);FileOutputStream output=cache.startWrite();output.write(json.getBytes(StandardCharsets.UTF_8));cache.finishWrite(output);
        assertEquals(2,repo(file,bundled()).current().revision);assertFalse(repo(file,bundled()).load().cached);
    }
}
