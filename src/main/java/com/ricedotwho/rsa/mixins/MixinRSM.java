package com.ricedotwho.rsa.mixins;

import com.ricedotwho.rsm.RSM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RSM.class)
public class MixinRSM {
    /**
     * @author ricedotwho
     * @reason change gui name
     */
    @Overwrite
    public static String getName() {
        return "RSA";
    }
}
