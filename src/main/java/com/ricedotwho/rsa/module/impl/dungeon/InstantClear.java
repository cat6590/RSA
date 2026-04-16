package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonInfo;
import com.ricedotwho.rsm.component.impl.map.map.*;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.game.DungeonEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.RotationUtils;
import com.ricedotwho.rsm.utils.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ModuleInfo(aliases = {"Instant Clear", "Insta Clear"}, id = "InstaClear", category = Category.DUNGEONS)
public class InstantClear extends Module {

    private static final float
            MAP_LEFT = -225,
            MAP_RIGHT = 25,
            MAP_TOP = -225,
            MAP_BOTTOM = 25;

    private final NumberSetting minDeathTick = new NumberSetting("Min Death Tick", 0, 40, 35, 1);
    private final BooleanSetting witherRooms = new BooleanSetting("Wither Rooms", false);

    public InstantClear() {
        this.registerProperty(
                minDeathTick,
                witherRooms
        );
    }

    private Room targetRoom;
    private boolean waiting;
    private Runnable onTeleport;
    private int deathTicks;
    private boolean oddPearl;

    @Override
    public void onEnable() {
        this.resetState();
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent.Start event) {
        if (targetRoom == null && Dungeon.isStarted()) {
            findRoom();
        }

        if (targetRoom == null || waiting) return;

        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        Room currentRoom = Map.getCurrentRoom();

        int roofHeight = currentRoom == null ? 100 : currentRoom.getRoofHeight();
        int bottomDepth = currentRoom == null ? 20 : currentRoom.getBottom();

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        boolean isAbove = playerY >= roofHeight;
        boolean isBelow = playerY <= bottomDepth;
        boolean isOutside = isOutside(playerX, playerZ);

        // pearl out
        if (!isAbove && !isBelow && !isOutside) {
            if (targetRoom.equals(currentRoom)) {
                RSA.chat("Reached target room.");
                waiting = false;
                targetRoom = null;
                return;
            }

            double roofDist = roofHeight - playerY;
            if (roofDist <= 2 && oddPearl && deathTicks < minDeathTick.getValue().intValue()) {
                return;
            }

            int distance = getCeilingDistance(playerX, playerY, playerZ, level);
            if (distance > 5) {
                teleport((int) Math.ceil(distance / 12F), player.getYRot(), -90.0F, player);
            } else {
                pearl(player.getYRot(), -90.0F, null);
            }

            return;
        }

        // teleport outside the map
        if (isAbove && !isOutside) {
            if (playerY % 2 == 1 && deathTicks < minDeathTick.getValue().intValue()) {
                return;
            }

            Vec2 targetPos = getOutsideVec((float) playerX, (float) playerZ);
            teleportTo(player, targetPos);

            // teleport down
            teleport(10, player.getYRot(), 90.0F, player);

            return;
        }

        // wait for teleports
        if (!isBelow) {
            return;
        }

        // teleport to the room
        Vec2 targetPos = new Vec2(targetRoom.getX(), targetRoom.getZ());
        teleportTo(player, targetPos);

        // teleport up to have less pearl hang time
        teleport(5, player.getYRot(), -90.0F, player);

        // double pearl in
        pearl(player.getYRot(), -90.0F, () -> pearl(player.getYRot(), -90.0F, null));
    }

    @SubscribeEvent
    public void onStateChangeEvent(DungeonEvent.StateChange event) {
        if (!event.getNewState().equals(RoomState.CLEARED)) return;

        Room currentRoom = Map.getCurrentRoom();
        if (!event.getRoom().equals(currentRoom)) return;

        RSA.chat("Successfully cleared %s.", currentRoom.getData().name());
    }

    @SubscribeEvent
    public void onServerTickEvent(ServerTickEvent event) {
        deathTicks--;
        if (deathTicks < 0) deathTicks = 40;
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            double packetY = packet.change().position().y;
            oddPearl = packetY % 2 == 0;

            if (waiting) {
                TaskComponent.onServerTick(() -> {
                    waiting = false;
                    if (onTeleport != null) {
                        onTeleport.run();
                        onTeleport = null;
                    }
                });
            }
        }

