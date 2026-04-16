package com.ricedotwho.rsa.module.impl.other;

import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

@Getter
@ModuleInfo(aliases = "Auto Gfs", id = "AutoGfs", category = Category.OTHER)
public class AutoGfs extends Module {
    private final BooleanSetting
            enderPearl = new BooleanSetting("Ender Pearl", false),
            spiritLeap = new BooleanSetting("Spirit Leap", false),
            superBoom = new BooleanSetting("Super Boom", false);

    private final NumberSetting worldLoadTicks = new NumberSetting("World Load Delay", 20, 80, 40, 1);
    private final NumberSetting getItemDelay = new NumberSetting("Get Item Delay", 20, 80, 40, 1);

    private int loadDelay = 0;
    private boolean worldLoaded = false;
    private boolean countdownStarted = false;
    private int globalDelay = 0;

    public AutoGfs() {
        this.registerProperty(
                enderPearl,
                spiritLeap,
                superBoom,
                getItemDelay,
                worldLoadTicks
        );
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event){
        if(Location.getArea() == Island.Unknown) return;
        if (!worldLoaded) return;
        
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        if(globalDelay > 0){
            globalDelay--;
            return;
        }

        boolean sentCommand = false;
        if (enderPearl.getValue()) {
            if (tryGetItem(16, "ENDER_PEARL")) {
                globalDelay = 20;
                sentCommand = true;
            }
        }

        if (!sentCommand && spiritLeap.getValue()) {
            if (tryGetItem(16, "ENDER_PEARL")) {
                globalDelay = 20;
                sentCommand = true;
            }
        }

        if (!sentCommand && superBoom.getValue()) {
            if (tryGetItem(64, "SUPERBOOM_TNT")) {
                globalDelay = 20;
            }
        }
    }

    public static boolean tryGetItem(int maxStack, String sbId) {
        return tryGetItem(maxStack, sbId, false);
    }

    public static boolean tryGetItem(int maxStack, String sbId, boolean notExisting) {
        int slot = SwapManager.getItemSlot(sbId);
        if (slot == -1) {
            if (notExisting) {
                mc.player.connection.sendCommand("gfs " + sbId + " " + maxStack);
                return true;
            }
            return false;
        }
        ItemStack stack = mc.player.getInventory().getItem(slot);
        int count = stack.getCount();
        if (count > 0 && count < maxStack) {
            int missing = maxStack - count;
            mc.player.connection.sendCommand("gfs " + sbId + " " + missing);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event){
        countdownStarted = true;
        loadDelay = worldLoadTicks.getValue().intValue();
    }

    @SubscribeEvent
    public void countDown(ServerTickEvent event) {
        if (Location.getArea() == Island.Unknown) return;
        if (countdownStarted) {
            worldLoaded = false;
            if (loadDelay > 0) {
                loadDelay--;
                return;
            }
            countdownStarted = false;
            worldLoaded = true;
        }
    }
}