package com.ricedotwho.rsa.utils;

public class Win10Detector {

    public static boolean isWindows10() {
        String os = System.getProperty("os.name");
        if (os == null || !os.startsWith("Windows")) return false;

        // windows for some reason gives windows 10 for both 10 and 11???
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "ver"});
            String output = new String(process.getInputStream().readAllBytes());

            int buildStart = output.indexOf("10.0.") + 5;
            int buildEnd = output.indexOf('.', buildStart);
            if (buildEnd == -1) buildEnd = output.indexOf(']', buildStart);
            int build = Integer.parseInt(output.substring(buildStart, buildEnd));
            return build < 22000; // 22000+ is windows 11
        } catch (Exception e) {
            System.out.println("Couldnt get windows version");
            return false;
        }
    }
}
