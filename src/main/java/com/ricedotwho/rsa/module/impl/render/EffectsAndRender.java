package com.ricedotwho.rsa.module.impl.render;

import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render2DEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.DragSetting;
import com.ricedotwho.rsm.utils.render.render2d.NVGUtils;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector2d;

@Getter
@Environment(EnvType.CLIENT)
@ModuleInfo(aliases = "Effects", id = "EffectsAndRender", category = Category.RENDER)
public class EffectsAndRender extends Module {
    private final BooleanSetting Explosions = new BooleanSetting("Explosions", false, () -> true);
    private final BooleanSetting Fires = new BooleanSetting("Fires", false, () -> true);
    private final BooleanSetting EtherWarp = new BooleanSetting("EtherWarp", false, () -> true);
    private final BooleanSetting SMOKE = new BooleanSetting("SMOKE", false, () -> true);
    private final BooleanSetting Nausea = new BooleanSetting("Nausea", false, () -> true);
    private final BooleanSetting Blindness = new BooleanSetting("Blindness", false, () -> true);
    private final BooleanSetting Slowness = new BooleanSetting("Slowness", false, () -> true);
    private final BooleanSetting Haste = new BooleanSetting("Haste", false, () -> true);
    private final BooleanSetting Darkness = new BooleanSetting("Darkness", false, () -> true);
    private final BooleanSetting Mining_Fatigue = new BooleanSetting("Mining Fatigue", false, () -> true);
    private final BooleanSetting Speedness = new BooleanSetting("Speedness", false, () -> true);
    private final BooleanSetting FpsToggled= new BooleanSetting("Fps display", false, () -> true);
    private final DragSetting Fps = new DragSetting("Fps display", new Vector2d(50, 50), new Vector2d(50, 50));

    public EffectsAndRender() {
        this.registerProperty(
                Explosions,
                Fires,
                EtherWarp,
                SMOKE,
                FpsToggled,
                Fps,
                Nausea,
                Blindness,
                Slowness,
                Haste,
                Speedness,
                Darkness,
                Mining_Fatigue
        );
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if(Nausea.getValue()) {
            mc.player.removeEffect(MobEffects.NAUSEA);
        }

        if(Blindness.getValue()) {
            mc.player.removeEffect(MobEffects.BLINDNESS);
        }

        if(Slowness.getValue()) {
            mc.player.removeEffect(MobEffects.SLOWNESS);
        }

        if(Haste.getValue()) {
            mc.player.removeEffect(MobEffects.HASTE);
        }

        if(Speedness.getValue()) {
            mc.player.removeEffect(MobEffects.SPEED);
        }

        if(Darkness.getValue()) {
            mc.player.removeEffect(MobEffects.DARKNESS);
        }

        if(Mining_Fatigue.getValue()) {
            mc.player.removeEffect(MobEffects.MINING_FATIGUE);
        }
    }

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        //FUCK YOU EXPLOSIONS
        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.EXPLOSION,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.HugeExplosionParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).Explosions.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                    // i lowk have no idea what g, h, or i is but :shrug: it works so it works. im assuming d, e, f = xyz, but i have no idea what the rest are
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.EXPLOSION_EMITTER,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.HugeExplosionSeedParticle.Provider();
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).Explosions.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        //fire
        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.DRAGON_BREATH,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.DragonBreathParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).Fires.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.FLAME,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.FlameParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).Fires.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        //etherwarp
        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.PORTAL,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.PortalParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).EtherWarp.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.WITCH,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.SpellParticle.WitchProvider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).EtherWarp.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        //smoke
        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.LARGE_SMOKE,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.LargeSmokeParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).SMOKE.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.SMOKE,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.SmokeParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).SMOKE.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.CampfireSmokeParticle.CosyProvider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).SMOKE.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.CampfireSmokeParticle.SignalProvider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).SMOKE.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );

        ParticleFactoryRegistry.getInstance().register(
                ParticleTypes.WHITE_SMOKE,
                spriteSet -> {
                    var originalFactory = new net.minecraft.client.particle.WhiteSmokeParticle.Provider(spriteSet);
                    return (simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource) ->
                            RSM.getModule(EffectsAndRender.class).SMOKE.getValue() ? null : originalFactory.createParticle(simpleParticleType, clientLevel, d, e, f, g, h, i, randomSource);
                }
        );
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void reset() {
    }

    @SubscribeEvent
    public void onRender2D(Render2DEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;

        if (player == null || level == null) return;
        int fps = Minecraft.getInstance().getFps();
        String fpsString = "Fps: " + fps;

        if (FpsToggled.getValue()) {
            this.Fps.renderScaled(event.getGfx(), () -> NVGUtils.drawText(fpsString, 0, 0, 50f, Colour.blue, NVGUtils.NUNITO), 60, 30);
        }
    }
}