package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.pathfinding.EtherwarpPathfinder;
import com.ricedotwho.rsa.component.impl.pathfinding.Goal;
import com.ricedotwho.rsa.component.impl.pathfinding.Path;
import com.ricedotwho.rsa.component.impl.pathfinding.PathfindingCalculationContext;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.Node;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.DynamicEtherwarpNode;
import com.ricedotwho.rsa.screen.MapScreen;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.DefaultGroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ColourSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.ChatUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ModuleInfo(aliases = "Dynamic Routes", id = "Dynamicroutes", category = Category.MOVEMENT)
public class DynamicRoutes extends Module {
    private final UniqueRoom EMPTY_UNIQUE;

    @Getter
    private final List<Node> nodes = new ArrayList<>();

    private final BooleanSetting oneUse = new BooleanSetting("Delete After Use", true);
    private final BooleanSetting editMode = new BooleanSetting("Edit Mode", false);

    private final DefaultGroupSetting render = new DefaultGroupSetting("Render", this);
    private static final BooleanSetting nodeDepth = new BooleanSetting("Node Depth", true);

    @Getter
    private static final ColourSetting nodeColor = new ColourSetting("Color", Colour.ORANGE);

    private final DefaultGroupSetting pathfinder = new DefaultGroupSetting("Pathfinding", this);
    private final NumberSetting heuristicThreshold = new NumberSetting("Heuristic Threshold", 0.1, 5, 0.5, 0.1);
    private final NumberSetting threadCount = new NumberSetting("Thead Count", 1d, 64d, 8d, 1d);
    private final NumberSetting nodeCost = new NumberSetting("Node Cost", 1d, 10000d, 500d, 1d);
    private final NumberSetting yawStep = new NumberSetting("Yaw Step", 0.1d, 10d, 4d, 0.1d);
    private final NumberSetting pitchStep = new NumberSetting("Pitch Step", 0.1d, 10d, 2d, 0.1d);

