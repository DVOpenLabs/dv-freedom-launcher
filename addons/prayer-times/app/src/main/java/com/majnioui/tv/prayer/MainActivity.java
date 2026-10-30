package com.majnioui.tv.prayer;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.res.ColorStateList;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Today's prayer times for one city from Aladhan (free, no account or key; only the configured city/country, method and
 * the date are sent), with the calculation method in res/values/config.xml. The next prayer is highlighted
 * with a countdown.
 */
public class MainActivity extends Activity {
    private static final String TAG = "PrayerTimes";
    private static final String[] NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private static final String[] ARABIC = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final int[] ICONS = {R.drawable.ic_fajr, R.drawable.ic_sunrise, R.drawable.ic_dhuhr,
            R.drawable.ic_asr, R.drawable.ic_maghrib, R.drawable.ic_isha};
    private static final long MINUTE_MS = 60_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private Typeface font;
    private int accent;
    private TextView dates, next, status;
    private LinearLayout rows;
    /** Today's times as minutes after midnight, in NAMES order; null until loaded. */
    private int[] minutes;
    private String[] times;
    /** The calculation method's name as Aladhan gives it, for the status line. */
    private String methodName = "";
    /** The day the times are for (yyyy-MM-dd), to reload after midnight. */
    private String loadedFor;
    private java.util.TimeZone zone=java.util.TimeZone.getDefault();
    private String formatted(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.ENGLISH);f.setTimeZone(zone);return f.format(new Date());}
    private boolean loading,visible;
    private int generation;
    private Thread requestThread;
    private android.content.SharedPreferences source(){return getSharedPreferences("prayer-source",0);}

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!today().equals(loadedFor)) load();
            else render();
            main.postDelayed(this, MINUTE_MS);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        font = getResources().getFont(R.font.outfit);
        accent = getColor(R.color.accent);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {(accent & 0x00FFFFFF) | 0x55000000, 0xFF050607, 0xFF050607}));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(80), dp(48), dp(80), dp(40));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        TextView place = text(18, 400, 0xB3FFFFFF);
        place.setLetterSpacing(0.25f);
        place.setText(source().getString("city","Choose your city").toUpperCase(Locale.ROOT));
        col.addView(place);

        next = text(52, 300, 0xFFFFFFFF);
        next.setPadding(0, dp(10), 0, dp(4));
        col.addView(next);
        dates = text(20, 400, 0xB3FFFFFF);
        col.addView(dates);

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, 0, 1f);
        rlp.setMargins(0, dp(36), 0, 0);
        col.addView(rows, rlp);

        status = text(16, 400, 0x80FFFFFF);
        status.setPadding(0, dp(18), 0, 0);
        status.setText("Loading…");
        col.addView(status);

        android.widget.Button configure=new android.widget.Button(this);configure.setText("Location & calculation method");configure.setAllCaps(false);configure.setOnClickListener(v->configure());col.addView(configure);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible=true;generation++;
        load();
        main.postDelayed(tick, MINUTE_MS);
    }

    @Override
    protected void onPause() {
        visible=false;generation++;loading=false;if(requestThread!=null)requestThread.interrupt();
        super.onPause();
        main.removeCallbacks(tick);
    }

    private String today() { return formatted("yyyy-MM-dd"); }

    private void load() {
        if(loading||!visible)return;
        android.content.SharedPreferences config=source();if(!config.getBoolean("enabled",false)){status.setText("Not connected. Choose your city/country and calculation method first. No request has been made.");return;}
        loading=true;final int token=generation;
        final String day=today();String url;
        try{url=PrayerSource.url(formatted("dd-MM-yyyy"),config.getString("city",""),config.getString("country",""),config.getInt("method",3));}catch(Exception bad){loading=false;status.setText("Check location and calculation method.");return;}
        requestThread=new Thread(() -> {
            JSONObject json = null;
            try {
                json = new JSONObject(fetch(url));
            } catch (Exception e) {
                // No request URL/location is written to Android logs.
            }
            final JSONObject result = json;
            main.post(() -> {
                if(!visible||token!=generation)return;
                loading = false;
                if (result == null) {
                    status.setText("Can't reach Aladhan. Showing the last times, if any.");
                    return;
                }
                try {
                    parse(result.getJSONObject("data"));
                    loadedFor = day;
                    if(!day.equals(today())){loadedFor=null;load();return;}
                    render();
                } catch (Exception e) {
                    status.setText("Unexpected answer from Aladhan.");
                }
            });
        },"Prayer source");requestThread.start();
    }

    private void parse(JSONObject data) throws Exception {
        JSONObject t = data.getJSONObject("timings");
        int[] m = new int[NAMES.length];
        String[] s = new String[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            String v = t.getString(NAMES[i]).trim();
            int sp = v.indexOf(' ');
            if (sp > 0) v = v.substring(0, sp);   // "05:12 (+01)" style suffixes
            s[i] = v;
            m[i] = Integer.parseInt(v.substring(0, 2)) * 60 + Integer.parseInt(v.substring(3, 5));
        }
        minutes = m;
        times = s;
        JSONObject meta = data.optJSONObject("meta");
        String timezone=meta!=null?meta.optString("timezone",""):"";
        if(!java.util.Arrays.asList(java.util.TimeZone.getAvailableIDs()).contains(timezone))throw new java.io.IOException("Unknown provider timezone");
        zone=java.util.TimeZone.getTimeZone(timezone);
        JSONObject method = meta != null ? meta.optJSONObject("method") : null;
        methodName = method != null ? method.optString("name", "") : "";
        JSONObject date = data.getJSONObject("date");
        JSONObject hijri = date.getJSONObject("hijri");
        dates.setText(formatted("EEEE d MMMM") + "  ·  "
                + hijri.getString("day") + " " + hijri.getJSONObject("month").getString("en") + " "
                + hijri.getString("year") + " AH");
    }

    private void render() {
        if (minutes == null) return;
        Calendar c = Calendar.getInstance(zone);
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int nextIdx = -1;
        for (int i = 0; i < NAMES.length; i++) {
            if (i == 1) continue;   // sunrise is shown but is not a prayer
            if (minutes[i] > now) { nextIdx = i; break; }
        }
        if (nextIdx >= 0) {
            int left = minutes[nextIdx] - now;
            next.setText(NAMES[nextIdx] + " in " + (left >= 60 ? left / 60 + " h " : "") + left % 60 + " min");
        } else {
            next.setText("Fajr tomorrow");
        }

        rows.removeAllViews();
        for (int i = 0; i < NAMES.length; i++) {
            boolean hl = i == nextIdx;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(6), dp(20), dp(6), dp(20));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(22));
            bg.setColor(hl ? accent : 0x14FFFFFF);
            card.setBackground(bg);
            int dim = i == 1 ? 0x80FFFFFF : 0xFFFFFFFF;
            int fg = hl ? 0xFFFFFFFF : dim;
            ImageView icon = new ImageView(this);
            icon.setImageResource(ICONS[i]);
            icon.setImageTintList(ColorStateList.valueOf(hl ? 0xFFFFFFFF : (i == 1 ? 0x80FFFFFF : accent)));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(44), dp(44));
            ilp.bottomMargin = dp(22);
            card.addView(icon, ilp);
            TextView ar = centered(text(26, 400, fg));
            ar.setText(ARABIC[i]);
            card.addView(ar);
            TextView name = centered(text(20, 500, hl ? 0xE6FFFFFF : (i == 1 ? 0x80FFFFFF : 0xB3FFFFFF)));
            name.setText(NAMES[i]);
            name.setPadding(0, dp(8), 0, dp(20));
            card.addView(name);
            TextView time = centered(text(42, hl ? 500 : 300, fg));
            time.setText(times[i]);
            // Shrink rather than cut "05:10" when the card is narrow.
            time.setMaxLines(1);
            time.setAutoSizeTextTypeUniformWithConfiguration(20, 42, 1, TypedValue.COMPLEX_UNIT_SP);
            card.addView(time, new LinearLayout.LayoutParams(-1, dp(52)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            lp.setMargins(i == 0 ? 0 : dp(8), 0, i == NAMES.length - 1 ? 0 : dp(8), 0);
            rows.addView(card, lp);
        }
        status.setText("Aladhan" + (methodName.isEmpty() ? "" : " · " + methodName) + " · updated "
                + formatted("HH:mm"));
    }

    private void configure(){android.widget.LinearLayout body=new android.widget.LinearLayout(this);body.setOrientation(android.widget.LinearLayout.VERTICAL);body.setPadding(dp(24),dp(12),dp(24),dp(12));android.widget.EditText city=new android.widget.EditText(this),country=new android.widget.EditText(this),method=new android.widget.EditText(this);city.setSingleLine(true);country.setSingleLine(true);method.setSingleLine(true);city.setHint("City");country.setHint("Country name or code");method.setHint("Aladhan method ID, 0–23");method.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);city.setText(source().getString("city",""));country.setText(source().getString("country",""));method.setText(String.valueOf(source().getInt("method",3)));body.addView(city);body.addView(country);body.addView(method);android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Prayer location & method").setMessage("Optional: sends this city/country and method to api.aladhan.com over HTTPS while this app is visible. No GPS, account, background alarms or boot service. Check the calculation method with your local mosque; 3 is Muslim World League.").setView(body).setNegativeButton("Cancel",null).setNeutralButton("Disconnect",(d,n)->{source().edit().clear().apply();recreate();}).setPositiveButton("Save & fetch",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{int selected=Integer.parseInt(method.getText().toString());PrayerSource.url("01-01-2026",city.getText().toString().trim(),country.getText().toString().trim(),selected);source().edit().putString("city",city.getText().toString().trim()).putString("country",country.getText().toString().trim()).putInt("method",selected).putBoolean("enabled",true).apply();dialog.dismiss();recreate();}catch(Exception invalid){city.setError("Use a city/country up to 80 characters and method 0–23");}}));dialog.show();}

    private static String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setInstanceFollowRedirects(false);
        try (InputStream in = c.getInputStream()) {
            if(c.getResponseCode()!=200)throw new java.io.IOException("Provider unavailable");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for(int n;(n=in.read(buf))>0;){if(Thread.currentThread().isInterrupted()||out.size()+n>262144)throw new java.io.IOException("Request stopped or too large");out.write(buf,0,n);}
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    /** Centred on its own line, whatever the script direction. */
    private static TextView centered(TextView t) {
        t.setGravity(Gravity.CENTER);
        t.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        return t;
    }

    private TextView text(float sp, int weight, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setTypeface(Build.VERSION.SDK_INT >= 28 ? Typeface.create(font, weight, false) : font);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
