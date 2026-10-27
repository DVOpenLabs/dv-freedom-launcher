package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private SettingsNavigation navigation;
    private final TextView[] columnChoices = new TextView[5];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        Config config = Config.load(this);
        choices = ProfileStore.preferences(this);
        CompanionControls.attachSettings(this);
        AppearanceSettings.attach(this, config);
        TvButtonSettings.attach(this, config);
        TextView inputs = findViewById(R.id.inputVisibilityChoice);
        if (config.inputVisibilityChoice) {
            inputs.setVisibility(View.VISIBLE);
            inputs.setText(getString(R.string.input_visibility_state, getString(config.hideInactiveHdmi
                    ? R.string.input_visibility_hide : R.string.input_visibility_all)));
            inputs.setOnClickListener(v -> {
                String[] labels = {getString(R.string.tv_button_follow), getString(R.string.input_visibility_all), getString(R.string.input_visibility_hide)};
                int selected = choices.contains(HdmiVisibility.CHOICE) ? (choices.getBoolean(HdmiVisibility.CHOICE, false) ? 2 : 1) : 0;
                AppearanceSettings.choose(this, R.string.input_visibility, labels, selected, which -> {
                    SharedPreferences.Editor edit = choices.edit();
                    if (which == 0) edit.remove(HdmiVisibility.CHOICE); else edit.putBoolean(HdmiVisibility.CHOICE, which == 2);
                    edit.putLong(Config.SETTINGS_REVISION_KEY, choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
                    setResult(RESULT_OK); finish();
                });
            });
        }
        View themes = findViewById(R.id.themesChoice);
        if (ThemeLibrary.enabled()) {
            themes.setVisibility(View.VISIBLE);
            themes.setOnClickListener(v -> ThemePicker.show(this));
        }
        TextView live = findViewById(R.id.liveWallpaperChoice);
        if (ThemeLibrary.liveAllowed()) {
            live.setVisibility(View.VISIBLE);
            ThemeLibrary.Entry theme = ThemeLibrary.active();
            boolean available = theme != null && new File(theme.directory, "video.mp4").isFile()
                    && config.wallpaperEnabled && LiveVideo.supported(this, config.reducedMotion);
            live.setText(available ? getString(R.string.live_wallpaper_state, getString(choices.getBoolean(ThemeLibrary.LIVE, false)
                    ? R.string.live_on : R.string.live_off)) : getString(R.string.live_wallpaper_unavailable));
            live.setOnClickListener(v -> {
                if (!available) { Toast.makeText(this, R.string.live_wallpaper_help, Toast.LENGTH_LONG).show(); return; }
                choices.edit().putBoolean(ThemeLibrary.LIVE, !choices.getBoolean(ThemeLibrary.LIVE, false)).apply();
                changed(); finish();
            });
        }
        View profiles = findViewById(R.id.profilesChoice);
        if (ProfileStore.enabled()) {
            profiles.setVisibility(View.VISIBLE);
            profiles.setOnClickListener(v -> ProfilePicker.show(this));
        }
        View styles = findViewById(R.id.stylesChoice);
        View undoStyle = findViewById(R.id.styleUndoChoice);
        if (config.homeStylePicker) {
            styles.setVisibility(View.VISIBLE);
            styles.setOnClickListener(v -> StyleSession.choose(this));
            undoStyle.setVisibility(View.VISIBLE);
            undoStyle.setEnabled(StyleSession.hasUndo(this));
            undoStyle.setAlpha(undoStyle.isEnabled() ? 1f : .5f);
            undoStyle.setOnClickListener(v -> {
                if (StyleSession.undo(this)) finish();
                else Toast.makeText(this, R.string.style_save_failed, Toast.LENGTH_LONG).show();
            });
        }
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7, R.id.columns8};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        Runnable restoreChoices=()->{
            if (!hasOnTvChoices()) return;
            SharedPreferences.Editor reset = choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove(TileSpacing.FOCUS).remove(TileSpacing.CHOICE)
                    .remove(HomeStyles.CHOICE).remove(HomeStyles.UNDO)
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY);
            if (ProfileStore.id().isEmpty()) reset.remove(OpenOnStart.KEY);
            reset.commit();
            Config current = Config.load(this);
            AppearanceSettings.attach(this, current);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        };
        followThemeChoice.setOnClickListener(v->{
            if(!hasOnTvChoices())return;
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.settings_reset_title)
                    .setMessage(R.string.settings_reset_help).setNegativeButton(android.R.string.cancel,null)
                    .setPositiveButton(R.string.follow_theme,(d,w)->restoreChoices.run()).create();
            dialog.show();dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        navigation=new SettingsNavigation(this,state);
    }

    @Override protected void onResume() {
        super.onResume();
        TvButtonSettings.refreshStatus(this);
    }


    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        return navigation!=null&&navigation.key(event)||super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if(navigation==null||!navigation.back())super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if(navigation!=null)navigation.save(state);super.onSaveInstanceState(state);
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = ThemeLibrary.active() == null ? Config.dir(this) : ThemeLibrary.active().directory;
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        ((TextView)findViewById(R.id.settingsColumnsCaption)).setText(getString(R.string.settings_columns_value,columns));
        ((TextView)findViewById(R.id.settingsRowsCaption)).setText(getString(R.string.settings_rows_value,rowsBelowDock));
        ((TextView)findViewById(R.id.settingsOpacityCaption)).setText(getString(R.string.settings_opacity_value,dockOpacity));
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains(TileSpacing.FOCUS) || choices.contains(TileSpacing.CHOICE)
                || choices.contains(HomeStyles.CHOICE) || choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || (ProfileStore.id().isEmpty() && choices.contains(OpenOnStart.KEY));
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == ProfilePicker.IMPORT && ProfileStore.enabled()) ProfilePicker.imported(this, result, data);
        if (request == ThemePicker.IMPORT && ThemeLibrary.enabled()) ThemePicker.imported(this, result, data);
    }
}
