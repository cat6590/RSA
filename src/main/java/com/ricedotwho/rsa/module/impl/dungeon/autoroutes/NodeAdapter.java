package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitClick;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitEWRaytrace;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitSecrets;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.*;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.FileUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NodeAdapter implements JsonDeserializer<Node>, JsonSerializer<Node> {
    private static final Type posType = new TypeToken<Pos>() {}.getType();
    private static final Gson gson = FileUtils.getGson();

    @Override
    public Node deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("type").getAsString().toUpperCase(); // converting pmo
        Pos localPos = gson.fromJson(obj.get("localPos").getAsJsonObject(), posType);
        boolean start = obj.has("start") && obj.get("start").getAsBoolean();

        AwaitManager awaits = null;
        if (obj.has("awaits")) {
            awaits = deserializeAwait(obj.get("awaits"));
        }

        return switch (type) {
            case "ETHERWARP" -> new EtherwarpNode(localPos, gson.fromJson(obj.get("localTarget").getAsJsonObject(), posType), awaits, start);
            case "BAT" -> new BatNode(localPos, obj.get("yaw").getAsFloat(), obj.get("pitch").getAsFloat(), awaits, start);
            case "BOOM" -> new BoomNode(localPos, gson.fromJson(obj.get("target").getAsJsonObject(), posType), awaits, start);
            case "AOTV" -> new AotvNode(localPos, gson.fromJson(obj.get("rotationVec").getAsJsonObject(), posType), awaits, start);
            case "BREAK" -> new BreakNode(localPos, gson.fromJson(obj.getAsJsonArray("blocks"), new TypeToken<ArrayList<Pos>>() {}.getType()), awaits, start);
            case "USE" -> new UseNode(localPos, gson.fromJson(obj.get("rotationVec").getAsJsonObject(), posType), obj.get("itemID").getAsString(), obj.get("sneak").getAsBoolean(), awaits, start);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public JsonElement serialize(Node src, Type typeOfSrc, JsonSerializationContext context) {
        return src.serialize();
    }

    public AwaitManager deserializeAwait(JsonElement json) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        HashMap<AwaitType, AwaitCondition<?>> map = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            AwaitType type = AwaitType.byName(entry.getKey());
            AwaitCondition<?> condition = switch (type) {
                case CLICK -> new AwaitClick();
                case SECRETS -> new AwaitSecrets(entry.getValue().getAsInt());
                case ETHERWARP_TRACE -> new AwaitEWRaytrace();
            };

            map.put(type, condition);
        }

        return new AwaitManager(map);
    }
}
