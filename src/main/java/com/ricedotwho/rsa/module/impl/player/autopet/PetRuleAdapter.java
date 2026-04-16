package com.ricedotwho.rsa.module.impl.player.autopet;

import com.google.gson.*;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.ChatPetRule;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.IslandPetRule;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.PetRule;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.RuleType;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import org.apache.commons.lang3.EnumUtils;

import java.lang.reflect.Type;

public class PetRuleAdapter implements JsonDeserializer<PetRule>, JsonSerializer<PetRule> {

    @Override
    public PetRule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        RuleType type = EnumUtils.getEnum(RuleType.class, obj.get("type").getAsString().toUpperCase());
        if (type == null) {
            throw new IllegalStateException("Unexpected value: " + obj.get("type").getAsString().toUpperCase());
        }
        String id = obj.get("id").getAsString();

        AutoPet ap = RSM.getModule(AutoPet.class);
        if (ap == null) throw new NullPointerException("AutoPet instance is null!");

        return switch (type) {
            case ISLAND ->  new IslandPetRule(id, ap::swapTo, EnumUtils.getEnum(Island.class, obj.get("island").getAsString(), Island.Unknown));
            case CHAT -> new ChatPetRule(id, ap::swapTo, obj.get("regex").getAsString());
        };
    }

    @Override
    public JsonElement serialize(PetRule src, Type typeOfSrc, JsonSerializationContext context) {
        return src.serialize();
    }
}
