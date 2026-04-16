package com.ricedotwho.rsa.component.impl.pathfinding.score;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.api.ModComponent;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonInfo;
import com.ricedotwho.rsm.component.impl.map.map.*;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;

import java.util.*;

public class DungeonRoomScore extends ModComponent {
    private static final Map<UniqueRoom, RoomNode> CACHE = new HashMap<>();

    public DungeonRoomScore() {
        super("DungeonRoomScore");
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        CACHE.clear();
    }

    private static RoomNode getNode(UniqueRoom room) {
        return CACHE.computeIfAbsent(room, RoomNode::new);
    }

    public static List<RoomNode> getOrderedRooms() {
        if (CACHE.isEmpty()) {
            Optional<UniqueRoom> entrance = DungeonInfo.getUniqueRooms().stream().filter(r -> r.getType() == RoomType.ENTRANCE).findFirst();
            if (entrance.isPresent()) {
                return score(entrance.get());
            } else{
                RSA.chat("Room Score: Failed to find entrance?");
            }
        }
        return CACHE.values().stream().sorted(RoomNode::compareTo).toList().reversed();
    }

    public static List<RoomNode> score(UniqueRoom e) {
        CACHE.clear();
        Queue<RoomNode> queue = new ArrayDeque<>();
        Set<UniqueRoom> visited = new HashSet<>();

        RoomNode entrance = getNode(e);

        entrance.distance = 0;
        queue.add(entrance);

        if (e.getDoors().isEmpty()) {
            Optional<Door> door = DungeonInfo.getDoorList().stream().filter(d -> d.getType() == DoorType.ENTRANCE).findFirst();
            if (door.isPresent()) {
                e.getDoors().add(door.get());
            } else {
                RSA.chat("Room Score: No entrance door? Type: %s", e.getName());
            }
        }

        while (!queue.isEmpty()) {
            RoomNode r = queue.poll();
            NeighbourInfo ne = r.getNeighbors(visited);

            // end of split if only 1 room is connected, the second is the real connected rooms, even if it has been visited by the scorer
            if (ne.realCount() == 1) r.end = true;
            if (ne.quiz()) r.nextToQuiz = true;

            for (RoomNode n : ne.neighbours()) {
                int d = r.distance + 1;
                if (d < n.distance) {
                    n.distance = d;
                    queue.add(n);
                }
            }
        }

        return CACHE.values().stream().sorted(RoomNode::compareTo).toList().reversed();
    }

    @Getter
    public static class RoomNode implements Comparable<RoomNode> {
        private final UniqueRoom room;
        private int distance = Integer.MAX_VALUE;
        private boolean end = false;
        private boolean nextToQuiz = false;

        public RoomNode(UniqueRoom room) {
            this.room = room;
        }

        public NeighbourInfo getNeighbors(Set<UniqueRoom> visited) {
            List<RoomNode> neighbors = new ArrayList<>();
            int realCount = 0;
            boolean quiz = false;
            for (UniqueRoom other : DungeonInfo.getUniqueRooms()) {
                if (other == this.room) continue;
                boolean connected = this.room.getDoors().stream().anyMatch(d1 -> other.getDoors().contains(d1));

                if (Utils.equalsOneOf(other.getType(), RoomType.TRAP, RoomType.CHAMPION, RoomType.PUZZLE)) {
                    if (Puzzle.fromName(other.getName()) == Puzzle.QUIZ && connected) {
                        quiz = true;
                    }
                    continue;
                }

                if (visited.contains(other)) {
                    realCount++;
                }
                if (connected) {
                    realCount++;
                    if (!visited.contains(other)) {
                        visited.add(other);
                        neighbors.add(getNode(other));
                    }
                }
            }

            return new NeighbourInfo(neighbors, realCount, quiz);
        }

        @Override
        public int compareTo(RoomNode other) {
            return Integer.compare(this.getDistance(), other.getDistance());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RoomNode other) {
                return other.room == this.room;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.room.getMainRoom().getCore();
        }
    }

    public record NeighbourInfo(List<RoomNode> neighbours, int realCount, boolean quiz) {}
}
