package com.ricedotwho.rsa.module.impl.dungeon.puzzle;

import com.google.common.math.DoubleMath;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.module.api.SubModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.EtherUtils;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Getter
@SubModuleInfo(name = "Ice Fill", alwaysDisabled = false)
public class IceFill extends com.ricedotwho.rsm.module.impl.dungeon.puzzle.IceFill {
	public final BooleanSetting autoEnabled = new BooleanSetting("Auto Ice Fill", false);
	public final NumberSetting autoDelay = new NumberSetting("Delay", 0, 8, 0, 1);

	List<Pos> autoPath = null;
	int autoIndex = -1;
	int autoTicks = 0;

	public IceFill(Puzzles module) {
		super(module);
		this.registerProperty(autoEnabled, autoDelay);
	}

	@SubscribeEvent
	public void onClientTickStart(ClientTickEvent.Start event) {
		if (!this.autoEnabled.getValue()) return;
		if (this.path == null) this.autoPath = null;
		if (autoIndex < 0) return;
		if (this.autoPath == null) {
			autoIndex = -1;
			return;
		}
		int delay = this.autoDelay.getValue().intValue();
		if (delay < 1) return;
		assert mc.player != null;
		if (mc.player.getMainHandItem().getItem() != Items.DIAMOND_SHOVEL) return;
		++autoTicks;
		if (autoTicks < delay) return;
		autoTicks = 0;
		if (++autoIndex >= this.autoPath.size() - 1) return;
		doTeleport(autoIndex);
	}

	@SubscribeEvent
	public void onPacketReceive(PacketEvent.Receive event) {
		if (!this.autoEnabled.getValue()) return;
		if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket packet)) return;
		if (this.path == null) {
			this.autoPath = null;
			return;
		}
		if (this.autoPath == null) this.buildAutoPath();
		assert mc.player != null;
		if (mc.player.getMainHandItem().getItem() != Items.DIAMOND_SHOVEL) return;
		int index = this.findIndex(packet.change().position());
		int delay = this.autoDelay.getValue().intValue();
		if (index < 0 || index >= this.autoPath.size() - 1) {
			autoIndex = -1;
			return;
		} else if (autoIndex < 0) {
			autoIndex = index;
			autoTicks = 0;
			if (delay > 0) doTeleport(index);
		}
		if (delay < 1) doTeleport(index);
	}

	private void doTeleport(int index) {
		Pos cur = this.autoPath.get(index);
		Pos next = this.autoPath.get(index + 1);
		Pos diff = next.subtract(cur);
		float yaw = EtherUtils.getYawAndPitch(diff.x, diff.y, diff.z)[0];
		float pitch = diff.y > 0 ? 14 : 48;
		PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
			if (!SwapManager.checkClientItem(Items.DIAMOND_SHOVEL) || !SwapManager.checkServerItem(Items.DIAMOND_SHOVEL)) return;
			SwapManager.sendAirC08(yaw, pitch, false, false);
		});
	}

	private void buildAutoPath() {
		this.autoPath = new ArrayList<>();
		for (int i = 0; i < this.path.size(); ++i) {
			Pos cur = this.path.get(i);
			if (i == this.path.size() - 1) {
				this.autoPath.add(cur);
				continue;
			}
			Pos next = this.path.get(i + 1);
			Pos diff = next.subtract(cur);
			Pos dir = new Pos(diff.x, 0, diff.z).normalize();
			if (!DoubleMath.fuzzyEquals(diff.y, 0, 1e-6)) {
				this.autoPath.add(cur);
			} else if (!DoubleMath.fuzzyEquals(diff.x, 0, 1e-6)) {
				for (int j = 0; j < Math.abs(diff.x); ++j) {
					this.autoPath.add(cur.add(dir.multiply(j)));
				}
			} else if (!DoubleMath.fuzzyEquals(diff.z, 0, 1e-6)) {
				for (int j = 0; j < Math.abs(diff.z); ++j) {
					this.autoPath.add(cur.add(dir.multiply(j)));
				}
			}
		}
	}

	private int findIndex(Vec3 pos) {
		for (int i = 0; i < this.autoPath.size(); ++i) {
			if (this.autoPath.get(i).squaredDistanceTo(pos) < 1e-6) return i;
		}
		return -1;
	}
}
