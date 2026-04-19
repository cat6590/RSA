package com.sicok.loader.inject;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
public class ClassInjector {
    private static volatile MemoryAddonClassLoader memoryLoader;
    public static MemoryAddonClassLoader getMemoryLoader() { return memoryLoader; }
    public static synchronized void cA() {
        if (memoryLoader != null) {
            memoryLoader.zeroClassBytes();
        }
        memoryLoader = null;
    }
    public static void cB() {
        if (memoryLoader != null) {
            memoryLoader.zeroClassBytes();
        }
    }
    public static void injectIntoKnot(byte[] bytes) throws Exception {
        ClassLoader knotLoader = FabricLauncherBase.getLauncher().getTargetClassLoader();
        memoryLoader = new MemoryAddonClassLoader(bytes, knotLoader);

        // On fabric-loader 0.18.4 (Mac/aarch64), KnotClassDelegate enforces
        // allowedPrefixes: classes loaded from URLs not registered as code sources
        // get blocked. Register our in-memory jar as a trusted code source.
        // We do this WITHOUT writing to disk — we use a RAM-backed URL via
        // MemoryJarURLHandler and register it directly with the delegate.
        URL memoryJarUrl = MemoryJarURLHandler.createJarUrl(bytes);
        try {
            Method getDelegate = knotLoader.getClass().getDeclaredMethod("getDelegate");
            getDelegate.setAccessible(true);
            Object delegate = getDelegate.invoke(knotLoader);
            // Convert our memjar:// URL to a Path the delegate can register.
            // We need a real Path — use a temp file but only in memory via /proc/self/fd
            // if available, otherwise fall through to URL-only injection.
            try {
                // Try addCodeSource with a Path derived from the URL
                // On Linux we can use /proc/self/fd; on Mac we need another approach.
                // Instead: use the delegate's addURL method if it exists (0.18.4 added it)
                Method addUrl2 = null;
                for (Method m : delegate.getClass().getMethods()) {
                    if (m.getName().equals("addUrl") && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] == URL.class) {
                        addUrl2 = m;
                        break;
                    }
                }
                if (addUrl2 != null) {
                    addUrl2.setAccessible(true);
                    addUrl2.invoke(delegate, memoryJarUrl);
                    
                } else {
                    // Try addCodeSource(Path) — requires a real Path, skip if not available
                    
                }
            } catch (Throwable ex2) {
                
            }
        } catch (Throwable delegateEx) {
            
        }

        // Inject memjar URL into knotLoader's URL chain so class bytes are found
        boolean added = false;
        if (!added) {
            try {
                Method m = knotLoader.getClass().getMethod("addUrlFwd", URL.class);
                m.setAccessible(true);
                m.invoke(knotLoader, memoryJarUrl);
                added = true;
            } catch (Exception ignored) {}
        }
        if (!added) {
            for (Class<?> iface : knotLoader.getClass().getInterfaces()) {
                try {
                    Method m = iface.getMethod("addUrlFwd", URL.class);
                    m.setAccessible(true);
                    m.invoke(knotLoader, memoryJarUrl);
                    added = true;
                    break;
                } catch (Exception ignored) {}
            }
        }
        if (!added) {
            try {
                Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                for (Field f : knotLoader.getClass().getDeclaredFields()) {
                    if (f.getType().getSimpleName().equals("DynamicURLClassLoader")) {
                        long off = unsafe.objectFieldOffset(f);
                        Object dynLoader = unsafe.getObject(knotLoader, off);
                        if (dynLoader != null) {
                            Method addUrl = java.net.URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                            addUrl.setAccessible(true);
                            addUrl.invoke(dynLoader, memoryJarUrl);
                            added = true;
                        }
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!added) {
            try {
                ClassLoader cl = knotLoader;
                while (cl != null) {
                    if (cl instanceof java.net.URLClassLoader) {
                        Method addUrl = java.net.URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                        addUrl.setAccessible(true);
                        addUrl.invoke(cl, memoryJarUrl);
                        added = true;
                        break;
                    }
                    cl = cl.getParent();
                }
            } catch (Exception ignored) {}
        }
        if (!added) {
            try {
            } catch (Throwable ignored) {}
        }
    }
}
