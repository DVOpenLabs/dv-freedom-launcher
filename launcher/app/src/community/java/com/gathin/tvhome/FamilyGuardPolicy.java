package com.gathin.tvhome;
import java.util.*;
/** Keep OS navigation/input controls reachable; arbitrary apps cannot escape by naming themselves Settings. */
final class FamilyGuardPolicy {
    static final Set<String> SYSTEM=new HashSet<>(Arrays.asList("android","com.android.systemui","com.android.settings","com.android.tv.settings","com.google.android.tv.settings","com.android.permissioncontroller","com.google.android.permissioncontroller"));
    static boolean system(String pkg,Set<String> inputs){return SYSTEM.contains(pkg)||inputs.contains(pkg);}
    private FamilyGuardPolicy(){}
}
