package com.ricedotwho.rsa.screen;

import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.component.impl.pathfinding.GoalDungeonRoom;
import com.ricedotwho.rsa.component.impl.pathfinding.GoalDungeonXYZ;
import com.ricedotwho.rsa.module.impl.dungeon.DynamicRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.AutoRoutes;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.Node;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonInfo;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonScanner;
import com.ricedotwho.rsm.component.impl.map.map.*;
import com.ricedotwho.rsm.utils.EtherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class MapScreen extends Screen {
    private double scaledMouseX;
    private double scaledMouseY;
    private final DynamicRoutes dynamicRoutes;
    private UniqueRoom selectedRoom;

    public MapScreen(DynamicRoutes dynamicRoutes) {
        super(Component.literal("Map Screen"));
        this.dynamicRoutes = dynamicRoutes;
    }

    public int getMapSize() {
        return 300;
    }

    private void renderUnique(GuiGraphics ctx, UniqueRoom uniqueRoom, int mapX, int mapY) {
        if (uniqueRoom.getTiles().isEmpty()) return;
        List<Room> rooms = uniqueRoom.getTiles().stream().filter(t -> !t.isSeparator()).toList();
        if (rooms.isEmpty()) return;

        int scaledConnectorSize = (int) (3F / 10.0F * getMapSize());
        int scaledRoomSize = (int) (13.16666667F / 10.0F * getMapSize());

        RoomData data = rooms.getFirst().getData();
        if (data == null) return;

        Room extra = null;
        if (rooms.size() == 3 && data.shape() == RoomShape.L) {
            Room a = rooms.get(0), b = rooms.get(1), c = rooms.get(2);
            if (a.getZ() == b.getZ()) {
                extra = c;
                rooms = List.of(a, b);
            } else if (a.getZ() == c.getZ()) {
                extra = b;
                rooms = List.of(a, c);
            } else {
                extra = a;
                rooms = List.of(b, c);
            }
        }

        int minX = (rooms.stream().min(Comparator.comparingInt(Room::getX)).get().getX() - DungeonScanner.startX) >> 5;
        int minZ = (rooms.stream().min(Comparator.comparingInt(Room::getZ)).get().getZ() - DungeonScanner.startZ) >> 5;
        int maxX = (rooms.stream().max(Comparator.comparingInt(Room::getX)).get().getX() - DungeonScanner.startX) >> 5;
        int maxZ = (rooms.stream().max(Comparator.comparingInt(Room::getZ)).get().getZ() - DungeonScanner.startZ) >> 5;

        int x1 = (int)(10.0f * mapX) + scaledConnectorSize + minX * (scaledRoomSize + scaledConnectorSize);
        int y1 = (int)(10.0f * mapY) + scaledConnectorSize + minZ * (scaledRoomSize + scaledConnectorSize);
        int x2 = x1 + (maxX - minX) * (scaledRoomSize + scaledConnectorSize) + scaledRoomSize;
        int y2 = y1 + (maxZ - minZ) * (scaledRoomSize + scaledConnectorSize) + scaledRoomSize;

        int color = getRoomColor(rooms.getFirst());

        boolean hovered = scaledMouseX >= x1 && scaledMouseX <= x2 && scaledMouseY >= y1 && scaledMouseY <= y2;

        int fx1 = 0, fy1 = 0, fx2 = 0, fy2 = 0;
        boolean extraHovered = false;

        if (extra != null) {
            int ex = (extra.getX() - DungeonScanner.startX) >> 5;
            int ez = (extra.getZ() - DungeonScanner.startZ) >> 5;

            int ex1 = (int)(10.0f * mapX) + scaledConnectorSize + ex * (scaledRoomSize + scaledConnectorSize);
            int ey1 = (int)(10.0f * mapY) + scaledConnectorSize + ez * (scaledRoomSize + scaledConnectorSize);
            int ex2 = ex1 + scaledRoomSize;
            int ey2 = ey1 + scaledRoomSize;

            if (ex < minX) {
                fx1 = ex1; fy1 = ey1; fx2 = x2; fy2 = ey2;
            } else if (ex > maxX) {
                fx1 = x1; fy1 = ey1; fx2 = ex2; fy2 = ey2;
            } else if (ez < minZ) {
                fx1 = ex1; fy1 = ey1; fx2 = ex2; fy2 = y2;
            } else {
                fx1 = ex1; fy1 = y1; fx2 = ex2; fy2 = ey2;
            }

            extraHovered = scaledMouseX >= fx1 && scaledMouseX <= fx2 && scaledMouseY >= fy1 && scaledMouseY <= fy2;
        }

        if (hovered || extraHovered) {
            ctx.fill(x1 - 10, y1 - 10, x2 + 10, y2 + 10, 0xFFFFFFFF);
            selectedRoom = uniqueRoom;
        }
        if (extra != null && (hovered || extraHovered)) ctx.fill(fx1 - 10, fy1 - 10, fx2 + 10, fy2 + 10, 0xFFFFFFFF);

        ctx.fill(x1, y1, x2, y2, color);
        if (extra != null) ctx.fill(fx1, fy1, fx2, fy2, color);

        // The text rendering thing is full AI btw
        // I cba
        Matrix3x2fStack matrices = ctx.pose();

        List<String> lines = new ArrayList<>(List.of(data.name().split("[ -]")));
        if (data.secrets() > 0) lines.add(String.valueOf(data.secrets()));
        if (lines.isEmpty()) return;

        int boxWidth = x2 - x1;
        int boxHeight = y2 - y1;
        int spacing = 2;

        int totalTextHeight = Minecraft.getInstance().font.lineHeight * lines.size() + spacing * (lines.size() - 1);
        int maxLineWidth = lines.stream().mapToInt(l -> Minecraft.getInstance().font.width(l)).max().orElse(1);

        int scaledTextSize = Math.max(1, Math.min(boxWidth * 110 / 100 / maxLineWidth, boxHeight * 110 / 100 / totalTextHeight));

        int textColor = getRoomTextColor(uniqueRoom);
        matrices.pushMatrix();
        matrices.scale(scaledTextSize);

        int totalHeight = Minecraft.getInstance().font.lineHeight * scaledTextSize * lines.size() + spacing * (lines.size() - 1);
        int yCoord = y1 + (boxHeight - totalHeight) / 2;

        for (String line : lines) {
            int textWidth = Minecraft.getInstance().font.width(line) * scaledTextSize;
            int xCoord = x1 + (boxWidth - textWidth) / 2;
            ctx.drawString(Minecraft.getInstance().font, line, xCoord / scaledTextSize, yCoord / scaledTextSize, textColor, true);
            yCoord += Minecraft.getInstance().font.lineHeight * scaledTextSize + spacing;
        }
        matrices.popMatrix();
    }

    private int getRoomTextColor(UniqueRoom uniqueRoom) {
        RoomState roomState = uniqueRoom.getTiles().getFirst().getState();
        return switch (roomState) {
            case GREEN -> 0xFF00FF00;
            case FAILED -> 0xFFFF0000;
            case CLEARED -> 0xFFFFFFFF;
            default -> 0xFFA0A0A0;
        };
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        // Don't call superclass

    }

    @Override
    public void mouseMoved(double d, double e) {
        this.scaledMouseX = d * 10;
        this.scaledMouseY = e * 10;
        super.mouseMoved(d, e);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        UniqueRoom room = selectedRoom;
        if (room == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return super.mouseClicked(mouseButtonEvent, bl);
        if (dynamicRoutes.isPathing()) dynamicRoutes.cancelPathing();
        switch (mouseButtonEvent.button()) {
            case 1 -> {
                AutoRoutes autoRoutes = RSM.getModule(AutoRoutes.class);
                if (autoRoutes.isEnabled()) {
                    List<Node> starts = autoRoutes.getStartNodes(selectedRoom);
                    if (starts != null && !starts.isEmpty()) {
                        Vec3 startPos = Minecraft.getInstance().player.position();

                        Node closestStart = starts
                                .stream()
                                .filter(n -> n.isStart() && Minecraft.getInstance().level.getChunkSource().hasChunk(Mth.floor(n.getRealPos().x) >> 4, Mth.floor(n.getRealPos().z) >> 4))
                                .min(Comparator.comparingDouble(n -> n.getRealPos().squaredDistanceTo(startPos)))
                                .orElse(null);
                        if (closestStart != null) {
                            BlockPos pos = closestStart.getRealPos().subtract(0d, EtherUtils.EPSILON, 0d).asBlockPos();

                            GoalDungeonXYZ goal = GoalDungeonXYZ.create(pos);
                            if (goal != null) {
                                dynamicRoutes.executePath(Minecraft.getInstance().player.position(), goal);
                                if (SwapManager.swapItem("ASPECT_OF_THE_VOID")) {
                                    SwapManager.sendAirC08(Minecraft.getInstance().player.getYRot(), 90f, true, false);
                                }

                                break;
                            }
                        }
                    }
                }

                GoalDungeonRoom goal = GoalDungeonRoom.create(room);
                if (goal == null) return true;
                dynamicRoutes.executePath(Minecraft.getInstance().player.position(), goal);
                if (shouldCenter() && SwapManager.swapItem("ASPECT_OF_THE_VOID")) {
                    SwapManager.sendAirC08(Minecraft.getInstance().player.getYRot(), 90f, true, false);
                }
                break;
            }

            case 0 -> {
                //BlockPos startPos = BlockPos.containing(Minecraft.getInstance().player.position().subtract(0, EtherUtils.EPSILON, 0d));
                GoalDungeonRoom goal = GoalDungeonRoom.create(room);
                dynamicRoutes.executePath(Minecraft.getInstance().player.position(), goal);
                if (shouldCenter() && SwapManager.swapItem("ASPECT_OF_THE_VOID")) {
                    SwapManager.sendAirC08(Minecraft.getInstance().player.getYRot(), 90f, true, false);
                }
                break;
            }



            case 2 -> {

            }
        }
        return true;
    }

    private static boolean shouldCenter() {
        return Minecraft.getInstance().player == null || Minecraft.getInstance().player.position().subtract(Minecraft.getInstance().player.blockPosition().getBottomCenter()).horizontalDistanceSqr() > EtherUtils.EPSILON;
    }

    @Override
    public void render(GuiGraphics context, int i, int j, float f) {
        if (Minecraft.getInstance().player == null) return;
        if (!Location.getArea().is(Island.Dungeon) || Dungeon.isInBoss()) {
            this.onClose();
            return;
        }
        super.render(context, i, j, f);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int mapX = (screenWidth - getMapSize()) / 2;
        int mapY = (screenHeight - getMapSize()) / 2;

        //RenderUtils.drawOutlinedRectangle(context, mapX, mapY, getMapSize(), getMapSize(), 0xFF000000, borderWidth);
        context.fill(mapX, mapY, mapX + getMapSize(), mapY + getMapSize(), 0x34FFFFFF);

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.scale(0.1f);

        int scaledConnectorSize = (int) (3F / 10.0F * getMapSize());
        int scaledRoomSize = (int) (13.16666667F / 10.0F * getMapSize());
        Tile[] tiles = DungeonInfo.getDungeonList();

        for (int x = 0; x < 11; x++) {
            for (int y = 0; y < 11; y++) {
                if ((x & 1) == 0 && (y & 1) == 0) continue;
                if ((x & 1) == 1 && (y & 1) == 1) continue;

                Tile tile = tiles[x + y * 11];
                if (!(tile instanceof Door door)) continue;

                int cellX = (int)(10.0f * mapX) + scaledConnectorSize + (x / 2) * (scaledRoomSize + scaledConnectorSize);
                int cellY = (int)(10.0f * mapY) + scaledConnectorSize + (y / 2) * (scaledRoomSize + scaledConnectorSize);

                int narrowSize = scaledConnectorSize / 3 * 4;
                int x1, y1, x2, y2;
                if (x % 2 == 1) {
                    // horizontal connector — narrow in Y, full connector width in X
                    x1 = cellX + scaledRoomSize;
                    y1 = cellY + (scaledRoomSize - narrowSize) / 2;
                    x2 = x1 + scaledConnectorSize;
                    y2 = y1 + narrowSize;
                } else {
                    // vertical connector — narrow in X, full connector width in Y
                    x1 = cellX + (scaledRoomSize - narrowSize) / 2;
                    y1 = cellY + scaledRoomSize;
                    x2 = x1 + narrowSize;
                    y2 = y1 + scaledConnectorSize;
                }

                context.fill(x1, y1, x2, y2, getDoorColor(door));
            }
        }

        selectedRoom = null;
        DungeonInfo.getUniqueRooms().forEach(unique -> renderUnique(context, unique, mapX, mapY));

        float playerX = (float)(Minecraft.getInstance().player.getX() - DungeonScanner.startX) / 32.0f;
        float playerZ = (float)(Minecraft.getInstance().player.getZ() - DungeonScanner.startZ) / 32.0f;

        int px = (int)((10.0f * mapX) + scaledRoomSize / 2.0f + scaledConnectorSize + playerX * (scaledRoomSize + scaledConnectorSize));
        int pz = (int)((10.0f * mapY) + scaledRoomSize / 2.0f + scaledConnectorSize + playerZ * (scaledRoomSize + scaledConnectorSize));

        int headSize = getMapSize() * 3 / 4;
        float yaw = Minecraft.getInstance().player.getYRot() + 180f;

        matrices.translate(px, pz);
        matrices.rotate((float) Math.toRadians(yaw));
        //matrices.translate(-px, -pz);

        PlayerSkin skin = Minecraft.getInstance().player.getSkin();
        PlayerFaceRenderer.draw(context, skin, - headSize / 2, - headSize / 2, headSize);
        // matrix is still rotated here btw

        matrices.popMatrix();
    }

    private static int getDoorColor(Door door) {
        return applyRoomStateDarken(door.getState(), getDoorPureColor(door));
    }

    private static int getDoorPureColor(Door door) {
        return switch (door.getType()) {
            case ENTRANCE -> 0xFF148500;
            case BLOOD -> 0xFFFF0000;
            case WITHER -> door.isOpened() ? 0xFF6B3A11 : 0xFF000000;
            default -> 0xFF6B3A11;
        };
    }

    private static int applyRoomStateDarken(RoomState state, int color) {
        if (state == RoomState.UNDISCOVERED || state == RoomState.UNOPENED)
            return darken(color, 0.5f);
        return color;
    }

    private static int getRoomColor(Room room) {
        return applyRoomStateDarken(room.getState(), getRoomPureColor(room));
    }

    private static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getRoomPureColor(Room room) {
        if (room.getData() == null) return 0xFF000000;
        switch (room.getData().type()) {
            case NORMAL -> {
                return 0xFF6B3A11;
                //return DungeonScan.isMimicRoom(room) ? mimicColor : normalColor;
            }

            case RARE -> {
                return 0xFFFFCB59;
            }

            case TRAP -> {
                return 0xFFFF7500;
            }

            case BLOOD -> {
                return 0xFFFF0000;
            }

            case FAIRY -> {
                return 0xFFE000FF;
            }

            case CHAMPION -> {
                return 0xFFFEDF00;
            }

            case PUZZLE -> {
                return 0xFF750085;
            }

            case ENTRANCE -> {
                return 0xFF148500;
            }

            default -> {
                return 0xFF000000;
            }
        }
    }
}
