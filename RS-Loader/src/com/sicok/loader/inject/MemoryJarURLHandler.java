package com.sicok.loader.inject;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Registers a custom "memjar" protocol handler and creates a URL that
 * KnotClassLoader's URLClassLoader internals can use to find class bytes
 * entirely in memory — no disk writes.
 *
 * When URLClassLoader calls getResource("com/foo/Bar.class"), it iterates
 * its URL list and for each URL calls the handler to open a connection and
 * check if the resource exists. Our handler returns the bytes from a static map.
 */
public class MemoryJarURLHandler extends URLStreamHandler {

    // Static registry: resource path -> bytes
    private static final Map<String, byte[]> entries = new HashMap<>();
    private static volatile boolean registered = false;

    public static void registerEntries(byte[] jarBytes) throws Exception {
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                entries.put(entry.getName(), jis.readAllBytes());
            }
        }
    }

    public static byte[] getEntry(String path) {
        return entries.get(path);
    }

    public static boolean hasEntry(String path) {
        return entries.containsKey(path);
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        String path = url.getPath();
        // Strip leading slash
        if (path.startsWith("/")) path = path.substring(1);
        final byte[] data = entries.get(path);
        if (data == null) throw new FileNotFoundException("memjar: " + path);

        final String finalPath = path;
        return new URLConnection(url) {
            @Override public void connect() {}
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(data); }
            @Override public int getContentLength() { return data.length; }
            @Override public String getContentType() { return "application/octet-stream"; }
        };
    }

    /**
     * Creates a URL using the memjar protocol.
     * We register our handler via the URL(protocol, host, file, handler) constructor
     * which bypasses the need for a system property or factory registration.
     * 
     * HOWEVER: URLClassLoader internally uses URL.openConnection() which goes through
     * the registered handler, so we need to register our protocol globally.
     * We do this by setting the URL stream handler factory via reflection.
     */
    public static URL createJarUrl(byte[] jarBytes) throws Exception {
        registerEntries(jarBytes);

        // Register our protocol handler factory so URL("memjar://...") works globally
        // We hook into the existing factory or set our own
        if (!registered) {
            try {
                // Set a custom URLStreamHandlerFactory that handles "memjar" protocol
                // and delegates everything else to the default
                URL.setURLStreamHandlerFactory(protocol -> {
                    if ("memjar".equals(protocol)) return new MemoryJarURLHandler();
                    return null; // null = use default handler
                });
                registered = true;
            } catch (Error e) {
                // Factory already set — use the URL(protocol,host,file,handler) constructor instead
            }
        }

        // Create a URL that URLClassLoader will use as a "jar root"
        // The path "/" means "root of this jar" — URLClassLoader appends resource names to it
        return new URL(null, "memjar://rsa-mod/", new MemoryJarURLHandler());
    }
}
