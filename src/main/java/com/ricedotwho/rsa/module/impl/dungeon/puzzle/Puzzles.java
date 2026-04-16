package com.ricedotwho.rsa.module.impl.dungeon.puzzle;

import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.module.impl.dungeon.puzzle.TicTacToe;
import com.ricedotwho.rsm.ui.clickgui.settings.group.GroupSetting;
import lombok.Getter;

@Getter
@ModuleInfo(aliases = "Puzzles", id = "Puzzles", category = Category.DUNGEONS, isOverwrite = true)
public class Puzzles extends com.ricedotwho.rsm.module.impl.dungeon.puzzle.Puzzles {

    private final GroupSetting<TicTacToe> ticTacToe = new GroupSetting<>("TTT", new AutoTTT(this));
    private final GroupSetting<com.ricedotwho.rsm.module.impl.dungeon.puzzle.IceFill> iceFill = new GroupSetting<>("Ice Fill", new com.ricedotwho.rsa.module.impl.dungeon.puzzle.IceFill(this));

    public Puzzles() {
        this.registerProperty(
                ticTacToe,
                iceFill
        );
    }
}