    private final DefaultGroupSetting instaclear = new DefaultGroupSetting("Routing", this);
    private final NumberSetting tickOffset = new NumberSetting("Tick Offset", -5d, 10d, 1d, 1d);
    KeybindSetting mapKey = new KeybindSetting("Open Map Key", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, null), this::openMap);
    private final BooleanSetting forceLoad = new BooleanSetting("Keep Chunks Loaded", true);
    private final BooleanSetting fullBlocks = new BooleanSetting("Only Full Blocks", true);


    private EtherwarpPathfinder currentPathfinder;
    private Thread pathfinderThread;
    private final List<Goal> pathQueue;
    private int queueSequence = 0;


    private int tickTime = 0;

    @Getter
    private boolean isRouting = false;
    private byte awaitState = 0;

    public DynamicRoutes() {
        this.registerProperty(
                editMode,
                oneUse,
                render,
                pathfinder,
                instaclear
        );
        this.pathQueue = new ArrayList<>();
        render.add(nodeDepth, nodeColor);
        pathfinder.add(threadCount, heuristicThreshold, nodeCost, yawStep, pitchStep);
        instaclear.add(tickOffset);
        instaclear.add(mapKey);
        instaclear.add(forceLoad);
        instaclear.add(fullBlocks);

        EMPTY_UNIQUE = UniqueRoom.emptyUnique();
    }

    public void openMap() {
        if (!Location.getArea().is(Island.Dungeon) || Dungeon.isInBoss()) {
            ChatUtils.chat("Cannot map pathfind here!");
            return;
        }

        Minecraft.getInstance().setScreen(new MapScreen(this));
    }

    @SubscribeEvent
    public void onReceiveChunkPacket(PacketEvent.Receive event) {
        if (Location.getArea() != Island.Dungeon || !forceLoad.getValue() || !(event.getPacket() instanceof ClientboundForgetLevelChunkPacket)) return;
        event.setCancelled(true);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.nodes.clear();
        this.awaitState = 0;
    }

    @SubscribeEvent
    public void onRender(Render3DEvent.Extract event) {
        if (nodes.isEmpty()) return;
        nodes.forEach(n -> n.render(nodeDepth.getValue()));
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        // We need to await S08s before we can actually wait for room to load
        // Although this is 2 way time, we could just wait x server ticks instead
        // However that might fail if server lags
        if (awaitState != 1 || !(event.getPacket() instanceof ClientboundPlayerPositionPacket packet)) return;
        awaitState = 2;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (awaitState < 2) return;
        awaitState++;
        if (awaitState > 9) awaitState = 0;

//        if ((event.getTime() + tickOffset.getValue().intValue()) % 5 != 0) return; // Hypixel checks for room opens every 5 ticks
//        awaitState = 0;s
    }

    @SubscribeEvent
    public void onClientTickStart(ClientTickEvent.Start event) {
        tickTime++;
        //if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getSelectedSlot() == 1) ChatUtils.chat(ScanUtils.getRoomFromPos((int) Minecraft.getInstance().player.getX(), (int) Minecraft.getInstance().player.getZ()).getData().name() + ", " + isRouting);

        if (awaitState != 0) {
            return;
        }
        this.isRouting = false;


        if (this.editMode.getValue() || Minecraft.getInstance().player == null || nodes.isEmpty()) return;

        Pos playerPos = new Pos(Minecraft.getInstance().player.position());
        nodes.forEach(n -> n.updateNodeState(playerPos, tickTime));

        while (true) {
            if (!handleQueue(playerPos, nodes)) break;
        }
    }

    @SubscribeEvent
    public void onPollInputs(InputPollEvent event) {
        if (!this.isRouting()) return;
        Input oldInputs = event.getClientInput();

        Input newInputs = new Input(oldInputs.forward(), oldInputs.backward(), oldInputs.left(), oldInputs.right(), oldInputs.jump(), true, oldInputs.sprint());
        event.getInput().apply(newInputs);
    }

    public void pathGoals(BlockPos startPos, List<? extends Goal> goals) {
        if ((!pathQueue.isEmpty()) || goals.isEmpty()) return;
        this.pathQueue.addAll(goals);
        pathNextQueued(startPos);
    }

    private void pathNextQueued(BlockPos pos) {
        if (pathQueue.isEmpty()) return;
        Goal goal = pathQueue.removeFirst();
        if (goal == null) return;

        BlockPos endPos = goal.getEndBlockPos();
        boolean bl = (endPos == null || Minecraft.getInstance().level == null || Minecraft.getInstance().level.getBlockState(endPos).isCollisionShapeFullBlock(Minecraft.getInstance().level, endPos)); // Check so it doesn't get stuck
        PathfindingCalculationContext ctx = new PathfindingCalculationContext(new Pos(pos).add(0.5, 0, 0.5), this.threadCount.getValue().intValue(), this.yawStep.getValue().floatValue(), this.pitchStep.getValue().floatValue(), this.nodeCost.getValue().floatValue(), this.heuristicThreshold.getValue().floatValue(), this.fullBlocks.getValue() && bl);
        executePath(new EtherwarpPathfinder(ctx, goal), (path) -> this.pathNextQueued(path.getEndNode().getBlockPos()));
    }

    public void executePath(Vec3 startPos, Goal goal) {
        if (goal == null) return;
        BlockPos endPos = goal.getEndBlockPos();
        boolean bl = (endPos == null || Minecraft.getInstance().level == null || Minecraft.getInstance().level.getBlockState(endPos).isCollisionShapeFullBlock(Minecraft.getInstance().level, endPos)); // Check so it doesn't get stuck

        PathfindingCalculationContext ctx = new PathfindingCalculationContext(new Pos(startPos), this.threadCount.getValue().intValue(), this.yawStep.getValue().floatValue(), this.pitchStep.getValue().floatValue(), this.nodeCost.getValue().floatValue(), this.heuristicThreshold.getValue().floatValue(), this.fullBlocks.getValue() && bl);
        executePath(new EtherwarpPathfinder(ctx, goal), null);
    }

    public void executePath(EtherwarpPathfinder pathfinder, Consumer<Path> callback) {
        if (this.currentPathfinder != null) {
            RSA.chat("Pathfinder already active!");
            return;
        }
        this.queueSequence = 0;

        this.currentPathfinder = pathfinder;
        this.pathfinderThread = new Thread(() -> {
            Path path = pathfinder.calculate();
            if (path == null) {
                this.cancelPathing();
                return;
            }

            this.queueSequence = path.consumeNodes(this::addNode, DynamicEtherwarpNode::fromPos, this.queueSequence);
            this.currentPathfinder = null;
            if (callback != null) callback.accept(path);
            this.pathfinderThread = null;
        });
        this.pathfinderThread.setDaemon(true);
        this.pathfinderThread.start();
    }


    public boolean isPathing() {
        return this.currentPathfinder != null || this.pathfinderThread != null;
    }

    public boolean cancelPathing() {
        this.pathQueue.clear();
        if (!isPathing()) return false;

        if (this.currentPathfinder != null) {
            this.currentPathfinder.cancel();
            this.currentPathfinder = null;
        }

        if (this.pathfinderThread != null) {
            this.pathfinderThread.interrupt();
            try {
                this.pathfinderThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.pathfinderThread = null;
        }
        return true;
    }

    public boolean clearNodes() {
        nodes.clear();
        return true;
    }

    public boolean removeNearest() {
        if (Minecraft.getInstance().player == null) return false;
        if (nodes.isEmpty()) return false;

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
        return true;
    }

    public boolean addNode(LocalPlayer player) {
       Node node = DynamicEtherwarpNode.supply(EMPTY_UNIQUE, player);

       addNode(node);
       return true;
    }

    public void addNode(Node node) {
        node.calculate(EMPTY_UNIQUE);
        this.nodes.add(node);
    }

    public boolean handleQueue(Pos playerPos, List<Node> nodes) {
        int bestIndex = -1;
        int bestPriority = Integer.MIN_VALUE;

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (!n.isInNode(playerPos)) continue;
            this.isRouting = true;

            if (n.isTriggered() || n.hasRanThisTick(tickTime) || n.getPriority() < bestPriority) continue;
            bestPriority = n.getPriority();
            bestIndex = i;
        }

        if (bestIndex < 0) {
            return false;
        }

        Node node = nodes.get(bestIndex);

        node.preTrigger(tickTime);
        boolean bl = node.run(playerPos);

        if (bl) {
            if (this.oneUse.getValue()) nodes.remove(bestIndex);

            if (node.shouldAwait()) {
                this.awaitState = 1;
                return false;
            }
        }

        return bl;
    }
}
