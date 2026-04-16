package com.ricedotwho.rsa.module.impl.dungeon.boss.p5;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.FastLeap;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.DungeonClass;
import com.ricedotwho.rsm.data.DungeonPlayer;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.data.Rotation;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.DefaultGroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.StringSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.RotationUtils;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.EnumUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@ModuleInfo(aliases = "Relics", id = "Relics", category = Category.DUNGEONS)
public class Relics extends Module {
    private static final Pattern leapPattern = Pattern.compile("^You have teleported to (\\w+)!$");

    private final BooleanSetting aura = new BooleanSetting("Aura", false);
    private final BooleanSetting placeAura = new BooleanSetting("Place Aura", false);
    private final BooleanSetting look = new BooleanSetting("Look", false);
    private final NumberSetting auraRange = new NumberSetting("Range", 3, 5, 4.5, 0.1);
    private final NumberSetting delay = new NumberSetting("Delay", 0, 1000, 500, 50);

    private final DefaultGroupSetting leap = new DefaultGroupSetting("Leap", this);
    private final BooleanSetting leapInMenu = new BooleanSetting("Leap If In Menu", false);
    private final NumberSetting leapDelay = new NumberSetting("Leap Delay", 0, 20, 5, 1);
    private final BooleanSetting lookAfterLeap = new BooleanSetting("Look After Leap", false);

    private final ModeSetting orangePlayer = new ModeSetting("Orange", "Berserk", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting orangePlayerCustom = new StringSetting("Orange Custom", "", true, false, () -> orangePlayer.is("Custom"));

    private final ModeSetting redPlayer = new ModeSetting("Red", "Archer", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting redPlayerCustom = new StringSetting("Red Custom", "", true, false, () -> redPlayer.is("Custom"));

    private boolean walk = false;
    private long lastClick = 0;
    private boolean leaping = false;

    private Type relic = Type.NONE;

    public Relics() {
        this.registerProperty(
                aura,
                placeAura,
                look,
                auraRange,
                delay,
                leap
        );

        leap.add(leapInMenu, lookAfterLeap, leapDelay, orangePlayer, orangePlayerCustom, redPlayer, redPlayerCustom);
    }

    @Override
    public void reset() {
        lastClick = 0;
        leaping = false;
        walk = false;
        relic = Type.NONE;
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundInteractPacket packet) || mc.player == null || !look.getValue() || !Location.getArea().is(Island.Dungeon) || !DungeonUtils.isPhase(Phase7.P5) || !Dungeon.isInBoss() || hasRelic()) return;

        // dumb stupid not exposed entity id
        try {
            Field idField = ServerboundInteractPacket.class.getDeclaredField("field_12870"); // entityId
            idField.setAccessible(true);
            int id = idField.getInt(packet);
            Entity e = mc.level.getEntity(id);
            if (!(e instanceof ArmorStand stand)) return;
            String name = ChatFormatting.stripFormatting(stand.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString());
            Type type = Type.getTypeByName(name);
            if (type == Type.NONE) return;

            if (Utils.equalsOneOf(type, Type.ORANGE, Type.RED)) {
                doRelicLook(type);
                return;
            }

            if (leapInMenu.getValue() && mc.screen != null && mc.screen.getTitle().getString().equals("Spirit Leap")) {
                DungeonPlayer player = Dungeon.getClazz(getClassForRelic(type));
                if (player == null) {
                    RSA.chat("Failed to find player!");
                    return;
                }
                TaskComponent.onTick(this.leapDelay.getValue().longValue(), () -> {
                    if (FastLeap.doLeapFromOpenMenu(player) && lookAfterLeap.getValue()) {
                        relic = type;
                        leaping = true;
                        TaskComponent.onTick(10, () -> leaping = false);
                    }
                });
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            RSA.getLogger().error("Error while finding entityId!", e);
        }
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (!leaping || mc.player == null || !lookAfterLeap.getValue() || relic == Type.NONE) return;
        String message = ChatFormatting.stripFormatting(event.getMessage().getString());
        if (leapPattern.matcher(message).find()) {
            leaping = false;
            doRelicLook(relic);
            relic = Type.NONE;
        }
    }

    private boolean hasRelic() {
        return mc.player != null && Type.getTypeByName(mc.player.getInventory().getItem(8).getHoverName().getString()) != Type.NONE;
    }

    private DungeonClass getClassForRelic(Type type) {
        return switch (type) {
            case BLUE, PURPLE -> DungeonClass.BERSERKER;
            case GREEN -> DungeonClass.ARCHER;
            default -> DungeonClass.NONE;
        };
    }

    private void doRelicLook(Type type) {
        if (type == Type.NONE) return;
        Rotation rot = RotationUtils.getRotation(mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0), type.place);
        mc.player.setXRot(rot.getPitch());
        mc.player.setYRot(rot.getYaw());
        walk = true;
    }

