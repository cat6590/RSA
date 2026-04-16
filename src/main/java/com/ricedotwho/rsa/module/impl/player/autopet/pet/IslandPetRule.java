package com.ricedotwho.rsa.module.impl.player.autopet.pet;

import com.google.gson.JsonObject;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.LocationEvent;

import java.util.function.Consumer;

public class IslandPetRule extends PetRule {
    private final Island location;

    public IslandPetRule(String id, Consumer<String> callback, Island location) {
        super(id, callback);
        this.location = location;
    }

    @SubscribeEvent
    public void onLocationChange(LocationEvent.Changed event) {
        if (event.getNewIsland() != location) return;
        this.callback.accept(getId());
    }

    @Override
    public String toString() {
        return "IslandPetRule -> " + location.getEnumName() + " -> " + this.getId();
    }

    @Override
    protected RuleType getType() {
        return RuleType.ISLAND;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("island", this.location.name());
        return obj;
    }
}
