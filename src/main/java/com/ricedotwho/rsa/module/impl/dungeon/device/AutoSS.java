package com.ricedotwho.rsa.module.impl.dungeon.device;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.BlockChangeEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ColourSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledOutlineBox;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Skidded NoobRoutes
@Getter
@ModuleInfo(aliases = "AutoSS", id = "AutoSS", category = Category.DUNGEONS)
public class AutoSS extends Module {
    private static final double REACH = 5.745d * 5.745d;
    private static final Vec3 START_BUTTON = new Vec3(110.875, 121.5, 91.5);
    private static final BlockPos DETECT = new BlockPos(110, 123, 92);
    //private static final AABB DEVICE_AABB = new AABB(START_BUTTON.subtract(-40d, -40d, -40d), START_BUTTON.add(40d, 40d, 40d));

    KeybindSetting resetKey = new KeybindSetting("Reset SS Key", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, null), this::SSR);
    BooleanSetting sendChat = new BooleanSetting("Send SSR Chat Message", true);
    BooleanSetting autoStart = new BooleanSetting("Autostart", true);
    BooleanSetting forceSkyblock = new BooleanSetting("Force Skyblock (Don't keep enabled)", false);

    private final NumberSetting clickDelay = new NumberSetting("Click Delay (MS)", 10.0d, 500.0d, 200.0d, 10.0d);
    private final NumberSetting autoStartDelay = new NumberSetting("Autostart Delay (MS)", 10.0d, 500.0d, 120.0d, 10.0d);

    private final ColourSetting fillColor = new ColourSetting("Button Fill Color", Colour.GREEN.brighter());
    private final ColourSetting outlineColor = new ColourSetting("Button Outline Color", Colour.GREEN.darker());

    private long lastClickTime = System.currentTimeMillis();
    private boolean next = false;
    private int state = 0;
    private boolean doneFirst = false;
    private boolean doingSS = false;
    private final List<BlockPos> clicks = new ArrayList<>();
    private final List<Vec3> allButtons = new ArrayList<>();
    private Vec3 clickedButton;

    public AutoSS() {
        this.registerProperty(
                resetKey,
                sendChat,
                autoStart,
                forceSkyblock,
                clickDelay,
                autoStartDelay
        );
    }

    private void start() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.distanceToSqr(START_BUTTON) > 25) return;

        allButtons.clear();
        //float[] angles = EtherUtils.getYawAndPitch(START_BUTTON, false, player, true);

        RSA.chat("Starting SS!");
        resetState();
        doingSS = true;

        new Thread(() -> {
            try {
                for (int i = 0; i < 2; i++) {
                    reset();
                    clickButton(START_BUTTON);
                    Thread.sleep(autoStartDelay.getValue().longValue());
                }

                doingSS = true;
                clickButton(START_BUTTON);
            } catch (Exception e) {
                RSA.chat("Error Occurred");
            }
        }).start();
    }

    @SubscribeEvent
    public void onRender(Render3DEvent.Last event) {
        if (!areaCheck()) return;
        if (System.currentTimeMillis() - lastClickTime + 1 < clickDelay.getValue().longValue()) return;
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        LocalPlayer player = Minecraft.getInstance().player;

        if (player.distanceToSqr(START_BUTTON) > 25) return;
//        boolean bl = !Minecraft.getInstance().level.getEntitiesOfClass(ArmorStand.class, DEVICE_AABB, stand -> stand.hasCustomName() && stand.getName().getString().contains("Device")).isEmpty();
//        List<ArmorStand> stands = Minecraft.getInstance().level.getEntitiesOfClass(ArmorStand.class, DEVICE_AABB);
//        boolean bl = false;
//        for (ArmorStand stand : stands) {
//            if (!stand.hasCustomName()) continue;
//            //if (stand.getCustomName().contains("Device"))
//            System.out.println(stand.getCustomName());
//        }
//
//        if (!bl) {
//            clicked = false;
//            return;
//        }


        if ((Minecraft.getInstance().level.getBlockState(DETECT).getBlock() == Blocks.STONE_BUTTON) && doingSS) {
            if (!doneFirst && clicks.size() == 3) {
                clicks.removeFirst();
                allButtons.removeFirst();
            }

            doneFirst = true;
            if (state < clicks.size()) {
                BlockPos next = clicks.get(state);
                if (Minecraft.getInstance().level.getBlockState(next).getBlock() == Blocks.STONE_BUTTON) {
                    clickButton(Vec3.atLowerCornerOf(next));
                    state++;
                }
            }
        }

    }

    private boolean areaCheck() {
        if (forceSkyblock.getValue()) return true;
        return Location.getArea().is(Island.Dungeon) && (Location.getFloor() == Floor.F7 || Location.getFloor() == Floor.M7) && DungeonUtils.isPhase(Phase7.P3); // && Dungeon.isInBoss() broke shit when rejoins
    }

    @SubscribeEvent
    public void onRenderButtons(Render3DEvent.Extract event) {
        if (!areaCheck()) return;
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;
        ClientLevel level = Minecraft.getInstance().level;

        if (System.currentTimeMillis() - lastClickTime > clickDelay.getValue().longValue()) clickedButton = null;
        if (Minecraft.getInstance().player.distanceToSqr(START_BUTTON) >= 1600) return;

        if (clickedButton != null) {
            renderButton(level, BlockPos.containing(clickedButton), fillColor.getValue(), outlineColor.getValue());
        }
    }

    private void renderButton(ClientLevel level, BlockPos pos, Colour colorFill, Colour colorOutline) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) return;

        Renderer3D.addTask(new FilledOutlineBox(shape.bounds().move(pos), colorFill, colorOutline, false));
    }


    private void clickButton(Vec3 vec3) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.distanceToSqr(vec3) > REACH) {
            RSA.chat("Button too far!");
            return;
        }
        lastClickTime = System.currentTimeMillis();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> clickButton0(vec3));
    }

    private void clickButton0(Vec3 vec3) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.distanceToSqr(vec3) > REACH) {
            RSA.chat("Button too far!");
            return;
        }

        clickedButton = vec3;
        SwapManager.sendBlockC08(vec3, Direction.WEST, true, false);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        resetState();
    }

    @SubscribeEvent
    public void onChatMessage(ChatEvent.Chat event) {
        if (!areaCheck() || !autoStart.getValue()) return;
        if (Minecraft.getInstance().player == null) return;
        String msg = event.getMessage().getString();

        if (!msg.equals("[BOSS] Goldor: Who dares trespass into my domain?")) return;
        start();
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        BlockPos pos = event.getBlockPos();
        if (event.getNewState().getBlock() != Blocks.SEA_LANTERN) return;
        if (!areaCheck()) return;
        if (pos.getX() != 111 || pos.getY() < 120 || pos.getY() > 123 || pos.getZ() < 92 || pos.getZ() > 95) return;

        BlockPos button = new BlockPos(110, event.getBlockPos().getY(), event.getBlockPos().getZ());

        if (clicks.size() == 2) {
            if (clicks.getFirst().equals(button) && !doneFirst) {
                doneFirst = true;
                clicks.removeFirst();
                allButtons.removeFirst();
            }
        }

        if (!clicks.contains(button)) {
            //devMessage("Added to clicks: x: ${event.pos.x}, y: ${event.pos.y}, z: ${event.pos.z}")
            state = 0;
            clicks.add(button);
            allButtons.add(Vec3.atLowerCornerOf(button));
        }
    }



    public void SSR() {
        if (!areaCheck()) return;
        if (sendChat.getValue() && Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand("pc SSRS SSRS SSRS!");
        }
        start();
    }

    public void resetState() {
        allButtons.clear();
        clicks.clear();
        next = false;
        state = 0;
        doneFirst = false;
        doingSS = false;
    }



    @Override
    public void onEnable() {
        resetState();
    }

}
