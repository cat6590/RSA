package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CenterType {
    POS("pos"),
    ANGLES("angles"),
    ALL("all"),
    YAW("yaw"),
    PITCH("pitch");

    private final String name;

    CenterType(String name) {
        this.name = name;
    }

    public static CenterType fromName(String name) {
        return Arrays.stream(CenterType.values()).filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

}
