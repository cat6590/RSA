package com.sicok.loader.security;

public class AgentWatcher {

    private static volatile String cachedHwid = "unknown";

    public static void aD(String hwid) {
        if (hwid != null && !hwid.isEmpty()) cachedHwid = hwid;
    }

    
    public static void aA() {}

    
    public static void aB() {}

    
    public static void aC() {}
}
