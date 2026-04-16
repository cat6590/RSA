package com.ricedotwho.rsa.module.impl;

import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import lombok.Getter;

@Getter
@ModuleInfo(aliases = "Template", id = "Template", category = Category.OTHER)
public class Template extends Module {

    public Template() {
        this.registerProperty(
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
}
