package com.gathin.tvhome;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inputs on Coocaa OS (Skyworth) TVs, which register nothing with TvInputManager. com.skyworth.tv lists
 * the sources its own Source panel shows (HDMI 1, HDMI 2, RF, AV), and switches to one and brings up its
 * player, as picking it in that panel does.
 */
final class CoocaaSources {

    private static final String LIST = "content://com.skyworth.tv.provider/system/source_list";
    private static final String LIST_SDK = "content://com.skyworth.tv.sdk.provider/system/source_list";
    private static final int MAX_ROWS = 96;
    private static final Uri METHOD = Uri.parse("content://com.skyworth.tv.sdk.provider/method");
    private static final String[] COLUMNS = {"sourceName", "sourceIndex", "displayName"};

    static final class Source {
        /** HDMI, AV, VOD (the RF tuner), ... */
        final String name;
        final int index;
        final String label;

        Source(String name, int index, String label) {
            this.name = name;
            this.index = index;
            this.label = label;
        }
    }

    private CoocaaSources() {
    }

    /** HDMI ports in port order, then AV, then the tuner; empty where the provider is missing. */
    static List<Source> list(Context ctx) {
        List<Source> out = query(ctx, LIST);
        if (out.isEmpty()) out = query(ctx, LIST_SDK);
        Collections.sort(out, (a, b) -> a.name.equals(b.name) ? Integer.compare(a.index, b.index)
                : Integer.compare(rank(a.name), rank(b.name)));
        return out;
    }

    private static List<Source> query(Context ctx, String uri) {
        List<Source> out = new ArrayList<>();
        try (Cursor c = ctx.getContentResolver().query(Uri.parse(uri), COLUMNS, null, null, null)) {
            int rows = 0;
            while (c != null && rows++ < MAX_ROWS && c.moveToNext()) {
                String name = c.getString(0);
                String label = c.getString(2);
                if (name == null) continue;
                out.add(new Source(name, c.getInt(1), label == null || label.isEmpty() ? name : label));
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
        return out;
    }

    private static int rank(String name) {
        switch (name) {
            case "HDMI": return 0;
            case "AV": return 1;
            case "VOD": return 3;
            default: return 2;
        }
    }

    static boolean open(Context ctx, Source source) {
        Bundle extras = new Bundle();
        extras.putString("name", source.name);
        extras.putInt("index", source.index);
        try {
            Bundle ret = ctx.getContentResolver().call(METHOD, "METHOD_GOTO_SOURCE", null, extras);
            return ret != null && ret.getBoolean("ret");
        } catch (RuntimeException e) {
            return false;
        }
    }
}
