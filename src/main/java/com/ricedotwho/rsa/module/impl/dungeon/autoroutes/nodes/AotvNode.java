package com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AutoRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AwaitManager;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.Node;
import com.ricedotwho.rsa.utils.render3d.type.Ring;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.RoomUtils;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.FileUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AotvNode extends Node implements Accessor {
    private final Pos rotationVec;
    private Pos realRotationVector;

    public AotvNode(Pos localPos, Pos localRotationVector, AwaitManager awaits, boolean start) {
        super(localPos, awaits, start);
        this.rotationVec = localRotationVector;
        this.realRotationVector = null;
    }

    @Override
    public void calculate(UniqueRoom room) {
        super.calculate(room);
        this.realRotationVector = RoomUtils.rotateRealFixed(this.rotationVec, room.getRotation());
    }

    @Override
    public boolean run(Pos playerPos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return cancel();

        AutoRoutes autoRoutes = RSM.getModule(AutoRoutes.class);
        autoRoutes.setForceSneak(true);
        if (!SwapManager.reserveSwap(Items.DIAMOND_SHOVEL)) return cancel();

        if (Minecraft.getInstance().player.getLastSentInput().shift()) {
            return cancel();
        }

        float[] angles = EtherUtils.getYawAndPitch(realRotationVector.x, realRotationVector.y, realRotationVector.z);
        boolean swap = SwapManager.isDesynced();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            if ((swap && !SwapManager.checkClientItem(Items.DIAMOND_SHOVEL)) || (!swap && !SwapManager.checkServerItem(Items.DIAMOND_SHOVEL))) {
                // Swap didn't work??? It got swapped back? WTF
                RSA.chat("Big fuck up! : " + swap + ", " + Minecraft.getInstance().player.getInventory().getItem(SwapManager.getServerSlot()).getItem());
                return;
            }

            if (!SwapManager.sendAirC08(angles[0], angles[1], swap, false)) {
                RSA.chat("Failed to send ether C08!");
                return;
            }
            autoRoutes.setForceSneak(false);
        });

        int slot = SwapManager.getItemSlot(Items.DIAMOND_SHOVEL);
        if (slot == -1) return false;
        Pos prediction = EtherUtils.predictTeleport(8 + ItemUtils.getTunerDistance(mc.player.getInventory().getItem(slot)), playerPos, angles[0], angles[1]);
        if (prediction == null) return false;
        playerPos.set(prediction);
        return true;
    }

    @Override
    public void render(boolean depth) {
        Vec3 playerRealPos = this.getRealPos().asVec3();
        Renderer3D.addTask(new Ring(playerRealPos.add(0.0d, 0.1d, 0.0d), depth, this.getRadius(), this.getColour()));
        //Renderer3D.addTask(new Line(playerRealPos, this.realRotationVector.normalize().multiply(12).asVec3(), AutoRoutes.getAotvColour().getValue(), AutoRoutes.getAotvColour().getValue(), true));
    }

    @Override
    public int getPriority() {
        return 8; // Slightly lower
    }

    @Override
    public String getName() {
        return "aotv";
    }

    @Override
    public Colour getColour() {
        return this.isStart() ? AutoRoutes.getStartColour().getValue() : AutoRoutes.getAotvColour().getValue();
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = super.serialize();
        json.add("rotationVec", FileUtils.getGson().toJsonTree(rotationVec));
        return json;
    }

    public static AotvNode supply(UniqueRoom fullRoom, LocalPlayer player, AwaitManager awaits, boolean start) {
        Room mainRoom = fullRoom.getMainRoom();
        Pos playerRelative = RoomUtils.getRelativePosition(new Pos(player.position()), mainRoom);
        Pos targetRelative = RoomUtils.rotateRelativeFixed(new Pos(player.getViewVector(1f)), fullRoom.getRotation());
        return new AotvNode(playerRelative, targetRelative, awaits, start);
    }
}
