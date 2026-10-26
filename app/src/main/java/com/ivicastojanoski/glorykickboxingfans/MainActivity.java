package com.ivicastojanoski.glorykickboxingfans;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ivicastojanoski.glorykickboxingfans.archive.Archive;
import java.util.List;

/** Offline archive/search. Query, year and native ScrollView position survive recreation. */
public final class MainActivity extends ArchiveScreen {
    private String query = "";
    private Integer year;
    private SearchView search;
    private Button yearButton;
    private LinearLayout results;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (archive == null) return;
        if (state != null) { query = state.getString("query", ""); year = state.containsKey("year") ? state.getInt("year") : null; }
        header(getString(R.string.archive_brand), getString(R.string.archive_subtitle), false);
        content.setFocusableInTouchMode(true);
        search = new SearchView(this); search.setId(R.id.archive_search); search.setIconifiedByDefault(false);
        search.setQueryHint(getString(R.string.archive_search)); search.setBackgroundResource(R.drawable.archive_field);
        search.setPadding(dp(4), dp(4), dp(4), dp(4)); search.setMinimumHeight(dp(56)); search.setSaveEnabled(false);
        search.findViewById(android.support.v7.appcompat.R.id.search_plate).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        android.widget.EditText input = (android.widget.EditText) search.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        // AppCompat23's abc_search_view.xml fixes this child at 36dp, clipping at 200%.
        android.widget.LinearLayout.LayoutParams inputParams = (android.widget.LinearLayout.LayoutParams) input.getLayoutParams();
        inputParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        inputParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        input.setLayoutParams(inputParams);
        input.setTextColor(getResources().getColor(R.color.archive_secondary, getTheme()));
        input.setHintTextColor(getResources().getColor(R.color.archive_secondary, getTheme()));
        View clear = search.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        clear.setContentDescription(getString(R.string.archive_clear_query));
        clear.setMinimumWidth(dp(48)); clear.setMinimumHeight(dp(48));
        for (int iconId : new int[] {android.support.v7.appcompat.R.id.search_mag_icon, android.support.v7.appcompat.R.id.search_close_btn}) {
            ((android.widget.ImageView) search.findViewById(iconId)).setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.archive_text, getTheme())));
        }
        search.setQuery(query, false); search.clearFocus(); add(content, search, 0);
        // Legacy SearchView measures to a fixed preferred height even when fonts grow.
        // Derive its floor from the actual scaled input metrics instead of capping text.
        android.graphics.Paint.FontMetricsInt font = input.getPaint().getFontMetricsInt();
        search.getLayoutParams().height = Math.max(dp(56), font.bottom - font.top
                + input.getCompoundPaddingTop() + input.getCompoundPaddingBottom()
                + search.getPaddingTop() + search.getPaddingBottom() + dp(16));
        yearButton = action(getString(R.string.archive_all_years), this::chooseYear, false); yearButton.setId(R.id.year_filter);
        yearButton.setTextAppearance(R.style.ArchiveText_Label);
        LinearLayout filters = new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(android.view.Gravity.CENTER_VERTICAL);
        filters.addView(yearButton, new LinearLayout.LayoutParams(0, -2, 0.44f));
        TextView sort = text(getString(R.string.archive_newest), R.style.ArchiveText_Meta);
        LinearLayout.LayoutParams sortParams = new LinearLayout.LayoutParams(0, -2, 0.56f); sortParams.setMarginStart(dp(12));
        filters.addView(sort, sortParams); add(content, filters, 12);
        notice(content, getString(R.string.archive_scope_title), getString(R.string.archive_scope_body), false);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); results.setId(R.id.archive_results);
        add(content, results, 0);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String value) { hideKeyboard(); return true; }
            @Override public boolean onQueryTextChange(String value) { query = value; renderResults(); return true; }
        });
        renderResults();
    }
    private void chooseYear() {
        hideKeyboard();
        List<Integer> years = archive.years(); String[] labels = new String[years.size()+1]; labels[0] = getString(R.string.archive_all_years);
        for (int i = 0; i < years.size(); i++) labels[i+1] = String.valueOf(years.get(i));
        int checked = year == null ? 0 : years.indexOf(year)+1;
        new AlertDialog.Builder(this).setTitle(R.string.archive_year_title).setSingleChoiceItems(labels, checked, (dialog, which) -> {
            year = which == 0 ? null : years.get(which-1); renderResults(); dialog.dismiss();
        }).setNegativeButton(R.string.archive_close, null).show();
    }
    private void renderResults() {
        results.removeAllViews();
        yearButton.setText(year == null ? getString(R.string.archive_all_years) : getString(R.string.archive_selected_year, year));
        yearButton.setBackgroundResource(year == null ? R.drawable.archive_field : R.drawable.archive_selected);
        yearButton.setTextColor(getResources().getColor(year == null ? R.color.archive_text : R.color.archive_on_dark, getTheme()));
        yearButton.setSelected(year != null);
        List<Archive.Event> events = archive.search(query, year);
        if (events.isEmpty()) {
            TextView empty = section(results, getString(R.string.archive_no_results)); empty.setId(R.id.empty_heading);
            add(results, text(getString(R.string.archive_no_results_body), R.style.ArchiveText), 12);
            Button reset = action(getString(R.string.archive_reset), this::clear, true); reset.setId(R.id.reset_filters); add(results, reset, 12);
        } else {
            TextView count = section(results, query.trim().isEmpty() && year == null ? getString(R.string.archive_heading)
                    : getResources().getQuantityString(R.plurals.archive_event_count, events.size(), events.size()));
            count.setId(R.id.result_count); count.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            for (Archive.Event event : events) eventLink(results, event, getString(R.string.archive_card_eyebrow, event.date.substring(0,4)), event.title,
                    metadata(event), getResources().getQuantityString(R.plurals.archive_bout_count, event.bouts.size(), event.bouts.size()), event.id);
            if (!query.trim().isEmpty() || year != null) {
                Button reset = action(getString(R.string.archive_reset), this::clear, false); reset.setId(R.id.reset_filters); add(results, reset, 12);
            }
        }
        provenanceAction(results, archive.sources.stream().map(s -> s.id).collect(java.util.stream.Collectors.toList()));
    }
    private void clear() { year = null; query = ""; search.setQuery("", false); hideKeyboard(); renderResults(); }
    private void hideKeyboard() {
        search.clearFocus(); ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("query", query); if (year != null) out.putInt("year", year); super.onSaveInstanceState(out);
    }
}
