package com.ivicastojanoski.glorykickboxingfans.archive;

import java.text.Normalizer;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Immutable, offline-only domain. Identifiers and coverage never depend on display names. */
public final class Archive {
    public final String id, title, notice, coverage;
    public final int revision;
    public final List<Source> sources;
    public final List<Fighter> fighters;
    public final List<Event> events;
    public final List<Correction> corrections;
    private final Map<String, Fighter> fighterIndex = new LinkedHashMap<>();
    private final Map<String, Event> eventIndex = new LinkedHashMap<>();

    public Archive(String id, int revision, String title, String notice, String coverage,
                   List<Source> sources, List<Fighter> fighters, List<Event> events, List<Correction> corrections) {
        this.id = id; this.revision = revision; this.title = title; this.notice = notice; this.coverage = coverage;
        this.sources = immutable(sources); this.fighters = immutable(fighters); this.corrections = immutable(corrections);
        List<Event> ordered = new ArrayList<>(events);
        ordered.sort(Comparator.comparing((Event e) -> e.date).reversed().thenComparing(e -> e.id));
        this.events = immutable(ordered);
        for (Fighter fighter : fighters) require(fighterIndex.put(fighter.id, fighter) == null, "Duplicate fighter");
        for (Event event : events) require(eventIndex.put(event.id, event) == null, "Duplicate event");
        validate();
    }

    public Fighter fighter(String id) { return fighterIndex.get(id); }
    public Event event(String id) { return eventIndex.get(id); }

    public List<Integer> years() {
        List<Integer> result = new ArrayList<>();
        for (Event event : events) {
            Integer year = Integer.valueOf(event.date.substring(0, 4));
            if (!result.contains(year)) result.add(year);
        }
        result.sort(Collections.reverseOrder());
        return immutable(result);
    }

