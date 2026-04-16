package com.ricedotwho.rsa.module.impl.other;

import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import lombok.Getter;
import net.minecraft.util.StringUtil;

@Getter
@ModuleInfo(aliases = "Kicked", id = "Kicked", category = Category.OTHER)
public class Kicked extends Module {
    boolean kicked = false;

    public void onChat(ChatEvent event) {

        String unformatted = StringUtil.stripColor(event.getMessage().getString());

    }

}
