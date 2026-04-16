package com.ricedotwho.rsa.module.impl.other;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ButtonSetting;
import com.ricedotwho.rsm.utils.ItemUtils;
import lombok.Getter;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Getter
@ModuleInfo(aliases = "Dev Utils", id = "DevUtils", category = Category.OTHER)
public class DevUtils extends Module {
    private final ButtonSetting pos = new ButtonSetting("Your XYZ", "List Pos", () -> {
        LocalPlayer player = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
        KeyboardHandler keyboard = mc.keyboardHandler;
        if(player == null) return;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String xyz = x + ", " + y + ", " + z;
        RSA.chat(xyz);
        keyboard.setClipboard(xyz);
        RSA.chat("Copied to clipboard!");
    });
    private final ButtonSetting yawPitch = new ButtonSetting("Yaw and Pitch", "Yaw/Pitch", () -> {
        LocalPlayer player = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
        KeyboardHandler keyboard = mc.keyboardHandler;
        if(player == null) return;
        float yaw =  player.getYRot();
        float pitch = player.getXRot();
        String yp = yaw + ", " + pitch;
        RSA.chat(yp);
        keyboard.setClipboard(yp);
    });
    private final ButtonSetting blockinfo = new ButtonSetting("Block info that you're lookin at", "Block Info", () -> {
        LocalPlayer player = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
        KeyboardHandler keyboard = mc.keyboardHandler;
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if(player == null) return;

        if(hitResult.getType() == HitResult.Type.BLOCK) {
            Minecraft client = Minecraft.getInstance();
            BlockHitResult blockHit = (BlockHitResult) client.hitResult;
            BlockPos pos = blockHit.getBlockPos();
            double x = pos.getX() + .5;
            int y = pos.getY();
            double z = pos.getZ() + .5;

            String BlockInfo = x + ", " + y + ", " + z;
            RSA.chat("XYZ: " + BlockInfo);
        }
    });
    private final ButtonSetting entityinfo = new ButtonSetting("Entity info that you're lookin at", "Entity Info", () -> {
        LocalPlayer player = Minecraft.getInstance().player;
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if(player == null) return;
        EntityHitResult entityHR = (EntityHitResult) hitResult;
        String entityInfo = entityHR.getEntity().getName().getString();
        String entityId = String.valueOf(entityHR.getEntity().getId());
        String simplePos = entityHR.getEntity().blockPosition().getX() + ", " + entityHR.getEntity().blockPosition().getY() + ", " + entityHR.getEntity().blockPosition().getZ();
        RSA.chat("Name: " + entityInfo);
        RSA.chat("ID: " + entityId);
        RSA.chat("Pos: " + simplePos);
    });
    private final ButtonSetting getSbID = new ButtonSetting("Gets the SBID of the item you're holding" , "Get SBID", () -> {
        LocalPlayer player = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
        if(player == null) return;
        ItemStack stack = player.getMainHandItem();
        String sbid = ItemUtils.getID(stack);
        RSA.chat("SBID: " + sbid);
    });

    public DevUtils() {
        this.registerProperty(
                pos,
                yawPitch,
                blockinfo,
                entityinfo,
                getSbID
        );
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void reset() {

    }
}
