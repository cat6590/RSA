package com.ricedotwho.rsa.module.impl.render;

import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.DefaultGroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ColourSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledBox;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledOutlineBox;
import com.ricedotwho.rsm.utils.render.render3d.type.OutlineBox;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

@Getter
@ModuleInfo(aliases = "Esp", id = "Esp", category = Category.RENDER)
public class Esp extends Module {

    //basically 1:1 Ported from Hyper's esp

    private final ModeSetting renderMode = new ModeSetting("Mode", "Filled Outline", List.of("Filled Outline", "Filled", "Outline"));

    private final BooleanSetting
            showStarredMobs = new BooleanSetting("Starred Mobs", true),
            onlyShowInCurrentRoom = new BooleanSetting("Current Room Only", true),
            drawBloodMobs = new BooleanSetting("Blood Mobs", false),
            withers = new BooleanSetting("Withers", true),
            bats = new BooleanSetting("Bats", false),
            depth = new BooleanSetting("Depth", false);

    private final DefaultGroupSetting colours = new DefaultGroupSetting("Colours", this);

    private final ColourSetting
            starredFill = new ColourSetting("Star Fill", new Colour(0x1A790091)),
            starredOutline = new ColourSetting("Star Outline", new Colour(0xFFD600FF)),
            bloodFill = new ColourSetting("Blood Fill", new Colour(0x1A720000)),
            bloodOutline = new ColourSetting("Blood Outline", new Colour(0xFFFF0000)),
            witherFill = new ColourSetting("Wither Fill", new Colour(0x1A003688)),
            witherOutline = new ColourSetting("Wither Outline", new Colour(0xFF0066FF)),
            batFill = new ColourSetting("Bat Fill", new Colour(173, 92, 173, 90)),
            batOutline = new ColourSetting("Bat Outline", new Colour(173, 92, 173));

    // Tracked entities
    private final Set<Integer> starredMobs = new HashSet<>();
    private final Set<Integer> bloodMobs = new HashSet<>();
    private final Set<Integer> batMobs = new HashSet<>();
    private final Set<Integer> bloodNames = new HashSet<>();
    
    private int wither = -1;
    private double witherDistance = Double.MAX_VALUE;
    public float updateInterval = 10;

    public Esp() {
        // Blood mob names
        addName("Revoker");
        addName("Psycho");
        addName("Reaper");
        addName("Cannibal");
        addName("Mute");
        addName("Ooze");
        addName("Putrid");
        addName("Freak");
        addName("Leech");
        addName("Tear");
        addName("Parasite");
        addName("Flamer");
        addName("Skull");
        addName("Mr. Dead");
        addName("Vader");
        addName("Frost");
        addName("Walker");
        addName("Wandering Soul");
        addName("Bonzo");
        addName("Scarf");
        addName("Livid");
        addName("Spirit Bear");
        
        this.registerProperty(
                renderMode,
                showStarredMobs,
                onlyShowInCurrentRoom,
                drawBloodMobs,
                bats,
                withers,
                colours
        );

        colours.add(starredFill, starredOutline, bloodFill, bloodOutline, witherFill, witherOutline, batFill, batOutline);
    }
    
