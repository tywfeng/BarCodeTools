package com.tyw.barcodetools.utils;

import android.util.Log;

import com.tyw.barcodetools.BuildConfig;

public class DebugLog {
    //public static boolean DEBUG = BuildConfig.DEBUG;
    public static String TAG = "CLog:Android";
    public static boolean W=BuildConfig.DEBUG;
    public static boolean E=BuildConfig.DEBUG;
    public static boolean I=BuildConfig.DEBUG;
    public static boolean D=BuildConfig.DEBUG;
    public static boolean V=BuildConfig.DEBUG;

    public static void w(String msg) {
        if (W) Log.w(TAG, msg);
    }
    public static void w(String tag,String msg) {
        if (W) Log.w(tag, msg);
    }

    public static void e(String msg) {
        if (E) Log.e(TAG, msg);
    }
    public static void e(String tag,String msg) {
        if (E) Log.e(tag, msg);
    }

    public static void i(String msg) {
        if (I) Log.i(TAG, msg);
    }
    public static void i(String tag,String msg) {
        if (I) Log.i(tag, msg);
    }

    public static void d(String msg) {
        if (D) Log.d(TAG, msg);
    }
    public static void d(String tag,String msg) {
        if (D) Log.d(tag, msg);
    }

    public static void v(String msg) {
        if (V) Log.v(TAG, msg);
    }
    public static void v(String tag,String msg) {
        if (V) Log.v(tag, msg);
    }
}