package com.gathin.tvhome;
import java.util.*;
/** Installing a utility must not silently add another Home row. Explicit shortcuts still win. */
final class OptionalToolPolicy {
    static final Set<String> PACKAGES=new HashSet<>(Arrays.asList("io.github.shaharukhs.tvinspector","com.gathin.btconnect","io.github.dennisoleary.digitalrain","com.gathin.newsdream"));
    static boolean home(String pkg,boolean explicitlyChosen){return explicitlyChosen||!PACKAGES.contains(pkg);}
    private OptionalToolPolicy(){}
}
