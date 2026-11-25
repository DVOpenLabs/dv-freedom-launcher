package com.dvopenlabs.freedom.panchang;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.content.Context;
import android.content.res.Configuration;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.time.LocalDate;

/** A standalone module card; it never writes the launcher's configuration. */
public final class PanchangActivity extends Activity {
    static final String PREFS = "panchang";
    static final String KEY_CITY = "city", KEY_REGION = "region";
    private static final int REQUEST_MONTH = 1;
    private PanchangPanel panel;
    static Context localized(Context base) {
        String language = base.getSharedPreferences(PREFS, MODE_PRIVATE).getString("language", "en");
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(Language.supported(language)));
        return base.createConfigurationContext(config);
    }
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(localized(base)); }
    static City savedCity(SharedPreferences prefs) {
        if (prefs.getBoolean("manual", false)) {
            try { return City.manual(prefs.getString("manual_name", ""), Double.parseDouble(prefs.getString("manual_lat", "")),
                    Double.parseDouble(prefs.getString("manual_lon", "")), prefs.getString("manual_zone", "")); }
            catch (RuntimeException ignored) { /* Recover a damaged local preference. */ }
        }
        return City.cityNamed(prefs.getString(KEY_CITY, City.forTimeZone().name));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        panel = new PanchangPanel(this, false);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        panel.setCity(savedCity(prefs));
        panel.setRegion(Region.named(prefs.getString(KEY_REGION, Region.forLocale().name())));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, panel.dp(8), 0, 0);
        LinearLayout navigation = new LinearLayout(this); navigation.setGravity(Gravity.CENTER);
        LinearLayout options = new LinearLayout(this); options.setGravity(Gravity.CENTER);
        actions.addView(navigation); actions.addView(options);
        button(navigation, R.string.previous).setOnClickListener(v -> panel.showDay(-1));
        Button today = button(navigation, R.string.today);
        today.setOnClickListener(v -> panel.showDay(0));
        button(navigation, R.string.next).setOnClickListener(v -> panel.showDay(1));
        button(navigation, R.string.month).setOnClickListener(v -> openMonth());
        button(options, R.string.city).setOnClickListener(v -> chooseCity());
        button(options, R.string.region).setOnClickListener(v -> chooseRegion());
        button(options, R.string.manual).setOnClickListener(v -> manualCity());
        button(options, R.string.language).setOnClickListener(v -> chooseLanguage());
        button(options, R.string.ambient).setOnClickListener(v -> startActivity(new Intent(this, AmbientActivity.class)));
        panel.addActions(actions);
        setContentView(panel);
        today.requestFocus();
    }

    private void openMonth() {
        startActivityForResult(new Intent(this, MonthActivity.class)
                .putExtra(MonthActivity.EXTRA_CITY, panel.city().name)
                .putExtra(MonthActivity.EXTRA_REGION, panel.region().name())
                .putExtra(MonthActivity.EXTRA_DATE, panel.date().toString()), REQUEST_MONTH);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQUEST_MONTH && result == RESULT_OK && data != null) {
            panel.show(LocalDate.parse(data.getStringExtra(MonthActivity.EXTRA_DATE)));
        }
    }

    private void chooseCity() {
        int current = City.indexOf(panel.city().name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.city)
                .setSingleChoiceItems(City.names(), current, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("manual", false).apply();
                    panel.setCity(City.ALL[which]);
                    save();
                    dialog.dismiss();
                })
                .show();
    }

    private void chooseLanguage() {
        String[] choices = {"English", "हिन्दी"};
        new AlertDialog.Builder(this).setTitle(R.string.language).setSingleChoiceItems(choices,
                getSharedPreferences(PREFS, MODE_PRIVATE).getString("language", "en").equals("hi") ? 1 : 0,
                (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("language", which == 1 ? "hi" : "en").apply();
                    dialog.dismiss(); recreate();
                }).show();
    }

    private void manualCity() {
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL);
        int padding = panel.dp(20); fields.setPadding(padding, 0, padding, 0);
        City current = panel.city();
        EditText name = field(fields, R.string.manual_name, current.name);
        EditText lat = field(fields, R.string.latitude, Double.toString(current.lat));
        EditText lon = field(fields, R.string.longitude, Double.toString(current.lon));
        EditText zone = field(fields, R.string.time_zone, current.zone.getId());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.manual).setView(fields)
                .setPositiveButton(android.R.string.ok, null).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                City city = City.manual(name.getText().toString(), Double.parseDouble(lat.getText().toString().trim()),
                        Double.parseDouble(lon.getText().toString().trim()), zone.getText().toString());
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("manual", true)
                        .putString("manual_name", city.name).putString("manual_lat", Double.toString(city.lat))
                        .putString("manual_lon", Double.toString(city.lon)).putString("manual_zone", city.zone.getId()).apply();
                panel.setCity(city); dialog.dismiss();
            } catch (RuntimeException invalid) { zone.setError(getString(R.string.invalid_location)); }
        }));
        dialog.show();
    }
    private EditText field(LinearLayout parent, int hint, String value) {
        TextView label = new TextView(this); label.setText(hint); parent.addView(label);
        EditText field = new EditText(this); field.setContentDescription(getString(hint)); field.setText(value); field.setSingleLine(true);
        parent.addView(field); return field;
    }

    private void chooseRegion() {
        Region[] regions = Region.values();
        String[] names = new String[regions.length];
        for (int i = 0; i < regions.length; i++) names[i] = regions[i].label + " · " + regions[i].era;
        new AlertDialog.Builder(this)
                .setTitle(R.string.region)
                .setSingleChoiceItems(names, panel.region().ordinal(), (dialog, which) -> {
                    panel.setRegion(regions[which]);
                    save();
                    dialog.dismiss();
                })
                .show();
    }

    private void save() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_CITY, panel.city().name)
                .putString(KEY_REGION, panel.region().name())
                .apply();
    }

    private Button button(LinearLayout parent, int label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        // The stock button barely changes when focused; on a TV the focus has to be plain to see.
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, shape(PanchangPanel.ACCENT));
        background.addState(new int[0], shape(0x33ffffff));
        button.setBackground(background);
        button.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{PanchangPanel.BACKGROUND, PanchangPanel.TEXT}));
        button.setPadding(panel.dp(16), panel.dp(8), panel.dp(16), panel.dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(panel.dp(4), 0, panel.dp(4), 0);
        button.setLayoutParams(params);
        parent.addView(button);
        return button;
    }

    private GradientDrawable shape(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(panel.dp(6));
        return shape;
    }

    /** Channel and media keys page the day from anywhere on the screen. */
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        switch (code) {
            case KeyEvent.KEYCODE_CHANNEL_UP: case KeyEvent.KEYCODE_PAGE_UP: case KeyEvent.KEYCODE_MEDIA_REWIND:
                panel.showDay(-1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN: case KeyEvent.KEYCODE_PAGE_DOWN: case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                panel.showDay(1);
                return true;
            default:
                return super.onKeyDown(code, event);
        }
    }

    @Override protected void onResume() { super.onResume(); panel.start(); }
    @Override protected void onPause() { panel.stop(); super.onPause(); }
}