    /** Literal normalized substring search: titles, locations, aliases and included fighter names. */
    public List<Event> search(String query, Integer year) {
        String needle = normalize(query == null ? "" : query.trim());
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (year != null && !event.date.startsWith(year + "-")) continue;
            StringBuilder haystack = new StringBuilder(event.title).append(' ').append(event.location).append(' ').append(event.venue);
            for (String alias : event.aliases) haystack.append(' ').append(alias);
            for (Bout bout : event.bouts) {
                for (String id : new String[] {bout.redId, bout.blueId}) {
                    Fighter fighter = fighter(id);
                    haystack.append(' ').append(fighter.name);
                    for (String alias : fighter.aliases) haystack.append(' ').append(alias);
                }
            }
            if (normalize(haystack.toString()).contains(needle)) result.add(event);
        }
        return immutable(result);
    }

    public List<Appearance> appearances(String fighterId) {
        List<Appearance> result = new ArrayList<>();
        if (!fighterIndex.containsKey(fighterId)) return immutable(result);
        for (Event event : events) for (Bout bout : event.bouts) {
            if (bout.redId.equals(fighterId) || bout.blueId.equals(fighterId)) result.add(new Appearance(event, bout));
        }
        return immutable(result);
    }

    public String sourceLabels(List<String> ids) {
        List<String> labels = new ArrayList<>();
        for (Source source : sources) if (ids.contains(source.id)) labels.add(source.label);
        return String.join(" · ", labels);
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private void validate() {
        require(revision > 0 && notice.toLowerCase(Locale.ROOT).contains("demo")
                && notice.toLowerCase(Locale.ROOT).contains("fictional"), "Demo disclosure/revision required");
        text(title); text(coverage);
        Set<String> ids = new HashSet<>(); addId(ids, id);
        Set<String> sourceIds = new HashSet<>();
        require(!sources.isEmpty() && !fighters.isEmpty() && !events.isEmpty(), "Empty archive");
        for (Source source : sources) { addId(ids, source.id); text(source.label); sourceIds.add(source.id); }
        for (Fighter fighter : fighters) {
            addId(ids, fighter.id); text(fighter.name); strings(fighter.aliases); provenance(fighter.provenance, sourceIds);
        }
        Map<String, String> tournaments = new HashMap<>();
        Map<String, Map<String, String>> values = new HashMap<>();
        values.put(id, fields("title", title, "coverage", coverage));
        for (Fighter fighter : fighters) values.put(fighter.id, fields("name", fighter.name));
        for (Event event : events) {
            addId(ids, event.id); text(event.title); text(event.venue); text(event.location); strings(event.aliases);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            require(event.date != null && event.date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                    && format.parse(event.date, position) != null && position.getIndex() == 10, "Invalid date");
            require(oneOf(event.coverage, "complete-demo-card", "partial-demo-card"), "Unknown coverage");
            provenance(event.provenance, sourceIds);
            values.put(event.id, fields("title", event.title, "coverage", event.coverage));
            require(!event.bouts.isEmpty(), "Empty bout card");
            int expected = 1;
            for (Bout bout : event.bouts) {
                addId(ids, bout.id); require(bout.order == expected++, "Noncontiguous bout order");
                require(fighter(bout.redId) != null && fighter(bout.blueId) != null && !bout.redId.equals(bout.blueId), "Invalid participants");
                require(oneOf(bout.status, "completed", "cancelled", "unknown"), "Invalid status");
                if (bout.status.equals("completed")) require(oneOf(bout.result, "red_win", "blue_win", "draw", "no_contest", "unknown"), "Invalid result");
                else {
                    require(bout.result.equals(bout.status.equals("cancelled") ? "not_applicable" : "unknown"), "Contradictory result");
                    require(bout.method == null && bout.round == null, "Unresolved method/round");
                }
                if (bout.method != null) text(bout.method);
                require(bout.round == null || bout.round > 0, "Invalid round");
                provenance(bout.provenance, sourceIds);
                values.put(bout.id, fields("method", bout.method, "result", bout.result));
                if (bout.tournamentId != null) {
                    validId(bout.tournamentId);
                    require(oneOf(bout.tournamentStage, "quarterfinal", "semifinal", "final", "reserve", "unknown"), "Invalid tournament stage");
                    String prior = tournaments.put(bout.tournamentId, event.id);
                    require(prior == null || prior.equals(event.id), "Tournament spans events");
                } else require(bout.tournamentStage == null, "Tournament ID required");
            }
        }
        Map<String, Correction> latest = new LinkedHashMap<>();
        for (Correction correction : corrections) {
            addId(ids, correction.id); text(correction.reason); text(correction.previousValue); text(correction.newValue);
            require(correction.fromRevision > 0 && correction.fromRevision < correction.toRevision && correction.toRevision <= revision, "Invalid correction revision");
            Map<String, String> target = values.get(correction.targetId);
            require(target != null && target.containsKey(correction.field), "Invalid correction target/field");
            provenance(correction.provenance, sourceIds);
            String key = correction.targetId + "/" + correction.field;
            Correction previous = latest.put(key, correction);
            require(previous == null || (previous.toRevision <= correction.fromRevision && previous.newValue.equals(correction.previousValue)), "Broken correction chain");
        }
        for (Correction correction : latest.values()) require(correction.newValue.equals(values.get(correction.targetId).get(correction.field)), "Correction/current value mismatch");
        for (String tournament : tournaments.keySet()) require(!ids.contains(tournament), "Tournament identity collision");
    }

    private static Map<String, String> fields(String... pairs) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i], pairs[i + 1]);
        return result;
    }
    static <T> List<T> immutable(List<T> values) { return Collections.unmodifiableList(new ArrayList<>(values)); }
    static void require(boolean value, String message) { if (!value) throw new IllegalArgumentException(message); }
    private static void text(String value) { require(value != null && !value.trim().isEmpty() && value.equals(value.trim()), "Invalid text"); }
    private static void strings(List<String> values) { Set<String> seen = new HashSet<>(); for (String value : values) { text(value); require(seen.add(value), "Duplicate string"); } }
    private static void validId(String id) { require(id != null && id.matches("demo-[a-z0-9]+(?:-[a-z0-9]+)*"), "Stable demo ID required"); }
    private static void addId(Set<String> values, String id) { validId(id); require(values.add(id), "Duplicate ID"); }
    private static boolean oneOf(String value, String... choices) { for (String choice : choices) if (choice.equals(value)) return true; return false; }
    private static void provenance(List<String> values, Set<String> sources) { strings(values); require(!values.isEmpty() && sources.containsAll(values), "Invalid provenance"); }

    public static final class Source {
        public final String id, label;
        public Source(String id, String label) { this.id = id; this.label = label; }
    }
    public static final class Fighter {
        public final String id, name;
        public final List<String> aliases, provenance;
        public Fighter(String id, String name, List<String> aliases, List<String> provenance) {
            this.id = id; this.name = name; this.aliases = immutable(aliases); this.provenance = immutable(provenance);
        }
    }
    public static final class Event {
        public final String id, title, date, venue, location, coverage;
        public final List<String> aliases, provenance;
        public final List<Bout> bouts;
        public Event(String id, String title, String date, String venue, String location, String coverage,
                     List<String> aliases, List<String> provenance, List<Bout> bouts) {
            this.id = id; this.title = title; this.date = date; this.venue = venue; this.location = location; this.coverage = coverage;
            this.aliases = immutable(aliases); this.provenance = immutable(provenance);
            List<Bout> ordered = new ArrayList<>(bouts); ordered.sort(Comparator.comparingInt(b -> b.order)); this.bouts = immutable(ordered);
        }
    }
    public static final class Bout {
        public final String id, redId, blueId, status, result, method, tournamentId, tournamentStage;
        public final int order;
        public final Integer round;
        public final List<String> provenance;
        public Bout(String id, int order, String redId, String blueId, String status, String result, String method,
                    Integer round, String tournamentId, String tournamentStage, List<String> provenance) {
            this.id = id; this.order = order; this.redId = redId; this.blueId = blueId; this.status = status; this.result = result;
            this.method = method; this.round = round; this.tournamentId = tournamentId; this.tournamentStage = tournamentStage; this.provenance = immutable(provenance);
        }
        public String outcomeFor(String fighterId) {
            if (!redId.equals(fighterId) && !blueId.equals(fighterId)) throw new IllegalArgumentException("Not a participant");
            if (status.equals("cancelled")) return "Cancelled";
            if (result.equals("draw")) return "Draw";
            if (result.equals("no_contest")) return "No contest";
            if (result.equals("unknown")) return "Result unknown";
            return (result.equals("red_win") == redId.equals(fighterId)) ? "Win" : "Loss";
        }
    }
    public static final class Appearance {
        public final Event event;
        public final Bout bout;
        Appearance(Event event, Bout bout) { this.event = event; this.bout = bout; }
    }
    public static final class Correction {
        public final String id, targetId, field, previousValue, newValue, reason;
        public final int fromRevision, toRevision;
        public final List<String> provenance;
        public Correction(String id, int fromRevision, int toRevision, String targetId, String field, String previousValue, String newValue, String reason, List<String> provenance) {
            this.id = id; this.fromRevision = fromRevision; this.toRevision = toRevision; this.targetId = targetId; this.field = field;
            this.previousValue = previousValue; this.newValue = newValue; this.reason = reason; this.provenance = immutable(provenance);
        }
    }
}
