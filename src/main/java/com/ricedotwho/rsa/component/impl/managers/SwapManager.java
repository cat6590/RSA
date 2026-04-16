package com.ricedotwho.rsa.component.impl.managers;

import com.ricedotwho.rsa.IMixin.IMultiPlayerGameMode;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.data.Rotation;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.RotationUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.function.Predicate;

public class SwapManager {
    @Getter
    private static int serverSlot;

    private static int lastSentServerSlot;
    private static boolean swappedThisTick = false;

    private static int requireSwap = -1;

    public static void onPreTickStart() {
        swappedThisTick = false;
        requireSwap = -1;
    }

    public static boolean onPostSendPacket(Packet<?> packet) {
        if (!(packet instanceof ServerboundSetCarriedItemPacket slotPacket)) return true;

        if (swappedThisTick || slotPacket.getSlot() == lastSentServerSlot) {
            RSA.chat("Prevented packet 0 tick swap! This shouldn't happen, tell hyper!");
            return false;
        }

        swappedThisTick = true;
        serverSlot = slotPacket.getSlot();
        lastSentServerSlot = slotPacket.getSlot();
        return true;
    }

    public static void onHandleLogin() {
        // The Minecraft.MultiPlayerGameMode is reset here, so its server slot is also reset
        serverSlot = 0;
        lastSentServerSlot = 0; // Scary but should be fine
    }

    // Cancels call if returns false
    public static boolean onEnsureHasSentCarriedItem(int managerServerSlot) {
        if (Minecraft.getInstance().player == null) return false;
        if (serverSlot != managerServerSlot) {
            RSA.chat("Slot mismatch! Tell Hyper if you see this!");
            RSA.chat("SwapManger : " + serverSlot);
            RSA.chat("GameMode : " + managerServerSlot);
        }
        int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
        if (!swappedThisTick && requireSwap > -1 && i != requireSwap) {
            if (requireSwap == managerServerSlot) return false;
            Minecraft.getInstance().player.getInventory().setSelectedSlot(requireSwap);
            i = requireSwap;
        }

        if (i != managerServerSlot && !swappedThisTick) {
            serverSlot = i;
            return true;
        }
        return false;
    }

    private static boolean reserveSwap0(int index) {
        if (index < 0 || index > 8) return false;

        if (!canSwap()) {
            // Should already be reserved or we already swapped so we can't swap off anyways
            return index == getNextUpdateIndex(); // Already on this item
        }
        requireSwap = index;
        return true;
    }


    public static boolean reserveSwap(int index) {
        if (!reserveSwap0(index)) return false;
        swapSlot(index);
        return true;
    }

    public static int getNextUpdateIndex() {
        if (swappedThisTick) return serverSlot;
        if (requireSwap > -1) return requireSwap;
        if (Minecraft.getInstance().player == null) return 0;
        return Minecraft.getInstance().player.getInventory().getSelectedSlot();
    }

    public static boolean canSwap() {
        return !swappedThisTick && requireSwap < 0;
    }

