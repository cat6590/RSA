package com.ricedotwho.rsa.module.impl.player.autopet.pet;

import com.google.gson.JsonObject;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ChatPetRule extends PetRule {
    private final String regex;
    private transient final Pattern pattern;

    public ChatPetRule(String id, Consumer<String> callback, String regex) {
        super(id, callback);
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        String message = event.getMessage().getString();
        if (!this.pattern.matcher(event.getMessage().getString()).find() && !this.regex.equals(message)) return;
        this.callback.accept(getId());
    }

    @Override
    public String toString() {
        return "ChatPetRule -> " + regex + " -> " + this.getId();
    }

    @Override
    protected RuleType getType() {
        return RuleType.CHAT;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("regex", this.regex);
        return obj;
    }
}
