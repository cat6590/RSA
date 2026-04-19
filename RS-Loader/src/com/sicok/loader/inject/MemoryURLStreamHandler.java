package com.sicok.loader.inject;

import java.io.*;
import java.net.*;

public class MemoryURLStreamHandler extends URLStreamHandler {

    private final byte[] data;

    public MemoryURLStreamHandler(byte[] data) {
        this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL url) {
        return new URLConnection(url) {
            @Override
            public void connect() {}

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(data);
            }

            @Override
            public String getContentType() {
                return "application/java-archive";
            }

            @Override
            public int getContentLength() {
                return data.length;
            }
        };
    }

    public static URL createURL(byte[] data) throws Exception {
        MemoryURLStreamHandler handler = new MemoryURLStreamHandler(data);
        return new URL(null, "memory://rsa-mod.jar", handler);
    }
}
