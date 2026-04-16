package com.ricedotwho.rsa.module.impl.dungeon.device;

import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.module.impl.render.ClickGUI;
import com.ricedotwho.rsm.utils.DungeonUtils;
import lombok.Getter;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@ModuleInfo(aliases = "Align Aura", id = "AlignAura", category = Category.DUNGEONS)
public class AlignAura extends Module {

    private static final int[][] SOLUTIONS = {
            {7, 7, 7, 7, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
            {-1, -1, -1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1},
            {5, 3, 3, 3, -1, 5, -1, -1, -1, -1, 7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, -1},
            {-1, -1, -1, -1, -1, -1, 1, -1, 1, -1, 7, 1, 7, 1, 3, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1},
            {-1, -1, 7, 7, 5, -1, 7, 1, -1, 5, -1, -1, -1, -1, -1, -1, 7, 5, -1, 1, -1, -1, 7, 7, 1},
            {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 1},
            {5, 3, 3, 3, 3, 5, -1, -1, -1, 1, 7, 7, -1, -1, 1, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
            {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, -1, 7, 5, -1, -1, -1, -1, 5, -1, -1, -1, 3, 3},
            {-1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, 7, 7, 7, 7, 1, -1, -1, -1, -1, -1}
    };

    private static final Pos DEVICE_MIDDLE = new Pos(-3, 120, 77);
    private static final Pos DEVICE_CORNER = new Pos(-2, 120, 75);
    private static final AABB DEVICE_BOX = new AABB(-1, 119, 74, -4, 125, 80);

    private final long[] recentClicks = new long[25];
    private FrameData[] currentFrames = null;

    private static class FrameData {
        public ItemFrame entity;
        public int rotation;

        FrameData(ItemFrame entity, int rotation) {
            this.entity = entity;
            this.rotation = rotation;
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Start event) {
        if ((!Location.getArea().is(Island.Dungeon) && !Objects.requireNonNull(RSM.getModule(ClickGUI.class)).getForceSkyBlock().getValue()) || !DungeonUtils.isPhase(Phase7.P3)) return;

        assert mc.player != null;
        if (mc.player.distanceToSqr(DEVICE_MIDDLE.x(), DEVICE_MIDDLE.y(), DEVICE_MIDDLE.z()) > 100) {
            currentFrames = null;
            return;
        }

        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, this::aura);
    }

    private void aura() {
        currentFrames = getCurrentFrames();
        if (currentFrames.length == 0 || mc.getConnection() == null || mc.player == null || mc.gameMode == null || mc.level == null) return;

        int[] rotations = new int[25];
        for (int i = 0; i < 25; i++) {
            rotations[i] = currentFrames[i] != null ? currentFrames[i].rotation : -1;
        }

        int[] solution = findMatchingSolution(rotations);
        if (solution == null) return;

        List<Integer> frameIndices = getIndices();
        boolean isFirst = true;

        for (int index : frameIndices) {
            FrameData frame = currentFrames[index];
            if (frame == null) continue;

            double distance = getDistanceToFrame(frame.entity);
            if (distance > 25) continue;

            int clicksNeeded = (solution[index] - frame.rotation + 8) % 8;
            if (clicksNeeded <= 0) continue;

            if (!Dungeon.isInP3() && (countFramesToSolve(solution) <= 1 || isFirst)) {
                clicksNeeded--;
                isFirst = false;
            }

            if (clicksNeeded > 0) {
                recentClicks[index] = System.currentTimeMillis();

                for (int i = 0; i < clicksNeeded; i++) {
                    frame.rotation = (frame.rotation + 1) % 8;
                    Vec3 vec3 = clamp(frame.entity.getBoundingBox(), mc.player.position().add(0.0d, mc.player.getEyeHeight(), 0.0d)).subtract(frame.entity.getX(), frame.entity.getY(), frame.entity.getZ());
                    InteractUtils.interactOnEntity(frame.entity, vec3);
                }
                break;
            }
        }
    }

    private Vec3 clamp(AABB aabb, Vec3 vec3) {
        return new Vec3(clamp(vec3.x, aabb.minX, aabb.maxX), clamp(vec3.y, aabb.minY, aabb.maxY), clamp(vec3.z, aabb.minZ, aabb.maxZ));
    }

    private double clamp(double d, double min, double max) {
        return Math.min(max, Math.max(d, min));
    }

    private @NotNull List<Integer> getIndices() {
        List<Integer> frameIndices = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            if (currentFrames[i] != null) {
                frameIndices.add(i);
            }
        }

        frameIndices.sort((a, b) -> {
            FrameData frameA = currentFrames[a];
            FrameData frameB = currentFrames[b];

            double distA = getDistanceToFrame(frameA.entity);
            double distB = getDistanceToFrame(frameB.entity);

            return Double.compare(distA, distB);
        });
        return frameIndices;
    }

    private FrameData[] getCurrentFrames() {
        FrameData[] array = new FrameData[25];
        assert mc.level != null;
        for (ItemFrame itemFrame : mc.level.getEntitiesOfClass(ItemFrame.class, DEVICE_BOX, e -> e.getItem().getItem() instanceof ArrowItem)) {
            int dy = (int) (itemFrame.getBlockY() - DEVICE_CORNER.y())  ;
            int dz = (int) (itemFrame.getBlockZ() - DEVICE_CORNER.z());
            int index = dy + dz * 5;

            if (currentFrames != null && System.currentTimeMillis() - recentClicks[index] < 1000) {
                array[index] = currentFrames[index];
                continue;
            }

            int rotation = itemFrame.getRotation();
            array[index] = new FrameData(itemFrame, rotation);
        }

        return array;
    }

    private int[] findMatchingSolution(int[] rotations) {
        for (int[] solution : SOLUTIONS) {
            boolean matches = true;
            for (int i = 0; i < 25; i++) {
                boolean solutionHasFrame = solution[i] != -1;
                boolean currentHasFrame = rotations[i] != -1;
                if (solutionHasFrame != currentHasFrame) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return solution;
            }
        }
        return null;
    }

    private double getDistanceToFrame(ItemFrame frame) {
        return mc.player.getEyePosition().distanceToSqr(frame.position());
    }

    private int countFramesToSolve(int[] solution) {
        int count = 0;
        for (int i = 0; i < 25; i++) {
            if (currentFrames[i] != null && solution[i] != -1) {
                int clicksNeeded = (solution[i] - currentFrames[i].rotation + 8) % 8;
                if (clicksNeeded > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        reset();
    }

    @Override
    public void reset() {
        currentFrames = null;
    }
}
