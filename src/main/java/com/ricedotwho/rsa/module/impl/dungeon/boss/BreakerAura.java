package com.ricedotwho.rsa.module.impl.dungeon.boss;

import com.google.common.reflect.TypeToken;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.event.impl.RawTickEvent;
import com.ricedotwho.rsa.module.impl.dungeon.DungeonBreaker;
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
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.FileUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.Utils;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledBox;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@ModuleInfo(aliases = "Breaker Aura", id = "BreakerAura", category = Category.DUNGEONS, hasKeybind = true)
public class BreakerAura extends Module {
    private final BooleanSetting edit = new BooleanSetting("Edit Mode", false);
    private final KeybindSetting addBlockBind = new KeybindSetting("Add Block Bind", new Keybind(GLFW.GLFW_KEY_SEMICOLON, false, this::addOrRemoveBlock));
    private final BooleanSetting swap = new BooleanSetting("Auto Swap", true);
    private final BooleanSetting renderBlocks = new BooleanSetting("Render Blocks", true);
    private final ColourSetting colour = new ColourSetting("Colour", Colour.YELLOW.copy());
    private final BooleanSetting zeroTick = new BooleanSetting("Zero Tick", false);
    private final NumberSetting timeout = new NumberSetting("Timeout", 0, 1000, 500, 10);
    private final SaveSetting<Set<Pos>> data = new SaveSetting<>("Aura Blocks", "dungeon/breaker", "breaker_aura.json", HashSet::new, new TypeToken<Set<Pos>>(){}.getType(), FileUtils.getPgson(), true, null, null);
    private final BooleanSetting debug = new BooleanSetting("Force Breaker Load", false);

    private int charges = 20;
    private static int delay = 0;

    public BreakerAura() {
        this.registerProperty(
                edit,
                addBlockBind,
                swap,
                renderBlocks,
                colour,
                zeroTick,
                timeout,
                data,
                debug
        );
    }

    public static void delay() {
        delay++;
    }

    public static void load(String file) {
        BreakerAura inst = RSM.getModule(BreakerAura.class);
        if (inst != null) {
            inst.data.setFileName(file);
            inst.data.load();
        }
    }

    @SubscribeEvent
    public void onTick(RawTickEvent event) {
        if (!debug.getValue() || RSA.isNotInTestEnv())
            if (event.isCancel() || !Location.getArea().is(Island.Dungeon) || mc.level == null || mc.player == null || !Dungeon.isInBoss() || !Utils.equalsOneOf(Location.getFloor(), Floor.M7, Floor.F7) || data.getValue().isEmpty() || edit.getValue() || charges <= 0) return;
//        if (delay > 0) {
//            delay--;
//            return;
//        }

        if (zeroTick.getValue()) {
            List<Pos> f = data.getValue().stream().filter(p -> {
                BlockPos bp = p.asBlockPos();
                BlockState state = mc.level.getBlockState(bp);
                VoxelShape shape = state.getShape(mc.level, bp);
                return !shape.isEmpty() && DungeonBreaker.canInstantMine(state) && InteractUtils.faceDistance(p.asVec3(), mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0)) <= InteractUtils.BLOCK_RANGE;
            }).toList();
            if (f.isEmpty() || (swap.getValue() && !SwapManager.swapItem("DUNGEONBREAKER"))) return;

            for (Pos pos : f) {
                InteractUtils.breakBlock(pos, true, SwapManager.isDesynced());
                if (--charges <= 0) return;
            }

        } else {
            Optional<Pos> closest = getClosest(data.getValue());
            closest.ifPresent(pos -> {
                if ((swap.getValue() && SwapManager.swapItem("DUNGEONBREAKER"))) {
                    InteractUtils.breakBlock(pos, true, SwapManager.isDesynced());
                    charges--;
                }
            });
        }
    }

    @SubscribeEvent
    public void onRender3D(Render3DEvent.Extract event) {
        if (!debug.getValue() || RSA.isNotInTestEnv())
            if (!Location.getArea().is(Island.Dungeon) || !renderBlocks.getValue() || mc.level == null || mc.player == null || !Dungeon.isInBoss() || !Utils.equalsOneOf(Location.getFloor(), Floor.M7, Floor.F7) || data.getValue().isEmpty()) return;

        for (Pos pos : data.getValue()) {
            BlockPos bp = pos.asBlockPos();
            BlockState state = mc.level.getBlockState(bp);
            VoxelShape shape = state.getShape(mc.level, bp);
            if (shape.isEmpty()) continue;
            AABB aabb = shape.bounds().move(bp);
            Renderer3D.addTask(new FilledBox(aabb, colour.getValue(), true));
        }
    }

    @SubscribeEvent
    public void onReset(WorldEvent.Load event) {
        charges = 20;
        delay = 0;
    }

    @SubscribeEvent
    public void onItemUpdate(PacketEvent.PostReceive event) {
        if (!(event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) || !"DUNGEONBREAKER".equals(ItemUtils.getID(packet.getItem())) || !Location.getArea().is(Island.Dungeon)) return;
        charges = ItemUtils.getDbCharges(packet.getItem()).getFirst();
    }

    private Optional<Pos> getClosest(Set<Pos> positions) {
        Pos closest = null;
        double dist = Integer.MAX_VALUE;
        assert mc.level != null;
        assert mc.player != null;
        for (Pos pos : positions) {
            BlockPos bp = pos.asBlockPos();
            BlockState state = mc.level.getBlockState(bp);
            VoxelShape shape = state.getShape(mc.level, bp);
            Vec3 vec3 = pos.asVec3();
            if (shape.isEmpty() || !DungeonBreaker.canInstantMine(state) || InteractUtils.faceDistance(vec3, mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0)) > InteractUtils.BLOCK_RANGE) continue;
            double d = vec3.distanceTo(mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0));
            if (d < dist) {
                closest = pos;
                dist = d;
            }
        }
        return Optional.ofNullable(closest);
    }

    public void addOrRemoveBlock() {
        if (!Location.getArea().is(Island.Dungeon) || !Dungeon.isInBoss() || mc.player == null) return;
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            RSA.chat(ChatFormatting.RED + "Not looking at a block");
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
