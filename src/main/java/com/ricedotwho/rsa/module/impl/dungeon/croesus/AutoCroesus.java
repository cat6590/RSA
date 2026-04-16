package com.ricedotwho.rsa.module.impl.dungeon.croesus;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.GuiEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.MultiBoolSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.NumberUtils;
import com.ricedotwho.rsm.utils.Utils;
import com.ricedotwho.rsm.utils.api.PriceData;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ModuleInfo(aliases = "Auto Croesus", id = "AutoCroesus", category = Category.DUNGEONS)
public class AutoCroesus extends Module {

    private final NumberSetting clickDelay = new NumberSetting("Click Delay", 100, 1000, 300, 25);

    private final BooleanSetting chestKeys = new BooleanSetting("Use chest keys", false);
    private final NumberSetting chestKeyMinProfit = new NumberSetting("Key min Profit", 0, 2, 0.5, 0.01, "m");

    private final BooleanSetting kismets = new BooleanSetting("Use Kismet", false);
    private final NumberSetting kismetsMinProfit = new NumberSetting("Kismet min Profit", 1, 3.5, 2, 0.05, "m");
    private final MultiBoolSetting kismetFloors = new MultiBoolSetting("Kismet Floors", List.of("F1", "F2", "F3", "F4", "F5", "F6", "F7", "M1", "M2", "M3", "M4", "M5", "M6", "M7"), new ArrayList<>());

    private final Pattern costPattern = Pattern.compile("^([\\d,]+) Coins$");
    private final Pattern bookPattern = Pattern.compile("^Enchanted Book \\(?([\\w ]+) (\\w+)\\)$");
    private final Pattern essencePattern = Pattern.compile("^(\\w+) Essence x(\\d+)$");

    private final Map<String, String> ITEM_REPLACEMENTS = new HashMap<>();

