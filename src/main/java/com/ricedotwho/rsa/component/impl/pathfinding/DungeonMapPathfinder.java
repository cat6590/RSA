package com.ricedotwho.rsa.component.impl.pathfinding;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonInfo;
import com.ricedotwho.rsm.component.impl.map.map.*;
import com.ricedotwho.rsm.data.Pair;
import lombok.Getter;
import net.minecraft.util.Mth;

import java.util.*;

public class DungeonMapPathfinder {
    private DungeonMapPathfinder() {

    }

    public static List<RoomCandidate> solve(Room start, Room end) {
        PriorityQueue<RoomNode> queue = new PriorityQueue<>();
        Set<RoomNode> closedSet = new HashSet<>();

        Pair<Integer, Integer> endPos = end.getArrayPosition();

        // Stupid array positions are stupid
        // Tspmo
        // they use 11x11 grid for some fuckass reason
        // instead of 6x6
        RoomNode startNode = new RoomNode(start, null, endPos.getFirst() >> 1, endPos.getSecond() >> 1);

        queue.add(startNode);

        while (!queue.isEmpty()) {
            RoomNode current = queue.poll();

            if (current.room == end) {
                return reconstructPath(current);
            }

            closedSet.add(current);

            for (RoomNode neighbor : current.getNeighbors()) {
                if (closedSet.contains(neighbor)) continue;

                // Don't need to worry about updating nodes because there should only be 1 way to path to them
                if (!queue.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private static List<RoomCandidate> reconstructPath(RoomNode roomNode) {
        List<RoomCandidate> path = new ArrayList<>();
        UniqueRoom lastUnique = null;
        Room lastRoom = null;
        int lastIndex = roomNode.index;
        while (roomNode != null) {
            UniqueRoom currentUnique = roomNode.getRoom().getUniqueRoom();
            if (currentUnique == null) {
                RSA.chat("Failed to find room path! Not loaded!");
                break;
            }
            Room nextDoorRoom = null;

            if (lastUnique != currentUnique) {
                nextDoorRoom = lastRoom;
                // Room has changed, put door
            }

            // Only add room if there is door change
            if (nextDoorRoom != null || roomNode.index == lastIndex) {
                path.add(new RoomCandidate(roomNode.getRoom().getUniqueRoom(), roomNode.getRoom(), nextDoorRoom, lastIndex - roomNode.index));
            }

            lastRoom = roomNode.room;
            lastUnique = lastRoom.getUniqueRoom();
            roomNode = roomNode.parent;
        }

        Collections.reverse(path);
        return path;
    }

    @Getter
    private static class RoomNode implements Comparable<RoomNode> {
        private static final int ROOM_COST = 1;
        private final Room room;
        private RoomNode parent;
        private int index;
        private final int endX;
        private final int endZ;
        private final int x;
        private final int z;

        public RoomNode(Room room, RoomNode parent, int endX, int endZ) {
            this.room = room;
            this.parent = parent;
            this.index = parent == null ? 0 : (parent.index + 1);
            this.endX = endX;
            this.endZ = endZ;
            Pair<Integer, Integer> arrayPos = this.room.getArrayPosition();
            x = arrayPos.getFirst() >> 1;
            z = arrayPos.getSecond() >> 1;
        }

        public int getMoveCost() {
            return this.index * ROOM_COST;
        }

        public int getCost() {
            return getMoveCost() + heuristic();
        }

        public int heuristic() {
            return Mth.abs(x - endX) + Mth.abs(z - endZ);
        }

        public List<RoomNode> getNeighbors() {
            List<RoomNode> neighbors = new ArrayList<>();
            int[][] directions = {{0,1},{1,0},{0,-1},{-1,0}};

            for (int[] d : directions) {
                int newX = this.x + d[0];
                int newZ = this.z + d[1];

                int doorX = this.x * 2 + d[0];
                int doorZ = this.z * 2 + d[1];
                //RSA.chat("X room : " + this.x * 2 + ", door : " + doorX);
                //RSA.chat("Y room : " + this.z * 2 + ", door : " + doorZ);

                if (newX >= 0 && newZ >= 0 && newX < 6 && newZ < 6 && doorX >= 0 && doorZ >= 0 && doorX < 11 && doorZ < 11) {
                    Tile tile = DungeonInfo.getDungeonList()[newX * 2 + newZ * 22];
                    if (!(tile instanceof Room newRoom)) continue;

                    if (newRoom.getUniqueRoom() != this.room.getUniqueRoom()) {
                        if (!(DungeonInfo.getDungeonList()[doorX + doorZ * 11] instanceof Door door) || ((door.getType() == DoorType.WITHER || door.getType() == DoorType.BLOOD) && !door.isOpened())) {
                            //RSA.chat("No door found!");
                            continue;
                        }

                        //if (door.getType() == DoorType.BLOOD || door.getType() == DoorType.WITHER) continue;

                        //RSA.chat("Found door at : " + door.getX() + ", " + door.getZ());
                        //RSA.chat(door.getType());
                    }


                    neighbors.add(new RoomNode(newRoom, this, endX, endZ));
                }
            }
            return neighbors;
        }

        @Override
        public int compareTo(RoomNode other) {
            return Integer.compare(this.getCost(), other.getCost());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RoomNode) {
                RoomNode other = (RoomNode) obj;
                return other.room == this.room;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.room.getCore();
        }
    }
}
