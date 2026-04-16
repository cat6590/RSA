package com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AutoRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AwaitManager;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.Node;
import com.ricedotwho.rsa.utils.render3d.type.Ring;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.RoomUtils;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BatNode extends Node {
    private final float yaw;
    private final float pitch;

    public BatNode(Pos localPos, float yaw, float pitch, AwaitManager awaits, boolean start) {
        super(localPos, awaits, start);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public boolean run(Pos playerPos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().level == null || Map.getCurrentRoom() == null || Map.getCurrentRoom().getUniqueRoom() == null) return cancel();

        if (!SwapManager.reserveSwap(BatNode::isWitherBlade) && !SwapManager.reserveSwap(Items.ALLIUM)) return cancel();
        if (!hasBatNear(playerPos, Minecraft.getInstance().level)) return cancel();

        boolean swap = SwapManager.isDesynced();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            SwapManager.sendAirC08(yaw, pitch, swap, false);
        });

        return false;
    }

    private boolean hasBatNear(Pos player, ClientLevel level) {
        Vec3 playerPos = player.asVec3();
        AABB aabb = new AABB(playerPos, playerPos).inflate(10.0d, 10.0d, 10.0d);
        return level.getEntitiesOfClass(Bat.class, aabb).stream().anyMatch(bat -> bat.distanceToSqr(playerPos) < 100);
    }

    private static boolean isWitherBlade(ItemStack itemStack) {
        if (itemStack == null) return false;
        String sbId = ItemUtils.getID(itemStack);
        if (sbId.isEmpty()) return false;
        return Utils.equalsOneOf(sbId, "NECRON_BLADE", "SCYLLA", "HYPERION", "VALKYRIE", "ASTRAEA") && ItemUtils.getCustomData(itemStack).getListOrEmpty("ability_scroll").size() == 3;
    }

    @Override
    public void render(boolean depth) {
        Renderer3D.addTask(new Ring(new Vec3(getRealPos().x, getRealPos().y + 0.3f, getRealPos().z), depth, this.getRadius(), this.getColour()));
    }

    @Override
    public int getPriority() {
        return 16;
    }

    @Override
    public String getName() {
        return "bat";
    }

    @Override
    public Colour getColour() {
        return this.isStart() ? AutoRoutes.getStartColour().getValue() : AutoRoutes.getBatColour().getValue();
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = super.serialize();
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        return json;
    }

    public static BatNode supply(UniqueRoom fullRoom, LocalPlayer player, AwaitManager awaits, boolean start) {
        Room mainRoom = fullRoom.getMainRoom();
        Pos playerRelative = RoomUtils.getRelativePosition(new Pos(player.position()), mainRoom);
        return new BatNode(playerRelative, 0.0f, 90.0f, awaits, start);
    }
}
