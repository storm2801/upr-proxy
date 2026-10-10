package com.upr.proxy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Buffer circular en memoria que guarda las últimas N líneas de log.
 * La pantalla de Logs se suscribe a este buffer para mostrarlas en vivo.
 */
public class LogBuffer {

    private static final int MAX_LINES = 1000;
    private static final List<Entry> entries = new LinkedList<>();
    private static final List<Listener> listeners = new ArrayList<>();
    private static final SimpleDateFormat fmt =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static synchronized void log(char level, String tag, String msg) {
        Entry e = new Entry(System.currentTimeMillis(), level, tag, msg);
        entries.add(e);
        while (entries.size() > MAX_LINES) entries.remove(0);
        for (Listener l : new ArrayList<>(listeners)) {
            try { l.onNewEntry(e); } catch (Throwable ignored) {}
        }
    }

    public static synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public static synchronized void clear() {
        entries.clear();
    }

    public static synchronized String fullText() {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            sb.append(formatEntry(e)).append('\n');
        }
        return sb.toString();
    }

    public static String formatEntry(Entry e) {
        return fmt.format(new Date(e.timestamp)) + " " + e.level + " " + e.tag + ": " + e.message;
    }

    public static synchronized void register(Listener l) { listeners.add(l); }
    public static synchronized void unregister(Listener l) { listeners.remove(l); }

    public static class Entry {
        public final long timestamp;
        public final char level;
        public final String tag;
        public final String message;
        public Entry(long ts, char lv, String tg, String ms) {
            timestamp = ts; level = lv; tag = tg; message = ms;
        }
    }

    public interface Listener {
        void onNewEntry(Entry e);
    }
}
