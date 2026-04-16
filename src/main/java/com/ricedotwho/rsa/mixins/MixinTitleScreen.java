package com.ricedotwho.rsa.mixins;

import com.ricedotwho.rsa.screen.sidl.SessionLoginScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component component) {
        super(component);
    }

    @Inject(at = @At("HEAD"), method = "init")
    private void onInit(CallbackInfo ci) {
        Button theButton = Button.builder(Component.literal("Session Login"), button -> Minecraft.getInstance().setScreen(SessionLoginScreen.getInstance())).width(100).pos(this.width - 110, 10).build();
        this.addRenderableWidget(theButton);
    }
}