        if (event.getPacket() instanceof ClientboundSetTimePacket packet) {
            deathTicks = (int) (40 - (packet.gameTime() % 40));
        }
    }

    @SubscribeEvent
    public void onWorldEventLoad(WorldEvent.Load event) {
        resetState();
    }

    private void resetState() {
        targetRoom = null;
        waiting = false;
        onTeleport = null;
        deathTicks = 0;
    }

    // this uses the current position of the player, so you can only use this once all teleports have been received
    private void teleportTo(LocalPlayer player, Vec2 vec) {
        double diffX = vec.x - player.getX();
        double diffZ = vec.y - player.getZ();
        float outsideYaw = (float) RotationUtils.wrapAngleTo180((double) ((float) Math.atan2(diffZ, diffX) * 180.0F) / Math.PI - 90.0D);

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        int teleports = (int) Math.ceil(distance / 12);

        teleport(teleports, outsideYaw, 3.0F, player);
    }

    private void teleport(int teleports, float yaw, float pitch, LocalPlayer player) {
        if (teleports <= 0) return;

        waiting = true;
        onTeleport = null;

        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            if (!SwapManager.swapItem("ASPECT_OF_THE_VOID") || player.getLastSentInput().shift()) {
                return;
            }

            for (int i = 0; i < teleports; i++) {
                if (!SwapManager.sendAirC08(yaw, pitch, true, false)) {
                    waiting = false;
                    break;
                }
            }
        });
    }

    private void pearl(float yaw, float pitch, Runnable onTP) {
        waiting = true;
        onTeleport = onTP;

        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            if (!SwapManager.swapItem("ENDER_PEARL")) return;

            if (!SwapManager.sendAirC08(yaw, pitch, true, false)) {
                waiting = false;
                onTeleport = null;
            }
        });
    }

    private void findRoom() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        List<String> orderedRooms = BloodBlink.getRooms();

        java.util.Map<String, UniqueRoom> roomMap = DungeonInfo.getUniqueRooms()
                .stream()
                .collect(Collectors.toMap(UniqueRoom::getName, r -> r));

        Optional<UniqueRoom> roomOptional = orderedRooms
                .stream()
                .map(roomMap::get)
                .filter(r -> r != null && isValidRoom(r, level))
                .findFirst();

        if (roomOptional.isEmpty()) return;

        UniqueRoom room = roomOptional.get();
        targetRoom = room.getMainRoom();

        RSA.chat("Targeted room: " + room.getName());
    }

    private boolean isValidRoom(UniqueRoom room, ClientLevel level) {
        Room mainRoom = room.getMainRoom();
        if (mainRoom == null) return false;

        RoomType roomType = mainRoom.getData().type();
        if (!Utils.equalsOneOf(roomType, RoomType.NORMAL, RoomType.RARE)) return false;

        boolean isUncleared =
                room.getTiles()
                        .stream()
                        .noneMatch(t -> Utils.equalsOneOf(t.getState(), RoomState.CLEARED, RoomState.GREEN)) &&
                room.getTiles()
                        .stream()
                        .anyMatch(t -> Utils.equalsOneOf(t.getState(), RoomState.UNDISCOVERED, RoomState.UNOPENED));

        if (!isUncleared) return false;

        boolean isValid = room.getDoors()
                .stream()
                .noneMatch(d -> d.getType() == DoorType.ENTRANCE || (!witherRooms.getValue() && d.getType() == DoorType.WITHER));

        if (!isValid) return false;

        return room.getTiles()
                .stream()
                .allMatch(tile -> {
                    List<Entity> entities = level.getEntities(null, getTileBounds(tile));
                    return entities.isEmpty();
                });
    }

    private AABB getTileBounds(Room tile) {
        int tileX = tile.getX();
        int tileZ = tile.getZ();

        int topX = tileX + 16;
        int topZ = tileZ + 16;

        int bottomX = tileX - 16;
        int bottomZ = tileZ - 16;

        return new AABB(
                bottomX, tile.getBottom(), bottomZ,
                topX, tile.getRoofHeight() + 1, topZ
        );
    }

    private int getCeilingDistance(double x, double y, double z, ClientLevel level) {
        int distance = 255;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = (int) y + 1; i < 255; i++) {
            BlockState blockState = level.getBlockState(pos.set(x, i, z));
            Block block = blockState.getBlock();

            if (block != Blocks.AIR) {
                distance = (int) (i - y);
                break;
            }
        }

        return distance;
    }

    private Vec2 getOutsideVec(float x, float z) {
        float leftDist = x - MAP_LEFT;
        float rightDist = MAP_RIGHT - x;
        float topDist = z - MAP_TOP;
        float bottomDist = MAP_BOTTOM - z;

        float dx = Math.min(leftDist, rightDist);
        float dz = Math.min(topDist, bottomDist);

        if (dx < dz) {
            return leftDist < rightDist ? new Vec2(MAP_LEFT, z) : new Vec2(MAP_RIGHT, z);
        } else {
            return topDist < bottomDist ? new Vec2(x, MAP_TOP) : new Vec2(x, MAP_BOTTOM);
        }
    }

    private boolean isOutside(double x, double z) {
        return x < MAP_TOP || x > MAP_BOTTOM || z < MAP_LEFT || z > MAP_RIGHT;
    }

}