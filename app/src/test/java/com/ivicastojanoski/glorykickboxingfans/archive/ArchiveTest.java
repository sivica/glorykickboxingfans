package com.ivicastojanoski.glorykickboxingfans.archive;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class ArchiveTest {
    private static final List<String> SOURCE = Collections.singletonList("demo-source-original");
    private static Archive.Bout bout(String id, int order, String status, String result, String red, String blue) {
        return new Archive.Bout(id, order, red, blue, status, result, null, null, null, null, SOURCE);
    }
    private static Archive.Event event(String id, String date, Archive.Bout... bouts) {
        return new Archive.Event(id, "Northern Demo", date, "Fictional Hall", "Northport", "partial-demo-card", Collections.singletonList("Polar Card"), SOURCE, Arrays.asList(bouts));
    }
    private static Archive data(List<Archive.Event> events, List<Archive.Correction> corrections) {
        return new Archive("demo-archive", 2, "Fictional archive", "DEMO: fictional data", "Included events only",
                Collections.singletonList(new Archive.Source("demo-source-original", "Original synthetic data")),
                Arrays.asList(new Archive.Fighter("demo-fighter-a", "Áster Vale", Collections.singletonList("The Navigator"), SOURCE),
                        new Archive.Fighter("demo-fighter-b", "Rowan Flint", Collections.emptyList(), SOURCE)), events, corrections);
    }
    private static Archive sample() {
        return data(Arrays.asList(event("demo-event-old", "2023-05-06", bout("demo-bout-old", 1, "completed", "draw", "demo-fighter-a", "demo-fighter-b")),
                event("demo-event-new", "2025-11-08", bout("demo-bout-new", 1, "completed", "red_win", "demo-fighter-a", "demo-fighter-b"))), Collections.emptyList());
    }
    @Test public void newestFirstAndYearIntersection() {
        Archive archive = sample();
        assertEquals("demo-event-new", archive.events.get(0).id);
        assertEquals(Arrays.asList(2025, 2023), archive.years());
        assertEquals(1, archive.search("northern", 2023).size());
        assertEquals(0, archive.search("northern", 2024).size());
        assertEquals(2, archive.search(null, null).size());
    }
    @Test public void searchIsCaseAccentAndAliasInsensitive() {
        for (String query : new String[] {"ASTER", "the navigator", "Polar Card", "Northport", "  rowan  "}) {
            assertEquals(query, 2, sample().search(query, null).size());
        }
        assertEquals(0, sample().search(".*", null).size());
        assertEquals(0, sample().search("missing", null).size());
    }
    @Test public void fighterAppearancesStayInsideIncludedDataset() {
        Archive archive = sample();
        assertEquals(2, archive.appearances("demo-fighter-a").size());
        assertEquals("demo-event-new", archive.appearances("demo-fighter-a").get(0).event.id);
        assertTrue(archive.appearances("unrepresented-career").isEmpty());
        assertNull(archive.event("missing"));
        assertNull(archive.fighter("missing"));
    }
    @Test public void outcomesPreserveDrawNoContestCancellationAndUnknown() {
        String[] results = {"red_win", "blue_win", "draw", "no_contest", "unknown"};
        String[] expected = {"Win", "Loss", "Draw", "No contest", "Result unknown"};
        for (int i = 0; i < results.length; i++) assertEquals(expected[i], bout("demo-bout-case", 1, "completed", results[i], "a", "b").outcomeFor("a"));
        assertEquals("Cancelled", bout("demo-bout-case", 1, "cancelled", "not_applicable", "a", "b").outcomeFor("a"));
    }
    @Test(expected = IllegalArgumentException.class) public void unrelatedFighterCannotReceiveOutcome() {
        bout("demo-bout-case", 1, "completed", "red_win", "a", "b").outcomeFor("c");
    }
    @Test(expected = UnsupportedOperationException.class) public void searchResultsAreImmutable() { sample().search("", null).clear(); }
    @Test(expected = UnsupportedOperationException.class) public void eventListIsImmutable() { sample().events.clear(); }
    @Test(expected = IllegalArgumentException.class) public void duplicateIdsRefused() {
        Archive.Event event = sample().events.get(0); data(Arrays.asList(event, event), Collections.emptyList());
    }
    @Test(expected = IllegalArgumentException.class) public void danglingFighterRefused() {
        data(Collections.singletonList(event("demo-event-a", "2025-01-01", bout("demo-bout-a", 1, "completed", "draw", "missing", "demo-fighter-a"))), Collections.emptyList());
    }
    @Test(expected = IllegalArgumentException.class) public void contradictoryCancellationRefused() {
        data(Collections.singletonList(event("demo-event-a", "2025-01-01", bout("demo-bout-a", 1, "cancelled", "red_win", "demo-fighter-a", "demo-fighter-b"))), Collections.emptyList());
    }
    @Test(expected = IllegalArgumentException.class) public void noncontiguousOrderRefused() {
        data(Collections.singletonList(event("demo-event-a", "2025-01-01", bout("demo-bout-a", 2, "completed", "draw", "demo-fighter-a", "demo-fighter-b"))), Collections.emptyList());
    }
    @Test(expected = IllegalArgumentException.class) public void impossibleDateRefused() {
        data(Collections.singletonList(event("demo-event-a", "2025-02-30", bout("demo-bout-a", 1, "completed", "draw", "demo-fighter-a", "demo-fighter-b"))), Collections.emptyList());
    }
    @Test public void correctionsRetainIdentityAndHistory() {
        Archive.Correction correction = new Archive.Correction("demo-correction-a", 1, 2, "demo-fighter-a", "name", "Old name", "Áster Vale", "Demonstrate correction", SOURCE);
        Archive archive = data(sample().events, Collections.singletonList(correction));
        assertEquals("demo-fighter-a", archive.fighter(correction.targetId).id);
        assertEquals("Old name", archive.corrections.get(0).previousValue);
    }
    @Test(expected = IllegalArgumentException.class) public void correctionCurrentValueMismatchRefused() {
        data(sample().events, Collections.singletonList(new Archive.Correction("demo-correction-a", 1, 2, "demo-fighter-a", "name", "Old name", "Wrong name", "Demonstrate rejection", SOURCE)));
    }
    @Test public void cardOrderIsIndependentOfInputArrayOrder() {
        Archive.Event event = event("demo-event-a", "2025-01-01", bout("demo-bout-two", 2, "completed", "draw", "demo-fighter-a", "demo-fighter-b"), bout("demo-bout-one", 1, "completed", "draw", "demo-fighter-a", "demo-fighter-b"));
        assertEquals("demo-bout-one", data(Collections.singletonList(event), Collections.emptyList()).events.get(0).bouts.get(0).id);
    }

    private static Archive withMethod(String method, int revision, List<Archive.Correction> corrections) {
        Archive.Bout bout = new Archive.Bout("demo-bout-method", 1, "demo-fighter-a", "demo-fighter-b", "completed", "red_win", method, 2, null, null, SOURCE);
        Archive base = data(Collections.singletonList(event("demo-event-method", "2025-01-01", bout)), Collections.emptyList());
        return new Archive(base.id, revision, base.title, base.notice, base.coverage, base.sources, base.fighters, base.events, corrections);
    }
    private static Archive.Correction methodChange(String id, int from, int to, String before, String after) {
        return new Archive.Correction(id, from, to, "demo-bout-method", "method", before, after, "Fictional correction test", SOURCE);
    }
    @Test public void methodCorrectionNullToKnownRetainsTruth() {
        Archive archive=withMethod("KO",2,Collections.singletonList(methodChange("demo-correction-method",1,2,null,"KO")));
        assertNull(archive.corrections.get(0).previousValue);assertEquals("KO",archive.corrections.get(0).newValue);
    }
    @Test public void methodCorrectionKnownToNullRetainsTruth() {
        Archive archive=withMethod(null,2,Collections.singletonList(methodChange("demo-correction-method",1,2,"KO",null)));
        assertEquals("KO",archive.corrections.get(0).previousValue);assertNull(archive.corrections.get(0).newValue);
    }
    @Test public void nullableCorrectionChainsSupportBothTransitions() {
        for(String[] values:new String[][] {{"KO",null,"KO"},{null,"KO",null}}) {
            Archive archive=withMethod(values[2],3,Arrays.asList(methodChange("demo-correction-one",1,2,values[0],values[1]),methodChange("demo-correction-two",2,3,values[1],values[2])));
            assertEquals(values[2],archive.corrections.get(1).newValue);
        }
    }
    @Test(expected=IllegalArgumentException.class) public void nullRemainsInvalidForNonNullableFields() {
        data(sample().events, Collections.singletonList(new Archive.Correction("demo-correction-name",1,2,"demo-fighter-a","name",null,"Áster Vale","Name cannot be null",SOURCE)));
    }
    @Test(expected=IllegalArgumentException.class) public void brokenNullableHistoryIsRejected() {
        withMethod("KO",3,Arrays.asList(methodChange("demo-correction-one",1,2,"KO",null),methodChange("demo-correction-two",2,3,"Wrong","KO")));
    }
    @Test(expected=IllegalArgumentException.class) public void correctionCannotUseTournamentIdentity() {
        Archive.Bout bout=new Archive.Bout("demo-bout-collision",1,"demo-fighter-a","demo-fighter-b","completed","red_win","KO",2,"demo-tournament-shared","final",SOURCE);
        data(Collections.singletonList(event("demo-event-a","2025-01-01",bout)),Collections.singletonList(new Archive.Correction("demo-tournament-shared",1,2,"demo-fighter-a","name","Prior","Áster Vale","Collision must fail",SOURCE)));
    }

}
