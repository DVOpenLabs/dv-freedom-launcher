package com.gathin.tvhome;

/** Presentation presets and a bounded, process-local preview. No app order, media or device state. */
final class HomeStyles {
    static final String CHOICE = "home_style_choice";
    static final String UNDO = "home_style_undo";
    static final String[] IDS = {"minimal", "tv_grid", "cinematic", "easy_view"};
    static final long PREVIEW_MS = 20_000;

    static boolean valid(String id) {
        if (id == null) return false;
        for (String candidate : IDS) if (candidate.equals(id)) return true;
        return false;
    }

    static final class Preview {
        private String id;
        private long deadline;
        private long revision;
        void begin(String next, long now) {
            if (!valid(next)) throw new IllegalArgumentException("Unknown style");
            id = next; deadline = now + PREVIEW_MS; revision++;
        }
        String active(long now) {
            if (id != null && now >= deadline) cancel();
            return id;
        }
        int seconds(long now) { return active(now) == null ? 0 : (int) ((deadline - now + 999) / 1000); }
        boolean cancel() {
            if (id == null) return false;
            id = null; revision++; return true;
        }
        long revision() { return revision; }
    }

    /** Undo is scoped to these four appearance preferences; unrelated preferences are never serialized. */
    static final class Snapshot {
        final String style;
        final Integer columns, rows, opacity;
        Snapshot(String style, Integer columns, Integer rows, Integer opacity) {
            if (style != null && !valid(style)) throw new IllegalArgumentException("Unknown style");
            if (columns != null && (columns < 4 || columns > 7)) throw new IllegalArgumentException("Columns");
            if (rows != null && (rows < 0 || rows > 3)) throw new IllegalArgumentException("Rows");
            if (opacity != null && (opacity < 30 || opacity > 100)) throw new IllegalArgumentException("Opacity");
            this.style = style; this.columns = columns; this.rows = rows; this.opacity = opacity;
        }
        String encode() { return value(style) + "|" + value(columns) + "|" + value(rows) + "|" + value(opacity); }
        static Snapshot decode(String text) {
            if (text == null || text.length() > 80) return null;
            try {
                String[] parts = text.split("\\|", -1);
                if (parts.length != 4) return null;
                return new Snapshot("-".equals(parts[0]) ? null : parts[0], number(parts[1]), number(parts[2]), number(parts[3]));
            } catch (IllegalArgumentException error) { return null; }
        }
        private static Integer number(String value) { return "-".equals(value) ? null : Integer.valueOf(value); }
        private static String value(Object value) { return value == null ? "-" : value.toString(); }
    }
    private HomeStyles() { }
}
