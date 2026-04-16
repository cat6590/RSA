package com.ricedotwho.rsa.module.impl.player;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.TickFreeze;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;

@Getter
@ModuleInfo(aliases = "Bonzo Helper", id = "BonzoHelper", category = Category.PLAYER)
public class BonzoHelper extends Module {
    private final BooleanSetting velo = new BooleanSetting("Await Velocity", false);
    private final NumberSetting timeout = new NumberSetting("Timeout", 0, 2000, 500, 10);
	private final NumberSetting time = new NumberSetting("Time", 0, 500, 100, 1);

    private boolean awaitingVelo = false;
    private long sentAt = 0;

	public BonzoHelper() {
		registerProperty(
                velo,
                timeout,
                time
        );
	}

	@SubscribeEvent
	public void onPacketSend(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundUseItemPacket packet) || mc.player == null || packet.getHand() != InteractionHand.MAIN_HAND) return;
		if (!Utils.equalsOneOf(ItemUtils.getID(mc.player.getMainHandItem()), "STARRED_BONZO_STAFF", "BONZO_STAFF")) return;
        if (velo.getValue() &&  mc.hitResult instanceof BlockHitResult && mc.player.getXRot() >= 70) {
            awaitingVelo = true;
            sentAt = System.currentTimeMillis();
            long a = sentAt;
            TaskComponent.onMilli(timeout.getValue().longValue(), () -> {
                if (a == sentAt && awaitingVelo) {
                    TickFreeze.unFreeze();
                    RSA.chat("Reached bonzo timeout!");
                }
            });
            TickFreeze.freeze();
        } else {
            TickFreeze.freeze(time.getValue().longValue());
        }
	}

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) || mc.player == null || motionPacket.getId() != mc.player.getId() || !awaitingVelo) return;
        TickFreeze.unFreeze();
        awaitingVelo = false;
    }
}
