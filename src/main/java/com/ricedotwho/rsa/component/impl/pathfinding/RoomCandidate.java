package com.ricedotwho.rsa.component.impl.pathfinding;

import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import lombok.Getter;

@Getter
public class RoomCandidate {
    private final UniqueRoom uniqueRoom;
    private final int index;
    private final float cost;
    private final Room doorRoom;
    private final Room nextDoorRoom;

    public RoomCandidate(UniqueRoom uniqueRoom, Room doorRoom, Room nextDoorRoom, int index) {
        this.uniqueRoom = uniqueRoom;
        this.doorRoom = doorRoom;
        this.index = index;
        this.cost = index * GoalDungeonXYZ.ROOM_COST;
        this.nextDoorRoom = nextDoorRoom;
    }

    public String getName() {
        return uniqueRoom.getName();
    }
}