    public static boolean sendAirC08(float yaw, float pitch, boolean syncSlots, boolean swing) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.gameMode() == GameType.SPECTATOR) return false;
        if (Minecraft.getInstance().gameMode == null || Minecraft.getInstance().level == null) return false;

        IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);

        int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
        if (syncSlots) manager.syncSlot();
        if (syncSlots && !checkServerSlot(i)) {
            RSA.chat("Failed to swap to slot : " + i);
            return false;
        }

        manager.sendPacketSequenced(Minecraft.getInstance().level, sequence -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, yaw, pitch));
        if (swing) Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean isDesynced() {
        return getNextUpdateIndex() != serverSlot;
    }

    public static boolean sendAirC08(float yaw, float pitch, boolean syncSlots) {
        return sendAirC08(yaw, pitch, syncSlots, false);
    }

    public static boolean sendAirC08(Rotation rot, boolean syncSlots) {
        return sendAirC08(rot.getYaw(), rot.getPitch(), syncSlots, false);
    }

    public static boolean sendAirC08(Rotation rot, boolean syncSlots, boolean swing) {
        return sendAirC08(rot.getYaw(), rot.getPitch(), syncSlots, swing);
    }

    public static boolean sendBlockC08(BlockHitResult result, boolean swing, boolean syncSlot) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.gameMode() == GameType.SPECTATOR) return false;
        if (Minecraft.getInstance().gameMode == null || Minecraft.getInstance().level == null) return false;

        if (syncSlot) {
            IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);
            int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
            manager.syncSlot();
            if (!checkServerSlot(i)) {
                RSA.chat("Failed to swap to slot : " + i);
                return false;
            }
        }

        ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode).sendPacketSequenced(Minecraft.getInstance().level, sequence -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, sequence));
        if (swing) Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean sendBlinkBlockC08(BlockHitResult result, boolean swing, boolean syncSlot) {
//        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.gameMode() == GameType.SPECTATOR) return false;
//        if (Minecraft.getInstance().gameMode == null || Minecraft.getInstance().level == null) return false;
//
//        Blink blink = RSM.getModule(Blink.class);
//
//        synchronized (blink) {
//            if (syncSlot && (!blink.isEnabled() || blink.isFlushing())) {
//                IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);
//                int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
//                manager.syncSlot();
//                if (!checkServerSlot(i)) {
//                    RSA.chat("Failed to swap to slot : " + i);
//                    return false;
//                }
//            } else if (syncSlot && blink.isEnabled() && !blink.isFlushing()) {
//                IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);
//                int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
//
//                blink.enableFlush();
//                manager.syncSlot();
//                blink.disableFlush();
//                if (!checkServerSlot(i)) {
//                    RSA.chat("Failed to swap to slot : " + i);
//                    return false;
//                }
//            }
//
//            if (blink.isEnabled()) {
//                blink.actuallySend(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, 0));
//                if (swing) blink.actuallySend(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
//                return true;
//            }
//
//            ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode).sendPacketSequenced(Minecraft.getInstance().level, sequence -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, sequence));
//            if (swing) Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
//            return true;
//        }
        return false;
    }

    public static boolean sendBlockC08(float yaw, float pitch, boolean swing, boolean syncSlot) {
        HitResult result = RotationUtils.getBlockHitResult(Minecraft.getInstance().player.getContainerInteractionRange(), yaw, pitch, Minecraft.getInstance().player.position().add(0d, EtherUtils.SNEAK_EYE_HEIGHT, 0d));
        if (result.getType() != HitResult.Type.BLOCK) {
            RSA.chat("Failed to send block C08!");
        }
        return sendBlockC08((BlockHitResult) result, swing, syncSlot);
    }

    // Haven't implement syncSlots because I haven't found the need
    public static boolean sendBlockC08(Vec3 pos, Direction direction, boolean swing, boolean syncSlot) {
        return sendBlockC08(new BlockHitResult(pos, direction, BlockPos.containing(pos), false), swing, syncSlot);
    }

    ///  The only Actions passed should be START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, and STOP_DESTROY_BLOCK
    public static boolean sendC07(BlockPos result, ServerboundPlayerActionPacket.Action action, Direction face, boolean swing, boolean syncSlot) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.gameMode() == GameType.SPECTATOR) return false;
        if (Minecraft.getInstance().gameMode == null || Minecraft.getInstance().level == null) return false;

        if (syncSlot) {
            IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);
            int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
            manager.syncSlot();
            if (!checkServerSlot(i)) {
                RSA.chat("Failed to swap to slot : " + i);
                return false;
            }
        }

        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, result, Direction.DOWN, 0));
        } else {
            ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode).sendPacketSequenced(Minecraft.getInstance().level, sequence -> new ServerboundPlayerActionPacket(action, result, face, sequence));
        }
        if (swing) Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean reserveSwap(Item item) {

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || item == null) return false;
        if (!canSwap()) {
            // Should already be reserved or we already swapped so we can't swap off anyways
            return item == player.getInventory().getItem(getNextUpdateIndex()).getItem(); // Already on this item
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i); // Hotbar is 0 - 8
            if (stack.getItem() != item) continue;
            boolean bl = swapSlot(i);
            if (bl) reserveSwap0(i);
            return bl;
        }
        return false;
    }

    public static boolean reserveSwap(Predicate<ItemStack> predicate) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        if (!canSwap()) {
            // Should already be reserved or we already swapped so we can't swap off anyways
            return predicate.test((player.getInventory().getItem(getNextUpdateIndex()))); // Already on this item
        }

        for (int i = 0; i < 9; i++) {
            if (!predicate.test(player.getInventory().getItem(i))) continue;
            boolean bl = swapSlot(i);
            if (bl) reserveSwap0(i);
            return bl;
        }
        return false;
    }

    public static boolean reserveSwap(String ...sbId) {

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || sbId == null || sbId.length == 0) return false;
        if (!canSwap()) {
            // Should already be reserved or we already swapped so we can't swap off anyways
            String next = ItemUtils.getID(player.getInventory().getItem(getNextUpdateIndex()));
            return Arrays.stream(sbId).anyMatch(id -> !id.isBlank() && next.equals(id)); // Already on this item
        }

        for (int i = 0; i < 9; i++) {
            String id = ItemUtils.getID(player.getInventory().getItem(i));
            if (Arrays.stream(sbId).noneMatch(id1 -> !id1.isBlank() && id.equals(id1))) continue;
            boolean bl = swapSlot(i);
            if (bl) reserveSwap0(i);
            return bl;
        }
        return false;
    }


    public static boolean swapItem(Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || item == null) return false;

        if (item == player.getInventory().getItem(getNextUpdateIndex()).getItem()) return true; // Already on this item
        if (!canSwap()) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i); // Hotbar is 0 - 8
            if (stack.getItem() != item) continue;
            return swapSlot(i);
        }
        return false;
    }

    /// Swap to an item with the specified SkyBlock ID
    public static boolean swapItem(String ...sbId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || sbId == null || sbId.length == 0) return false;

        String heldId = ItemUtils.getID(player.getInventory().getItem(getNextUpdateIndex()));
        if (Arrays.stream(sbId).anyMatch(id -> !id.isBlank() && heldId.equals(id))) return true;

        if (!canSwap()) return false;
        for (int i = 0; i < 9; i++) {
            String id = ItemUtils.getID(player.getInventory().getItem(i));
            if (Arrays.stream(sbId).noneMatch(id1 -> !id1.isBlank() && id.equals(id1))) continue;
            return swapSlot(i);
        }
        return false;
    }

    public static boolean swapItem(Predicate<ItemStack> predicate) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        if (predicate.test(player.getInventory().getItem(getNextUpdateIndex()))) return true;

        if (!canSwap()) return false;
        for (int i = 0; i < 9; i++) {
            if (!predicate.test(player.getInventory().getItem(i))) continue;
            return swapSlot(i);
        }
        return false;
    }

    public static boolean swapSlot(int slot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (slot == getNextUpdateIndex()) return true;
        if (player == null || swappedThisTick) return false;
        if (slot < 0 || slot > 8) {
            RSA.getLogger().error("Invalid swap slot! : {}", slot);
            return false;
        }

        player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static boolean checkServerSlot(int slot) {
        return serverSlot == slot;
    }

    public static boolean checkServerItem(Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || serverSlot < 0 || serverSlot > 8) return false;

        ItemStack stack = player.getInventory().getItem(serverSlot);
        return stack.getItem() == item;
    }

    public static boolean checkServerItem(String ...sbId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || serverSlot < 0 || serverSlot > 8 || sbId.length == 0) return false;

        String heldId = ItemUtils.getID(player.getInventory().getItem(serverSlot));
        return Arrays.stream(sbId).anyMatch(id -> !id.isBlank() && heldId.equals(id));
    }

    public static boolean checkClientItem(Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        ItemStack stack = player.getInventory().getItem(player.getInventory().getSelectedSlot());
        return stack.getItem() == item;
    }

    public static boolean checkClientItem(String ...sbId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || sbId.length == 0) return false;

        String heldId = ItemUtils.getID(player.getInventory().getItem(player.getInventory().getSelectedSlot()));
        return Arrays.stream(sbId).anyMatch(id -> !id.isBlank() && heldId.equals(id));
    }

    public static int getItemSlot(Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || item == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i); // Hotbar is 0 - 8
            if (stack.getItem() != item) continue;
            return i;
        }
        return -1;
    }

    public static int getItemSlot(String ...id) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || id == null || id.length == 0) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i); // Hotbar is 0 - 8
            if (Arrays.stream(id).anyMatch(s -> s.equals(ItemUtils.getID(stack)))) return i;
        }
        return -1;
    }

    // TODO
    // Hook these functions at a lower level
}
