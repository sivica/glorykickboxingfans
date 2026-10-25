package com.ivicastojanoski.glorykickboxingfans.archive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Strict bundled JSON adapter. Never queries a service, installs data or substitutes sample results on error. */
public final class ArchiveReader {
    private ArchiveReader() {}
    public static Archive read(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int count;
        while ((count = input.read(chunk)) != -1) {
            if (buffer.size() + count > 1024 * 1024) throw new IOException("Bundled archive exceeds size limit");
            buffer.write(chunk, 0, count);
        }
        try { return parse(new String(buffer.toByteArray(), StandardCharsets.UTF_8)); }
        catch (JSONException | IllegalArgumentException exception) { throw new IOException("Bundled archive is invalid", exception); }
    }

    public static Archive parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        keys(root, "schema_version id revision kind title notice coverage sources fighters events corrections");
        Archive.require(number(root, "schema_version") == 1 && string(root, "kind").equals("synthetic-demo"), "Unsupported archive schema/kind");
        List<Archive.Source> sources = new ArrayList<>();
        JSONArray rows = root.getJSONArray("sources");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i); keys(row, "id kind label");
            Archive.require(string(row, "kind").equals("synthetic"), "Unapproved source");
            sources.add(new Archive.Source(string(row, "id"), string(row, "label")));
        }
        List<Archive.Fighter> fighters = new ArrayList<>();
        rows = root.getJSONArray("fighters");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i); keys(row, "id name aliases provenance");
            fighters.add(new Archive.Fighter(string(row, "id"), string(row, "name"), strings(row, "aliases"), strings(row, "provenance")));
        }
        List<Archive.Event> events = new ArrayList<>();
        rows = root.getJSONArray("events");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i); keys(row, "id title date venue location aliases coverage provenance bouts");
            List<Archive.Bout> bouts = new ArrayList<>(); JSONArray card = row.getJSONArray("bouts");
            for (int j = 0; j < card.length(); j++) {
                JSONObject bout = card.getJSONObject(j); keys(bout, "id order red_id blue_id status result method round tournament provenance");
                String tournamentId = null, stage = null;
                if (!bout.isNull("tournament")) {
                    JSONObject tournament = bout.getJSONObject("tournament"); keys(tournament, "id stage");
                    tournamentId = string(tournament, "id"); stage = string(tournament, "stage");
                }
                bouts.add(new Archive.Bout(string(bout, "id"), number(bout, "order"), string(bout, "red_id"), string(bout, "blue_id"),
                        string(bout, "status"), string(bout, "result"), bout.isNull("method") ? null : string(bout, "method"),
                        bout.isNull("round") ? null : number(bout, "round"), tournamentId, stage, strings(bout, "provenance")));
            }
            events.add(new Archive.Event(string(row, "id"), string(row, "title"), string(row, "date"), string(row, "venue"),
                    string(row, "location"), string(row, "coverage"), strings(row, "aliases"), strings(row, "provenance"), bouts));
        }
        List<Archive.Correction> corrections = new ArrayList<>(); rows = root.getJSONArray("corrections");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i); keys(row, "id from_revision to_revision target_id field previous_value new_value reason provenance");
            corrections.add(new Archive.Correction(string(row, "id"), number(row, "from_revision"), number(row, "to_revision"),
                    string(row, "target_id"), string(row, "field"), string(row, "previous_value"), string(row, "new_value"), string(row, "reason"), strings(row, "provenance")));
        }
        return new Archive(string(root, "id"), number(root, "revision"), string(root, "title"), string(root, "notice"), string(root, "coverage"), sources, fighters, events, corrections);
    }
    private static void keys(JSONObject object, String fields) {
        Set<String> expected = new HashSet<>(Arrays.asList(fields.split(" ")));
        Iterator<String> actual = object.keys();
        while (actual.hasNext()) Archive.require(expected.remove(actual.next()), "Unknown field");
        Archive.require(expected.isEmpty(), "Missing field");
    }
    private static String string(JSONObject object, String key) throws JSONException {
        Object value = object.get(key); Archive.require(value instanceof String, "String required"); return (String) value;
    }
    private static int number(JSONObject object, String key) throws JSONException {
        Object value = object.get(key); Archive.require(value instanceof Integer && (Integer) value > 0, "Positive integer required"); return (Integer) value;
    }
    private static List<String> strings(JSONObject object, String key) throws JSONException {
        List<String> result = new ArrayList<>(); JSONArray array = object.getJSONArray(key);
        for (int i = 0; i < array.length(); i++) { Object value = array.get(i); Archive.require(value instanceof String, "String array required"); result.add((String) value); }
        return result;
    }
}
