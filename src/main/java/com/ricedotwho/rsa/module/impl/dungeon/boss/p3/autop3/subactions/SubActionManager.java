package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions;

import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public record SubActionManager(HashMap<SubActionType, SubAction> actions) {
    public SubActionManager() {
        this(new HashMap<>());
    }

    public boolean run() {
        for (SubAction arg : actions.values()) {
            if (!arg.execute()) return true;
        }
        return false;
    }

    public Collection<SubAction> getActions() {
        return actions.values();
    }

    public void addAction(SubAction action) {
        actions.put(action.getType(), action);
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        for (SubAction arg : actions.values()) {
            arg.serialize(obj);
        }
        return obj;
    }

    public boolean has(SubActionType type) {
        return actions.containsKey(type);
    }

    public String getList() {
        if (actions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        List<SubAction> actions = getActions().stream().toList();
        for (int i = 0; i < actions.size(); i++) {
            SubAction action = actions.get(i);
            boolean last = i == actions.size() - 1;
            sb.append(action.getType().name().toLowerCase());
            if (!last) sb.append(", ");
        }
        return sb.toString();
    }
}
