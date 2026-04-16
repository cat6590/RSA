package com.ricedotwho.rsa.utils;

import com.ricedotwho.rsm.utils.Accessor;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.TickRateManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Util implements Accessor {
    private final Pattern timestampPattern = Pattern.compile("(\\d+)\\s*([dhms])");

    public void setTickRate(float tickRate, boolean frozen) {
        if (tickRate > 20 || tickRate < 0) {
            throw new IllegalArgumentException("tickRate must be between 0 and 20!");
        }

        TickRateManager tickRateManager = mc.level.tickRateManager();
        tickRateManager.setTickRate(tickRate);

        tickRateManager.setFrozen(frozen);
        if (frozen) {
            tickRateManager.setFrozenTicksToRun(0);
        }
    }

    public void setTickRate(float tickRate) {
        setTickRate(tickRate, tickRate == 0);
    }

    public boolean isZero() {
        if (FabricLoader.getInstance().isModLoaded("zeroclient")) {
            try {
                Class<?> clazz = Class.forName("com.ricedotwho.zero.ZeroClient");
                return (boolean) clazz.getMethod("isZero").invoke(null);
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    public long getMillisFromDHMS(String input) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*([dhms])");
        Matcher matcher = pattern.matcher(input);
        long total = 0;
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 'd':
                    total += value * 86_400_000L;
                    break;
                case 'h':
                    total += value * 3_600_000L;
                    break;
                case 'm':
                    total += value * 60_000L;
                    break;
                case 's':
                    total += value * 1_000L;
                    break;
            }
        }
        return total;
    }

    public String millisToDHMS(long millis) {
        long days = millis / 86_400_000;
        long hours = (millis / 3_600_000) % 24;
        long minutes = (millis / 60_000) % 60;
        long seconds = (millis / 1_000) % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
