package com.gathin.tvhome;
/** A checkmark represents a saved preset, never an inherited or custom value. */
final class SettingsChoicePolicy {
    static int spacingIndex(boolean overridden,int value){if(!overridden)return 0;return value==0?1:value==8?2:value==16?3:value==24?4:-1;}
    private SettingsChoicePolicy(){}
}
