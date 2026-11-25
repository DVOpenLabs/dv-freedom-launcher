package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/** Optional, foreground-only vector clock. It owns neither the date nor the scene's drift. */
final class FlowingClock {
    private static final String KEY = "freedom_clock_style";
    private FlowingClock() { }

    static AutoCloseable attach(Context context, View root) {
        return attach(context,root,ProfileStore.preferences(context).getString(KEY,"current"));
    }
    private static AutoCloseable attach(Context context,View root,String style){
        if (!"flow".equals(style)) return null;
        View found = root.findViewById(R.id.dreamContent);
        if (!(found instanceof LinearLayout)) return null;
        LinearLayout host = (LinearLayout) found;
        if (host.getChildCount() == 0) return null;
        View original = host.getChildAt(0);
        int visibility = original.getVisibility();
        ClockView clock = new ClockView(context);
        original.setVisibility(View.GONE);
        host.addView(clock, 0, new LinearLayout.LayoutParams(-2, -2));
        return new AutoCloseable() {
            private boolean closed;
            @Override public void close() {
                if (closed) return;
                closed = true;
                clock.close();
                host.removeView(clock);
                original.setVisibility(visibility);
            }
        };
    }

    static void preview(android.app.Activity a,String style){
        if(!"current".equals(style)&&!"flow".equals(style))throw new IllegalArgumentException("Invalid clock style");
        LinearLayout body=FreedomUi.body(a),host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);host.setId(R.id.dreamContent);
        android.widget.TextView original=FreedomUi.text(a,DateFormat.getTimeFormat(a).format(new java.util.Date()),64);host.addView(original);body.addView(host);
        body.addView(FreedomUi.text(a,"Clock preview · no changes saved. Uses the TV time format; reduced motion and power saving keep digits still.",16));
        AutoCloseable flow=attach(a,body,style);android.app.AlertDialog dialog=FreedomUi.sheet(a,"Preview clock",body,"Close preview");
        android.app.Application.ActivityLifecycleCallbacks lifecycle=new android.app.Application.ActivityLifecycleCallbacks(){public void onActivityPaused(android.app.Activity x){if(x==a)dialog.dismiss();}public void onActivityDestroyed(android.app.Activity x){if(x==a)dialog.dismiss();}public void onActivityCreated(android.app.Activity x,android.os.Bundle b){}public void onActivityStarted(android.app.Activity x){}public void onActivityResumed(android.app.Activity x){}public void onActivityStopped(android.app.Activity x){}public void onActivitySaveInstanceState(android.app.Activity x,android.os.Bundle b){}};
        a.getApplication().registerActivityLifecycleCallbacks(lifecycle);dialog.setOnDismissListener(d->{if(flow!=null)try{flow.close();}catch(Exception ignored){}a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);});
    }

    private static final class ClockView extends View implements Runnable {
        // Standard seven-segment masks, ordered top, upper-right, lower-right, bottom,
        // lower-left, upper-left, centre. Lines are rounded and crossfade independently.
        private static final int[] MASKS = {63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Calendar calendar = Calendar.getInstance();
        private final int[] digits = new int[6], previous = new int[6];
        private final String[] periods = DateFormatSymbols.getInstance().getAmPmStrings();
        private final ActivityManager activity;
        private final PowerManager power;
        private final boolean configuredMotion;
        private long lastSecond = Long.MIN_VALUE, changedAt;
        private boolean initialized, closed, twentyFour;
        private String period = "";

        ClockView(Context context) {
            super(context);
            Config config = Config.load(context);
            configuredMotion = config.animations && !config.reducedMotion;
            activity = context.getSystemService(ActivityManager.class);
            power = context.getSystemService(PowerManager.class);
            ink.setStrokeCap(Paint.Cap.ROUND);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            float density = getResources().getDisplayMetrics().density;
            setMeasuredDimension(resolveSize((int)(560 * density), widthSpec),
                    resolveSize((int)(118 * density), heightSpec));
        }

        private boolean motion() {
            return FlowingClockPolicy.motionAllowed(configuredMotion, false, ValueAnimator.areAnimatorsEnabled(),
                    power != null && power.isPowerSaveMode(), activity != null && activity.isLowRamDevice());
        }
        private void schedule() {
            handler.removeCallbacks(this);
            if (closed || !isAttachedToWindow() || !isShown() || getWindowVisibility() != VISIBLE) {
                return;
            }
            handler.post(this);
        }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); schedule(); }
        @Override protected void onDetachedFromWindow() { handler.removeCallbacks(this); super.onDetachedFromWindow(); }
        @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); schedule(); }
        @Override public void onVisibilityAggregated(boolean visible) { super.onVisibilityAggregated(visible); schedule(); }
        void close() { closed = true; handler.removeCallbacks(this); }

        @Override public void run() {
            if (closed || !isAttachedToWindow() || !isShown() || getWindowVisibility() != VISIBLE) { return; }
            long wall = System.currentTimeMillis();
            long second = Math.floorDiv(wall, 1000L);
            if (second != lastSecond) {
                calendar.setTimeInMillis(wall);
                twentyFour = DateFormat.is24HourFormat(getContext());
                int hour = FlowingClockPolicy.hour(calendar.get(Calendar.HOUR_OF_DAY), twentyFour);
                int minute = calendar.get(Calendar.MINUTE), seconds = calendar.get(Calendar.SECOND);
                System.arraycopy(digits, 0, previous, 0, digits.length);
                digits[0] = hour / 10; digits[1] = hour % 10;
                digits[2] = minute / 10; digits[3] = minute % 10;
                digits[4] = seconds / 10; digits[5] = seconds % 10;
                if (!initialized) System.arraycopy(digits, 0, previous, 0, digits.length);
                initialized = true;
                period = twentyFour ? "" : periods[calendar.get(Calendar.AM_PM)];
                setContentDescription(String.format(Locale.getDefault(), "%02d:%02d:%02d %s", hour, minute, seconds, period).trim());
                changedAt = SystemClock.uptimeMillis();
                lastSecond = second;
            }
            invalidate();
            // Static mode updates only at the next second; animation never exceeds 30 fps.
            long delay = motion() && SystemClock.uptimeMillis() - changedAt < FlowingClockPolicy.TRANSITION_MS
                    ? 34L : Math.max(50L, 1000L - Math.floorMod(wall, 1000L));
            handler.postDelayed(this, delay);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!initialized) return;
            float scale = Math.min(getWidth() / 560f, getHeight() / 118f);
            canvas.save();
            canvas.translate((getWidth() - 560f * scale) / 2f, (getHeight() - 118f * scale) / 2f);
            canvas.scale(scale, scale);
            ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(7f);
            float progress = motion() ? FlowingClockPolicy.progress(SystemClock.uptimeMillis() - changedAt) : 1f;
            for (int i = 0; i < 6; i++) {
                float x = 8 + i * 67 + (i >= 2 ? 17 : 0) + (i >= 4 ? 17 : 0);
                int before = MASKS[previous[i]], after = MASKS[digits[i]];
                for (int segment = 0; segment < 7; segment++) {
                    float from = (before & (1 << segment)) != 0 ? 1 : 0;
                    float to = (after & (1 << segment)) != 0 ? 1 : 0;
                    int alpha = (int)(225 * (from + (to - from) * progress));
                    if (alpha == 0) continue;
                    ink.setColor(0x00e9f7ff | (alpha << 24));
                    float left = x + 6, right = x + 45, top = 12, middle = 52, bottom = 92;
                    switch (segment) {
                        case 0: canvas.drawLine(left, top, right, top, ink); break;
                        case 1: canvas.drawLine(right + 5, top + 5, right + 5, middle - 5, ink); break;
                        case 2: canvas.drawLine(right + 5, middle + 5, right + 5, bottom - 5, ink); break;
                        case 3: canvas.drawLine(left, bottom, right, bottom, ink); break;
                        case 4: canvas.drawLine(left - 5, middle + 5, left - 5, bottom - 5, ink); break;
                        case 5: canvas.drawLine(left - 5, top + 5, left - 5, middle - 5, ink); break;
                        default: canvas.drawLine(left, middle, right, middle, ink);
                    }
                }
            }
            ink.setStyle(Paint.Style.FILL); ink.setColor(0xffc3eaff);
            canvas.drawCircle(142, 39, 4, ink); canvas.drawCircle(142, 68, 4, ink);
            canvas.drawCircle(293, 39, 4, ink); canvas.drawCircle(293, 68, 4, ink);
            if (!twentyFour) { ink.setTextSize(19); canvas.drawText(period, 450, 93, ink); }
            canvas.restore();
        }
    }
}
