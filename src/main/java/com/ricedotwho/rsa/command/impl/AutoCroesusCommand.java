package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.module.impl.dungeon.croesus.AutoCroesus;
import com.ricedotwho.rsa.module.impl.dungeon.croesus.CroesusLoader;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.utils.api.PriceData;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

@CommandInfo(name = "autocroesus", aliases = "ac", description = "Configuring and starting AutoCroesus")
public class AutoCroesusCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("go")
                        .executes(ctx -> {
                            RSM.getModule(AutoCroesus.class).start();
                            return 1;
                        })
                )
                .then(literal("forcego")
                        .executes(ctx -> {
                            RSM.getModule(AutoCroesus.class).start(false);
                            return 1;
                        })
                )
                .then(literal("alwaysbuy")
                        .executes(ctx -> {
                            AutoCroesus.modMessage("Always Buy: " + CroesusLoader.getAlwaysBuy());
                            return 1;
                        })
                        .then(literal("reset")
                                .executes(ctx -> {
                                    CroesusLoader.getAlwaysBuy().clear();
                                    AutoCroesus.modMessage("Cleared the always buy list");
                                    return 1;
                                })
                        )
                        .then(argument("sbId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String sbId = StringArgumentType.getString(ctx, "sbId").toUpperCase();

                                    if (sbId.equalsIgnoreCase("reset")) {
                                        CroesusLoader.getAlwaysBuy().clear();
                                        AutoCroesus.modMessage("Cleared the always buy list");
                                    } else if (CroesusLoader.getAlwaysBuy().contains(sbId)) {
                                        CroesusLoader.getAlwaysBuy().remove(sbId);
                                        CroesusLoader.saveAlwaysBuy();
                                        AutoCroesus.modMessage("Removed " + sbId + " from always buy");
                                    } else {
                                        CroesusLoader.getAlwaysBuy().add(sbId);
                                        CroesusLoader.saveAlwaysBuy();
                                        if (PriceData.getItemCache().containsKey(sbId)) {
                                            AutoCroesus.modMessage("Added " + sbId + "to always buy");
                                        } else {
                                            AutoCroesus.modMessage("Added " + sbId + " to always buy (This item is not known, please double check!)");
                                        }
                                    }
                                    return 1;
                                })
                        )
                )
                .then(literal("worthless")
                        .executes(ctx -> {
                            AutoCroesus.modMessage("Worthless: " + CroesusLoader.getWorthless());
                            return 1;
                        })
                        .then(literal("reset")
                                .executes(ctx -> {
                                    CroesusLoader.getWorthless().clear();
                                    AutoCroesus.modMessage("Cleared the worthless list");
                                    return 1;
                                })
                        )
                        .then(argument("sbId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String sbId = StringArgumentType.getString(ctx, "sbId").toUpperCase();

                                    if (sbId.equalsIgnoreCase("reset")) {
                                        CroesusLoader.getWorthless().clear();
                                        AutoCroesus.modMessage("Cleared the worthless list");
                                    } else if (CroesusLoader.getWorthless().contains(sbId)) {
                                        CroesusLoader.getWorthless().remove(sbId);
                                        CroesusLoader.saveWorthless();
                                        AutoCroesus.modMessage("Removed " + sbId + " from worthless");
                                    } else {
                                        CroesusLoader.getWorthless().add(sbId);
                                        CroesusLoader.saveWorthless();
                                        if (PriceData.getItemCache().containsKey(sbId)) {
                                            AutoCroesus.modMessage("Added " + sbId + "to worthless");
                                        } else {
                                            AutoCroesus.modMessage("Added " + sbId + " to worthless (This item is not known, please double check!)");
                                        }
                                    }
                                    return 1;
                                })
                        )
                );
    }
}