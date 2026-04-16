package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.pathfinding.Goal;
import com.ricedotwho.rsa.component.impl.pathfinding.GoalDungeonXYZ;
import com.ricedotwho.rsa.event.impl.RawTickEvent;
import com.ricedotwho.rsa.module.impl.dungeon.DynamicRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitClick;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitSecrets;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.BatNode;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.BreakNode;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.RoomData;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.DungeonEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.DefaultGroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ColourSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.SaveSetting;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.FileUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Getter
@ModuleInfo(aliases = "Auto Routes", id = "Autoroutes", category = Category.DUNGEONS)
public class AutoRoutes extends Module implements Accessor {
    private final HashMap<RoomData, List<Node>> activeNodes = new HashMap<>();
    private final HashMap<String, List<Node>> redoMap = new HashMap<>();

    @Getter private static final BooleanSetting centerOnly = new BooleanSetting("Center Only", false);
    @Getter private static final BooleanSetting zeroTickBreak = new BooleanSetting("0t Break", false);
    @Getter private static final BooleanSetting use1_8Height = new BooleanSetting("Use 1.8 height for placing node", true);
    private final BooleanSetting editMode = new BooleanSetting("Edit Mode", false);
    private final KeybindSetting triggerBind = new KeybindSetting("Trigger Bind", new Keybind(GLFW.GLFW_MOUSE_BUTTON_1, true, this::onTrigger));
    private final KeybindSetting triggerBindGui = new KeybindSetting("Trigger Bind Gui", new Keybind(GLFW.GLFW_KEY_UNKNOWN, true, false, false, () -> {
        if (mc.screen != null) this.onTrigger();
    }));
    private final KeybindSetting addBlockBind = new KeybindSetting("Add Block Bind", new Keybind(GLFW.GLFW_KEY_SEMICOLON, false, this::addBlockToInNode));
    private final KeybindSetting routeStartBind = new KeybindSetting("Route to start Bind", new Keybind(GLFW.GLFW_KEY_ENTER, false, this::routeToStart));

    // uhh surely this won't cause issues...
    private final DefaultGroupSetting render = new DefaultGroupSetting("Render", this);
    @Getter private static final BooleanSetting startDepth = new BooleanSetting("Start Depth", false);
    @Getter private static final BooleanSetting nodeDepth = new BooleanSetting("Node Depth", true);
    @Getter private static final ColourSetting startColour = new ColourSetting("Start", Colour.GREEN.copy());
    @Getter private static final ColourSetting etherwarpColour = new ColourSetting("Etherwarp", Colour.CYAN.copy());
    @Getter private static final ColourSetting breakColour = new ColourSetting("Break", Colour.YELLOW.copy());
    @Getter private static final ColourSetting boomColour = new ColourSetting("Boom", Colour.RED.copy());
    @Getter private static final ColourSetting batColour = new ColourSetting("Bat", Colour.BLUE.copy());
    @Getter private static final ColourSetting aotvColour = new ColourSetting("Aotv", Colour.MAGENTA.copy());
    @Getter private static final ColourSetting useColour = new ColourSetting("Use", Colour.WHITE.copy()); // idk

    private final SaveSetting<HashMap<String, List<Node>>> data = new SaveSetting<>("Nodes", "routes", "routes.json", HashMap::new,
            new TypeToken<HashMap<String, List<Node>>>() {}.getType(),
            new GsonBuilder()
                    .registerTypeHierarchyAdapter(Node.class, new NodeAdapter())
                    .setPrettyPrinting().create(),
            true, this::reload, null);

    private int tickTime = 0;
    private boolean forceNextNotSneak = false;
    private Node realNode;
    private Node inNode;
    @Getter
    private boolean isRouting = false;
    private byte crouchDataShiftRegister = 0;
    public int lastBlockC08 = 0;

    private Class<? extends Node> lastType = null;

    // Player inputs are sent after C08s and keybinding events, in level.tickEntities

