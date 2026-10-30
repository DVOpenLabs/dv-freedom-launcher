package com.gathin.tvhome;
import java.util.*;
/** Installing a utility must not silently add another Home row. Explicit shortcuts still win. */
final class OptionalToolPolicy {
    static final Set<String> PACKAGES=new HashSet<>(Arrays.asList("io.github.shaharukhs.tvinspector","com.gathin.btconnect","io.github.dennisoleary.digitalrain","com.gathin.newsdream","io.github.maxhayim.headphones","com.lionlev.tv.capacity","io.github.maxhayim.tvhub","io.github.maxhayim.adblocking","io.github.cleanyourtv.flock","com.lionlev.tv.jewishcalendar","com.oneyuv.tv.nepalicalendar","com.majnioui.tv.prayer","com.dvopenlabs.freedom.camera"));
    static boolean home(String pkg,boolean explicitlyChosen){return explicitlyChosen||!PACKAGES.contains(pkg);}
    private OptionalToolPolicy(){}
}
