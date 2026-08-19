package com.hilight.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * ADB host: `app_process` entry point, launched under the shell UID.
 *
 * This class ships inside the APK, so it can be started straight out of the installed app with no
 * file to push:
 *
 *   adb shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) \
 *              app_process / com.hilight.core.AdbHelper"
 *
 * The app cannot bind a cross-UID binder to us (a shell-UID process is killed by ActivityManager as
 * soon as it touches a ContentProvider), so state arrives as a JSON file that we poll.
 *
 * File ownership matters: on external storage a file keeps its creator's UID, and a file created
 * here would be unreadable by the app. The app creates both files; we only overwrite in place.
 */
public final class AdbHelper {

    private static final long POLL_MS = 100;
    private static final long STATUS_MS = 1000;
    private static final String DEFAULT_DIR =
            "/storage/emulated/0/Android/data/com.hilight.studio/files/hilight";

    private final File stateFile;
    private final File statusFile;
    private final Engine engine = new Engine();

    private long stamp = -1, size = -1, lastStatusWarn;

    public static void main(String[] args) {
        String dir = DEFAULT_DIR;
        for (int i = 0; i < args.length - 1; i++) if ("--dir".equals(args[i])) dir = args[i + 1];
        try {
            new AdbHelper(new File(dir)).run();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private AdbHelper(File dir) {
        if (!dir.isDirectory()) {
            Log.w("no " + dir + " yet — open HiLight Studio once so it can create the bridge files");
        }
        stateFile = new File(dir, "state.json");
        statusFile = new File(dir, "helper_status.json");
    }

    private void run() throws Exception {
        engine.start();
        Log.i("watching " + stateFile);
        long lastStatus = 0;
        while (true) {
            long now = System.currentTimeMillis();
            reloadIfChanged();
            if (now - lastStatus >= STATUS_MS) {
                lastStatus = now;
                writeStatus(now);
            }
            Thread.sleep(POLL_MS);
        }
    }

    private void reloadIfChanged() {
        long m = stateFile.lastModified(), s = stateFile.length();
        if (m == stamp && s == size) return;
        stamp = m;
        size = s;
        String raw = read(stateFile);
        if (raw != null && !raw.isEmpty()) engine.setState(raw);
    }

    private void writeStatus(long now) {
        if (!statusFile.exists()) {
            if (now - lastStatusWarn > 10_000) {
                lastStatusWarn = now;
                Log.w("no " + statusFile.getName() + " — open HiLight Studio once");
            }
            return;
        }
        try {
            // in-place truncating write, so the app stays the file's owner
            byte[] data = engine.status().getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream f = new FileOutputStream(statusFile, false)) {
                f.write(data);
            }
        } catch (Exception e) {
            Log.w("status write failed: " + e);
        }
    }

    private static String read(File f) {
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            byte[] b = new byte[(int) r.length()];
            r.readFully(b);
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
