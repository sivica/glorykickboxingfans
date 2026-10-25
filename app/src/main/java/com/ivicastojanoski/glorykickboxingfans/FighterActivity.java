package com.ivicastojanoski.glorykickboxingfans;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Button;
import com.ivicastojanoski.glorykickboxingfans.archive.Archive;
import java.util.List;

public final class FighterActivity extends ArchiveScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (archive == null) return;
        Archive.Fighter fighter = archive.fighter(getIntent().getStringExtra(ITEM_ID));
        if (fighter == null) { notFound(); return; }
        header(fighter.name, getString(R.string.archive_appearances_subtitle), true);
        List<Archive.Appearance> appearances = archive.appearances(fighter.id);
        notice(content, getResources().getQuantityString(R.plurals.archive_appearance_count, appearances.size(), appearances.size()), getString(R.string.archive_career_scope), false);
        if (!fighter.aliases.isEmpty()) add(content, text(getString(R.string.archive_aliases, android.text.TextUtils.join(", ", fighter.aliases)), R.style.ArchiveText_Meta), 12);
        for (Archive.Correction correction : archive.corrections) if (correction.targetId.equals(fighter.id)) {
            Button button = action(getString(R.string.archive_correction_action), () -> new AlertDialog.Builder(this)
                    .setTitle(R.string.archive_correction_title)
                    .setMessage(getString(R.string.archive_correction_format, correction.fromRevision, correction.toRevision,
                            correction.previousValue, correction.newValue, correction.reason) + "\n\n" + archive.sourceLabels(correction.provenance))
                    .setPositiveButton(R.string.archive_close, null).show(), false);
            button.setId(R.id.correction_action); add(content, button, 12);
        }
        section(content, getString(R.string.archive_appearances_heading));
        for (Archive.Appearance appearance : appearances) {
            Archive.Bout bout = appearance.bout;
            String opponent = archive.fighter(fighter.id.equals(bout.redId) ? bout.blueId : bout.redId).name;
            eventLink(content, appearance.event, getString(R.string.archive_appearance_eyebrow), getString(R.string.archive_opponent, opponent),
                    getString(R.string.archive_meta, appearance.event.title, date(appearance.event.date)),
                    bout.outcomeFor(fighter.id) + " · " + boutDetail(bout), "appearance/" + bout.id);
        }
        provenanceAction(content, fighter.provenance);
    }
}
