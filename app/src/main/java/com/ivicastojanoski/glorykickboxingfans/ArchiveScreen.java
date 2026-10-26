package com.ivicastojanoski.glorykickboxingfans;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ivicastojanoski.glorykickboxingfans.archive.Archive;
import com.ivicastojanoski.glorykickboxingfans.archive.ArchiveRepository;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/** Shared XML shell and small native Views patterns; no UI toolkit or network layer added. */
public abstract class ArchiveScreen extends AppCompatActivity {
    static final String ITEM_ID = "archive_item_id";
    protected Archive archive;
    protected LinearLayout content;
    protected boolean cachedArchive;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive_screen);
        EdgeToEdge.apply(getWindow(), findViewById(R.id.main_content));
        content = (LinearLayout) findViewById(R.id.screen_content);
        try {
            ArchiveRepository.Snapshot snapshot = ArchiveRepository.get(this).load();
            archive = snapshot.archive; cachedArchive = snapshot.cached;
        }
        catch (IOException exception) {
            header(getString(R.string.archive_error_title), getString(R.string.archive_subtitle), !(this instanceof MainActivity));
            notice(content, getString(R.string.archive_error_title), getString(R.string.archive_error_body), false);
        }
    }

    protected void header(String title, String subtitle, boolean back) {
        TextView heading = (TextView) findViewById(R.id.screen_title);
        heading.setText(title);
        if (!back) heading.setTextAppearance(R.style.ArchiveText_Display);
        ((TextView) findViewById(R.id.screen_subtitle)).setText(subtitle);
        View action = findViewById(R.id.back_action);
        action.setVisibility(back ? View.VISIBLE : View.GONE);
        action.setOnClickListener(v -> finish());
        setTitle(title);
    }

    protected TextView text(String value, int style) {
        TextView view = new TextView(this);
        view.setTextAppearance(style); view.setText(value); view.setIncludeFontPadding(false);
        return view;
    }
    protected void add(LinearLayout parent, View view, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topDp); parent.addView(view, params);
    }
    protected int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    protected TextView section(LinearLayout parent, String title) {
        TextView heading = text(title, R.style.ArchiveText_Section); add(parent, heading, 12); return heading;
    }
    protected View notice(LinearLayout parent, String title, String body, boolean warning) {
        View view = getLayoutInflater().inflate(R.layout.archive_notice, parent, false);
        ((TextView) view.findViewById(R.id.notice_title)).setText(title);
        ((TextView) view.findViewById(R.id.notice_body)).setText(body);
        if (warning) view.setBackgroundResource(R.drawable.archive_warning);
        add(parent, view, parent.getChildCount() == 0 ? 0 : 12); return view;
    }
    protected Button action(String title, Runnable onClick, boolean selected) {
        Button button = new android.support.v7.widget.AppCompatButton(this);
        button.setTextAppearance(R.style.ArchiveText_Strong); button.setAllCaps(false); button.setSingleLine(false);
        button.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        button.setStateListAnimator(null); button.setElevation(0);
        button.setText(title); button.setMinHeight(dp(48)); button.setMinimumHeight(dp(48));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackgroundResource(selected ? R.drawable.archive_selected : R.drawable.archive_action);
        button.setTextColor(getResources().getColor(selected ? R.color.archive_on_dark : R.color.archive_text, getTheme()));
        button.setOnClickListener(v -> onClick.run()); return button;
    }
    protected void eventLink(LinearLayout parent, Archive.Event event, String eyebrow, String title, String metadata, String detail, String tag) {
        View card = getLayoutInflater().inflate(R.layout.archive_event_card, parent, false);
        card.setClipToOutline(true); // API21 setter works on min24; XML attribute is newer.
        ((TextView) card.findViewById(R.id.card_eyebrow)).setText(eyebrow);
        ((TextView) card.findViewById(R.id.card_title)).setText(title);
        ((TextView) card.findViewById(R.id.card_meta)).setText(metadata);
        ((TextView) card.findViewById(R.id.card_detail)).setText(detail);
        card.setTag(tag);
        card.setContentDescription(eyebrow + ". " + title + ". " + metadata + ". " + detail + ". " + getString(R.string.archive_view_results));
        card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info); info.setClassName(Button.class.getName());
            }
        });
        card.setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class).putExtra(ITEM_ID, event.id)));
        add(parent, card, 12);
    }
    protected String date(String iso) {
        try { return new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(iso)); }
        catch (ParseException exception) { return iso; } // Validated domain dates remain visible if formatting fails.
    }
    protected String metadata(Archive.Event event) { return getString(R.string.archive_meta, date(event.date), event.location); }
    protected String boutDetail(Archive.Bout bout) {
        if (bout.status.equals("cancelled")) return getString(R.string.archive_cancelled);
        String method = bout.method == null ? getString(R.string.archive_method_missing) : bout.method;
        String round = bout.round == null ? getString(R.string.archive_round_missing) : getString(R.string.archive_round, bout.round);
        String details = getString(R.string.archive_meta, method, round);
        return bout.result.equals("unknown") ? getString(R.string.archive_missing_result) + "\n" + details : details;
    }
    protected void provenanceAction(LinearLayout parent, List<String> sources) {
        Button button = action(getString(R.string.archive_provenance_action), () -> new AlertDialog.Builder(this)
                .setTitle(R.string.archive_provenance_title)
                .setMessage(archive.notice + "\n\n" + archive.coverage + "\n\n" + archive.sourceLabels(sources) + "\n\n" + getString(cachedArchive ? R.string.archive_saved_revision : R.string.archive_revision, archive.revision))
                .setPositiveButton(R.string.archive_close, null).show(), false);
        button.setId(R.id.provenance_action); add(parent, button, 12);
    }
    protected void notFound() {
        header(getString(R.string.archive_error_title), getString(R.string.archive_subtitle), true);
        notice(content, getString(R.string.archive_error_title), getString(R.string.archive_not_found), false);
    }
}
