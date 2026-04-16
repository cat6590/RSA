package com.ricedotwho.rsa.module.impl.dungeon.croesus;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ricedotwho.rsm.utils.FileUtils;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CroesusLoader {
    private static final File worthlessFile = FileUtils.getSaveFileInCategory("croesus", "worthless.json");
    private static final File alwaysBuyFile = FileUtils.getSaveFileInCategory("croesus", "always_buy.json");
    private static final File runLogFile = FileUtils.getSaveFileInCategory("croesus", "run_log.json");
    private static final Gson gson = new Gson();

    @Getter
    private static List<String> worthless = Arrays.asList(
            "DUNGEON_DISC_5",
            "DUNGEON_DISC_4",
            "DUNGEON_DISC_3",
            "DUNGEON_DISC_2",
            "DUNGEON_DISC_1",
            "MAXOR_THE_FISH",
            "STORM_THE_FISH",
            "GOLDOR_THE_FISH",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_1",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_2",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_3",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_4",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_5",
            "ENCHANTMENT_ULTIMATE_COMBO_1",
            "ENCHANTMENT_ULTIMATE_COMBO_2",
            "ENCHANTMENT_ULTIMATE_COMBO_3",
            "ENCHANTMENT_ULTIMATE_COMBO_4",
            "ENCHANTMENT_ULTIMATE_COMBO_5",
            "ENCHANTMENT_ULTIMATE_BANK_1",
            "ENCHANTMENT_ULTIMATE_BANK_2",
            "ENCHANTMENT_ULTIMATE_BANK_3",
            "ENCHANTMENT_ULTIMATE_BANK_4",
            "ENCHANTMENT_ULTIMATE_BANK_5",
            "ENCHANTMENT_ULTIMATE_JERRY_1",
            "ENCHANTMENT_ULTIMATE_JERRY_2",
            "ENCHANTMENT_ULTIMATE_JERRY_3",
            "ENCHANTMENT_ULTIMATE_JERRY_4",
            "ENCHANTMENT_ULTIMATE_JERRY_5",
            "ENCHANTMENT_FEATHER_FALLING_6",
            "ENCHANTMENT_FEATHER_FALLING_7",
            "ENCHANTMENT_FEATHER_FALLING_8",
            "ENCHANTMENT_FEATHER_FALLING_9",
            "ENCHANTMENT_FEATHER_FALLING_10",
            "ENCHANTMENT_INFINITE_QUIVER_6",
            "ENCHANTMENT_INFINITE_QUIVER_7",
            "ENCHANTMENT_INFINITE_QUIVER_8",
            "ENCHANTMENT_INFINITE_QUIVER_9",
            "ENCHANTMENT_INFINITE_QUIVER_10",
            "SPIRIT_SHORTBOW",
            "SPIRIT_BOW",
            "ITEM_SPIRIT_BOW",
            "WITHER_BOOTS",
            "WITHER_CHESTPLATE",
            "WITHER_LEGGINGS",
            "WITHER_HELMET",
            "WITHER_CLOAK",
            "AUTO_RECOMBOBULATOR",
            "MASTER_SKULL_TIER_5",
            "MASTER_SKULL_TIER_4",
            "SHADOW_ASSASSIN_BOOTS",
            "SHADOW_ASSASSIN_LEGGINGS",
            "SHADOW_ASSASSIN_CHESTPLATE",
            "SHADOW_ASSASSIN_HELMET",
            "WARPED_STONE"
    );

    @Getter
    private static List<String> alwaysBuy = Arrays.asList(
            "NECRON_HANDLE",
            "DARK_CLAYMORE",
            "FIRST_MASTER_STAR",
            "SECOND_MASTER_STAR",
            "THIRD_MASTER_STAR",
            "FOURTH_MASTER_STAR",
            "FIFTH_MASTER_STAR",
            "SHADOW_FURY",
            "SHADOW_WARP_SCROLL",
            "IMPLOSION_SCROLL",
            "WITHER_SHIELD_SCROLL",
            "DYE_LIVID"
    );

    @Getter
    public static List<AutoCroesus.ChestInfo> runLog = new ArrayList<>();

    public static void load() {
        loadWorthless();
        loadAlwaysBuy();
        loadRunLog();
    }

    public static void saveWorthless() {
        FileUtils.writeJson(worthless, worthlessFile);
    }

    private static void loadWorthless() {
        try {
            FileUtils.checkDir(worthlessFile, worthless);
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> temp = gson.fromJson(
                    new InputStreamReader(Files.newInputStream(worthlessFile.toPath())),
                    type
            );
            if(temp == null) {
                System.out.println("Failed to read AutoCroesus worthless data!");
                return;
            }
            worthless = temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveAlwaysBuy() {
        FileUtils.writeJson(alwaysBuy, alwaysBuyFile);
    }

    private static void loadAlwaysBuy() {
        try {
            FileUtils.checkDir(alwaysBuyFile, alwaysBuy);
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> temp = gson.fromJson(
                    new InputStreamReader(Files.newInputStream(alwaysBuyFile.toPath())),
                    type
            );
            if(temp == null) {
                System.out.println("Failed to read AutoCroesus always buy data!");
                return;
            }
            alwaysBuy = temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean addRunLog(AutoCroesus.ChestInfo info) {
        if (info.items.isEmpty() || info.value == 0 || runLog.contains(info)) return false;
        runLog.add(info);
        saveRunLog();
        return true;
    }

    public static void saveRunLog() {
        FileUtils.writeJson(runLog, runLogFile);
    }

    private static void loadRunLog() {
        try {
            FileUtils.checkDir(runLogFile, runLog);
            Type type = new TypeToken<List<AutoCroesus.ChestInfo>>(){}.getType();
            List<AutoCroesus.ChestInfo> temp = gson.fromJson(
                    new InputStreamReader(Files.newInputStream(runLogFile.toPath())),
                    type
            );
            if(temp == null) {
                System.out.println("Failed to read AutoCroesus run log data!");
                return;
            }
            runLog = temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}