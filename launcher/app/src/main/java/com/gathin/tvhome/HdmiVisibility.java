package com.gathin.tvhome;

import android.media.tv.TvInputManager;

/** Adapted from midhatn's MIT policy. Only bare inactive HDMI ports are filtered, never CEC entries. */
final class HdmiVisibility {
    static final String CHOICE = "hide_inactive_hdmi_choice";
    static boolean visible(boolean hideInactive, int state) {
        return !hideInactive || (state != TvInputManager.INPUT_STATE_CONNECTED_STANDBY
                && state != TvInputManager.INPUT_STATE_DISCONNECTED);
    }
    private HdmiVisibility() { }
}
