package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import java.io.InputStream;
import java.util.List;

/** Native D-pad chooser and offline Storage Access Framework import; no storage permission. */
final class ProfilePicker {
    static final int IMPORT = 4103;
    static void show(Activity activity) {
        show(activity,true);
    }
    static void show(Activity activity,boolean allowImport) {
        List<LocalProfiles.Profile> rows = ProfileStore.read(activity);
        String[] names = new String[rows.size() + (allowImport?2:1)];
        names[0] = activity.getString(R.string.profile_current);
        int checked = 0;
        for (int i = 0; i < rows.size(); i++) {
            names[i + 1] = rows.get(i).name;
            if (rows.get(i).id.equals(ProfileStore.id())) checked = i + 1;
        }
        if(allowImport)names[names.length - 1] = activity.getString(R.string.profile_import);
        final int selected = checked;
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.local_profiles)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    if (allowImport && which == names.length - 1) {
                        if (!ProfileStore.id().isEmpty()) { message(activity, R.string.profile_import_current); return; }
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json")
                                .addCategory(Intent.CATEGORY_OPENABLE);
                        try { activity.startActivityForResult(intent, IMPORT); }
                        catch (android.content.ActivityNotFoundException e) { message(activity, R.string.not_available); }
                    } else if (ProfileStore.select(activity, which == 0 ? "" : rows.get(which - 1).id)) {
                        if(activity instanceof MainActivity)activity.recreate();else activity.finish();
                    }
                    else message(activity, R.string.profile_save_failed);
                }).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> { dialog.getListView().requestFocus(); dialog.getListView().setSelection(selected); });
        dialog.show();
    }
    static void imported(Activity activity, int result, Intent data) {
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = activity.getContentResolver().openInputStream(data.getData())) {
            byte[] bytes = ProfileStore.readBytes(in);
            int count = ProfileStore.parse(bytes).size();
            AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.profile_import)
                    .setMessage(activity.getString(R.string.profile_import_confirm, count))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.profile_import_action, (d, w) -> {
                        try { ProfileStore.install(activity, bytes); message(activity, R.string.profile_import_done); }
                        catch (Exception e) { message(activity, R.string.profile_import_failed); }
                    }).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());
            dialog.show();
        } catch (Exception e) {
            Log.w("TvHome.Profiles", "Could not import profile library: " + e.getClass().getSimpleName());
            message(activity, R.string.profile_import_failed);
        }
    }
    private static void message(Activity activity, int text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private ProfilePicker() { }
}
