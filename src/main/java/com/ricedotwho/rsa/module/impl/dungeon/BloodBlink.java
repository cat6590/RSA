package com.ricedotwho.rsa.module.impl.dungeon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.component.impl.pathfinding.score.DungeonRoomScore;
import com.ricedotwho.rsa.module.impl.other.AutoGfs;
import com.ricedotwho.rsa.packet.sb.BloodClipHelperStartPacket;
import com.ricedotwho.rsa.utils.Util;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonScanner;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.RoomRotation;
import com.ricedotwho.rsm.component.impl.map.map.RoomType;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.RoomUtils;
import com.ricedotwho.rsm.component.impl.map.utils.ScanUtils;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.game.DungeonEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.module.impl.render.ClickGUI;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@ModuleInfo(aliases = "Blood Blink", id = "BloodBlink", category = Category.DUNGEONS)
public class BloodBlink extends Module {
    private static final Pos SLAB_BLOCK_OFFSET_1 = new Pos(-9.5, 82, -12.5); // Sometimes y = 81.5
    private static final Pos SLAB_BLOCK_OFFSET_2 = new Pos(-12.5, 83, -9.5);
    private static final Pos SLAB_BLOCK_OFFSET_3 = new Pos(-5.5, 82, -12.5);
    private static final Pos SLAB_BLOCK_OFFSET_4 = new Pos(6.5, 82, -12.5);
    private static final Pos SLAB_BLOCK_OFFSET_5 = new Pos(10.5, 82, -12.5);
    private static final Vec3 MIDDLE_MAP_COORDS = new Vec3(-104.5, 0, -104.5);

    private Room targetRoom;
    private Room startRoom;

    // Packet Order
    // C09
    // C08
    // C03 ??

    private int serverTickTimer = -1;
    private int serverTotalTickTimer = 0;
    private int state = 0;
    private boolean isLower = false;
    private int ticksTilStart = -67;
    public boolean forceNextSneak = false;
    private boolean explored = false;

    // Room priorities
    private final List<Entry> roomPriority = new ArrayList<>(5);

    //todo: add coords to target?
    @Getter
    private static List<String> rooms = List.of();

