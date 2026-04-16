package com.ricedotwho.rsa.component.impl.pathfinding;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.ScanUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;

public class GoalDungeonRoom implements Goal {
    private static final float MAX = 100000000f;
    private final UniqueRoom endRoom;
    private final HashMap<String, RoomCandidate> rooms;

    public GoalDungeonRoom(UniqueRoom endRoom, List<RoomCandidate> rooms) {
        this.endRoom = endRoom;
        this.rooms = new HashMap<>(rooms.size());
        for (int i = 0; i < rooms.size(); i++) {
            RoomCandidate candidate = rooms.get(i);
            this.rooms.put(candidate.getName(), candidate);
        }
    }

    public static GoalDungeonRoom create(UniqueRoom endRoom) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        Room startRoom = ScanUtils.getRoomFromPos(player.getBlockX(), player.getBlockZ());
        if (startRoom == null || endRoom == null) {
            RSA.chat("Room is not loaded!");
            return null;
        }

        List<RoomCandidate> candidates = DungeonMapPathfinder.solve(startRoom, endRoom.getTiles().getFirst());
        if (candidates.isEmpty()) {
            RSA.chat("Failed to find path!");
            return null;
        }

        return new GoalDungeonRoom(endRoom, candidates);
    }

    @Override
    public boolean test(int x, int y, int z) {
        Room current = ScanUtils.getRoomFromPos(x, z);
        if (current == null || current.getUniqueRoom() == null) return false;
        if (current.getUniqueRoom() != this.endRoom) return false;
        return current.getRoofHeight() - 1 > y && Mth.abs(current.getX() - x) <= 14f && Mth.abs(current.getZ() - z) <= 14f; // So it doesn't fucking teleport out of bounds // 28 x 28 instead of 32 x 32
    }

    @Override
    public double heuristic(int x, int y, int z) {
        Room room = ScanUtils.getRoomFromPos(x, z);
        if (room == null || room.getUniqueRoom() == null || room.getUniqueRoom().getMainRoom() == null) return MAX;
        RoomCandidate candidate = rooms.get(room.getData().name());
        if (candidate == null) return MAX;
        if (y >= candidate.getDoorRoom().getRoofHeight() - 1) return MAX;
        boolean bl = candidate.getNextDoorRoom() != null;
        if (bl) {
            int endX = (candidate.getDoorRoom().getX() + candidate.getNextDoorRoom().getX()) >> 1;
            int endZ = (candidate.getDoorRoom().getZ() + candidate.getNextDoorRoom().getZ()) >> 1;
            int xDif = x - endX;
            int zDif = z - endZ;

            return (xDif * xDif + zDif * zDif) + candidate.getCost();
        }

        return 0;
    }

    @Override
    public boolean isPossible() {
        return this.endRoom != null;
    }

    @Override
    public BlockPos getEndBlockPos() {
        return null;
    }
}
