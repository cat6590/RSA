package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.solver;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.solver.types.Rubix;
import com.ricedotwho.rsm.data.TerminalType;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.module.impl.dungeon.boss.p3.terminal.types.Term;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@ModuleInfo(aliases = "Terminal Solver", id = "TerminalSolver", category = Category.DUNGEONS, isOverwrite = true)
public class TerminalSolver extends com.ricedotwho.rsm.module.impl.dungeon.boss.p3.terminal.TerminalSolver {

    @Getter private static final BooleanSetting anyRubix = new BooleanSetting("Any Click Rubix", false);
    private final BooleanSetting offTickSlots = new BooleanSetting("Off Tick Slots", false);

    public TerminalSolver() {
        super();
        this.registerProperty(
                anyRubix,
                offTickSlots
        );

        getClickDelay().setMin(BigDecimal.ZERO);
    }

    @Override
    public Term create(TerminalType type, String title) {
        if (type == TerminalType.RUBIX) return new Rubix(title);
        return type.create(title);
    }
}
