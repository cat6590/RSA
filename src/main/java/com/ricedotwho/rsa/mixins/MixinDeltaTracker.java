package com.ricedotwho.rsa.mixins;

import com.ricedotwho.rsa.component.impl.TickFreeze;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public abstract class MixinDeltaTracker {

    @Shadow
    public abstract float getGameTimeDeltaPartialTick(boolean bl);

    @Inject(method = "getGameTimeDeltaPartialTick", at = @At("HEAD"), cancellable = true)
    private void isEntityFrozen(boolean bl, CallbackInfoReturnable<Float> cir) {
        if (TickFreeze.isFrozen()) {
            cir.setReturnValue(TickFreeze.getPartialTick());
        }
    }

    @Inject(method = "advanceGameTime", at = @At("HEAD"))
    public void advanceGameTime(long l, CallbackInfoReturnable<Integer> cir) {
        TickFreeze.setLastTickPartialTicks(this.getGameTimeDeltaPartialTick(true));
    }
}
