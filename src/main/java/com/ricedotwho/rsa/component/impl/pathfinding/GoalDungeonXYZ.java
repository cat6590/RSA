package com.ricedotwho.rsa.component.impl.pathfinding;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.utils.ScanUtils;
import com.ricedotwho.rsm.utils.EtherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;

public class GoalDungeonXYZ implements Goal {
    public static final float ROOM_COST = 100000f;
    private static final float MAX = 100000000f;
    private final BlockPos endPos;
    private final HashMap<String, RoomCandidate> rooms;

    public GoalDungeonXYZ(BlockPos endPos, List<RoomCandidate> rooms) {
        this.endPos = endPos;
        this.rooms = new HashMap<>(rooms.size());
        for (int i = 0; i < rooms.size(); i++) {
            RoomCandidate candidate = rooms.get(i);
            this.rooms.put(candidate.getName(), candidate);
        }
    }

    public static GoalDungeonXYZ create(BlockPos endPos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        Room startRoom = ScanUtils.getRoomFromPos(player.getBlockX(), player.getBlockZ());
        Room endRoom = ScanUtils.getRoomFromPos(endPos.getX(), endPos.getZ());
        if (startRoom == null || endRoom == null) {
            RSA.chat("Room is not loaded!");
            return null;
        }

        List<RoomCandidate> candidates = DungeonMapPathfinder.solve(startRoom, endRoom);
        if (candidates.isEmpty()) {
            RSA.chat("Failed to find path!");
            return null;
        }
//        for (RoomCandidate roomCandidate : candidates) {
//            ChatUtils.chat("Path " + roomCandidate.getIndex() + " : " + roomCandidate.getDoorRoom().getData().name() + " -> " + (roomCandidate.getNextDoorRoom() == null ? "null" : (roomCandidate.getNextDoorRoom().getData().name())));
//        }
        return new GoalDungeonXYZ(endPos, candidates);
    }

    @Override
    public boolean test(int x, int y, int z) {
        return x == endPos.getX() && y == endPos.getY() && z == endPos.getZ();
    }

    @Override
    public double heuristic(int x, int y, int z) {
        Room room = ScanUtils.getRoomFromPos(x, z);
        if (room == null || room.getUniqueRoom() == null || room.getUniqueRoom().getMainRoom() == null) return MAX;
        RoomCandidate candidate = rooms.get(room.getData().name());
        if (candidate == null) return MAX;
        if (y >= candidate.getDoorRoom().getRoofHeight() - 1) return MAX;

        int endX;
        int endY;
        int endZ;
        boolean bl = candidate.getNextDoorRoom() != null;
        if (bl) {
            endX = (candidate.getDoorRoom().getX() + candidate.getNextDoorRoom().getX()) >> 1;
            endY = y; // We don't know the Y dif of door, we could get it but this is easier
            endZ = (candidate.getDoorRoom().getZ() + candidate.getNextDoorRoom().getZ()) >> 1;
        } else {
            endX = this.endPos.getX();
            endY = this.endPos.getY();
            endZ = this.endPos.getZ();
        }

        int xDif = x - endX;
        int yDif = y - endY;
        int zDif = z - endZ;

        return (xDif * xDif + yDif * yDif + zDif * zDif) + candidate.getCost();
    }

    @Override
    public boolean isPossible() {
        return EtherUtils.isValidEtherwarpPosition(endPos);
    }

    @Override
    public BlockPos getEndBlockPos() {
        return endPos;
    }
}
