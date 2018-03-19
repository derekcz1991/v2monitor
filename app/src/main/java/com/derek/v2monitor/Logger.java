package com.derek.v2monitor;

import android.util.Log;

/**
 * Created by derek on 2018/3/19.
 */

public class Logger {

    static Callback callback;

    interface Callback {
        void onLog(String log);
    }

    public static void setCallback(Callback callback) {
        Logger.callback = callback;
    }

    public static void d(String TAG, String msg) {
        Log.d(TAG, msg);
        callback.onLog(msg);
    }

    public static void e(String TAG, String msg, Throwable tr) {
        Log.e(TAG, msg, tr);
        if (tr != null) {
            callback.onLog(msg + tr.getMessage());
        } else {
            callback.onLog(msg);
        }
    }
}