    // Options
    private final BooleanSetting waitForGround = new BooleanSetting("Wait For Ground", false);
    private final BooleanSetting proxyPearl = new BooleanSetting("Proxy Pearl", false);
    private final BooleanSetting auto = new BooleanSetting("Auto Blink", true);
    private final BooleanSetting africanSlavePingMode = new BooleanSetting("African Slave Ping Mode", false);
    private final NumberSetting deathTickOffset = new NumberSetting("Death Tick Offset", 0.0d, 20.0d, 0.0d, 1.0d);
    private final NumberSetting earlyExit = new NumberSetting("Early Exit", 0, 20, 0, 1);
    private final NumberSetting exploreExit = new NumberSetting("Explore Exit", 10, 40, 25, 1);
    private final NumberSetting bloodLoadTickTime = new NumberSetting("Map Load Tick Time", 5, 35, 10, 1);
    private final KeybindSetting cancel = new KeybindSetting("Cancel", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, this::cancel));

    private final ModeSetting mode = new ModeSetting("Mode", "Blood", List.of("Blood", "InstaClear"));
    // each party member must have a different prio!
    private final NumberSetting priority = new NumberSetting("Priority", 1, 4, 1, 1);

    public BloodBlink() {
        this.registerProperty(
                waitForGround,
                proxyPearl,
                auto,
                africanSlavePingMode,
                bloodLoadTickTime,
                deathTickOffset,
                earlyExit,
                exploreExit,
                mode,
                priority,
                cancel
        );

        rooms = new Gson().fromJson(
                new InputStreamReader(Objects.requireNonNull(ClickGUI.class.getResourceAsStream("/assets/rsm/room_priority.json"))),
                new TypeToken<List<String>>(){}.getType()
        );
    }

    @Override
    public void onEnable() {
        this.resetState();
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @SubscribeEvent
    public void WorldEventLoad(WorldEvent.Load event) {
        this.targetRoom = null;
        this.startRoom = null;
        this.serverTickTimer = -1;
        this.serverTotalTickTimer = 0;
        this.ticksTilStart = -67;
        resetState();
        roomPriority.clear();
        state = -1;
    }

    public void resetState() {
        state = -1;
        this.isLower = false;
        this.forceNextSneak = false;
        explored = false;
    }

    private Pos getSlabBlockOffset() {
        return switch (this.priority.getValue().intValue()) {
            case 1 -> SLAB_BLOCK_OFFSET_1;
            //case 2 -> SLAB_BLOCK_OFFSET_2;
            case 2 -> SLAB_BLOCK_OFFSET_3;
            case 3 -> SLAB_BLOCK_OFFSET_4;
            default -> SLAB_BLOCK_OFFSET_5;
        };
    }


    public long encodeIndex(int x, int z) {
        return (long) x | (((long) z) << 32);
    }

    public long encodeIndex(Point p) {
        return encodeIndex(p.x, p.y);
    }


    @SubscribeEvent
    public void onTickStart(ClientTickEvent.Start event) {
        if (Location.getArea() != Island.Dungeon || mc.player == null || Dungeon.isInBoss()) return;
        LocalPlayer player = mc.player;

        if (serverTotalTickTimer <= 2) return;

        switch (state) {
            case -1: {
                if (!auto.getValue() || DungeonUtils.isPositionInF7Boss(mc.player.position())) { // Should stop it from blood blinking if your rejoin in boss ?
                    state = 31;
                    break;
                }
                KeyMapping.releaseAll();
                state = 0;
                // Don't break here, overflow into next state
            }

            case 0: {
                if (targetRoom == null && Dungeon.isStarted()) {
                    RSA.chat("Cannot blood blink while run has started and blood has not been loaded!");
                    state = 31;
                    break;
                }

                if (mc.level == null) break;
                forceNextSneak = true; // Must be setup for etherwarp

                if (waitForGround.getValue() && !player.onGround()) break;
                if (startRoom == null) {
                    startRoom = ScanUtils.getRoomFromPos(player.getBlockX(), player.getBlockZ());
                }
                if (startRoom == null || startRoom.getUniqueRoom() == null) break;
                if (startRoom.getUniqueRoom().getRotation() == RoomRotation.UNKNOWN) break;

                PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                    if (!SwapManager.swapItem(Items.DIAMOND_SHOVEL) || !player.getLastSentInput().shift() || startRoom == null) return;

                    Pos slab = RoomUtils.getRealPosition(getSlabBlockOffset(), startRoom);


                    Block block = mc.level.getBlockState(slab.asBlockPos()).getBlock();
                    if (block == Blocks.AIR) {
                        isLower = true;
                        slab.selfAdd(0.0, -1.0, 0.0);
                    }

                    float[] angles = EtherUtils.getYawAndPitch(slab.asVec3(), true, player, true);
                    SwapManager.sendAirC08(angles[0], angles[1], true, false);
                    state = 2;
                });
                break;
            }

            case 2: {
                // Wait for ew S08
                break;
            }

            case 4: {
                pearl(player.getYRot(), -90f, () -> state = 5);
                break;
            }

            case 5: {
                // Wait for pearl S08s
                break;
            }

            case 6: {
                if (targetRoom != null) {
                    // Loaded we can skip to next, since we break here it will be 1 tick behind but whatever
                    state = 17;
                    break;
                }
                if ((serverTickTimer % 40) < exploreExit.getValue().intValue()) {
                    PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                        if (!SwapManager.swapItem(Items.DIAMOND_SHOVEL)) return;
                        float playerYaw = player.getYRot();

                        float[] angles = EtherUtils.getYawAndPitch(MIDDLE_MAP_COORDS.add(0.0d, player.getY(), 0.0d), false, player, false);
                        float deltaX = (float) (player.getX() - MIDDLE_MAP_COORDS.x);
                        float deltaZ = (float) (player.getZ() - MIDDLE_MAP_COORDS.z);

                        aotv0(8, playerYaw, -90f); // Upped by 1
                        aotv0(Math.round(Mth.sqrt(deltaX * deltaX + deltaZ * deltaZ) / 12f), angles[0], 0.0f);
                        explored = true;
                        state = 10;
                    });
                }
                break;
            }

            case 10: {
                // Await S08 to respawn in start room
                break;
            }

            case 11: {
                if (mc.level == null) break;
                forceNextSneak = true; // Must be setup for etherwarp

                if (waitForGround.getValue() && !player.onGround()) break;

                if (startRoom == null) {
                    startRoom = ScanUtils.getRoomFromPos(player.getBlockX(), player.getBlockZ());
                }
                if (startRoom == null) break;
                if (startRoom.getUniqueRoom().getRotation() == RoomRotation.UNKNOWN) break;

                if (explored) {
                    if (targetRoom == null) findTargetRoom();

                    if (targetRoom == null) {
                        // Still null, we didn't find it
                        RSA.chat("Could not find target room!");
                        state = 31;
                        break;
                    }
                }

                PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                    if (!SwapManager.swapItem(Items.DIAMOND_SHOVEL) || !player.getLastSentInput().shift() || startRoom == null) return;

                    Pos slab = RoomUtils.getRealPosition(getSlabBlockOffset(), startRoom);

                    Block block = mc.level.getBlockState(slab.asBlockPos()).getBlock();
                    if (block == Blocks.AIR) {
                        isLower = true;
                        slab.selfAdd(0.0, -1.0, 0.0);
                    }

                    float[] angles = EtherUtils.getYawAndPitch(slab.asVec3(), true, player, true);
                    SwapManager.sendAirC08(angles[0], angles[1], true, false);
                    state = 13;
                });

                break;
            }

            case 13: {
                // Wait for S08
                break;
            }

            case 15: {
                pearl(player.getYRot(), -90f, () -> state = 16);
                break;
            }

            case 16: {
                // Wait for pearl S08
                break;
            }

            case 17: {
                SwapManager.swapItem(Items.DIAMOND_SHOVEL);

                //todo: this is slow, it should try tp before the dungeon starts for high ping players (me), in theory we should be able to get 0.05s opens
                // *doing this is annoying for low ping
                if (((africanSlavePingMode.getValue() && ticksTilStart != -67 && ticksTilStart <= 0) || Dungeon.isStarted()) && (serverTickTimer % 40) < (40 - bloodLoadTickTime.getValue().intValue())) {
                    ticksTilStart = -67;
                    PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                        if (!SwapManager.swapItem(Items.DIAMOND_SHOVEL)) return;

                        float playerYaw = player.getYRot();

                        Direction dir = getVoidRotation();
                        aotv0(4, dir.toYRot(), 0f);
                        aotv0(10, playerYaw, 90f);

                        Vec3 playerPos = player.position().add(fastRotateVec(dir, 0, 0d, -48d)); // We don't care about the Y

                        float deltaX = (float) ((targetRoom.getX() + 0.5d) - playerPos.x());
                        float deltaZ = (float) ((targetRoom.getZ() + 0.5d) - playerPos.z());

                        float[] angles = EtherUtils.getYawAndPitch(deltaX, 0.0d, deltaZ);
                        // pitch of 3 makes sure you don't teleport up any blocks, as that might cause you to get caught on Withermancer or Lower Blaze
                        aotv0(Math.round(Mth.sqrt(deltaX * deltaX + deltaZ * deltaZ) / 12f), angles[0], 3f);
                        aotv0(5, playerYaw, -90f);

                        // If we can't pearl
                        state = 29;
                        // Can't use pearl() because concurrentModification exception
                        if (!SwapManager.swapItem(i -> i.getItem() == Items.ENDER_PEARL && isNormalEnderpearlID(ItemUtils.getID(i)))) return;
                        if (!SwapManager.sendAirC08(player.getYRot(), -90f, true, true)) {
                            RSA.chat("Pearl failed!");
                            return;
                        }
                        state = 30;
                    });
                }
                break;
            }

            case 29: {
                pearl(player.getYRot(), -90f, () -> state = 30);
                break;
            }

            case 30: {
                // Wait for pearl S08
                break;
            }

            case 31: {
                // Done
                break;
            }

            default:
                break;
        }
    }

    private Direction getVoidRotation() {
        Direction rotation;
        int xIndex = (startRoom.getX() - DungeonScanner.startX) / DungeonScanner.roomSize;
        int zIndex = (startRoom.getZ() - DungeonScanner.startZ) / DungeonScanner.roomSize;
        if (xIndex == 0) {
            rotation = Direction.WEST;
        } else if (zIndex == 0) {
            rotation = Direction.NORTH;
        } else if (xIndex > zIndex) {
            rotation = Direction.EAST;
        } else {
            rotation = Direction.SOUTH;
        }
        return rotation;
    }

    private boolean isNormalEnderpearlID(String s) {
        return s.equals("ENDER_PEARL");
    }

    private static Vec3 fastRotateVec(Direction direction, double x, double y, double z) {
        return switch (direction) {
            case NORTH -> new Vec3(x, y, z);
            case EAST -> new Vec3(-z, y, x);
            case SOUTH -> new Vec3(-x, y, -z);
            case WEST -> new Vec3(z, y, -x);
            default -> Vec3.ZERO;
        };
    }

    private void aotv0(int count, float yaw, float pitch) {
        for (int i = 0; i < count; i++) {
            SwapManager.sendAirC08(yaw, pitch, true, false);
        }
    }

    public boolean isBlinking() {
        return state < 31 && state > -1;
    }

    public void doBlink() {
        resetState();
        state = 0; // Bypass auto
        KeyMapping.releaseAll();
    }

    @SubscribeEvent
    public void onPollInputs(InputPollEvent event) {
        if (!this.isEnabled() || !this.isBlinking() || Dungeon.isInBoss()) return;
        Input input = event.getClientInput();
        if (input.forward() && input.backward() && input.left() && input.right()) {
            this.cancel();
            return;
        }

        Input newInputs = new Input(false, false, false, false, false, this.forceNextSneak, false);
        this.forceNextSneak = false;
        event.getInput().apply(newInputs);
    }

    private void cancel() {
        reset();
        this.state = 31; // End
        RSA.chat("Cancelling blood blink!");
    }

    private void pearl(float yaw, float pitch, Runnable succeed) {
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            if (!SwapManager.swapItem(i -> i.getItem() == Items.ENDER_PEARL && isNormalEnderpearlID(ItemUtils.getID(i)))) return;
            if (!SwapManager.sendAirC08(yaw, pitch, true, false)) return;
            if (succeed != null) succeed.run();
        });
    }

    @SubscribeEvent
    public void onLoadRoom(DungeonEvent.RoomLoad event) {
        if (this.mode.is("Blood")) {
            if (event.getRoom().getData().type() == RoomType.BLOOD) {
                this.targetRoom = event.getRoom();
                RSA.chat("Found blood at : " + targetRoom.getX() + ", " + targetRoom.getZ());
            }
        }
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (Location.getArea() != Island.Dungeon || Minecraft.getInstance().player == null) return;
        if (event.getMessage().getString().equals("Starting in 1 second.")) {
            ticksTilStart = Math.max(20 - this.earlyExit.getValue().intValue(), 0);
            AutoGfs.tryGetItem(16, "ENDER_PEARL", true);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!isBlinking() || Dungeon.isInBoss()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSetTimePacket timePacket) {
            long time = timePacket.gameTime();
            this.serverTickTimer = (int) (time + deathTickOffset.getValue().intValue()) % 40;
        }

        if (packet instanceof ClientboundPlayerPositionPacket s08) {
            switch (state) {
                case 2: {
                    if (proxyPearl.getValue() && Util.isZero()) {
                        sendStartPearling(isLower ? 98 : 99);
                        state = 5;
                        break;
                    }
                    state = 4;
                    break;
                }

                case 13: {
                    if (proxyPearl.getValue() && Util.isZero()) {
                        sendStartPearling(isLower ? 98 : 99);
                        state = 16;
                        break;
                    }
                    state = 15;
                    break;
                }

                case 5: {
                    if (s08.change().position().y <= (isLower ? 97.0 : 98.0)) {
                        if (proxyPearl.getValue() && Util.isZero()) return;
                        state = 4;
                    }
                    else
                        state = 6;
                    break;
                }


                case 16: {
                    if (s08.change().position().y <= (isLower ? 97.0 : 98.0)) {
                        if (proxyPearl.getValue() && Util.isZero()) return;
                        state = 15;
                    }
                    else
                        state = 17;
                    break;
                }

                case 30: {
                    Vec3 pos = s08.change().position();
                    if (!isInRoom(Mth.floor(pos.x()), Mth.floor(pos.z()), targetRoom) || pos.y < targetRoom.getBottom() - 1 || pos.y > targetRoom.getBottom() + 7) break; // 66 bedrock block blood, this stops aotv S08s from getting considered as pearls
                    //System.out.println("Found pearl S08!");
                    if (pos.y <= targetRoom.getBottom() + 1)
                        state = 29;
                    else
                        state = 31;
                    break;
                }

                case 10: {
                    double y = s08.change().position().y;
                    if (y == 76.5 || y == 75.5) // respawn Y, should be reliable *enough
                        state = 11;
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        serverTickTimer++;
        serverTotalTickTimer++;
        if (ticksTilStart != -67) {
            ticksTilStart--;
        }
    }

    private boolean isInRoom(int posX, int posZ, Room room) {
        return Mth.abs(room.getX() - posX) < 16 && Mth.abs(room.getZ() - posZ) < 16;
    }

    private void findTargetRoom() {
        // Blood will already be set if the mode is Blood
        if (!this.mode.is("InstaClear")) return;

        DungeonRoomScore.getOrderedRooms().forEach(rn -> {
            if (!rn.getRoom().isOnBloodRush() && rooms.contains(rn.getRoom().getName())) {
                addRoom(new Entry(
                        rn.getRoom(),
                        rooms.indexOf(rn.getRoom().getName()),
                        rn.getDistance(),
                        rn.isEnd(),
                        rn.isNextToQuiz())
                );
            }
        });

        if (roomPriority.size() < this.priority.getValue().intValue()) {
            RSA.chat("No room found with priority %s to InstaClear!", this.priority.getValue());
            return;
        }

        this.targetRoom = this.roomPriority.get(this.priority.getValue().intValue() - 1).room().getTiles().getFirst();
        if (this.targetRoom != null) {
            RSA.chat("Found a room to insta: \"%s\"", this.targetRoom.getData().name());
            if (RSM.getModule(ClickGUI.class).getDevInfo().getValue()) {
                RSA.chat("InstaClear candidates: %s", this.roomPriority.stream().map(r -> r.room().getName() + (r.quiz ? " (quiz)" : "")).toList());
            }
        } else {
            RSA.chat("Failed to find a target? report pls %s", this.roomPriority);
        }
    }

    private void addRoom(Entry e) {
        if (e.room().isOnBloodRush() || this.roomPriority.stream().anyMatch(e1 -> e1.room().getName().equals(e.room().getName()))) return;
        int total = rooms.size();
        int i = 0;
        while (i < this.roomPriority.size() && this.roomPriority.get(i).getScore(total) >= e.getScore(total)) {
            i++;
        }

        if (i < 5) {
            this.roomPriority.add(i, e);
            if (this.roomPriority.size() > 5) {
                this.roomPriority.remove(5);
            }
        }
    }

    private void sendStartPearling(int roof) {
        if (mc.getConnection() == null) return;
        if (!SwapManager.swapItem("ENDER_PEARL")) {
            RSA.chat("Failed swap to pearl!");
            return;
        }
        TaskComponent.onTick(0, () -> mc.getConnection().send(new ServerboundCustomPayloadPacket(new BloodClipHelperStartPacket(roof))));
    }

    private record Entry(UniqueRoom room, int index, int distance, boolean end, boolean quiz) {
        public int getScore(int total) {
            int score = 0;
            score += ((total * 10) - this.index * 10); // highest index will make this 0
            score += this.distance * 10;
            score += this.end ? 200 : 0;
            if (this.quiz) {
                score += this.end ? 500 : 200;
            }
            return score;
        }
    }
}