    private static final Set<String> SECRET_NAMES  = Set.of(
            "Health Potion VIII Splash Potion",
            "Healing Potion 8 Splash Potion",
            "Healing Potion VIII Splash Potion",
            "Healing VIII Splash Potion",
            "Healing 8 Splash Potion",
            "Decoy",
            "Inflatable Jerry",
            "Spirit Leap",
            "Trap",
            "Training Weights",
            "Defuse Kit",
            "Dungeon Chest Key",
            "Treasure Talisman",
            "Revive Stone",
            "Architect's First Draft",
            "Secret Dye",
            "Candycomb"
    );

    public AutoRoutes() {
        this.registerProperty(
                editMode,
                centerOnly,
                zeroTickBreak,
                use1_8Height,
                triggerBind,
                triggerBindGui,
                addBlockBind,
                routeStartBind,
                data,
                render
        );
        render.add(startDepth, nodeDepth, startColour, etherwarpColour, breakColour, boomColour, batColour, aotvColour);
        this.inNode = null;
        this.realNode = null;
        createBackup();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.inNode = null;
        this.realNode = null;
        this.activeNodes.clear();
        this.crouchDataShiftRegister = 0;
        this.lastBlockC08 = 0;
    }

    @SubscribeEvent
    public void onRoomEnter(DungeonEvent.ChangeRoom event) {
        if (event.unique == null || event.room == null || event.oldRoom == null) return;
        Room room = event.getRoom();
        this.inNode = null;
        this.realNode = null;
        if (activeNodes.containsKey(room.getData())) return;
        cacheRoomNodes(room);
    }

