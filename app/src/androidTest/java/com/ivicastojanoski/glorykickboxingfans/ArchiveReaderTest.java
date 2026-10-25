package com.ivicastojanoski.glorykickboxingfans;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ivicastojanoski.glorykickboxingfans.archive.Archive;
import com.ivicastojanoski.glorykickboxingfans.archive.ArchiveReader;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Uses Android's actual JSON adapter and the packaged synthetic data, without a live service. */
@RunWith(AndroidJUnit4.class)
public class ArchiveReaderTest {
    private String bundled() throws Exception {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getTargetContext().getAssets().open("archive-demo-v1.json")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int size;
            while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    @Test public void packagedDatasetParsesAndQueriesRealDomain() throws Exception {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getTargetContext().getAssets().open("archive-demo-v1.json")) {
            Archive archive = ArchiveReader.read(input);
            assertEquals(5, archive.events.size()); assertEquals(8, archive.fighters.size());
            assertEquals(2, archive.revision);
            assertTrue(archive.notice.contains("fictional"));
            assertEquals(1, archive.search("Navigator", 2023).size());
            assertEquals(4, archive.appearances("demo-fighter-aster").size());
            assertEquals("Aster Vale", archive.fighter("demo-fighter-aster").name);
        }
    }
    @Test public void missingRequiredFieldFailsClosed() throws Exception {
        JSONObject root = new JSONObject(bundled()); root.remove("notice"); invalid(root.toString());
    }
    @Test public void unapprovedFactualKindFailsClosed() throws Exception {
        JSONObject root = new JSONObject(bundled()); root.put("kind", "official-results"); invalid(root.toString());
    }
    @Test public void danglingParticipantFailsClosed() throws Exception {
        JSONObject root = new JSONObject(bundled()); root.getJSONArray("events").getJSONObject(0).getJSONArray("bouts").getJSONObject(0).put("red_id", "demo-missing"); invalid(root.toString());
    }
    @Test public void numericStringIsNotAcceptedAsRevision() throws Exception {
        JSONObject root = new JSONObject(bundled()); root.put("revision", "2"); invalid(root.toString());
    }
    @Test public void malformedDataDoesNotSubstituteInventedResults() throws Exception { invalid("{malformed"); }
    private JSONObject change(String id, String field, Object before, Object after, int from, int to) throws Exception {
        return new JSONObject().put("id",id).put("from_revision",from).put("to_revision",to)
                .put("target_id", "demo-bout-north-final").put("field",field)
                .put("previous_value", before==null ? JSONObject.NULL : before).put("new_value",after==null ? JSONObject.NULL : after)
                .put("reason","Original fictional correction test").put("provenance",new org.json.JSONArray().put("demo-source-original"));
    }
    @Test public void nullableMethodCorrectionsRoundTripTruthfully() throws Exception {
        for(String[] values:new String[][] {{null,"KO"},{"KO",null}}) {
            JSONObject root=new JSONObject(bundled());
            root.getJSONArray("events").getJSONObject(0).getJSONArray("bouts").getJSONObject(0).put("method",values[1]==null ? JSONObject.NULL : values[1]);
            root.getJSONArray("corrections").put(change("demo-correction-method","method",values[0],values[1],1,2));
            Archive archive=ArchiveReader.parse(root.toString());
            assertEquals(values[0],archive.corrections.get(1).previousValue);assertEquals(values[1],archive.corrections.get(1).newValue);
        }
    }
    @Test public void nullableMethodHistoryChainsRoundTrip() throws Exception {
        for(String[] values:new String[][] {{"KO",null,"KO"},{null,"KO",null}}) {
            JSONObject root=new JSONObject(bundled());root.put("revision",3);
            root.getJSONArray("events").getJSONObject(0).getJSONArray("bouts").getJSONObject(0).put("method",values[2]==null ? JSONObject.NULL : values[2]);
            root.getJSONArray("corrections").put(change("demo-correction-one","method",values[0],values[1],1,2));
            root.getJSONArray("corrections").put(change("demo-correction-two","method",values[1],values[2],2,3));
            Archive archive=ArchiveReader.parse(root.toString());assertEquals(values[2],archive.corrections.get(2).newValue);
        }
    }
    @Test public void nullHistoryRemainsInvalidForName() throws Exception {
        JSONObject root=new JSONObject(bundled());root.getJSONArray("corrections").getJSONObject(0).put("previous_value",JSONObject.NULL);invalid(root.toString());
    }
    @Test public void correctionTournamentIdentityCollisionFailsClosed() throws Exception {
        JSONObject root=new JSONObject(bundled());root.getJSONArray("corrections").getJSONObject(0).put("id","demo-tournament-northern");invalid(root.toString());
    }
    private static void invalid(String json) throws Exception {
        try {
            ArchiveReader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            fail("Invalid data must be refused");
        } catch (IOException expected) {
            assertEquals("Bundled archive is invalid", expected.getMessage());
        }
    }
}
