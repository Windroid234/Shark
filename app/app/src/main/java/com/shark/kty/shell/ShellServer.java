package com.shark.kty.shell;

import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;

/**
 * app_process entry point. Runs as the shell user and publishes ShellBinder to
 * the app through its ShizukuProvider, then re-sends it when the app touches
 * the request file.
 */
public final class ShellServer {
    public static final String TAG = "SharkShell";
    public static final String PACKAGE = "com.shark.kty";
    public static final String EXTERNAL_DIR = "/sdcard/Android/data/com.shark.kty/files";
    public static final String REQUEST_FILE = "binder-request";

    private ShellServer() {
    }

    public static void main(String[] args) {
        Looper.prepareMainLooper();
        Log.i(TAG, "server starting uid=" + Process.myUid());
        ShellBinder binder = new ShellBinder();
        BinderDelivery.send(binder, PACKAGE);
        watchRequests(binder);
        Looper.loop();
    }

    private static void watchRequests(ShellBinder binder) {
        Thread thread = new Thread(() -> {
            File request = new File(EXTERNAL_DIR, REQUEST_FILE);
            long lastModified = 0;
            while (true) {
                try {
                    long current = request.lastModified();
                    if (request.exists() && current != lastModified) {
                        boolean delivered = false;
                        try {
                            delivered = BinderDelivery.send(binder, PACKAGE);
                        } catch (Throwable ignored) {
                        }
                        if (delivered) {
                            lastModified = current;
                        }
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                } catch (Throwable ignored) {
                }
            }
        }, "shark-request");
        thread.setDaemon(true);
        thread.start();
    }
}
