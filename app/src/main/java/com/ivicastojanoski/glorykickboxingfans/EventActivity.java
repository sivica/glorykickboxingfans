package com.ivicastojanoski.glorykickboxingfans;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import com.ivicastojanoski.glorykickboxingfans.archive.Archive;
import java.util.Locale;

public final class EventActivity extends ArchiveScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (archive == null) return;
        Archive.Event event = archive.event(getIntent().getStringExtra(ITEM_ID));
        if (event == null) { notFound(); return; }
        header(event.title, metadata(event), true);
        notice(content, getResources().getQuantityString(R.plurals.archive_bout_count, event.bouts.size(), event.bouts.size()),
                getString(event.coverage.equals("partial-demo-card") ? R.string.archive_partial_card : R.string.archive_complete_card)
                        + ". " + getString(R.string.archive_synthetic_result), false);
        section(content, getString(R.string.archive_results_heading));
        for (Archive.Bout bout : event.bouts) {
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.archive_card); card.setPadding(dp(16), dp(16), dp(16), dp(16)); card.setTag(bout.id);
            add(card, text(getString(R.string.archive_bout_label, bout.order, bout.tournamentStage == null
                    ? getString(R.string.archive_bout_demo) : bout.tournamentStage.toUpperCase(Locale.ROOT)), R.style.ArchiveText_Label), 0);
            String first = bout.result.equals("blue_win") ? bout.blueId : bout.redId;
            String second = first.equals(bout.redId) ? bout.blueId : bout.redId;
            for (String id : new String[] {first, second}) {
                Archive.Fighter fighter = archive.fighter(id); String outcome = bout.outcomeFor(id);
                Button link = action(getString(R.string.archive_fighter_outcome, fighter.name, outcome),
                        () -> startActivity(new Intent(this, FighterActivity.class).putExtra(ITEM_ID, id)), outcome.equals("Win"));
                link.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                link.setTag(bout.id + "/" + id); link.setContentDescription(getString(R.string.archive_fighter_link, fighter.name, outcome));
                add(card, link, 8);
            }
            add(card, text(boutDetail(bout), R.style.ArchiveText_Meta), 8);
            add(card, text(getString(R.string.archive_synthetic_result), R.style.ArchiveText_Meta), 8);
            add(content, card, 12);
        }
        provenanceAction(content, event.provenance);
    }
}
