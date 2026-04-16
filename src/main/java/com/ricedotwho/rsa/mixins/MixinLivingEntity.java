package com.ricedotwho.rsa.mixins;

import com.ricedotwho.rsa.module.impl.render.HidePlayers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    public void isPickable(CallbackInfoReturnable<Boolean> cir) {
        if (HidePlayers.shouldHitThrough((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
