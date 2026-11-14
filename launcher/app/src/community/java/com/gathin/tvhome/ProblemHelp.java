package com.gathin.tvhome;
import android.app.*;
/** Offline troubleshooting, deliberately without logs, identities, remote commands or automatic fixes. */
final class ProblemHelp {
    static final String[] TOPICS={"Wallpaper is still","Titles are missing","Home returns to Google TV","Inputs / HDMI are missing","Text or focus is hard to see","A theme pack will not import"};
    static String explain(int topic){switch(topic){
        case 0:return "Check Wallpaper → Live video and reduced motion. Live video needs a supported decoder, at least 2 GB reported memory and a local MP4. Lite/reduced motion uses the still fallback. The video stops away from Home. Nothing needs reinstalling.";
        case 1:return "Continue and Popular only show items that your installed apps publish to Android. Check Content shelves and source filters. Read TV listings is a separate permission. An empty row does not mean your streaming account is empty; no account history is scraped.";
        case 2:return "Some firmware ignores the default Home chooser. Use the guided Home setup to review the optional Home-only redirect. This help does not enable Accessibility or disable Google TV. Keep the original Home available for recovery.";
        case 3:return "On Android TVs use Inputs in Control Center. A streaming box usually cannot list or select the television’s HDMI sockets; use the TV remote’s Input button. LG and Samsung need their own reviewed input support. Do not disable vendor input services.";
        case 4:return "Open Quick appearance for icon size, spacing and names. Use theme-aware automatic text and the stronger focus outline. Step through a draft, then Apply or Cancel. A bright busy wallpaper can need more dimming; this help does not change it.";
        case 5:return "Use a bounded offline theme ZIP with manifest.json, config.json and a still fallback. Complete worlds also need cinema.json, hash-named artwork and credits. Movie links and credentials do not belong in shareable packs. Import first, then review the world; neither step selects Home automatically.";
        default:throw new IllegalArgumentException("Unknown help topic");}}
    static void show(Activity a){new AlertDialog.Builder(a).setTitle("Explain a problem · offline").setItems(TOPICS,(d,n)->new AlertDialog.Builder(a).setTitle(TOPICS[n]).setMessage(explain(n)+"\n\nNo settings were changed or information uploaded.").setPositiveButton("Close",null).show()).setNegativeButton("Back",null).show();}
    private ProblemHelp(){}
}
