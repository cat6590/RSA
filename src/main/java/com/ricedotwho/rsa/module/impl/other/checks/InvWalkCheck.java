package com.ricedotwho.rsa.module.impl.other.checks;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.other.AntiCheat;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvWalkCheck {
    public static boolean startChecking;
    private static final Pattern playerName = Pattern.compile("^(\\w+)\\s+activated a terminal");
    public static String username;

    private static final Map<BlockPos, Entity> inactiveTerminals = new HashMap<>();
    private static final List<String> termCompleter = new ArrayList<>();
    private static final List<Double> TermPos = new ArrayList<>();
    private static final List<Double> PlayerPos = new ArrayList<>();

    @SubscribeEvent
    public static void setRunning(){
        if(AntiCheat.termWalking.getValue()){
            startChecking = true;
        }
    }

    @SubscribeEvent
    public static void Check1(){
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if(player == null || level == null) return;

        AABB searchBox = player.getBoundingBox().inflate(192);
        List<Entity> entities = level.getEntities(null, searchBox);
        Set<BlockPos> currentInactive = new HashSet<>();

        for(Entity entity : entities) {
            String name = entity.getName().getString();
            BlockPos pos = entity.blockPosition();

            if(entity instanceof ArmorStand) {
                if (name.contains("Inactive Terminal")) {
                    currentInactive.add(pos);
                    inactiveTerminals.putIfAbsent(pos, entity);
                }
                else if (name.contains("Terminal Active")) {
                    if (inactiveTerminals.containsKey(pos)) {
                        double TermX = entity.getX();
                        double TermY = entity.getY();
                        double TermZ = entity.getZ();
                        TermPos.add(TermX);
                        TermPos.add(TermY);
                        TermPos.add(TermZ);
                        inactiveTerminals.remove(pos);
                    }
                }
            }

            if(entity instanceof Player) {
                if (!termCompleter.isEmpty() && name.contains(termCompleter.getFirst())) {
                    double playerx = entity.getX();
                    double playery = entity.getY();
                    double playerz = entity.getZ();
                    PlayerPos.add(playerx);
                    PlayerPos.add(playery);
                    PlayerPos.add(playerz);
                    termCompleter.removeFirst();
                }
            }
        }

        if(!PlayerPos.isEmpty() && !TermPos.isEmpty()) {
            double xOffset = PlayerPos.getFirst() - TermPos.getFirst();
            double yOffset = PlayerPos.get(1) - TermPos.get(1);
            double zOffset = PlayerPos.get(2) - TermPos.get(2);
            PlayerPos.clear();
            TermPos.clear();

            if(xOffset > 7 && xOffset < 40|| xOffset < -7 && xOffset > -40){
                RSA.chat("§b" + username + " §7Failed InvWalk Check §4§lxOffSet§r§7: §8" + xOffset);
                username = null;
            }else if(yOffset > 13 || yOffset < -13) {
                RSA.chat("§b" + username + " §7Failed InvWalk Check §4§lyOffSet§r§7: §8" + yOffset);
                username = null;
            }else if(zOffset > 7 && zOffset < 40|| zOffset < -7 && zOffset > -40) {
                RSA.chat("§b" + username + " §7Failed InvWalk Check §4§lzOffSet§r§7: §8" + zOffset);
                username = null;
            }else if(xOffset > 40 || xOffset < -40) {
                RSA.chat("§b" + username + " §7Failed AutoLeap Check §4§lzOffSet§r§7: §8" + xOffset);
                username = null;
            }else if(zOffset > 40 || zOffset < -40) {
                RSA.chat("§b" + username + " §7Failed AutoLeap Check §4§lzOffSet§r§7: §8" + zOffset);
                username = null;
            }
            PlayerPos.clear();
            TermPos.clear();
        }
        termCompleter.clear();
        inactiveTerminals.keySet().retainAll(currentInactive);
    }

    @SubscribeEvent
    public static void terminalCompletedMsg(ChatEvent.Chat event){
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        String unformatted = StringUtil.stripColor(event.getMessage().getString());
        Matcher matcher = playerName.matcher(unformatted);

        if(matcher.find()){
            termCompleter.add(matcher.group(1));
            username = matcher.group(1);
        }
    }
}