    private static final double AURA_RANGE = 3.5d;
    private static final TextColor ULT_COLOUR = TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE);

    private boolean running = false;
    private Action action = Action.IDLE;
    private boolean kismetting = false;
    private int currentPage = 1;

    public AutoCroesus() {
        this.registerProperty(
                clickDelay,
                chestKeys,
                chestKeyMinProfit,
                kismets,
                kismetsMinProfit,
                kismetFloors
        );

        ITEM_REPLACEMENTS.put("Shiny Wither Boots", "WITHER_BOOTS");
        ITEM_REPLACEMENTS.put("Shiny Wither Leggings", "WITHER_LEGGINGS");
        ITEM_REPLACEMENTS.put("Shiny Wither Chestplate", "WITHER_CHESTPLATE");
        ITEM_REPLACEMENTS.put("Shiny Wither Helmet", "WITHER_HELMET");
        ITEM_REPLACEMENTS.put("Shiny Necron's Handle", "NECRON_HANDLE");
        ITEM_REPLACEMENTS.put("Wither Shard", "SHARD_WITHER");
        ITEM_REPLACEMENTS.put("Thorn Shard", "SHARD_THORN");
        ITEM_REPLACEMENTS.put("Apex Dragon Shard", "SHARD_APEX_DRAGON");
        ITEM_REPLACEMENTS.put("Power Dragon Shard", "SHARD_POWER_DRAGON");
        ITEM_REPLACEMENTS.put("Scarf Shard", "SHARD_SCARF");
        ITEM_REPLACEMENTS.put("Necron Dye", "DYE_NECRON");
        ITEM_REPLACEMENTS.put("Livid Dye", "DYE_LIVID");

        CroesusLoader.load();
    }

    @Override
    public void reset() {
        running = false;
        action = Action.IDLE;
        kismetting = false;
        currentPage = 1;
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Load event) {
        if (!action.equals(Action.IDLE)) {
            modMessage("Stopping!");
        }
        reset();
    }

    public static void modMessage(String text) {
        RSA.chat(ChatFormatting.YELLOW + "AutoCroesus » " + ChatFormatting.RESET + text);
    }

    public void start() {
        this.start(true);
    }

    public void start(boolean checkPrice) {
        if (!this.isEnabled()) {
            modMessage("Module is not Enabled!");
            return;
        }
        if (!Location.getArea().is(Island.DungeonHub)) {
            modMessage("You are not in the dungeon hub!");
            return;
        }

        if (running) {
            modMessage("Already claiming!");
            return;
        }

        if (checkPrice && System.currentTimeMillis() - PriceData.getLastFetched() > 1_800_000) {
            modMessage("Updating price data from the API...");
            PriceData.updatePrices(this::start);
            return;
        }

        running = true;
        action = Action.CROESUS;
        if (!clickCroesus()) {
            running = false;
            modMessage("Failed to click Croesus!");
            reset();
        }
    }

    private boolean clickCroesus() {
        if (action != Action.CROESUS) return false;
        Player entity = findCroesus();
        if (entity == null) {
            modMessage("No croesus entity returned!");
            return false;
        }

        double dist = entity.distanceToSqr(mc.player);

        if (dist > 16) {
            modMessage("Croesus too far! " + dist);
            return false;
        }

//        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
//            Vec3 eyePos = mc.player.position().add(0.0d, mc.player.getEyeHeight(), 0.0d);
//            Vec3 vec3 = MathUtils.clamp(entity.getBoundingBox(), eyePos).subtract(entity.getX(), entity.getY(), entity.getZ());
//            InteractUtils.interactOnEntity(entity, vec3);
//        });

        if (!(Minecraft.getInstance().hitResult instanceof EntityHitResult entityHitResult) || entityHitResult.getType() == HitResult.Type.MISS) {
            RSA.chat(ChatFormatting.RED + "Not looking at an entity");
            return false;
        }
        Entity e = entityHitResult.getEntity();
        if (entity.distanceToSqr(e) > 9) {
            RSA.chat(ChatFormatting.RED + "Blocked by entity!");
            return false;
        }

        PacketOrderManager.register(PacketOrderManager.STATE.ATTACK, () -> InteractUtils.attackEntity(entity));
        return true;
    }

    private Player findCroesus() {
        Vec3 eyePos = mc.player.position().add(0.0d, mc.player.getEyeHeight(), 0.0d);
        AABB box = new AABB(eyePos, eyePos).inflate(AURA_RANGE, AURA_RANGE, AURA_RANGE);
        List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, box, e -> e.getDisplayName().getString().contains("Croesus"));

        if (stands.isEmpty()) {
            modMessage("Failed to find an entity named Croesus!");
            return null;
        }

        if (stands.size() > 1) {
            modMessage("found mode than one croesus stand??");
            return null;
        }

        ArmorStand stand = stands.getFirst();


        mc.level.getEntitiesOfClass(Player.class, box, e -> e.distanceToSqr(stand) == 0);

        List<Player> list = mc.level.getEntitiesOfClass(Player.class, box, e -> e.distanceToSqr(stand) == 0);

        if (list.isEmpty()) {
            modMessage("no croesus?");
            return null;
        }

        if (list.size() > 1) {
            modMessage("Found multiple croesus?");
            return null;
        }
        return list.getFirst();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiEvent.Loaded event) {
        if (mc.player == null || !(mc.player.containerMenu instanceof ChestMenu menu) || !running || action != Action.CROESUS || mc.screen == null || !mc.screen.getTitle().getString().equals("Croesus") || !Location.getArea().is(Island.DungeonHub)) return;

        currentPage = getPage(menu.slots);

        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.getItem().equals(Items.PLAYER_HEAD)) continue;

            RunType type = RunType.findByDisplayName(stack.getHoverName().getString());
            if (type == RunType.NONE) continue;

            if (ItemUtils.getCleanLore(stack).stream().noneMatch(s -> s.contains("No chests opened yet!"))) continue;

            TaskComponent.onMilli(this.getClickDelay().getValue().longValue(), () -> {
                action = Action.REWARDS;
                click(slot.index, inCroesus());
            });
            return;
        }

        ItemStack nextArrow = menu.slots.get(53).getItem();

        if (nextArrow.getItem() == Items.ARROW)  {
            clickOnDelay(53, this::inCroesus);
            return;
        }

        modMessage("All chests looted!");
        reset();
        close();
    }

    @SubscribeEvent
    public void onRewards(GuiEvent.Loaded event) {
        if (mc.player == null
                || !(mc.player.containerMenu instanceof ChestMenu menu)
                || !running
                || action != Action.REWARDS
                || mc.screen == null
                || !Location.getArea().is(Island.DungeonHub)) return;

        String title = mc.screen.getTitle().getString();

        RunType type = RunType.findByTitle(title);
        if(type == RunType.NONE) return;

        Floor floor = Floor.findByIndex(NumberUtils.convertRomanToArabic(title.split("- Floor")[1].trim()));
        if (type == RunType.MASTER_CATACOMBS) floor = Floor.findByIndex(floor.getIndex() + 7);

        List<Reward> chests = new ArrayList<>();

        for (Slot slot : menu.slots) {
            if (menu.slots.indexOf(slot) > 45) break;
            ItemStack stack = slot.getItem();
            if (!stack.getItem().equals(Items.PLAYER_HEAD)) continue;
            List<Component> components = ItemUtils.getLore(stack);
            List<String> lore = ItemUtils.getCleanLore(stack);
            ChestType chestType = Utils.findEnumByName(ChestType.class, ChatFormatting.stripFormatting(stack.getHoverName().getString()), ChestType.NONE);
            if (chestType == ChestType.NONE) continue;
            int costLine = lore.indexOf("Cost");

            Reward chest = getRewards(lore.subList(1, costLine - 1), lore.get(costLine + 1), components.subList(1, costLine - 1));
            if(chest == null) return;

            chest.slot = slot.index;
            chest.name = stack.getHoverName().getString();
            chest.chest.type = chestType;
            chests.add(chest);
        }

        Optional<Reward> bedrock = chests.stream().filter(c -> c.chest.type == ChestType.BEDROCK).findFirst();
        Optional<Reward> alwaysBuy = chests.stream().filter(c -> c.alwaysBuy).findFirst();

        ItemStack modifiers = menu.slots.get(32).getItem();
        List<Component> modiLore = ItemUtils.getLore(modifiers);

        Optional<Component> kismetLine = modiLore.stream().filter(s -> s.getString().contains("Kismet Feather")).findAny();

        boolean canKismet = kismetLine.isPresent() && kismetLine.get().getSiblings().size() > 1 && !kismetLine.get().getSiblings().get(1).getStyle().isStrikethrough();
//        boolean canChestKey = !modiLore.get(5).getStyle().isStrikethrough();

        if (bedrock.isPresent() && this.getKismets().getValue() && canKismet && this.getKismetFloors().get(floor.getName())) {
            if (bedrock.get().chest.profit < this.getKismetsMinProfit().getValue().floatValue() * 1000) {
                kismetting = true;
                action = Action.CHEST;

                if(bedrock.get().slot < 0 || bedrock.get().slot > 45) {
                    modMessage(ChatFormatting.DARK_RED + "Invalid slot! (" + bedrock.get().slot + ")");
                    reset();
                    return;
                }

                clickOnDelay(bedrock.get().slot, this::inRewards);
                return;
            }
        }
        Reward best = alwaysBuy.orElseGet(() -> getBestProfit(chests));

        if (best.slot < 0 || best.slot > 45) {
            modMessage(ChatFormatting.DARK_RED + "Invalid slot! (" + best.slot + ")");
            reset();
            return;
        }

        //todo: chest keys

        modMessage("Claiming the " + best.name + ChatFormatting.RESET + " chest, Profit: " + best.chest.profit);

        action = Action.CHEST;
        clickOnDelay(best.slot, this::inRewards);

        CroesusLoader.addRunLog(best.chest);
    }

    @SubscribeEvent
    public void onChest(GuiEvent.Loaded event) {

        if (mc.player == null
                || !(mc.player.containerMenu instanceof ChestMenu menu)
                || !running
                || action != Action.CHEST
                || mc.screen == null
                || !Location.getArea().is(Island.DungeonHub)) return;

        String title = mc.screen.getTitle().getString();

        ChestType chestType = Utils.findEnumByName(ChestType.class, ChatFormatting.stripFormatting(title.split(" ")[0]), ChestType.NONE);
        if(chestType == ChestType.NONE) return;

        if (kismetting) {
            kismetting = false;
            ItemStack kismetStack = menu.slots.get(50).getItem();
            if(ItemUtils.getCleanLore(kismetStack).stream().anyMatch(s -> s.contains("Bring a Kismet Feather"))) {
                modMessage(ChatFormatting.RED + "No kismets!");
                reset();
                close();
                return;
            }

            action = Action.REWARDS;
            clickOnDelay(50, this::inChest);
            return;
        }
        clickOnDelay(31, this::inChest);
        action = Action.CROESUS;
        TaskComponent.onMilli(this.getClickDelay().getValue().longValue() * 2, () -> TaskComponent.onTick(0, this::clickCroesus));
    }

    @SubscribeEvent()
    public void onClose(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundContainerClosePacket) || !Location.getArea().is(Island.DungeonHub)) return;
        if (action.equals(Action.IDLE)) return;
        modMessage("Stopped!");
        reset();
    }

    private boolean inCroesus() {
        return mc.screen != null && mc.screen.getTitle().getString().equals("Croesus");
    }

    private boolean inRewards() {
        if (mc.screen == null) return false;
        RunType type = RunType.findByTitle(mc.screen.getTitle().getString());
        return type != RunType.NONE;
    }

    private boolean inChest() {
        if (mc.screen == null) return false;
        String title = ChatFormatting.stripFormatting(mc.screen.getTitle().getString());
        return Utils.findEnumByName(ChestType.class, title.split(" ")[0].trim(), ChestType.NONE) != ChestType.NONE;
    }

    private void close() {
        TaskComponent.onTick(0, () -> {
            if (mc.player != null) mc.player.closeContainer() ;
        });
    }


    private int getPage(NonNullList<Slot> inv) {
        ItemStack nextArrow = inv.get(53).getItem();
        ItemStack lastArrow = inv.get(45).getItem();

        if(nextArrow.getItem() == Items.ARROW) {
            String line = ChatFormatting.stripFormatting(ItemUtils.getCleanLore(nextArrow).getFirst());
            return Integer.parseInt(line.split(" ")[1]) - 1;
        }
        else if(lastArrow.getItem() == Items.ARROW) {
            String line = ChatFormatting.stripFormatting(ItemUtils.getCleanLore(lastArrow).getFirst());
            return Integer.parseInt(line.split(" ")[1]) + 1;
        }
        return 1;
    }


    private void click(int slot, boolean inWindow) {
        if (mc.player == null || mc.gameMode == null || !inWindow) return;
        int wid = mc.player.containerMenu.containerId;
        if (wid < 0 || wid > 100) return;
        mc.gameMode.handleInventoryMouseClick(wid, slot, 0, ClickType.PICKUP, mc.player);
    }

    private void clickOnDelay(int slot, BooleanSupplier supplier) {
        TaskComponent.onMilli(this.getClickDelay().getValue().longValue(), () -> click(slot, supplier.getAsBoolean()));
    }

    private Reward getBestProfit(List<Reward> rewards, Reward ...excluding) {
        Reward best = null;
        for (Reward reward : rewards) {
            boolean skip = false;
            for (Reward r : excluding) {
                if (r.equals(reward)) {
                    skip = true;
                    break;
                }
            }
            if(skip) continue;
            if(best == null) {
                best = reward;
                continue;
            }
            if(reward.chest.profit > best.chest.profit) {
                best = reward;
            }
        }
        return best;
    }

    private Reward getRewards(List<String> itemLines, String cost, List<Component> components) {
        ChestInfo chestInfo = new ChestInfo();

        if(!cost.equals("§aFREE")) {
            Matcher matcher = costPattern.matcher(ChatFormatting.stripFormatting(cost));
            if (matcher.find()) {
                chestInfo.cost = Integer.parseInt(matcher.group(1).replace(",", ""));
            }
        }

        boolean alwaysBuy = false;
        for (int i = 0; i < itemLines.size(); i++) {
            String itemLine = ChatFormatting.stripFormatting(itemLines.get(i));
            Component component = components.get(i);
            ChestItem item = parseItem(itemLine, component);
            if(item == null) continue;
            double price = getSellPrice(item.getId(), true);

            if(price == -1) {
                modMessage(ChatFormatting.DARK_RED + "Failed to get a price! Exiting early");
                return null;
            }
            chestInfo.value += price * item.getQuantity();
            chestInfo.items.add(item);
            if (CroesusLoader.getAlwaysBuy().contains(item.getId())) alwaysBuy = true;
        }
        chestInfo.profit = chestInfo.value - chestInfo.cost;
        return new Reward(chestInfo, alwaysBuy);
    }

    private double getSellPrice(String sbId, boolean sellOrder) {
        if(CroesusLoader.getWorthless().contains(sbId)) {
            return 0;
        }

        if(PriceData.getBazaarCache().containsKey(sbId)) {
            PriceData.Price price = PriceData.getBazaarCache().get(sbId);
            return sellOrder ? price.order() : price.instant();
        }

        if(PriceData.getBinCache().containsKey(sbId)) {
            return PriceData.getBinCache().get(sbId);
        }
        modMessage(ChatFormatting.RED + "Failed to get price for " + sbId  +"! (" + PriceData.getBazaarCache().size() + " items in bazaar, " + PriceData.getBinCache().size() + " items in bin)");
        return 0;
    }

        private ChestItem parseItem(String item, Component component) {
        if (item.contains("Enchanted Book")) {
            Matcher matcher = bookPattern.matcher(item);
            if (!matcher.find()) return null;


            boolean ult = false;

            if (component.getSiblings().size() > 1) {
                Component comp = component.getSiblings().get(1);
                ult = comp.getStyle().isBold() && comp.getStyle().getColor() != null && comp.getStyle().getColor().equals(ULT_COLOUR);;
            }

            String bookName = matcher.group(1);
            String levelNumeral = matcher.group(2);

            int tier;
            if (!NumberUtils.isInteger(levelNumeral)) {
                tier = NumberUtils.convertRomanToArabic(levelNumeral);
            } else {
                tier = Integer.parseInt(levelNumeral);
            }

            String id = ("ENCHANTMENT_" + (ult ? "ULTIMATE_" : "") + bookName.toUpperCase().replace(" ", "_") + "_" + tier).replace("ULTIMATE_ULTIMATE_", "ULTIMATE_");

            return new ChestItem(id, 1);

        } else if (item.contains(" Essence ")) {
            Matcher matcher = essencePattern.matcher(item);
            if(!matcher.find()) return null;
            String type = matcher.group(1);
            String amount = matcher.group(2);
            return new ChestItem("ESSENCE_" + type.toUpperCase(), Integer.parseInt(amount));
        }

        String ite = ChatFormatting.stripFormatting(item);

        if (ITEM_REPLACEMENTS.containsKey(ite)) {
            return new ChestItem(ITEM_REPLACEMENTS.get(ite), 1);
        }

        Map<String, String> items = PriceData.getItemCache();

        for (Map.Entry<String, String> entry : items.entrySet()) {
            if(Objects.equals(entry.getValue(), ite) && !entry.getKey().startsWith("STARRED_")) {
                return new ChestItem(entry.getKey(), 1);
            }
        }
        modMessage("Failed to find id for " + ite);
        return null;
    }

    @Getter
    private enum RunType {
        MASTER_CATACOMBS("Master Mode The Catacombs", "Master Catacombs - Floor "),
        CATACOMBS("The Catacombs", "Catacombs - Floor "),
        KUUDRA("?", "?"),
        NONE("None", "None");

        private final String displayName;
        private final String rewardsTitle;

        RunType(String displayName, String rewardsTitle) {
            this.displayName = displayName;
            this.rewardsTitle = rewardsTitle;
        }
        public static RunType findByDisplayName(String nameFormatted) {
            String name = ChatFormatting.stripFormatting(nameFormatted);
            return Arrays.stream(RunType.values())
                    .filter(type -> name.equals(type.getDisplayName()))
                    .findFirst()
                    .orElse(RunType.NONE);
        }
        public static RunType findByTitle(String title) {
            String name = ChatFormatting.stripFormatting(title);
            return Arrays.stream(RunType.values())
                    .filter(type -> name.startsWith(type.getRewardsTitle()))
                    .findFirst()
                    .orElse(RunType.NONE);
        }
    }

    public enum ChestType {
        BEDROCK,
        OBSIDIAN,
        DIAMOND,
        GOLD,
        WOOD,
        NONE;
    }

    public static class ChestInfo {
        public ChestType type;
        public double cost;
        public transient double value; // transient = no save
        public transient double profit;
        public final List<ChestItem> items;
        public ChestInfo() {
            this.type = ChestType.NONE;
            this.cost = 0;
            this.value = 0;
            this.profit = 0;
            this.items = new ArrayList<>();
        }
    }

    @Getter
    public static class ChestItem {
        private final String id;
        private final int quantity;
        public ChestItem(String id, int quantity) {
            this.id = id;
            this.quantity = quantity;
        }
    }

    private static class Reward {
        public final ChestInfo chest;
        private int slot;
        private String name;
        private final boolean alwaysBuy;
        public Reward(ChestInfo chest, boolean alwaysBuy) {
            this.chest = chest;
            this.alwaysBuy = alwaysBuy;
            this.slot = -1;
            this.name = "unknown";
        }
    }

    private enum Action {
        CROESUS,
        REWARDS,
        CHEST,
        IDLE
    }
}
