package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto;

import com.ricedotwho.rsa.component.impl.TickFreeze;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals.TerminalRenderer;
import com.ricedotwho.rsm.component.impl.Terminals;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.TerminalType;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.render.Render2DEvent;
import com.ricedotwho.rsm.module.SubModule;
import com.ricedotwho.rsm.module.api.SubModuleInfo;
import com.ricedotwho.rsm.module.impl.dungeon.boss.p3.terminal.TerminalSolver;
import com.ricedotwho.rsm.module.impl.dungeon.boss.p3.terminal.types.Melody;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.Utils;
import com.ricedotwho.rsm.utils.render.render2d.NVGUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import org.joml.Vector2d;

import java.util.Arrays;
import java.util.List;

@Getter
@SubModuleInfo(name = "InvWalk", alwaysDisabled = false)
public class InvWalk extends SubModule<AutoTerms> {

    private final ModeSetting style = new ModeSetting("Style", "Items", Arrays.asList("Solver", "Items"));
    @Getter private static final BooleanSetting useOverrides = new BooleanSetting("Use Overrides", true);
    private final BooleanSetting renderTitles = new BooleanSetting("Render title thing", true);
    private final BooleanSetting titleMCFont = new BooleanSetting("Title MC Font", true);
    private final BooleanSetting renderClicksLeft = new BooleanSetting("Render clicks left", true);
    private final BooleanSetting clicksMCFont = new BooleanSetting("Clicks MC Font", true);
    private final ColourSetting titleColour = new ColourSetting("Title Colour", new Colour(96,31,158));
    private final ColourSetting remainingColour = new ColourSetting("Remaining Colour", new Colour(96,31,158));
    private final ColourSetting clicksColour = new ColourSetting("Clicks Colour", new Colour(0, 191, 0));
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", false);

    private final ModeSetting moveDelayMode = new ModeSetting("Mode Delay", "Freeze", List.of("Stop Inputs", "Freeze"));
    private final NumberSetting melodyMoveDelay = new NumberSetting("Melody Move Delay", 0, 500, 300, 50);
    private final BooleanSetting invwalkMaybeFix = new BooleanSetting("Invwalk maybe fix", false);

    private final DragSetting termTitle = new DragSetting("Term Title", new Vector2d(10, 10), new Vector2d(150, 15));
    private final DragSetting clicksText = new DragSetting("Clicks Text", new Vector2d(10, 10), new Vector2d(150, 15));
    private final DragSetting gui = new DragSetting("Visualiser Gui", new Vector2d(551, 330), new Vector2d(144, 80));

    private final TerminalRenderer terminalRenderer;
    public int melodyMoveCounter = 0;
    private long lastMelodyClick = 0;

    public InvWalk(AutoTerms module) {
        super(module);
        this.registerProperty(
                style,
                useOverrides,
                renderTitles,
                titleMCFont,
                renderClicksLeft,
                clicksMCFont,
                titleColour,
                remainingColour,
                clicksColour,
                textShadow,
                moveDelayMode,
                melodyMoveDelay,
                termTitle,
                clicksText,
                gui,
                invwalkMaybeFix
        );
        this.terminalRenderer = new TerminalRenderer();
    }

    @Override
    public void reset() {
        melodyMoveCounter = 0;
    }

    @SubscribeEvent
    public void onRenderGui(Render2DEvent event) {
        try {
            if (!module.isInTerm()) return;
            int slots = Utils.getGuiSlotCount(module.getTerminalContainer().getType());

            if (this.renderClicksLeft.getValue() && Terminals.getCurrent() != null) {
                String remainingText = "Clicks remaining: ";
                String clicks = Terminals.getCurrent() instanceof Melody mel ? mel.getProgress() + "/4" : String.valueOf(Terminals.getCurrent().getSolution().size());
                if (clicksMCFont.getValue()) {
                    clicksText.renderScaledGFX(event.getGfx(), () -> {
                        event.getGfx().drawString(mc.font, remainingText, 0, 0, this.remainingColour.getValue().getRGB());
                        event.getGfx().drawString(mc.font, clicks, mc.font.width(remainingText), 0, this.clicksColour.getValue().getRGB());
                    }, 150, 15);
                } else {
                    clicksText.renderScaled(event.getGfx(), () -> {
                        NVGUtils.drawText(remainingText, 0, 0, 14, this.remainingColour.getValue(), textShadow.getValue(), NVGUtils.PRODUCT_SANS);
                        NVGUtils.drawText(clicks, NVGUtils.getTextWidth(remainingText, 14, NVGUtils.PRODUCT_SANS), 0, 14, this.clicksColour.getValue(), textShadow.getValue(), NVGUtils.PRODUCT_SANS);
                    }, 150, 15);
                }
            }

            if (this.renderTitles.getValue() && Terminals.getCurrent() != null) {
                String termText = "In " + Utils.capitalise(Terminals.getCurrent().getType().name().replace("_", " ").toLowerCase());
                if (Terminals.getCurrent().getType().equals(TerminalType.MELODY)) {
                    int moveDelay = melodyMoveDelay.getValue().intValue();
                    long now = System.currentTimeMillis();
                    if (lastMelodyClick + moveDelay > now) termText += " " + (lastMelodyClick - now + moveDelay) + "ms";
                }
                String finalTermText = termText;
                if (titleMCFont.getValue()) {
                    termTitle.renderScaledGFX(event.getGfx(), () -> {
                        event.getGfx().drawString(mc.font, finalTermText, 0, 0, this.titleColour.getValue().getRGB());
                    }, 150, 15);
                } else {
                    termTitle.renderScaled(event.getGfx(), () -> NVGUtils.drawText(finalTermText, 0, 0, 14, this.titleColour.getValue(), textShadow.getValue(), NVGUtils.PRODUCT_SANS), 150, 15);
                }
            }

            if (this.style.is("Items")) {
                float width = 9 * 16f;
                float height = (float) (Math.floor(slots / 9f) * 16);
                gui.renderScaledGFX(event.getGfx(), () -> this.terminalRenderer.renderItems(event.getGfx(), module.getTerminal()), width, height);
            } else {
                float gap = 32 + TerminalSolver.getGap().getValue().floatValue();
                gui.renderScaled(event.getGfx(), () -> this.terminalRenderer.renderSolver(gap), 9 * gap, slots / 9f * gap);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // This works for strafe but not for forwards and backwards for some reason
    @SubscribeEvent
    public void onPollInput(InputPollEvent event) {
        if (this.melodyMoveCounter < 1) return;

        if (Minecraft.getInstance().screen == null && !module.isInTerm()) {
            this.melodyMoveCounter = 0;
            return;
        }

        Input oldInputs = event.getClientInput();
        Input newInputs = new Input(false, false, false, false, false, oldInputs.shift(), false);
        event.getInput().apply(newInputs);

        this.melodyMoveCounter--;
    }

    public void onMelodyClick() {
        lastMelodyClick = System.currentTimeMillis();
        if (this.moveDelayMode.is("Freeze")) {
            TickFreeze.freeze(this.melodyMoveDelay.getValue().longValue(), true);
        } else {
            this.melodyMoveCounter = (this.melodyMoveDelay.getValue().intValue() / 50);
        }
    }
}
