package com.ricedotwho.rsa.module.impl.dungeon.boss.p3;

import com.google.common.reflect.TypeToken;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.event.impl.RawTickEvent;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.FileUtils;
import com.ricedotwho.rsm.utils.Utils;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledBox;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@ModuleInfo(aliases = "Lava Bounce", id = "LavaBounce", category = Category.DUNGEONS, hasKeybind = true)
public class LavaBounce extends Module {

    private static final Map<Integer, Long> cooldownMap = new HashMap<>();

    private final NumberSetting cooldown = new NumberSetting("Cooldown", 0, 2000, 500, 50);
    private final NumberSetting addBlockRange = new NumberSetting("Add Range", 4.5, 50, 4.5, 0.1);
    private final BooleanSetting useConfig = new BooleanSetting("Use Config", true);
    private final KeybindSetting addBlockBind = new KeybindSetting("Add Block Bind", new Keybind(GLFW.GLFW_KEY_SEMICOLON, false, this::addOrRemoveBlock));
    private final BooleanSetting renderBlocks = new BooleanSetting("Render Blocks", true, useConfig::getValue);
    private final ColourSetting colour = new ColourSetting("Colour", Colour.RED.alpha(90), useConfig::getValue);
    private final BooleanSetting debug = new BooleanSetting("Force LavaBounce Load", false);
    private final SaveSetting<Set<Pos>> data = new SaveSetting<>("Blocks", "dungeon/lavabounce", "lavabounce.json", HashSet::new, new TypeToken<Set<Pos>>(){}.getType(), FileUtils.getPgson(), true, null, useConfig::getValue);

    public LavaBounce() {
        this.registerProperty(
                cooldown,
                addBlockRange,
                useConfig,
                debug,
                addBlockBind,
                renderBlocks,
                colour,
                data
        );
    }

    public static void load(String file) {
        LavaBounce inst = RSM.getModule(LavaBounce.class);
        if (inst != null) {
            inst.data.setFileName(file);
            inst.data.load();
        }
    }

    @SubscribeEvent
    public void onTick(RawTickEvent event) {
        if(!debug.getValue() || RSA.isNotInTestEnv()) {
            if (event.isCancel() || !Location.getArea().is(Island.Dungeon) || !Dungeon.isInBoss() || !Utils.equalsOneOf(Location.getFloor(), Floor.M7, Floor.F7) || mc.level == null || mc.player == null || mc.player.isInLava() || mc.player.onGround())
                return;
        }
        BlockPos under = findLava();
        if (under == null) return;

        if (useConfig.getValue()) {
            Pos above = new Pos(under.above());
            if (!data.getValue().contains(above)) return;
        }

        BlockState state = mc.level.getBlockState(under);
        if (state.getShape(mc.level, under).isEmpty()) return;
        Vec3 eyePos = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

        Vec3 top = new Vec3(under.getX() + 0.5, under.getY() + 0.999, under.getZ() + 0.5);
        double motionY = mc.player.getDeltaMovement().y;
        double nextMotionY = Math.max((motionY - 0.08) * 0.98, -3.9) * 2;
        double nextPos = nextMotionY + mc.player.position().y();

        if (nextPos > top.y() || eyePos.distanceToSqr(top) > 20.25) return;

        if (SwapManager.swapItem("SOUL_SAND", "CHEST", "ENDER_CHEST")) {
            long now = System.currentTimeMillis();
            int hash = under.hashCode();
            if (now - cooldownMap.getOrDefault(hash, 0L) < cooldown.getValue().longValue()) return;

            PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                if (InteractUtils.interactOnBlock0(under, eyePos, top)) {
                    cooldownMap.put(under.hashCode(), now);
                }
            });
        }
    }

    private BlockPos findLava() {
        BlockPos.MutableBlockPos bp = mc.player.blockPosition().mutable();
        int y = mc.player.getBlockY();
        for (int i = y; i > 0; i--) {
            bp.setY(i);
            BlockState state = mc.level.getBlockState(bp);
            if (state.is(Blocks.LAVA)) {
                return bp.setY(i - 1).immutable();
            } else if (!state.is(Blocks.AIR)) {
                return null;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onRender3D(Render3DEvent.Extract event) {
        if(!debug.getValue() || RSA.isNotInTestEnv()) {
            if (!Location.getArea().is(Island.Dungeon) || !renderBlocks.getValue() || !useConfig.getValue() || !Dungeon.isInBoss() || !Utils.equalsOneOf(Location.getFloor(), Floor.M7, Floor.F7) || data.getValue().isEmpty() || mc.level == null || mc.player == null)
                return;
        }
        for (Pos pos : data.getValue()) {
            BlockPos bp = pos.asBlockPos();
            AABB aabb = Shapes.block().bounds().move(bp);
            Renderer3D.addTask(new FilledBox(aabb, colour.getValue(), true));
        }
    }

    public void addOrRemoveBlock() {
        if(!debug.getValue() || RSA.isNotInTestEnv()) {
            if (!Location.getArea().is(Island.Dungeon) || !Dungeon.isInBoss() || mc.player == null) return;
        }
        HitResult result = mc.player.pick(addBlockRange.getValue().doubleValue(), 1f, true);
        if (!(result instanceof BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            RSA.chat(ChatFormatting.RED + "Not looking at a block");
            return;
        }

        BlockPos bp = blockHitResult.getBlockPos();
        if (!mc.level.getBlockState(bp).is(Blocks.LAVA)) {
            RSA.chat(ChatFormatting.RED + "Not lava!");
            return;
        }
        Pos pos = new Pos(blockHitResult.getBlockPos());

        if (data.getValue().contains(pos)) {
            data.getValue().remove(pos);
            RSA.chat(ChatFormatting.RED + "Removed " + pos.toChatString());
        } else {
            data.getValue().add(pos);
            RSA.chat(ChatFormatting.GREEN + "Added " + pos.toChatString());
        }
        data.save();
    }
}
