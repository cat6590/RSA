package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@ModuleInfo(aliases = "Hitboxes", id = "SecretHitboxes", category = Category.DUNGEONS)
public class SecretHitboxes extends Module {

    private final BooleanSetting essence = new BooleanSetting("Essence", false);

    private final ModeSetting buttons = new ModeSetting("Buttons", "Off", List.of("Full", "Flat", "Off"));
    private final BooleanSetting ssButtonsOnly = new BooleanSetting("SS Buttons Only", false);

    private final ModeSetting levers = new ModeSetting("Levers", "Off", List.of("Full", "Half", "1.8", "Off"));
    private final ModeSetting preDevLevers = new ModeSetting("Predev Levers", "Off", List.of("Full", "Half", "1.8", "Off"));

    private static final ShapeData V47_LEVERS;
    private static final ShapeData HALF_LEVERS;

    private static final ShapeData BUTTON;
    private static final ShapeData BUTTON_POWERED;

    static {
        V47_LEVERS = new ShapeData(Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0));
        V47_LEVERS.add(Direction.DOWN, Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0));
        V47_LEVERS.add(Direction.NORTH, Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0));
        V47_LEVERS.add(Direction.SOUTH, Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0));
        V47_LEVERS.add(Direction.EAST, Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0));
        V47_LEVERS.add(Direction.WEST, Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0));

        HALF_LEVERS = new ShapeData(Block.box(0, 0.0, 0.0, 16.0, 10.0, 16.0));
        HALF_LEVERS.add(Direction.DOWN, Block.box(0, 0.0, 0.0, 16.0, 10.0, 16.0));
        HALF_LEVERS.add(Direction.NORTH, Block.box(0, 0.0, 10.0, 16, 16, 16.0));
        HALF_LEVERS.add(Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 6.0));
        HALF_LEVERS.add(Direction.EAST, Block.box(0.0, 0.0, 0.0, 6.0, 16.0, 16.0));
        HALF_LEVERS.add(Direction.WEST, Block.box(10.0, 0.0, 0.0, 16.0, 16.0, 16.0));

        double pow = 1 / 16.0;
        BUTTON_POWERED = new ShapeData(Shapes.box(0.0, 1.0 - pow, 0.0, 1.0, 1.0, 1.0), Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + pow, 1.0));
        BUTTON_POWERED.add(Direction.EAST, Shapes.box(0.0, 0.0, 0.0, pow, 1.0, 1.0));
        BUTTON_POWERED.add(Direction.WEST, Shapes.box(1.0 - pow, 0.0, 0.0, 1.0, 1.0, 1.0));
        BUTTON_POWERED.add(Direction.SOUTH, Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, pow));
        BUTTON_POWERED.add(Direction.NORTH, Shapes.box(0.0, 0.0, 1.0 - pow, 1.0, 1.0, 1.0));
        BUTTON_POWERED.add(Direction.UP, Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + pow, 1.0));
        BUTTON_POWERED.add(Direction.DOWN, Shapes.box(0.0, 1.0 - pow, 0.0, 1.0, 1.0, 1.0));

        double unpow = 2 / 16.0;
        BUTTON = new ShapeData(Shapes.box(0.0, 1.0 - unpow, 0.0, 1.0, 1.0, 1.0), Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + unpow, 1.0));
        BUTTON.add(Direction.EAST, Shapes.box(0.0, 0.0, 0.0, unpow, 1.0, 1.0));
        BUTTON.add(Direction.WEST, Shapes.box(1.0 - unpow, 0.0, 0.0, 1.0, 1.0, 1.0));
        BUTTON.add(Direction.SOUTH, Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, unpow));
        BUTTON.add(Direction.NORTH, Shapes.box(0.0, 0.0, 1.0 - unpow, 1.0, 1.0, 1.0));
        BUTTON.add(Direction.UP, Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + unpow, 1.0));
        BUTTON.add(Direction.DOWN, Shapes.box(0.0, 1.0 - unpow, 0.0, 1.0, 1.0, 1.0));
    }


    public SecretHitboxes() {
        this.registerProperty(
                essence,
                buttons,
                ssButtonsOnly,
                levers,
                preDevLevers
        );
    }

    public static VoxelShape getShape(BlockState state, BlockPos pos) {
        SecretHitboxes module = RSM.getModule(SecretHitboxes.class);
        if (!Location.getArea().is(Island.Dungeon) ||  module == null || !module.isEnabled() || mc.level == null) return null;

        Block block = state.getBlock();

        return switch (block) {
            case SkullBlock ignored when SecretAura.isValidSkull(pos, mc.level) -> module.essence.getValue() ? Shapes.block() : null;
            case LeverBlock ignored -> switch (isLamps(pos) ? module.preDevLevers.getValue() : module.levers.getValue()) {
                case "Full" -> Shapes.block();
                case "Half" ->
                        HALF_LEVERS.getShape(state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE), state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING));
                case "1.8" ->
                        V47_LEVERS.getShape(state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE), state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING));
                case null, default -> null;
            };
            case ButtonBlock ignored -> {
                if (module.buttons.is("Off")) yield null;
                if (!module.ssButtonsOnly.getValue() || isSS(pos)) {
                    if (module.buttons.is("Full")) yield Shapes.block();
                    ShapeData data = state.getValue(ButtonBlock.POWERED) ? BUTTON_POWERED : BUTTON;
                    yield data.getShape(state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE), state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING));
                }
                yield null;
            }
            default -> null;
        };
    }

    private static boolean isSS(BlockPos pos) {
        return pos.getX() == 110
                && pos.getY() >= 120 && pos.getY() <= 123
                && pos.getZ() >= 91 && pos.getZ() <= 95;
    }

    private static boolean isLamps(BlockPos pos) {
        return pos.getX() >= 58 && pos.getX() <= 62
                && pos.getY() >= 133 && pos.getY() <= 136
                && pos.getZ() == 142;
    }

    private static class ShapeData {
        private final VoxelShape ceil;
        private final VoxelShape floor;
        private final Map<Direction, VoxelShape> directions = new HashMap<>();

        public ShapeData(VoxelShape ceil, VoxelShape floor) {
            this.ceil = ceil;
            this.floor = floor;
        }

        public ShapeData(VoxelShape ceil) {
            this.ceil = ceil;
            this.floor = ceil;
        }

        public void add(Direction dir, VoxelShape shape) {
            this.directions.put(dir, shape);
        }

        public VoxelShape getShape(AttachFace face, Direction direction) {
            return switch (face) {
                case FLOOR -> this.floor;
                case CEILING -> this.ceil;
                default -> directions.getOrDefault(direction, null);
            };
        }
    }
}
