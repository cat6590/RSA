package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import lombok.Getter;

@Getter
@ModuleInfo(aliases = "Test", id = "Test", category = Category.OTHER)
public class Test extends Module {

//    KeybindSetting funnyKey = new KeybindSetting("Key", new Keybind(GLFW.GLFW_KEY_B, false, null), this::funny);
//    NumberSetting magnitude = new NumberSetting("Magnitude", 0.01, 10, 0.1, 0.01);
//    private boolean bl = false;
//    private boolean sneak = false;
//    private double lastVelocity = 0.0d;
//
//    public Test() {
//        this.registerProperty(
//                funnyKey,
//                magnitude
//        );
//    }
//
//    private void funny() {
////        Vec3 lookDir = Minecraft.getInstance().player.getViewVector(1f);
////        lookDir = lookDir.subtract(0d, lookDir.y, 0d).scale(magnitude.getValue());
////        Vec3 velocity = Minecraft.getInstance().player.getDeltaMovement();
////        Minecraft.getInstance().player.setDeltaMovement(lookDir.x, velocity.y, lookDir.z);
//        if (lastVelocity == 0.0d) {
//            lastVelocity = Minecraft.getInstance().player.getDeltaMovement().y * 0.1;
//            this.sneak = true;
//        } else {
//            lastVelocity = 0.0d;
//        }
//    }
//
//    @SubscribeEvent
//    public void onTickStart(ClientTickEvent.Start event) {
//        if (lastVelocity == 0.0d) return;
//        Vec3 velocity = Minecraft.getInstance().player.getDeltaMovement();
//        Minecraft.getInstance().player.setDeltaMovement(velocity.x, lastVelocity, velocity.z);
//    }
//
//    @SubscribeEvent
//    public void onPollInput(InputPollEvent event) {
//        if (lastVelocity == 0.0d) return;
//        Input oldInputs = event.getClientInput();
//        Input newInputs = new Input(oldInputs.forward(), oldInputs.backward(), oldInputs.left(), oldInputs.right(), oldInputs.jump(), this.sneak, oldInputs.sprint());
//        this.sneak = !this.sneak;
//        event.getInputConsumer().accept(newInputs);
//    }
//
//    @SubscribeEvent
//    public void onWorldLoad(WorldEvent.Load event) {
//
//    }
//
//
//
//    @Override
//    public void onEnable() {
//
//    }
//
//    @Override
//    public void onDisable() {
//
//    }

}
