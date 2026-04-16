package com.ricedotwho.rsa.module.impl.player.autopet;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.PetRule;
import com.ricedotwho.rsa.utils.GuiUtils;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.SaveSetting;
import com.ricedotwho.rsm.utils.ChatUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(aliases = "Auto Pet", id = "AutoPet", category = Category.PLAYER)
public class AutoPet extends Module {
    private boolean swapping, listing;
    private boolean awaitingOpen;
    private boolean awaitingPhoenix;
    private boolean clicked;
    private boolean foundPet;
    private String swapID;
    private String last;
    private AbstractContainerMenu container;

    private BooleanSetting yap = new BooleanSetting("Feedback", true);
    private ModeSetting phoenixSwap = new ModeSetting("Phoenix Swap", "Death Tick", List.of("None", "Death Tick", "Duration", "Use"));
    private int phoenixTicks = -1;
    private final SaveSetting<List<PetRule>> rules = new SaveSetting<>("Rules", "player", "autopet.json", ArrayList::new,
            new TypeToken<List<PetRule>>(){}.getType(),
            new GsonBuilder()
                    .registerTypeHierarchyAdapter(PetRule.class, new PetRuleAdapter())
                    .setPrettyPrinting().create(),
            false, this::reload, null);

    public AutoPet() {
        registerProperty(
                yap,
                phoenixSwap,
                rules
        );
        clear();
    }

    private void reload() {
        // todo: on unload they should unsubscribe
        rules.getValue().forEach(r -> RSM.getInstance().getEventBus().register(r));
    }

    public void removeRule(int index) {
        if (index < 0 || index >= rules.getValue().size()) return;
        PetRule rule = rules.getValue().remove(index);
        RSM.getInstance().getEventBus().unregister(rule);
    }

    public Iterator<PetRule> iterateRules() {
        return rules.getValue().iterator();
    }

    public void addPetRule(PetRule petRule) {
        this.rules.getValue().add(petRule);
        this.rules.save();
        RSM.getInstance().getEventBus().register(petRule);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.swapping = false;
        this.listing = false;
        this.phoenixTicks = -1;
        this.awaitingPhoenix = false;
        clear();
    }

    @Override
    public void onDisable() {
        swapping = false;
        this.phoenixTicks = -1;
        this.awaitingPhoenix = false;
        clear();
    }

    @Override
    public void onEnable() {
        swapping = false;
        listing = false;
        this.phoenixTicks = -1;
        this.awaitingPhoenix = false;
        clear();
    }

    public void swapTo(String swapID) {
        if (Minecraft.getInstance().getConnection() == null || swapping || listing || swapID.isEmpty()) return;
        this.swapID = swapID.toLowerCase();
        this.swapping = true;
        Minecraft.getInstance().getConnection().sendCommand("pet");
    }

    public void listPets() {
        if (Minecraft.getInstance().getConnection() == null || swapping || listing) return;
        this.listing = true;
        Minecraft.getInstance().getConnection().sendCommand("pet");
    }

    private void clear() {
        this.foundPet = false;
        awaitingOpen = false;
        clicked = false;
        this.container = null;
    }

    @SubscribeEvent
    public void onDeathTick(ServerTickEvent event) {
        if (!awaitingPhoenix) return;
        if (phoenixTicks > 0)
            phoenixTicks--;

        if (phoenixSwap.getIndex() == 2 && phoenixTicks <= 0) {
            swapTo(last);
            return;
        }

        if (phoenixSwap.getIndex() == 1 && phoenixTicks <= 0 && event.getTime() % 40 == 0) {
            swapTo(last);
            return;
        }
    }

