package com.upr.proxy;

import android.util.Log;

/**
 * Wrapper de android.util.Log que además manda cada línea a LogBuffer,
 * para que la pantalla de logs las pueda mostrar en vivo.
 */
public class L {

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        LogBuffer.log('D', tag, msg);
    }

    public static void d(String tag, String msg, Throwable t) {
        Log.d(tag, msg, t);
        LogBuffer.log('D', tag, msg + ": " + (t == null ? "null" : t.getMessage()));
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        LogBuffer.log('I', tag, msg);
    }

    public static void i(String tag, String msg, Throwable t) {
        Log.i(tag, msg, t);
        LogBuffer.log('I', tag, msg + ": " + (t == null ? "null" : t.getMessage()));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        LogBuffer.log('W', tag, msg);
    }

    public static void w(String tag, String msg, Throwable t) {
        Log.w(tag, msg, t);
        LogBuffer.log('W', tag, msg + ": " + (t == null ? "null" : t.getMessage()));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        LogBuffer.log('E', tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
        LogBuffer.log('E', tag, msg + ": " + (t == null ? "null" : t.getMessage()));
    }
}
