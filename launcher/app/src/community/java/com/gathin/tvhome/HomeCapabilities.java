package com.gathin.tvhome;
/** Explicit renderer contracts used by settings and the support view. */
final class HomeCapabilities {
    static boolean inlineShelves(String look){return ExperiencePolicy.GLASS.equals(look)||ExperiencePolicy.CLEAN.equals(look)||ExperiencePolicy.CINEMA.equals(look);}
    static String summary(String look){return ExperiencePolicy.SHOWCASE.equals(look)?"Showcase: one pinned world; shared titles open in their browser. Layout, size, spacing and names apply. Long-press an app to edit it.":ExperiencePolicy.LEGACY.equals(look)?"Classic: grid/category layout. Shared titles open in their browser; local app edits and wallpapers remain available.":"Inline shared titles, app editing, profile appearance, local wallpapers and Control Center are available.";}
    static boolean reuse(boolean saved,boolean sameProfileAndSettings,boolean nextSaved){return saved&&nextSaved&&sameProfileAndSettings;}
    private HomeCapabilities(){}
}