    @SubscribeEvent
    public void onChatPacket(ChatEvent.Chat event) {
        if (phoenixSwap.getIndex() != 3 || !awaitingPhoenix) return;
        if (!event.getMessage().getString().equals("Your Phoenix Pet saved you from certain death!")) return;
        swapTo(last);
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (Minecraft.getInstance().player == null || !Location.isInSkyblock()) return;
        if ((swapping || listing) && event.getPacket() instanceof ClientboundOpenScreenPacket openScreenPacket && !swapID.isEmpty()) { // Only check for swapping here so we don't get errors if swap changes during other sections
            clear();
            if (openScreenPacket.getContainerId() < 1 || openScreenPacket.getContainerId() > 100) return;
            this.clicked = false;
            if (openScreenPacket.getTitle().getString().equals("Pets")) {
                this.container = openScreenPacket.getType().create(openScreenPacket.getContainerId(), Minecraft.getInstance().player.getInventory());
                this.awaitingOpen = true;
                event.setCancelled(true);
                if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> abstractContainerScreen && abstractContainerScreen.getMenu().containerId != 0) {
                    // o7 Balding
                    Minecraft.getInstance().setScreen(null);
                }
            } else {
                this.container = null;
                this.awaitingOpen = false;
                // Don't reset swap, could just have opened a different gui
            }
            return;
        }

        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            if (packet.getContainerId() < 1 || packet.getContainerId() > 100 || mc.player == null || this.container == null || this.container.containerId != packet.getContainerId()) return;
            event.setCancelled(true);
            container.setItem(packet.getSlot(), packet.getStateId(), packet.getItem());

            if (packet.getSlot() < 10) return; // First pet slot


            if (swapping) {
                if (packet.getSlot() > 43 && awaitingOpen) {
                    this.swapping = false;
                    if (!foundPet)
                        ChatUtils.chat("Failed to find pet " + swapID + "!");
                    close();
                    return;
                }

                ItemStack item = packet.getItem();
                if (!item.getItem().equals(Items.PLAYER_HEAD)) return;

                foundPet = true;
                boolean isPhoenix = item.getDisplayName().getString().contains("Phoenix");

                if (((ItemLore) item.getOrDefault(DataComponents.LORE, CustomData.EMPTY)).lines().stream().anyMatch(p -> p.getString().equals("Click to despawn!"))) {
                    // Check if it's a phoenix before setting to last
                    if (isPhoenix) return;
                    this.last = ItemUtils.getUUID(item);
                    return;
                }

                if (clicked || !awaitingOpen) return; // Don't return earlier so we can check last
                if (!ChatFormatting.stripFormatting(item.getHoverName().getString()).toLowerCase().contains(swapID) && !ItemUtils.getUUID(item).equals(swapID)  && !ItemUtils.getID(item).equals(swapID)) return;

                GuiUtils.sendWindowClick(packet.getSlot(), mc.player, this.container);
                if (yap.getValue()) ChatUtils.chat(Component.literal("Swapping to ").append(item.getDisplayName()));
                this.phoenixTicks = -1;
                if (isPhoenix && phoenixSwap.getIndex() == 2) phoenixTicks = 45;
                if (isPhoenix && phoenixSwap.getIndex() == 1) phoenixTicks = 5;
                this.awaitingPhoenix = isPhoenix;

                awaitingOpen = false;
                swapping = false;
            } else if (listing) {
                if (packet.getSlot() > 43 && awaitingOpen) {
                    listing = false;
                    close();
                    return;
                }

                // listing pets
                ItemStack item = packet.getItem();
                if (!item.getItem().equals(Items.PLAYER_HEAD)) return;
                String uuid = ItemUtils.getUUID(item);
                if (uuid.isBlank()) return;
                // now i should chat the pet name and the uuid as a click to copy component

                Component hover = Component.literal("Click to copy UUID").withStyle(ChatFormatting.YELLOW);
                Style style = Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(ItemUtils.getUUID(item))).withHoverEvent(new HoverEvent.ShowText(hover));
                Component text = item.getHoverName().copy().setStyle(style);
                RSA.chat(text);
            }
        }

        if (event.getPacket() instanceof ClientboundContainerClosePacket packet) {
            if (this.container != null && packet.getContainerId() == container.containerId) {
                reset();
                event.setCancelled(true);
            }
        }
    }

    private void close() {
        if (container == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundContainerClosePacket(this.container.containerId));
        clear();
    }
}