    private void addName(String name) {
        bloodNames.add(name.hashCode());
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void reset() {
        starredMobs.clear();
        bloodMobs.clear();
        batMobs.clear();
        wither = -1;
        witherDistance = Double.MAX_VALUE;
    }

    @SubscribeEvent
    public void onRender3dEvent(Render3DEvent.Extract event) {
        if (mc.player == null || mc.level == null) return;
        
        if (!(Location.getArea() == Island.Dungeon)) return;

        float partialTicks = event.getContext().tickCounter().getGameTimeDeltaPartialTick(false);

        // Render starred mobs
        if (showStarredMobs.getValue() && !starredMobs.isEmpty()) {
            handleRender(starredMobs, this.getStarredOutline().getValue(), this.getStarredFill().getValue(), partialTicks);
        }

        // Render blood mobs
        if (drawBloodMobs.getValue() && !bloodMobs.isEmpty()) {
            handleRender(bloodMobs, this.getBloodOutline().getValue(), this.getBloodFill().getValue(), partialTicks);
        }

        if (bats.getValue() && !batMobs.isEmpty()) {
            handleRender(batMobs, this.getBatOutline().getValue(), this.getBatFill().getValue(), partialTicks);
        }

        // Render wither
        if (withers.getValue() && wither != -1) {
            Entity entity = mc.level.getEntity(wither);
            if (entity != null) {
                renderEntityBox(entity, this.getWitherOutline().getValue(), this.getWitherFill().getValue(), partialTicks);
            } else {
                wither = -1;
            }
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        if (mc.level == null || mc.player == null || !Location.getArea().is(Island.Dungeon)) return;
        if (event.getTime() % updateInterval == 0) {
            updateTrackedEntities(mc.level);
        }
    }

    private void updateTrackedEntities(ClientLevel level) {
        starredMobs.clear();
        bloodMobs.clear();
        wither = -1;
        witherDistance = Double.MAX_VALUE;

        for (Entity entity : level.entitiesForRendering()) {
            // Starred mobs (armor stands with ✯ name)
            if (showStarredMobs.getValue() && entity instanceof ArmorStand stand) {
                if (!isValidStarredEntity(stand)) continue;
                Entity mob = getMobEntity(stand, level);
                if (mob != null) {
                    starredMobs.add(mob.getId());
                    stand.setCustomNameVisible(true);
                    mob.setInvisible(false);
                }
                continue;
            }

            // Shadow Assassin (player entities with specific name)
            if (showStarredMobs.getValue() && entity instanceof Player && !(entity instanceof LocalPlayer)) {
                String name = entity.getName().getString().trim();
                if (name.hashCode() == -0x277A5F7B) { // Shadow Assassin
                    starredMobs.add(entity.getId());
                    entity.setInvisible(false);
                }
                continue;
            }

            // Fels (enderman with specific name)
            if (showStarredMobs.getValue() && entity instanceof EnderMan) {
                if (entity.getName().getString().hashCode() == -0x3BEF85AA) {
                    entity.setInvisible(false);
                }
                continue;
            }

            // Blood mobs - players
            if (drawBloodMobs.getValue() && entity instanceof Player && !(entity instanceof LocalPlayer)) {
                String name = entity.getName().getString().trim();
                if (bloodNames.contains(name.hashCode())) {
                    bloodMobs.add(entity.getId());
                    entity.setInvisible(false);
                }
                continue;
            }

            // Blood mobs - giants
            if (drawBloodMobs.getValue() && entity instanceof Giant && !Dungeon.isInBoss()) { // why can I see goldors fucking sword giant thing :sob:
                bloodMobs.add(entity.getId());
                entity.setInvisible(false);
                continue;
            }

            if (bats.getValue() && entity instanceof Bat && !entity.isInvisible()) {
                batMobs.add(entity.getId());
                continue;
            }

            // Withers
            if (withers.getValue() && entity instanceof WitherBoss e && !entity.isInvisible()) {
                LocalPlayer Player = Minecraft.getInstance().player;
                if (e.getMaxHealth() != 300f && e.getScale() == 1f) {
                    if (wither == -1) {
                        wither = entity.getId();
                        continue;
                    }
                    
                    double dist = entity.distanceToSqr(Player);
                    if (dist < witherDistance) {
                        witherDistance = dist;
                        wither = entity.getId();
                    }
                }
            }
        }
    }

    private void handleRender(Set<Integer> entityIds, Colour outlineColor, Colour fillColor, float partialTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        List<Integer> toRemove = new ArrayList<>();
        for (int entityId : entityIds) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || entity instanceof LivingEntity living && living.isDeadOrDying()) {
                toRemove.add(entityId);
                continue;
            }
            renderEntityBox(entity, outlineColor, fillColor, partialTicks);
        }
        
        toRemove.forEach(entityIds::remove);
    }

    private void renderEntityBox(Entity entity, Colour outline, Colour fill, float partialTicks) {
        Vec3 interpolatedPos = entity.getPosition(partialTicks);
        
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();

        AABB aabb = new AABB(
                interpolatedPos.x - width / 2,
                interpolatedPos.y,
                interpolatedPos.z - width / 2,
                interpolatedPos.x + width / 2,
                interpolatedPos.y + height,
                interpolatedPos.z + width / 2
        );

        switch (this.renderMode.getIndex()) {
            case 0 -> Renderer3D.addTask(new FilledOutlineBox(aabb, fill, outline, this.getDepth().getValue()));
            case 1 -> Renderer3D.addTask(new FilledBox(aabb, fill, this.getDepth().getValue()));
            default -> Renderer3D.addTask(new OutlineBox(aabb, outline, this.getDepth().getValue()));
        }
    }

    private boolean isValidStarredEntity(ArmorStand entity) {
        if (!entity.hasCustomName()) return false;
        String name = StringUtil.stripColor(Objects.requireNonNull(entity.getCustomName()).getString());
        return name.contains("✯ ") && name.endsWith("❤");
    }

    private Entity getMobEntity(ArmorStand stand, ClientLevel level) {
        AABB searchBox = stand.getBoundingBox().move(0.0, -1.0, 0.0);
        
        return level.getEntities(stand, searchBox)
                .stream()
                .filter(e -> e instanceof LivingEntity 
                        && !(e instanceof ArmorStand) 
                        && !(e instanceof LocalPlayer)
                        && !(e instanceof WitherBoss && e.isInvisible()))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(stand)))
                .orElse(null);
    }
}
