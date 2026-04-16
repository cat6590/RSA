package com.ricedotwho.rsa.module.impl.render;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.utils.render.render3d.type.Beacon;
import com.ricedotwho.rsm.utils.render.render3d.type.Circle;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.StringUtil;

@Getter
@ModuleInfo(aliases = "PWaypoints", id = "PresetWaypoints", category = Category.RENDER)
public class PresetWaypoints extends Module {
    private final BooleanSetting AbiPhones = new BooleanSetting("AbiPhones", false, () -> true); // done
    private final BooleanSetting Dean = new BooleanSetting("Dean", false, () -> true);//done
    private final BooleanSetting Dailies = new BooleanSetting("Dailies", false, () -> true);//done
    private final BooleanSetting Keys = new BooleanSetting("Kuudra Keys", false, () -> true);//done
    private final BooleanSetting BlackSmiths = new BooleanSetting("BlackSmiths", false, () -> true);//done
    private final BooleanSetting MinionShops = new BooleanSetting("Minion Shops", false, () -> true);//done
    private final BooleanSetting ChickenCoop = new BooleanSetting("Chicken Coop", false, () -> true);//done
    private final BooleanSetting Dojo = new BooleanSetting("Dojo", false, () -> true);//done
    private final BooleanSetting Matriarch = new BooleanSetting("Matriarch", false, () -> true);
    private final BooleanSetting MiniBosses = new BooleanSetting("MiniBosses", false, () -> true);//done
    private final BooleanSetting Duels = new BooleanSetting("Duels", false, () -> true);//done
    private final BooleanSetting BaarBerserkers = new BooleanSetting("Baar Npc (+250 rep)", false, () -> true);//done

    //Abiphones.
    Pos AbiphoneMage = new Pos(-78.5, 107, -791.5);
    Pos AbiphoneHub = new Pos(66.5, 72, -63.5);

    //Baar
    Pos BeerLocation = new Pos(-637.5, 123, -792);

    //Dean
    Pos DeanLocation = new Pos(-16.5, 123, -882.5);

    //Dailies
    Pos DailiesLocationMage = new Pos(-124.5, 92, -754.5);
    Pos DailiesLocationBers = new Pos(-579.5, 100, -687.5);

    //Kuudra Keys
    Pos KeyLocationMage = new Pos(-132.5, 89, -721.5);
    Pos KeyLocationBers = new Pos(-581.5, 99, -711.5);

    //Blacksmiths
    Pos BersBlacksmith = new Pos(-548.5, 98, -707.5);
    Pos MageBlacksmith = new Pos(-81.5, 92, -734.5);

    //Minion Shops
    Pos BersMinionShop = new Pos(-645.5, 101, -825.5);
    Pos MageMinionShop = new Pos(-45.5, 107, -779.5);

    //Mini Bosses
    Pos BersMiniBoss = new Pos(-535.5, 117, -904.5);
    Pos MageMiniBoss = new Pos(-180.5, 105, -859.5);
    Pos Ashfang = new Pos(-485.5, 135, -1016.5);
    Pos MagmaBoss = new Pos(-367.5, 63, -792.5);
    Pos BladeSoul = new Pos(-296.5, 82, -517.5);

    //Duels
    Pos BersDuels = new Pos(-597.5, 113, -638.5);
    Pos MageDuels = new Pos(149.5, 106, -852.5);

    //dojo
    Pos DojoLocation = new Pos(-235.5, 108, -597.5);

    //Chicken coop
    Pos ChickenCoopLocation = new Pos(-32.5, 93, -816.5);

    //Matriarch
    Pos MatriarchLocation = new Pos(-531.5, 40, -889.5);


    public PresetWaypoints() {
        this.registerProperty(
                BlackSmiths,
                MinionShops,
                ChickenCoop,
                AbiPhones,
                Dean,
                Dailies,
                Keys,
                Dojo,
                Matriarch,
                MiniBosses,
                Duels,
                BaarBerserkers
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
    public void onChat(ChatEvent.Chat event){
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        String unformatted = StringUtil.stripColor(event.getMessage().getString());


       if(BaarBerserkers.getValue()){
           if(unformatted.contains("Hello there, adventurer!")){
               RSA.chat("Hello BAAR!!");
           }
           if(unformatted.contains("To start out I'll need some generic gold to experiment on, could you get me a stack?")){
               RSA.chat("Baar: 64x Gold ingots.");
           }
           if(unformatted.contains("Next I need some flat gold to test how reflective gold is, could you try forging gold into 5 gold plates?")){
               RSA.chat("Baar: 5x Golden Plates. (/bz Golden plate)");
           }
           if(unformatted.contains("I heard there exist golden boots that helps you swim faster in water. I would like to test their magical properties, could you bring me them?")){
               RSA.chat("Baar: Divers Boots. (/ahs Diver's Boots)");
           }
           if(unformatted.contains("It seems like the first piece you brought me is only 25% of the magical power, could you get me the chestplate?")){
               RSA.chat("Baar: Divers Shirt. (/ahs Diver's Shirt)");
           }
           if(unformatted.contains("Now I need a lot of compacted gold, it has to be extremely dense. A half stack should do.")){
               RSA.chat("Baar: 32x Enchanted Gold Block. (/bz Enchanted Gold Block)");
           }
           if(unformatted.contains("There is a fine-grained gold substance somewhere in the Hub, I'll need 5 of that.")){
               RSA.chat("Baar: 5x Golden Powder. (/bz Golden Powder)");
           }
           if(unformatted.contains("Next I'm going to need a vegetable that is made out of solid gold. I want to experiment with how gold interacts with organics, maybe you can find some, like a half stack?")){
               RSA.chat("Baar: 32x Enchanted Golden Carrot (/bz Enchanted Golden Carrot)");
           }
           if(unformatted.contains("I just need one last thing, there's an extremely dangerous scientist who sells an assortment of items, he has a special rounded type of gold. Try to convince him to sell you it.")){
               RSA.chat("Baar: 1x Golden Ball (/bz Golden Ball)");
           }
           if(unformatted.contains("As promised, here is your reward.")){
               RSA.chat("yw for the help :)");
           }
       }

    }

    @SubscribeEvent
    public void onRender3D(Render3DEvent.Extract event){
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        if(AbiPhones.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
            Renderer3D.addTask(new Beacon(AbiphoneMage, Colour.MAGENTA));
            Renderer3D.addTask(new Circle(AbiphoneMage, false, .5F, Colour.MAGENTA, 12));
            }
            if(Location.getArea() == Island.Hub){
            Renderer3D.addTask(new Beacon(AbiphoneHub, Colour.MAGENTA));
            Renderer3D.addTask(new Circle(AbiphoneHub, false, .5F, Colour.MAGENTA, 12));
            }
        }

        if(BaarBerserkers.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(BeerLocation, Colour.RED));
                Renderer3D.addTask(new Circle(BeerLocation, false, .5F, Colour.RED, 12));
            }
        }

