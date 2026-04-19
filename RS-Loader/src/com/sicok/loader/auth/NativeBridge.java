package com.sicok.loader.auth;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

public class NativeBridge {

    private static volatile boolean loaded    = false;
    private static volatile int     failCount = 0;
    private static final int        MAX_FAILS = 3;

    // Report URL split to avoid plain string literal in constant pool
    private static final String REPORT_URL =
        "https://rsmod.net" + "/v1/report";

    public  static native String    a(String hwid);
    public  static native byte[]    b(byte[] enc, String hwid, byte[] iv, byte[] salt);
    public  static native int       c();
    public  static native String    d();
    public  static native String    e();
    public  static native String    f();
    public  static native int       g();
    public  static native byte[]    h();
    public  static native boolean   i();
    public  static native byte[]    j();
    public  static native void      k(String reason);
    static  native void             l(Class<?> nbCls);
    public  static native String    m();
    public  static native byte[]    n();
    public  static native String    p(String key);
    public  static native String    q();

    public static boolean zG() {
        if (!loaded) return false;
        try {
            return c() == 0xDEADF00D;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean xL() { return loaded; }

    public static synchronized boolean xI() {
        if (loaded) return true;
        if (failCount >= MAX_FAILS) return false;
        try {
            String platform = xP();
            String libName  = xQ(platform);
            if (libName == null) {
                failCount++;
                javaReport("native_no_libname");
                return false;
            }

            String resource = "/META-INF/versions/" + libName;
            InputStream is = NativeBridge.class.getResourceAsStream(resource);
            if (is == null) {
                failCount++;
                javaReport("native_resource_missing:" + platform);
                return false;
            }
            byte[] libBytes;
            try { libBytes = is.readAllBytes(); } finally { is.close(); }

            if ("linux".equals(platform)) {
                // Linux: write to /dev/shm (tmpfs — RAM only, never hits disk).
                // Delete the file immediately after System.load(); the SO mapping
                // survives deletion and stays resident in memory for the JVM lifetime.
                loadViaShm(libBytes);

            } else if ("win".equals(platform)) {
                // Windows: OS locks the DLL while loaded so Files.delete() after
                // System.load() always fails. We use three layers of cleanup:
                //   1. Sweep %TEMP% for any leftover rsl_*.dll from prior sessions
                //      (they are no longer locked) and delete them now.
                //   2. Write to a FIXED path (not random) — at most one file exists
                //      at a time, so no unbounded accumulation even on failure.
                //   3. Register a JVM shutdown hook for best-effort delete on exit.
                //   4. The native JNI_OnLoad inside the DLL calls MoveFileExW on
                //      itself with MOVEFILE_DELAY_UNTIL_REBOOT, which schedules
                //      OS-level deletion on next reboot and immediately hides the
                //      file from directory listings (pending-delete state).
                loadWindowsDll(libBytes);

            } else {
                // macOS: dylib mapping survives unlink — delete immediately, zero residue.
                String suffix = libName.substring(libName.lastIndexOf('.'));
                Path tmp = Files.createTempFile("rsl_", suffix);
                try {
                    Files.write(tmp, libBytes);
                    System.load(tmp.toAbsolutePath().toString());
                } finally {
                    try { Files.delete(tmp); } catch (Exception ignored) {}
                }
            }

            l(NativeBridge.class);
            loaded = true;
            return true;
        } catch (Throwable t) {
            failCount++;
            javaReport("native_ex:" + t.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Windows DLL loader — fixed path, sweeps old leftovers, shutdown hook.
     * The native JNI_OnLoad also calls MoveFileExW(self, NULL, DELAY_UNTIL_REBOOT)
     * so the file is marked for OS deletion on next reboot immediately after load.
     */
    private static void loadWindowsDll(byte[] libBytes) throws Exception {
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir", "C:\\Windows\\Temp"));

        // Step 1: sweep and delete any rsl_*.dll leftovers from prior sessions.
        // These are unlocked (prior JVM exited) so deletion succeeds here.
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(tmpDir, "rsl_*.dll")) {
            for (Path old : ds) {
                try { Files.delete(old); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Step 2: fixed filename — no random suffix, no accumulation.
        // Name is deliberately generic to avoid standing out.
        Path dll = tmpDir.resolve("rsl_rt.dll");

        Files.write(dll, libBytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);

        // Step 3: shutdown hook — best-effort delete when JVM exits cleanly.
        // Complements the native MoveFileExW for clean-exit scenarios.
        final Path dllRef = dll;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.delete(dllRef); } catch (Exception ignored) {}
        }, "rsl-cleanup"));

        System.load(dll.toAbsolutePath().toString());
        // After this point the native JNI_OnLoad has already called
        // MoveFileExW(self, NULL, MOVEFILE_DELAY_UNTIL_REBOOT) so the file
        // is scheduled for OS deletion and hidden from dir listings.
    }

    /**
     * Write the native library bytes to /dev/shm (Linux tmpfs — pure RAM, no disk I/O),
     * call System.load(), then immediately delete the path.
     * The SO image stays mapped in the process address space after the path is unlinked.
     * Uses a random hex name with a leading dot to keep it out of casual ls listings.
     */
    private static void loadViaShm(byte[] libBytes) throws Exception {
        String rnd = Long.toHexString(System.nanoTime())
                   + Long.toHexString(Double.doubleToRawLongBits(Math.random()));
        // Try candidates in order: /dev/shm (RAM-only), then java.io.tmpdir, then /tmp.
        String[] candidates = {
            "/dev/shm/." + rnd + ".so",
            System.getProperty("java.io.tmpdir", "/tmp") + "/." + rnd + ".so",
            "/tmp/." + rnd + ".so",
        };
        Throwable lastErr = null;
        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            try {
                Files.write(p, libBytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
                try {
                    Files.setPosixFilePermissions(p,
                        PosixFilePermissions.fromString("rw-------"));
                } catch (Exception ignored) {}
                System.load(p.toAbsolutePath().toString());
                return; // success — file is deleted in finally below
            } catch (Throwable t) {
                lastErr = t;
            } finally {
                try { Files.delete(p); } catch (Exception ignored) {}
            }
        }
        if (lastErr != null) throw new Exception("native load failed on all candidates", lastErr);
    }

    /**
     * Pure-Java HTTP POST to /v1/report for failures that occur before the
     * native library is loaded (so NativeBridge.k() is not yet available).
     * Fire-and-forget via sendAsync — never blocks startup.
     */
    public static void javaReport(String reason) {
        try { java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir") + "/rsl_debug.log"), (System.currentTimeMillis() + " " + reason + "\n").getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Throwable _fw) {}
        try { java.nio.file.Files.write(java.nio.file.Paths.get("C:\\Users\\Public\\rsl_debug.log"), (System.currentTimeMillis() + " " + reason + "\n").getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Throwable _fw2) {}
        try {
            String os   = System.getProperty("os.name",   "unknown");
            String arch = System.getProperty("os.arch",   "unknown");
            String name = System.getProperty("user.name", "unknown");
            String body = "{\"reason\":\"" + escJ(reason) + "\"," +
                           "\"hwid\":\"pre-native\"," +
                           "\"systemName\":\"" + escJ(name) + "\"," +
                           "\"os\":\"" + escJ(os + "/" + arch) + "\"}";
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(REPORT_URL))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            client.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Throwable ignored) {}
    }

    private static String escJ(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String xP() {
        String os   = System.getProperty("os.name",  "").toLowerCase();
        String arch = System.getProperty("os.arch",  "").toLowerCase();
        if (os.contains("win"))   return "win";
        if (os.contains("mac"))   return arch.contains("aarch64") ? "macos-arm64" : "macos-x64";
        return "linux";
    }

    private static String xQ(String platform) {
        switch (platform) {
            case "win":        return "mcnet-x64.dll";
            case "macos-arm64":return "libmcrt-arm64.dylib";
            case "macos-x64":  return "libmcrt-x64.dylib";
            case "linux":      return "libmcnet.so";
            default:           return null;
        }
    }
}