    @SubscribeEvent
    public void onRender(Render3DEvent.Extract event) {
        try {
            if (!Location.getArea().is(Island.Dungeon) || Map.getCurrentRoom() == null) return;
            Room currentRoom = Map.getCurrentRoom();
            List<Node> nodes = this.activeNodes.get(currentRoom.getData());
            if (nodes == null || nodes.isEmpty()) return;
            nodes.forEach(n -> n.render(nodeDepth.getValue() && (!n.isStart() || startDepth.getValue())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientTickStart(RawTickEvent event) {
        lastBlockC08--;
        this.isRouting = false;
        if (!Location.getArea().is(Island.Dungeon)) return;
        tickTime++;

        if (hasGuiOpen()) return;
        if (this.editMode.getValue() || Map.getCurrentRoom() == null || Minecraft.getInstance().player == null) return;

        Room currentRoom = Map.getCurrentRoom();
        List<Node> nodes = this.activeNodes.get(currentRoom.getData());
        if (nodes == null || nodes.isEmpty()) {
            inNode = null;
            return;
        }
        Pos playerPos = new Pos(Minecraft.getInstance().player.position());
        Pos realPos = new Pos(playerPos);


        nodes.forEach(n -> n.updateNodeState(playerPos, tickTime));

        this.lastType = null;

        while (true) {
            if (!handleQueue(playerPos, realPos, nodes)) break;
        }
    }

    public boolean willBeCrouchingForEtherwarpEvaluation() {
        return ((this.crouchDataShiftRegister >> 1) & 1) == 1;
    }

    @SubscribeEvent
    public void onPollInputs(InputPollEvent event) {
        if (!this.isRouting() || !Location.getArea().is(Island.Dungeon) || hasGuiOpen()) return;
        Input oldInputs = event.getClientInput();

//        ChatUtils.chat("Poll Input: " + this.forceNextNotSneak);

        Input newInputs = new Input(oldInputs.forward(), oldInputs.backward(), oldInputs.left(), oldInputs.right(), oldInputs.jump(), !this.forceNextNotSneak, oldInputs.sprint());
        this.forceNextNotSneak = false;
        event.getInput().apply(newInputs);
    }

    public List<Node> getStartNodes(UniqueRoom uniqueRoom) {
        if (uniqueRoom.getTiles().isEmpty()) return null;
        Room room = uniqueRoom.getMainRoom();
        if (room == null || room.getData() == null) return null;

        if (activeNodes.containsKey(room.getData())) return activeNodes.get(room.getData()).stream().filter(Node::isStart).toList();

        List<Node> rawNodes = this.data.getValue().get(uniqueRoom.getName());
        if (rawNodes == null || rawNodes.isEmpty()) return null;

        List<Node> ret = rawNodes.stream().filter(Node::isStart).toList();
        ret.forEach(n -> n.calculate(uniqueRoom));
        return ret;
    }

    private void cacheRoomNodes(Room room) {
        List<Node> nodes = data.getValue().get(room.getData().name());
        if (nodes == null || nodes.isEmpty()) return;
        UniqueRoom uniqueRoom = room.getUniqueRoom();
        nodes.forEach(n -> n.calculate(uniqueRoom));
        activeNodes.put(room.getData(), nodes);
    }

    public void load() {
        data.load();
    }

    private void reload() {
        this.activeNodes.clear();
        this.inNode = null;
        this.realNode = null;
        if (!Location.getArea().is(Island.Dungeon)) return;
        Room room = Map.getCurrentRoom();
        if (room == null || room.getUniqueRoom() == null) return;
        cacheRoomNodes(room);
    }

    private boolean hasGuiOpen() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> && false;
    }

    public boolean clearNodes(UniqueRoom uniqueRoom) {
        if (Minecraft.getInstance().player == null || !this.activeNodes.containsKey(uniqueRoom.getMainRoom().getData())) return false;
        List<Node> nodes = activeNodes.get(uniqueRoom.getMainRoom().getData());
        if (nodes.isEmpty()) return false;
        nodes.clear();
        save();
        return true;
    }

    public boolean removeNearest(UniqueRoom uniqueRoom) {
        if (Minecraft.getInstance().player == null || !this.activeNodes.containsKey(uniqueRoom.getMainRoom().getData())) return false;
        List<Node> nodes = activeNodes.get(uniqueRoom.getMainRoom().getData());
        if (nodes.isEmpty()) return false;
        // Once again don't need to use saveNodes because they share the same list, if you're trying to remove them they should already be loaded

        int bestIndex = -1;
        double bestDistance = Double.MAX_VALUE;
        Vec3 playerPos = Minecraft.getInstance().player.position();
        for (int i = 0; i < nodes.size(); i++) {
            double d = nodes.get(i).getRealPos().squaredDistanceTo(playerPos);
            if (d >= bestDistance) continue;
            bestIndex = i;
            bestDistance = d;
        }

        if (bestIndex < 0) return false;
        nodes.remove(bestIndex);
        save();
        return true;
    }

    public boolean undoNode(UniqueRoom uniqueRoom) {
        if (Minecraft.getInstance().player == null || !this.activeNodes.containsKey(uniqueRoom.getMainRoom().getData())) return false;
        List<Node> nodes = activeNodes.get(uniqueRoom.getMainRoom().getData());
        if (nodes.isEmpty()) return false;

        if (!redoMap.containsKey(uniqueRoom.getName())) {
            redoMap.put(uniqueRoom.getName(), new ArrayList<>());
        }

        Node node = nodes.removeLast();
        redoMap.get(uniqueRoom.getName()).add(node);
        save();

        RSA.chat("Undid %s at %s", node.getName(), node.getRealPos().toChatString());
        return true;
    }

    public boolean redoNode(UniqueRoom uniqueRoom) {
        if (Minecraft.getInstance().player == null || !this.activeNodes.containsKey(uniqueRoom.getMainRoom().getData())) return false;
        List<Node> nodes = activeNodes.get(uniqueRoom.getMainRoom().getData());
        if (!redoMap.containsKey(uniqueRoom.getName())) return false;
        List<Node> redo = redoMap.get(uniqueRoom.getName());
        if (redo.isEmpty()) return false;
        Node node = redo.removeLast();
        nodes.add(node);
        save();

        RSA.chat("Redid %s at %s", node.getName(), node.getRealPos().toChatString());
        return true;
    }

    public void addNode(Node node, UniqueRoom uniqueRoom) {
        this.data.getValue().putIfAbsent(uniqueRoom.getName(), new ArrayList<>());
        List<Node> nodes = data.getValue().get(uniqueRoom.getName());
        node.calculate(uniqueRoom);
        nodes.add(node); // Don't add to active nodes list because they share the same list objects
        if (!activeNodes.containsKey(uniqueRoom.getMainRoom().getData())) {
            // But we might need to add the list in the first place
            activeNodes.put(uniqueRoom.getMainRoom().getData(), nodes);
        }
        save();
    }

    public void setForceSneak(boolean bl) {
        this.forceNextNotSneak = bl;
    }

    public void onTrigger() {
        if (!this.isEnabled() || !Location.getArea().is(Island.Dungeon) || Map.getCurrentRoom() == null) return;

        List<Node> nodes = this.activeNodes.get(Map.getCurrentRoom().getData());
        if (nodes != null)
            nodes.forEach(Node::reset);
        if (this.realNode instanceof BatNode) this.realNode.setTriggered(true);
        if (this.realNode == null || !this.realNode.hasAwaits()) return;
        this.realNode.getAwaitManager().consume(AwaitClick.class, true);
        this.realNode.getAwaitManager().consume(AwaitSecrets.class, 100); // Skip secret
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (!Location.getArea().is(Island.Dungeon)  || Minecraft.getInstance().level == null) return;
        if (event.getPacket() instanceof ServerboundPlayerInputPacket(Input input)) {
            this.crouchDataShiftRegister <<= 1;
            this.crouchDataShiftRegister |= (byte) (input.shift() ? 1 : 0);
            return;
        }


        if (this.realNode == null || Map.getCurrentRoom() == null) return;
        if (!this.realNode.hasAwaits() || !this.realNode.getAwaitManager().hasAwait(AwaitType.SECRETS)) return;
        if (!(event.getPacket() instanceof ServerboundUseItemOnPacket useItemOnPacket)) return;
        Block block = Minecraft.getInstance().level.getBlockState(useItemOnPacket.getHitResult().getBlockPos()).getBlock();
        if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.PLAYER_HEAD && block != Blocks.LEVER) return;
        this.realNode.getAwaitManager().consume(AwaitSecrets.class, 1);
        this.lastBlockC08 = 2; // Hypixel voids C08s sometimes after secret auraing
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!Location.getArea().is(Island.Dungeon)
                || Map.getCurrentRoom() == null
                || this.realNode == null
                || mc.level == null
                || !this.realNode.hasAwaits()
                || !this.realNode.getAwaitManager().hasAwait(AwaitType.SECRETS)
        ) return;

        if (event.getPacket() instanceof ClientboundTakeItemEntityPacket packet) {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(packet.getItemId());
            if (!(entity instanceof ItemEntity itemEntity)) return;
            String name = ChatFormatting.stripFormatting(itemEntity.getItem().getHoverName().getString());
            if (!SECRET_NAMES.contains(name)) return;
            this.realNode.getAwaitManager().consume(AwaitSecrets.class, 1);
        } else if (event.getPacket() instanceof ClientboundRemoveEntitiesPacket packet) {
            packet.getEntityIds().forEach(id -> {
                Entity entity = mc.level.getEntity(id);
                if (entity instanceof ItemEntity itemEntity
                        && entity.distanceToSqr(mc.player) < 64
                        && SECRET_NAMES.contains(ChatFormatting.stripFormatting(itemEntity.getItem().getHoverName().getString()))) {
                    this.realNode.getAwaitManager().consume(AwaitSecrets.class, 1);
                }
            });
        }
    }

    private void trySetInNode(Node node) {
        if (this.inNode == node) return;

        this.inNode = node;
        if (node.hasAwaits()) node.getAwaitManager().onEnterNode();
    }

    public boolean handleQueue(Pos playerPos, Pos realPos, List<Node> nodes) {
        List<Node> activeNodes = new ArrayList<>();

        // real node
        this.realNode = null;
        for (Node node : nodes) {
            if (!node.isInNode(realPos)) continue;
            this.isRouting = true;
            if (node.isTriggered() || node.hasRanThisTick(tickTime)) continue;
            if (this.realNode == null || this.realNode.getPriority() < node.getPriority()) this.realNode = node;
        }

        for (Node node : nodes) {
            if (!node.isInNode(playerPos)) continue;
            this.isRouting = true;
            if (node.isTriggered() || node.hasRanThisTick(tickTime)) continue;
            activeNodes.add(node);
        }

        if (activeNodes.isEmpty()) {
            this.inNode = null;
            return false;
        }

        activeNodes.sort(Comparator.comparingInt(n -> ((Node) n).getPriority()).reversed());

        Node node = activeNodes.getFirst();
        trySetInNode(node);

        if (node.shouldAwait() || lastBlockC08 > 0 || (lastType != null && lastType != node.getClass())) return false;

        node.preTrigger(tickTime);
        boolean bl = node.run(playerPos);
        if (bl) lastType = node.getClass();
        return bl;
    }

    private void addBlockToInNode() {
        Room currentRoom = Map.getCurrentRoom();
        if (!Location.getArea().is(Island.Dungeon) || Dungeon.isInBoss() || currentRoom == null || this.activeNodes.isEmpty() || mc.player == null || !this.activeNodes.containsKey(currentRoom.getData())) return;
        Pos playerPos = new Pos(mc.player.position());
        Optional<BreakNode> opt = this.activeNodes.get(currentRoom.getData())
                .stream().filter(n -> n.isInNode(playerPos) && n instanceof BreakNode).map(n -> (BreakNode) n).findFirst();
        if (opt.isEmpty()) {
            RSA.chat("Not in break node");
            return;
        }
        opt.get().addOrRemoveBlock();
    }

    private void routeToStart() {
        if (mc.player == null || hasGuiOpen()) return;
        if (!Location.getArea().is(Island.Dungeon) || Dungeon.isInBoss() || this.activeNodes.isEmpty()) return;

        Room currentRoom = Map.getCurrentRoom();
        if (currentRoom == null || !this.activeNodes.containsKey(currentRoom.getData())) return;

        Vec3 startPos = mc.player.position();

        Node closestStart = this.activeNodes.get(currentRoom.getData())
                .stream()
                .filter(Node::isStart)
                .min(Comparator.comparingDouble(n -> n.getRealPos().squaredDistanceTo(startPos)))
                .orElse(null);

        if (closestStart == null) {
            RSA.chat("Couldn't find a start node.");
            return;
        }

        Pos goalPos = closestStart.getRealPos();
        Goal goal = GoalDungeonXYZ.create(goalPos.asBlockPos().below(goalPos.y % 1 == 0 ? 1 : 0));

        DynamicRoutes dynamicRoutes = RSM.getModule(DynamicRoutes.class);
        if (!dynamicRoutes.isEnabled()) {
            RSA.chat("Couldn't use dynamic routes (disabled).");
            return;
        }

        dynamicRoutes.cancelPathing();
        dynamicRoutes.executePath(startPos, goal);
    }

    public void save() {
        data.save();
    }

    public void createBackup() {
        File backUpDir = FileUtils.getCategoryFolder(data.getPath() + "/backups");
        List<Long> timeStamps = getTimeStamps(backUpDir);
        pruneBackups(backUpDir, timeStamps, 9);

        File newBackup = new File(backUpDir, System.currentTimeMillis() + ".json");
        try {
            Files.copy(data.getFile().toPath(), newBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            RSA.getLogger().error("Failed to create autoroute backup!", e);
            return;
        }
        RSA.getLogger().info("Created autoroute backup");
    }

    private static @NotNull List<Long> getTimeStamps(File backUpDir) {
        List<Long> timeStamps = new ArrayList<>();

        for (File file : Objects.requireNonNull(backUpDir.listFiles())) {
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            String timeString = name.substring(0, name.length() - 5);
            if (timeString.isEmpty()) continue;
            try {
                timeStamps.add(Long.parseLong(timeString));
            } catch (NumberFormatException ignored) {
            }
        }
        return timeStamps;
    }

    private static void pruneBackups(File backUpDir, List<Long> timeStamps, int maxSize) {
        if (timeStamps.size() <= maxSize) return;

        // Increasing order
        timeStamps.sort(Long::compareTo);

        for (int i = 0; i < timeStamps.size() - maxSize; i++) {
            Long ts = timeStamps.get(i);
            File file = new File(backUpDir, ts + ".json.bak");

            if (file.exists() && !file.delete()) {
                RSA.getLogger().error("Failed to delete old backup: {}", file.getName());
            }
        }
    }
}