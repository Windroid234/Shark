package com.shark.kty.shell;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import moe.shizuku.server.IShizukuServiceConnection;

/**
 * Shizuku-compatible server binder, running inside the privileged shell process.
 */
public final class ShellBinder extends IShizukuService.Stub {
    public static final String TAG = "SharkShell";
    private static final int SERVER_VERSION = 13;
    private static final int PROBE_MAGIC = 0x53484b;
    public static final int TRANSACTION_PROBE = 0x5348;

    private static String readCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            process.waitFor();
            return builder.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    @Override
    public int getVersion() {
        return SERVER_VERSION;
    }

    @Override
    public int getUid() {
        return Process.myUid();
    }

    @Override
    public int checkPermission(String permission) {
        return 0;
    }

    @Override
    public boolean checkSelfPermission() {
        return true;
    }

    @Override
    public boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    @Override
    public void requestPermission(int requestCode) {
    }

    @Override
    public IRemoteProcess newProcess(String[] cmd, String[] env, String dir) {
        return RemoteProcessImpl.start(cmd, env, dir);
    }

    @Override
    public String getSELinuxContext() {
        return readCommand("getenforce").trim();
    }

    @Override
    public String getSystemProperty(String key, String defaultValue) {
        String value = readCommand("getprop " + key).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    @Override
    public void setSystemProperty(String key, String value) {
    }

    @Override
    public int addUserService(IShizukuServiceConnection connection, Bundle options) {
        return 0;
    }

    @Override
    public int removeUserService(IShizukuServiceConnection connection, Bundle options) {
        return 0;
    }

    @Override
    public void attachUserService(IBinder binder, Bundle options) {
    }

    @Override
    public void attachApplication(IShizukuApplication application, Bundle options) {
    }

    @Override
    public void dispatchPackageChanged(Intent intent) {
    }

    @Override
    public boolean isHidden(int uid) {
        return false;
    }

    @Override
    public void dispatchPermissionConfirmationResult(int requestUid, int requestCode,
                                                    int result, Bundle data) {
    }

    @Override
    public int getFlagsForUid(int uid, int flags) {
        return 0;
    }

    @Override
    public void updateFlagsForUid(int uid, int mask, int flags) {
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == TRANSACTION_PROBE) {
            if (reply != null) {
                reply.writeInt(PROBE_MAGIC);
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
