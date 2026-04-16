package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.SubModule;
import com.ricedotwho.rsm.module.api.SubModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ButtonSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.SaveSetting;
import com.ricedotwho.rsm.utils.ItemUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SubModuleInfo(name = "Movement")
public class MovementRecorder extends SubModule<AutoP3> {
    private final KeybindSetting recordKey = new KeybindSetting("Record", new Keybind(InputConstants.UNKNOWN, this::toggleRecording));
    private final ButtonSetting prune = new ButtonSetting("Prune Inputs", "Prune", this::prune);
    @Getter
    private static final SaveSetting<List<PlayerInput>> data = new SaveSetting<>("Route", "dungeon/recorder", "inputs.json", ArrayList::new,
            new TypeToken<List<PlayerInput>>() {}.getType(),
            new GsonBuilder()
                    .registerTypeHierarchyAdapter(PlayerInput.class, new PlayerInputAdapter())
                    .setPrettyPrinting().create(),
            true, null, null);

    private static State state = State.IDLE;
    private static final List<PlayerInput> recorded = new ArrayList<>();
    @Setter
    @Getter
    private static int playIndex = 0;

    public MovementRecorder(AutoP3 module) {
        super(module);
        registerProperty(recordKey, prune, data);
    }

    @Override
    public void reset() {
        playIndex = 0;
        recorded.clear();
        state = State.IDLE;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        reset();
    }

    private void toggleRecording() {
        switch (state) {
            case RECORDING -> {
                state = State.IDLE;
                data.setValue(recorded);
                data.save();
                AutoP3.modMessage("Saved %s tick recording to \"%s.%s\"", recorded.size(), data.getFileName(), data.getExt());
            }
            case IDLE -> {
                recorded.clear();
                state = State.RECORDING;
                AutoP3.modMessage("Started recording!");
            }
            case PLAYING -> AutoP3.modMessage("Cannot record while playing!");
        }
    }

    public static boolean isIdle() {
        return state == State.IDLE;
    }
    private void prune() {
        List<PlayerInput> inputs = data.getValue();
        int changed = 0;
        while (inputs.size() > 1 && inputs.get(0).equals(inputs.get(1))) {
            inputs.remove(1);
            changed++;
        }

        while (inputs.size() > 1 &&
                inputs.getLast().equals(inputs.get(inputs.size() - 2))) {
            inputs.removeLast();
            changed++;
        }
        data.save();
        AutoP3.modMessage("Removed %s ticks", changed);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Send event) {
        if (state != State.RECORDING || mc.player == null || !(event.getPacket() instanceof ServerboundUseItemPacket packet) || recorded.isEmpty()) return;
        ItemStack held = mc.player.getItemInHand(packet.getHand());
        String itemId = ItemUtils.getID(held);
        if (itemId.isBlank() || packet.getYRot() == 0.0 && packet.getXRot() == 0.0) return;
        PlayerInput last = recorded.getLast();
        last.using = true;
        last.useItem = new UseItem(itemId, packet.getYRot(), packet.getXRot());
    }

    @SubscribeEvent
    public void record(InputPollEvent event) {
        if (mc.player == null) return;
        if (!event.isActualLocalPlayer() && state == State.PLAYING) {
            event.getInput().forward(true);
            event.getInput().left(true);
            event.getInput().jump(true);
            return;
        }
        Input in = event.getClientInput();
        if (state == State.RECORDING) {
            PlayerInput next = new PlayerInput(mc.gameRenderer.getMainCamera().yaw(), mc.gameRenderer.getMainCamera().getXRot(), in);
            recorded.add(next);
        } else if (state == State.PLAYING) {
            if (in.forward() || in.backward() || in.left() || in.right() || in.shift()) {
                AutoP3.modMessage("Cancelling movement");
                reset();
                return;
            }

            List<PlayerInput> inputs = data.getValue();
            if (playIndex >= inputs.size()) {
                state = State.IDLE;
                AutoP3.modMessage("Done playing %s", data.getFileName());
                return;
            }
            PlayerInput next = inputs.get(playIndex);
            event.getInput().apply(next.input());
            mc.player.setYRot(next.yaw);
            mc.player.setXRot(next.pitch);

            if (next.using && next.useItem != null) {
                if (SwapManager.swapItem(next.useItem.item)) {
                    PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> SwapManager.sendAirC08(next.useItem.yaw, next.useItem.pitch, SwapManager.isDesynced(), false));
                }
            }
            playIndex++;
        }
    }

    public static void resumeRecording() {
        if (state == State.PAUSED) state = State.PLAYING;
        if (data.getValue() == null || playIndex >= data.getValue().size() || data.getValue().get(playIndex) == null) return;
        PlayerInput next = data.getValue().get(playIndex);
        mc.player.setYRot(next.yaw);
        mc.player.setXRot(next.pitch);
    }

    public static void pauseRecording() {
        if (state == State.PLAYING) state = State.PAUSED;
    }

    public static void playRecording(String name) {
        if (state != State.IDLE && state != State.PAUSED) {
            AutoP3.modMessage("Cannot start playing while not idle! State: %s", state);
            return;
        }
        playIndex = 0;
        data.setFileName(name);
        data.updateFile();
        data.load();
        if (data.getValue().isEmpty()) {
            AutoP3.modMessage("Cannot play empty recording!");
            return;
        }
        state = State.PLAYING;
    }


    public static List<PlayerInput> getInputs(String name) {
        data.setFileName(name);
        data.updateFile();
        data.load();
        return data.getValue();
    }

    private enum State {
        RECORDING,
        PAUSED,
        PLAYING,
        IDLE
    }

    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class PlayerInput {
        public final float yaw;
        public final float pitch;
        public boolean using = false;
        public UseItem useItem = null;
        public final boolean forward;
        public final boolean back;
        public final boolean left;
        public final boolean right;
        public final boolean jump;
        public final boolean sneak;
        public final boolean sprint;

        public PlayerInput(float yaw, float pitch, Input in) {
            this(yaw, pitch, in.forward(), in.backward(), in.left(), in.right(), in.jump(), in.shift(), in.sprint());
        }

        public Input input() {
            return new Input(this.forward, this.back, this.left, this.right, this.jump, this.sneak, this.sprint);
        }

        public boolean equals(PlayerInput other) {
            return this.yaw == other.yaw && this.pitch == other.pitch
                    && this.forward == other.forward && this.back == other.back
                    && this.left == other.left && this.right == other.right
                    && this.jump == other.jump && this.sneak == other.sneak
                    && this.sprint == other.sprint;
        }
    }
    @AllArgsConstructor
    public static class UseItem {
        public String item;
        public float yaw;
        public float pitch;
    }
}
