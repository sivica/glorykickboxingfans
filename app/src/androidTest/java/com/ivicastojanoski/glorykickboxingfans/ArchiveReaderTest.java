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
    private static void invalid(String json) throws Exception {
        try {
            ArchiveReader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            fail("Invalid data must be refused");
        } catch (IOException expected) {
            assertEquals("Bundled archive is invalid", expected.getMessage());
        }
    }
}
