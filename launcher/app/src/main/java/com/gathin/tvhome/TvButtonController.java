package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.widget.Toast;

/** Reuses existing helpers. No config reload, package query, listener, receiver or timer per key down. */
final class TvButtonController {
    // All three receivers run in the same application process. Never retain an Activity or service here.
    private static final TvButtonPress PRESSES = new TvButtonPress();
    private final Context context;
    private final SharedPreferences prefs;
    private final String livePackage, liveAction;
    TvButtonController(Context context, Config config) {
        this.context = context;
        prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        livePackage = config.liveTvPackage; liveAction = config.liveTvAction;
    }
    boolean handle(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_TV) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN && event.getAction() != KeyEvent.ACTION_UP) return false;
        String button = event.getDeviceId() + ":" + event.getScanCode();
        TvButtonPress.Result result = PRESSES.update(button, event.getAction() == KeyEvent.ACTION_DOWN,
                event.getRepeatCount(), event.isCanceled(), event.getEventTime(), prefs.getString(TvButtonChoice.KEY, TvButtonChoice.FOLLOW));
        if (result.launch != null) launch(result.launch);
        return result.consume;
    }
    private void launch(String choice) {
        try {
            Intent destination;
            if (TvButtonChoice.INPUTS.equals(choice)) {
                destination = new Intent(context, MainActivity.class).setAction(MainActivity.ACTION_VIEW_INPUTS);
            } else {
                String pkg = TvButtonChoice.LIVE.equals(choice) ? livePackage : TvButtonChoice.appPackage(choice);
                destination = pkg == null || pkg.isEmpty() ? null : context.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (destination == null && pkg != null && !pkg.isEmpty()) destination = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (destination == null && TvButtonChoice.LIVE.equals(choice) && !liveAction.isEmpty() && !livePackage.isEmpty()) {
                    Intent fallback = new Intent(liveAction).setPackage(livePackage);
                    if (fallback.resolveActivity(context.getPackageManager()) != null) destination = fallback;
                }
            }
            if (destination == null) throw new ActivityNotFoundException();
            context.startActivity(destination.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(context, R.string.tv_button_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
