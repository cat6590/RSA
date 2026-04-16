package com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.DungeonBreaker;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AutoRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AwaitManager;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.Node;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsa.utils.render3d.type.Ring;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.RoomUtils;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.FileUtils;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledBox;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class BreakNode extends Node implements Accessor {
    public BreakNode(Pos localPos, AwaitManager awaits, boolean start) {
        super(localPos, awaits, start);
        this.blocks = new ArrayList<>();
    }

    public BreakNode(Pos localPos, List<Pos> blocks, AwaitManager awaits, boolean start) {
        super(localPos, awaits, start);
        this.blocks = blocks;
    }

    @Getter
    private final List<Pos> blocks;
    private List<Pos> rotated = null;
    private boolean running = false;

    @Override
    public void calculate(UniqueRoom room) {
        super.calculate(room);
        this.rotated = new ArrayList<>();
        this.rotated = blocks.stream().map(pos -> RoomUtils.getRealPositionFixed(pos, room.getMainRoom())).toList();
    }

    @Override
    public boolean run(Pos playerPos) {
        if (!SwapManager.reserveSwap("DUNGEONBREAKER")) return cancel();
        if (running) return cancel();

        List<Pos> f = rotated.stream().filter(p -> {
            BlockPos bp = p.asBlockPos();
            BlockState state = mc.level.getBlockState(bp);
            VoxelShape shape = state.getShape(mc.level, bp);
            return !shape.isEmpty() && DungeonBreaker.canInstantMine(state) && InteractUtils.faceDistance(p.asVec3(), mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0)) <= InteractUtils.BLOCK_RANGE;
        }).toList();

        if (f.isEmpty()) return true;
        running = true;

        if (AutoRoutes.getZeroTickBreak().getValue()) {
            for (Pos pos : f) {
                InteractUtils.breakBlock(pos, true, SwapManager.isDesynced());
            }
            running = false;
        } else {
            for (int i = 0; i < f.size(); i++) {
                Pos block = f.get(i);
                TaskComponent.onTick(i, () -> InteractUtils.breakBlock(block, true, SwapManager.isDesynced()));
            }
            TaskComponent.onTick(f.size(), () -> running = false);
        }

        return cancel();
    }

    public boolean cancel() {
        this.reset();
        return false;
    }

    @Override
    public void render(boolean depth) {
        Renderer3D.addTask(new Ring(this.getRealPos().asVec3(), depth, this.getRadius(), this.getColour()));
        if (this.rotated == null || this.rotated.isEmpty()) return;

        Colour colour = AutoRoutes.getBreakColour().getValue();
        Colour transparentColour = colour.alpha(colour.getAlpha() * 0.35F);

        for (Pos pos : rotated) {
            BlockPos bp = pos.asBlockPos();
            BlockState state = mc.level.getBlockState(bp);
            VoxelShape shape = state.getShape(mc.level, bp);
            if (shape.isEmpty()) continue;
            AABB aabb = shape.bounds().move(bp);
            Renderer3D.addTask(new FilledBox(aabb, transparentColour, true));
        }
    }

    @Override
    public int getPriority() {
        return 18;
    }

    @Override
    public String getName() {
        return "break";
    }

    @Override
    public Colour getColour() {
        return this.isStart() ? AutoRoutes.getStartColour().getValue() : AutoRoutes.getBreakColour().getValue();
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = super.serialize();
        json.add("blocks", FileUtils.getGson().toJsonTree(this.blocks));
        return json;
    }

    public static BreakNode supply(UniqueRoom fullRoom, LocalPlayer player, AwaitManager awaits, boolean start) {
        Room mainRoom = fullRoom.getMainRoom();
        Pos playerRelative = RoomUtils.getRelativePosition(new Pos(player.position()), mainRoom);
        return new BreakNode(playerRelative, awaits, start);
    }

    public void addOrRemoveBlock() {
        if (Map.getCurrentRoom() == null) {
            RSA.chat(ChatFormatting.RED + "Room is null!");
        }

        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            RSA.chat(ChatFormatting.RED + "Not looking at a block");
            return;
        }

        Pos pos = new Pos(blockHitResult.getBlockPos());

        Pos relPos = RoomUtils.getRelativePositionFixed(pos, Map.getCurrentRoom().getUniqueRoom().getMainRoom());

        if (blocks.contains(relPos)) {
            blocks.remove(relPos);
            RSA.chat(ChatFormatting.RED + "Removed " + relPos.toChatString() + " from break node");
        } else {
            this.blocks.add(relPos);
            RSA.chat(ChatFormatting.GREEN + "Added " + relPos.toChatString() + " to break node!");
        }
        this.calculate(Map.getCurrentRoom().getUniqueRoom());

        RSM.getModule(AutoRoutes.class).save();
        //AutoroutesFileManager.save();
    }

}
