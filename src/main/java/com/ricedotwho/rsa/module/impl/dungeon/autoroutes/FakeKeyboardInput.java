package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;

public class FakeKeyboardInput extends KeyboardInput {
    public FakeKeyboardInput(Options options) {
        super(options);
    }

    private static float calculateImpulse(boolean bl, boolean bl2) {
        if (bl == bl2) {
            return 0.0F;
        } else {
            return bl ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {
        float f = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        float g = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
        this.moveVector = new Vec2(g, f).normalized();
    }
}
