package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: the launcher's thin clock and date on black, over a slow ambient glow (ClockScene).
 * Network-free; the full product can opt in to a separate companion's local weather cache.
 */
public class ClockDream extends DreamService {

    private ClockScene scene;
    private AutoCloseable weather;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        scene = new ClockScene(getWindow().getDecorView());
        scene.useHomeWallpaper(this);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.start();
        closeWeather();
        weather = ProductFeatures.dreamWeather(this, getWindow().getDecorView());
    }

    @Override
    public void onDreamingStopped() {
        scene.stop();
        closeWeather();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (scene != null) scene.stop();
        closeWeather();
        super.onDetachedFromWindow();
    }
    private void closeWeather() {
        if (weather != null) try { weather.close(); } catch (Exception ignored) { }
        weather = null;
    }
}