        if(Dean.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(DeanLocation, Colour.PINK));
                Renderer3D.addTask(new Circle(DeanLocation, false, .5F, Colour.PINK, 12));
            }
        }

        if(Dailies.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(DailiesLocationMage, Colour.YELLOW));
                Renderer3D.addTask(new Circle(DailiesLocationMage, false, .5F, Colour.YELLOW, 12));
                Renderer3D.addTask(new Beacon(DailiesLocationBers, Colour.YELLOW));
                Renderer3D.addTask(new Circle(DailiesLocationBers, false, .5F, Colour.YELLOW, 12));
            }
        }

        if(Keys.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(KeyLocationMage, Colour.ORANGE));
                Renderer3D.addTask(new Circle(KeyLocationMage, false, .5F, Colour.ORANGE, 12));
                Renderer3D.addTask(new Beacon(KeyLocationBers, Colour.ORANGE));
                Renderer3D.addTask(new Circle(KeyLocationBers, false, .5F, Colour.ORANGE, 12));
            }
        }

        if(BlackSmiths.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(BersBlacksmith, Colour.GRAY));
                Renderer3D.addTask(new Circle(BersBlacksmith, false, .5F, Colour.GRAY, 12));
                Renderer3D.addTask(new Beacon(MageBlacksmith, Colour.GRAY));
                Renderer3D.addTask(new Circle(MageBlacksmith, false, .5F, Colour.GRAY, 12));
            }
        }

         if(MinionShops.getValue()){
            if(Location.getArea() == Island.CrimsonIsle){
                Renderer3D.addTask(new Beacon(BersMinionShop, Colour.GREEN));
                Renderer3D.addTask(new Circle(BersMinionShop, false, .5F, Colour.GREEN, 12));
                Renderer3D.addTask(new Beacon(MageMinionShop, Colour.GREEN));
                Renderer3D.addTask(new Circle(MageMinionShop, false, .5F, Colour.GREEN, 12));
            }
         }

         if(MiniBosses.getValue()){
             if(Location.getArea() == Island.CrimsonIsle){
                 Renderer3D.addTask(new Beacon(BersMiniBoss, Colour.RED));
                 Renderer3D.addTask(new Circle(BersMiniBoss, false, .5F, Colour.RED, 12));
                 Renderer3D.addTask(new Beacon(MageMiniBoss, Colour.RED));
                 Renderer3D.addTask(new Circle(MageMiniBoss, false, .5F, Colour.RED, 12));
                 Renderer3D.addTask(new Beacon(Ashfang, Colour.RED));
                 Renderer3D.addTask(new Circle(Ashfang, false, .5F, Colour.RED, 12));
                 Renderer3D.addTask(new Beacon(MagmaBoss, Colour.RED));
                 Renderer3D.addTask(new Circle(MagmaBoss, false, .5F, Colour.RED, 12));
                 Renderer3D.addTask(new Beacon(BladeSoul, Colour.RED));
                 Renderer3D.addTask(new Circle(BladeSoul, false, .5F, Colour.RED, 12));
             }
         }

         if(Duels.getValue()) {
             if (Location.getArea() == Island.CrimsonIsle) {
                 Renderer3D.addTask(new Beacon(BersDuels, Colour.BLUE));
                 Renderer3D.addTask(new Circle(BersDuels, false, .5F, Colour.BLUE, 12));
                 Renderer3D.addTask(new Beacon(MageDuels, Colour.BLUE));
                 Renderer3D.addTask(new Circle(MageDuels, false, .5F, Colour.BLUE, 12));
             }
         }
         if(Dojo.getValue()){
             if(Location.getArea() == Island.CrimsonIsle){
                 Renderer3D.addTask(new Beacon(DojoLocation, Colour.CYAN));
                 Renderer3D.addTask(new Circle(DojoLocation, false, .5F, Colour.CYAN, 12));
             }
         }
         if(ChickenCoop.getValue()){
             if(Location.getArea() == Island.CrimsonIsle){
                 Renderer3D.addTask(new Beacon(ChickenCoopLocation, Colour.WHITE));
                 Renderer3D.addTask(new Circle(ChickenCoopLocation, false, .5F, Colour.WHITE, 12));
             }
         }
         if(Matriarch.getValue()){
             if(Location.getArea() == Island.CrimsonIsle){
                 Renderer3D.addTask(new Beacon(MatriarchLocation, Colour.ORANGE));
                 Renderer3D.addTask(new Circle(MatriarchLocation, false, .5F, Colour.ORANGE, 12));
             }
         }
    }
}
