package com.ricedotwho.rsa.module.impl.dungeon.boss.p3;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.AutoTerms;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.ChatUtils;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


@ModuleInfo(aliases = "Term Aura", id = "TermAura", category = Category.DUNGEONS, hasKeybind = true)
public class TermAura extends Module {
    private static final double AURA_RANGE = 4d; // Vanilla is 3.0F
    private static final double AURA_RANGE_SQ = AURA_RANGE * AURA_RANGE;

    private final NumberSetting delay = new NumberSetting("Delay", 50d, 5000d, 500d, 50d);
    private final BooleanSetting showArmorStands = new BooleanSetting("Show Hitboxes", false);
    private final BooleanSetting forceSkyblock = new BooleanSetting("Force Skyblock", false);

    private long lastClick = 0L;

    public TermAura() {
        registerProperty(
                delay,
                showArmorStands,
                forceSkyblock
        );
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        if (mc.screen != null) return;
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, this::rapeArmorstands);
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (!Location.getArea().is(Island.Dungeon) || Minecraft.getInstance().player == null || !DungeonUtils.isPositionInF7Boss(Minecraft.getInstance().player.position())) return;
        if (!event.getMessage().getString().startsWith("This Terminal doesn't seem to be responsive at the moment.")) return;

        this.lastClick = 0;
        event.setCancelled(true);
    }

    private void rapeArmorstands() {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null || Minecraft.getInstance().getConnection() == null) return;
        if (System.currentTimeMillis() - lastClick < delay.getValue().longValue()) return;
        if (!locationCheck()) return;
        if (AutoTerms.isInTerminal() || Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>) return;

        Vec3 eyePos = Minecraft.getInstance().player.position().add(0.0d, Minecraft.getInstance().player.getEyeHeight(), 0.0d);

        double bestDistance = AURA_RANGE_SQ;
        ArmorStand bestCandidate = null;

        Vec3 retardedPos = Minecraft.getInstance().player.position().add(0, -2, 0);

        AABB box = new AABB(retardedPos, retardedPos).inflate(AURA_RANGE, AURA_RANGE, AURA_RANGE);
        for (ArmorStand stand : Minecraft.getInstance().level.getEntitiesOfClass(ArmorStand.class, box, TermAura::filterEntities)) {
            double distance = stand.position().distanceToSqr(retardedPos);
            if (distance <= bestDistance) {
                bestCandidate = stand;
                bestDistance = distance;
            }
        }

        if (bestCandidate == null) return;
        //RSA.chat(bestDistance);


        Vec3 vec3 = MathUtils.clamp(bestCandidate.getBoundingBox(), eyePos).subtract(bestCandidate.getX(), bestCandidate.getY(), bestCandidate.getZ());
//        Minecraft.getInstance().getConnection().send(ServerboundInteractPacket.createInteractionPacket(bestCandidate, Minecraft.getInstance().player.isShiftKeyDown(), InteractionHand.MAIN_HAND, vec3));

        // so this should be how vailla does it, can't check if there's flags bcs of reach flagging hitboxes, and grim ignores armourstands for PacketOrderC
        InteractUtils.interactOnEntity(bestCandidate, vec3);

        lastClick = System.currentTimeMillis();
    }

    public static boolean getEntityVisibility(Entity entity) {
        if (!entity.isInvisible()) return true;
        TermAura termAura = RSM.getModule(TermAura.class);
        if (termAura == null) return false;
        return termAura.isEnabled() && termAura.showArmorStands.getValue() && termAura.locationCheck();
    }

    private boolean locationCheck() {
        return forceSkyblock.getValue() || (Location.getArea().is(Island.Dungeon) && (Location.getFloor() == Floor.F7 || Location.getFloor() == Floor.M7) && DungeonUtils.isPhase(Phase7.P3) && Dungeon.isInBoss());
    }


    private static boolean filterEntities(ArmorStand armorStand) {
        if (armorStand.isDeadOrDying()) return false;
        Component name = armorStand.getCustomName();
        if (name == null) return false;
        return name.getString().equals("Inactive Terminal");
    }
}
