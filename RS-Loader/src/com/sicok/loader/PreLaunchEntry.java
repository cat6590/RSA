package com.sicok.loader;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import com.sicok.loader.mixin.DynamicMixinPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.io.ByteArrayInputStream;

public class PreLaunchEntry implements PreLaunchEntrypoint {

    private static volatile boolean xR = false;

    @Override
    public void onPreLaunch() {
        if (xR) return;
        xR = true;
        try {
            byte[] modBytes = DynamicMixinPlugin.cachedModBytes;
            if (modBytes == null || modBytes.length == 0) {
                System.err.println("[RSLoader] enable backend digga");
                return;
            }

            final byte[] bytes = modBytes;
            Thread t = new Thread(() -> injectIntoRsm(bytes), "rsl-rsm-inject");
            t.setDaemon(true);
            t.start();

        } catch (Throwable t) {
            System.err.println("[RSLoader] PreLaunchEntry error: " + t.getMessage());
        }
    }

    private static void injectIntoRsm(byte[] modBytes) {
        try {
            ClassLoader knotCl = net.fabricmc.loader.impl.launch.FabricLauncherBase
                    .getLauncher().getTargetClassLoader();

            Class<?> rsmClass = null;
            Object rsmInst = null;
            for (int i = 0; i < 6000; i++) {
                try {
                    rsmClass = knotCl.loadClass("com.ricedotwho.rsm.RSM");
                    Object inst = rsmClass.getMethod("getInstance").invoke(null);
                    if (inst != null) {
                        Object mm = rsmClass.getMethod("getModuleManager").invoke(inst);
                        Object al = rsmClass.getMethod("getAddonLoader").invoke(inst);
                        if (mm != null && al != null) { rsmInst = inst; break; }
                    }
                } catch (Exception ignored) {}
                Thread.sleep(50);
            }
            if (rsmInst == null) { System.err.println("[RSLoader] RSM never became ready"); return; }

            try {
                Object existing = rsmClass.getMethod("getModule", Class.class).invoke(null,
                    knotCl.loadClass("com.ricedotwho.rsa.module.impl.dungeon.boss.Blink"));
                if (existing != null) { System.out.println("[RSLoader] RSA already loaded"); return; }
            } catch (Throwable ignored) {}

            Object addonLoader = rsmClass.getMethod("getAddonLoader").invoke(rsmInst);

            String rsaMain = null;
            try (JarInputStream ji = new JarInputStream(new ByteArrayInputStream(modBytes))) {
                JarEntry e;
                while ((e = ji.getNextJarEntry()) != null) {
                    if (e.getName().equals("fabric.mod.json")) {
                        String js = new String(ji.readAllBytes(), "UTF-8");
                        int idx = js.indexOf("\"rsm\"");
                        if (idx >= 0) {
                            int a = js.indexOf('"', js.indexOf('[', idx) + 1);
                            int b = js.indexOf('"', a + 1);
                            if (a >= 0 && b > a) rsaMain = js.substring(a + 1, b);
                        }
                        break;
                    }
                }
            }
            if (rsaMain == null) { System.err.println("[RSLoader] rsaMain not found"); return; }
            System.out.println("[RSLoader] rsaMain=" + rsaMain);

            java.lang.reflect.Field unsafeF = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeF.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeF.get(null);

            Class<?> delegateClass = null;
            long disoOffset = -1;
            Object disoBase = null;
            try {
                java.lang.reflect.Method getDel = knotCl.getClass().getDeclaredMethod("getDelegate");
                getDel.setAccessible(true);
                Object delegateInst = getDel.invoke(knotCl);
                delegateClass = delegateInst.getClass();
                java.lang.reflect.Field disoField = delegateClass.getDeclaredField("DISABLE_ISOLATION");
                disoField.setAccessible(true);
                disoOffset = unsafe.staticFieldOffset(disoField);
                disoBase = unsafe.staticFieldBase(disoField);
                unsafe.putBoolean(disoBase, disoOffset, true);
            } catch (Throwable ignored) {}

            try {
                Class<?> rsaClass = knotCl.loadClass(rsaMain);
                Object rsaAddon = rsaClass.getDeclaredConstructor().newInstance();

                Class<?> metaClass = knotCl.loadClass("com.ricedotwho.rsm.addon.AddonMeta");
                Object meta = metaClass.getDeclaredConstructor(
                        String.class, String.class, String.class,
                        net.fabricmc.loader.api.Version.class,
                        java.util.Collection.class)
                    .newInstance("rsa", "rsa", rsaMain, null, Collections.emptyList());

                Class<?> containerClass = knotCl.loadClass("com.ricedotwho.rsm.addon.AddonContainer");
                Class<?> aclClass = knotCl.loadClass("com.ricedotwho.rsm.addon.AddonClassLoader");
                Object container = containerClass
                    .getDeclaredConstructor(
                        knotCl.loadClass("com.ricedotwho.rsm.addon.Addon"),
                        aclClass, metaClass, boolean.class)
                    .newInstance(rsaAddon, null, meta, true);

                try {
                    java.lang.reflect.Field maf = addonLoader.getClass().getDeclaredField("mixinAddons");
                    maf.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Set<Object> mixinAddons = (Set<Object>) maf.get(addonLoader);
                    mixinAddons.add(container);
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Field tif = addonLoader.getClass().getDeclaredField("takenIds");
                    tif.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Set<String> takenIds = (Set<String>) tif.get(addonLoader);
                    takenIds.add("rsa");
                } catch (Throwable ignored) {}

                java.lang.reflect.Method loadMethod = containerClass.getMethod("load", boolean.class);
                loadMethod.invoke(container, true);
                System.out.println("[RSLoader] container loaded");

                try {
                    Object mods = containerClass.getMethod("getModules").invoke(container);
                    if (mods instanceof List) {
                        for (Object mod : (List<?>) mods) {
                            try {
                                java.lang.reflect.Field instField = mod.getClass().getDeclaredField("INSTANCE");
                                instField.setAccessible(true);
                                if (instField.get(null) == null) instField.set(null, mod);
                            } catch (NoSuchFieldException ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}

                try {
                    Object modManager = rsmClass.getMethod("getModuleManager").invoke(rsmInst);
                    java.lang.reflect.Method getMap = modManager.getClass().getMethod("getMap");
                    @SuppressWarnings("unchecked")
                    java.util.HashMap<Object, Object> modMap =
                        (java.util.HashMap<Object, Object>) getMap.invoke(modManager);
                    List<java.util.Map.Entry<Object, Object>> entries = new java.util.ArrayList<>(modMap.entrySet());
                    entries.sort((a, b) -> {
                        try {
                            String na = (String) a.getValue().getClass().getMethod("getName").invoke(a.getValue());
                            String nb = (String) b.getValue().getClass().getMethod("getName").invoke(b.getValue());
                            return na.compareToIgnoreCase(nb);
                        } catch (Throwable t2) { return 0; }
                    });
                    java.util.LinkedHashMap<Object, Object> sorted = new java.util.LinkedHashMap<>();
                    for (java.util.Map.Entry<Object, Object> e : entries) sorted.put(e.getKey(), e.getValue());
                    java.lang.reflect.Field mapField = modManager.getClass().getSuperclass().getDeclaredField("map");
                    mapField.setAccessible(true);
                    mapField.set(modManager, sorted);
                } catch (Throwable ignored) {}

                try {
                    Object cg = rsmClass.getMethod("getConfigGui").invoke(rsmInst);
                    if (cg != null) cg.getClass().getMethod("reloadModules").invoke(cg);
                } catch (Throwable ignored) {}

                System.out.println("[RSLoader] RSA injection complete");

            } finally {
                if (delegateClass != null && disoOffset >= 0) {
                    try { unsafe.putBoolean(disoBase, disoOffset, false); }
                    catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            System.err.println("[RSLoader] RSM injection error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            if (t.getCause() != null)
                System.err.println("[RSLoader] caused by: " + t.getCause().getClass().getSimpleName() + ": " + t.getCause().getMessage());
        }
    }
}