    @SubscribeEvent
    public void onPollInputs(InputPollEvent event) {
        if (!walk || !Location.getArea().is(Island.Dungeon) || !DungeonUtils.isPhase(Phase7.P5) || !Dungeon.isInBoss()) return;

        Input input = event.getClientInput();

        if (input.forward() || input.backward() || input.left() || input.right() | input.shift()) {
            walk = false;
            RSA.chat("Relic look cancelled");
            return;
        }

        event.getInput().apply(new Input(true, false, false, false, false, false, true));
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        // Stupid in boss check but ok
        if (!aura.getValue() || mc.player == null || mc.level == null) return;
        if ((!Location.getArea().is(Island.Dungeon) || !DungeonUtils.isPhase(Phase7.P5) || (Location.getFloor() != Floor.M7)) || !Dungeon.isInBoss()) return;
        long now = System.currentTimeMillis();
        if (now - lastClick <  delay.getValue().longValue()) return;

        Vec3 playerPos = mc.player.position();

        double max = auraRange.getValue().doubleValue() * auraRange.getValue().doubleValue();

        if (placeAura.getValue()) {
            Type type = Type.getTypeByName(mc.player.getInventory().getItem(8).getHoverName().getString());
            if (type != Type.NONE && playerPos.distanceToSqr(type.place) < max) {
                SwapManager.swapSlot(8);

                InteractUtils.interactOnBlock0(BlockPos.containing(type.place));
                lastClick = now;
                walk = false;
                return;
            }
        }

        if (aura.getValue() && !hasRelic()) {
            Vec3 eye = playerPos.add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
            AABB box = new AABB(eye, eye).inflate(4.5, 4.5, 4.5);
            List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, box);

            for (ArmorStand stand : stands) {
                String name = ChatFormatting.stripFormatting(stand.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString());
                Type type = Type.getTypeByName(name);
                if (type == Type.NONE) continue;
                double dist = playerPos.distanceToSqr(stand.position());
                if (dist > max) continue;

                if (InteractUtils.interactOnEntity(stand)) {
                    lastClick = now;
                    return;
                }
            }
        }
    }

    public void test(String in) {
        Type type = EnumUtils.getEnum(Type.class, in.toUpperCase(), Type.ORANGE);
        doRelicLook(type);
    }

    private enum Type {
        RED(new Vec3(51.5, 7.5, 42.5), new Vec3(20, 6, 59)),
        ORANGE(new Vec3(57.5, 7.5, 42.5), new Vec3(92, 6, 56)),
        GREEN(new Vec3(49.5, 7.5, 44.5), new Vec3(20, 6, 94)),
        BLUE(new Vec3(59.5, 7.5, 44.5), new Vec3(91, 6, 94)),
        PURPLE(new Vec3(54.5, 7.5, 41.5), new Vec3(56, 8, 132)),
        NONE(null, null);

        public final Vec3 pickup;
        public final Vec3 place;

        Type(Vec3 place, Vec3 pickup) {
            this.place = place;
            this.pickup = pickup;
        }

        public static Type getTypeByName(String itemName) {
            String name = ChatFormatting.stripFormatting(itemName.toLowerCase());
            if (!name.contains("corrupted") || !name.contains("relic")) return Type.NONE;
            for (Type t : values()) {
                if (name.contains(t.name().toLowerCase())) return t;
            }
            return NONE;
        }
    }
}
