package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder;

import com.google.gson.*;

import java.lang.reflect.Type;

public class PlayerInputAdapter implements JsonDeserializer<MovementRecorder.PlayerInput>, JsonSerializer<MovementRecorder.PlayerInput> {

    @Override
    public MovementRecorder.PlayerInput deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        boolean using = obj.has("using");
        return new MovementRecorder.PlayerInput(
                obj.get("yaw").getAsFloat(),
                obj.get("pitch").getAsFloat(),
                using,
                using ? de(obj.get("useItem").getAsJsonObject()) : null,
                obj.get("forward").getAsBoolean(),
                obj.get("backward").getAsBoolean(),
                obj.get("left").getAsBoolean(),
                obj.get("right").getAsBoolean(),
                obj.get("jump").getAsBoolean(),
                obj.get("sneak").getAsBoolean(),
                obj.get("sprint").getAsBoolean()
        );
    }

    private MovementRecorder.UseItem de(JsonObject obj) {
        return new MovementRecorder.UseItem(
                obj.get("itemId").getAsString(),
                obj.get("yaw").getAsFloat(),
                obj.get("pitch").getAsFloat()
        );
    }

    @Override
    public JsonElement serialize(MovementRecorder.PlayerInput src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("yaw", src.yaw);
        obj.addProperty("pitch", src.pitch);
        if (src.using && src.useItem != null) {
            obj.addProperty("using", true);
            JsonObject use = new JsonObject();
            use.addProperty("itemId", src.useItem.item);
            use.addProperty("yaw", src.useItem.yaw);
            use.addProperty("pitch", src.useItem.pitch);
            obj.add("useItem", use);
        }
        obj.addProperty("forward", src.forward);
        obj.addProperty("backward", src.back);
        obj.addProperty("left", src.left);
        obj.addProperty("right", src.right);
        obj.addProperty("jump", src.jump);
        obj.addProperty("sneak", src.sneak);
        obj.addProperty("sprint", src.sprint);
        return obj;
    }
}

