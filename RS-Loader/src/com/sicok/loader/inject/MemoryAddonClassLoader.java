package com.sicok.loader.inject;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class MemoryAddonClassLoader extends java.net.URLClassLoader {

    private volatile Map<String, byte[]> classBytes;
    private final Map<String, byte[]> resources = new HashMap<>();
    private volatile int defined = 0;
    private volatile boolean zeroed = false;

    public MemoryAddonClassLoader(byte[] jarBytes, ClassLoader parent) throws Exception {
        super(new URL[0], parent);
        Map<String, byte[]> cb = new HashMap<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                byte[] data = jis.readAllBytes();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    cb.put(name.substring(0, name.length() - 6).replace('/', '.'), data);
                }
                resources.put(name, data);
            }
        }
        this.classBytes = cb;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rsl-zero");
            t.setDaemon(true);
            return t;
        });
        scheduler.schedule(() -> { zeroClassBytes(); scheduler.shutdown(); }, 2, TimeUnit.SECONDS);
    }

    public synchronized void zeroClassBytes() {
        if (zeroed) return;
        Map<String, byte[]> cb = classBytes;
        if (cb != null) {
            for (byte[] b : cb.values()) if (b != null) Arrays.fill(b, (byte) 0);
            cb.clear();
        }
        classBytes = null;
        for (Map.Entry<String, byte[]> e : resources.entrySet()) {
            if (e.getKey().endsWith(".class") && e.getValue() != null)
                Arrays.fill(e.getValue(), (byte) 0);
        }
        zeroed = true;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            if (name.startsWith("java.") || name.startsWith("javax.") ||
                name.startsWith("sun.") || name.startsWith("jdk.")) {
                return getParent().loadClass(name);
            }

            Map<String, byte[]> cb = classBytes;
            if (cb != null && cb.containsKey(name)) {
                byte[] bytes = cb.get(name);
                if (bytes != null && bytes.length > 0) {
                    c = defineClass(name, bytes, 0, bytes.length);
                    defined++;
                    if (resolve) resolveClass(c);
                    return c;
                }
            }

            ClassLoader parent = getParent();
            try {
                c = (parent != null) ? parent.loadClass(name) : ClassLoader.getSystemClassLoader().loadClass(name);
                if (resolve) resolveClass(c);
                return c;
            } catch (ClassNotFoundException knotRejected) {
                // fabric-loader 0.18.4: parent.getResourceAsStream also enforces origin checks.
                // Walk parent URL chain directly, reading jar files on disk to bypass knotCl.
                String resourcePath = name.replace('.', '/') + ".class";
                byte[] bytes = readClassBytesFromParentUrls(resourcePath, parent);
                if (bytes != null && bytes.length > 0) {
                    int lastDot = name.lastIndexOf('.');
                    if (lastDot > 0) {
                        String pkgName = name.substring(0, lastDot);
                        if (getDefinedPackage(pkgName) == null) {
                            try { definePackage(pkgName, null, null, null, null, null, null, null); }
                            catch (IllegalArgumentException ignored) {}
                        }
                    }
                    try {
                        c = defineClass(name, bytes, 0, bytes.length);
                        defined++;
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (Throwable ignored) {}
                }
                throw knotRejected;
            }
        }
    }

    private static byte[] readClassBytesFromParentUrls(String resourcePath, ClassLoader parent) {
        ClassLoader cl = parent;
        while (cl != null) {
            if (cl instanceof java.net.URLClassLoader) {
                byte[] found = scanUrls(((java.net.URLClassLoader) cl).getURLs(), resourcePath);
                if (found != null) return found;
            }
            try {
                for (Field f : cl.getClass().getDeclaredFields()) {
                    String typeName = f.getType().getSimpleName();
                    if (typeName.contains("URLClassLoader") || typeName.contains("DynamicURL")) {
                        f.setAccessible(true);
                        Object inner = f.get(cl);
                        if (inner instanceof java.net.URLClassLoader) {
                            byte[] found = scanUrls(((java.net.URLClassLoader) inner).getURLs(), resourcePath);
                            if (found != null) return found;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            cl = cl.getParent();
        }
        return null;
    }

    private static byte[] scanUrls(URL[] urls, String resourcePath) {
        for (URL url : urls) {
            try {
                if (!"file".equals(url.getProtocol())) continue;
                File f = new File(url.toURI());
                if (f.isFile() && (f.getName().endsWith(".jar") || f.getName().endsWith(".zip"))) {
                    try (JarFile jf = new JarFile(f)) {
                        JarEntry entry = jf.getJarEntry(resourcePath);
                        if (entry != null) {
                            try (InputStream is = jf.getInputStream(entry)) {
                                return is.readAllBytes();
                            }
                        }
                    }
                } else if (f.isDirectory()) {
                    File cf = new File(f, resourcePath.replace('/', File.separatorChar));
                    if (cf.exists()) {
                        try (InputStream is = new FileInputStream(cf)) {
                            return is.readAllBytes();
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> already = findLoadedClass(name);
        if (already != null) return already;
        Map<String, byte[]> cb = classBytes;
        if (cb != null) {
            byte[] bytes = cb.get(name);
            if (bytes != null && bytes.length > 0) {
                Class<?> c = defineClass(name, bytes, 0, bytes.length);
                defined++;
                return c;
            }
        }
        throw new ClassNotFoundException("Rsl: Class not in memory: " + name);
    }

    public Class<?> injectClass(String name, byte[] bytes) {
        synchronized (getClassLoadingLock(name)) {
            Class<?> existing = findLoadedClass(name);
            if (existing != null) return existing;
            Class<?> c = defineClass(name, bytes, 0, bytes.length);
            defined++;
            return c;
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] data = resources.get(name);
        if (data != null && data.length > 0) return new ByteArrayInputStream(data);
        return super.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String name) {
        if (resources.containsKey(name)) {
            try { return MemoryURLStreamHandler.createURL(resources.get(name)); }
            catch (Exception e) { return null; }
        }
        return super.getResource(name);
    }
}
