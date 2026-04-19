package com.sicok.loader.mixin;

import com.sicok.loader.inject.ClassInjector;
import com.sicok.loader.inject.MemoryAddonClassLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class DynamicMixinPlugin implements IMixinConfigPlugin {

    private static final String MOD_URL = "http://localhost:3000/v1/mod";

    // Shared mod bytes — fetched here, reused by PreLaunchEntry so we only download once
    public static volatile byte[] cachedModBytes = null;

    private final List<String> mixinNames = new ArrayList<>();

    @Override
    public void onLoad(String mixinPackage) {
        try {
            byte[] modBytes = fetchMod();
            if (modBytes == null || modBytes.length == 0) {
                System.err.println("[RSLoader] DynamicMixinPlugin: failed to fetch mod");
                return;
            }
            cachedModBytes = modBytes;

            // Inject into knot now so classes are available for mixin application
            ClassInjector.injectIntoKnot(modBytes);

            // Parse mixin names from the mod jar's .mixins.json
            try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(modBytes))) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().endsWith(".mixins.json")) {
                        String js = new String(jis.readAllBytes(), "UTF-8");
                        for (String key : new String[]{"\"mixins\"", "\"client\""}) {
                            int ki = js.indexOf(key);
                            if (ki < 0) continue;
                            int ab = js.indexOf('[', ki);
                            int ae = js.indexOf(']', ab);
                            if (ab < 0 || ae < 0) continue;
                            for (String tok : js.substring(ab + 1, ae).split(",")) {
                                tok = tok.trim().replaceAll("^\"|\"$", "");
                                if (!tok.isEmpty() && !tok.equals("MixinRSM"))
                                    mixinNames.add(tok);
                            }
                        }
                        break;
                    }
                }
            }
            System.out.println("[RSLoader] DynamicMixinPlugin: registered " + mixinNames.size() + " mixins");
        } catch (Throwable t) {
            System.err.println("[RSLoader] DynamicMixinPlugin.onLoad error: " + t.getMessage());
        }
    }

    private static byte[] fetchMod() {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(MOD_URL))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                System.err.println("[RSLoader] Server returned HTTP " + resp.statusCode());
                return null;
            }
            System.out.println("[RSLoader] Fetched " + resp.body().length + " bytes");
            return resp.body();
        } catch (Throwable t) {
            System.err.println("[RSLoader] fetchMod error: " + t.getMessage());
            return null;
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String t, String m) { return true; }
    @Override public void acceptTargets(Set<String> a, Set<String> b) {}
    @Override public List<String> getMixins() { return mixinNames.isEmpty() ? null : mixinNames; }
    @Override public void preApply(String t, org.objectweb.asm.tree.ClassNode c, String m, IMixinInfo i) {}
    @Override public void postApply(String t, org.objectweb.asm.tree.ClassNode c, String m, IMixinInfo i) {}
}
