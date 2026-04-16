package com.ricedotwho.rsa.event.impl;

import com.ricedotwho.rsm.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RawTickEvent extends Event {
    private final boolean cancel;
}
