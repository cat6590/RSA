package com.ricedotwho.rsa.utils;

import lombok.Getter;
import net.minecraft.world.phys.AABB;

import java.awt.*;

public class Box2D {
    @Getter
    int minX;
    @Getter
    int minY;
    @Getter
    int maxX;
    @Getter
    int maxY;

    public Box2D(Point point1, Point point2) {
        this(point1.x, point1.y, point2.x, point2.y);
    }

    public Box2D(int x1, int y1, int x2, int y2) {
        this.maxX = Math.max(x1, x2);
        this.minX = Math.min(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.minY = Math.min(y1, y2);
    }

    public Box2D(Point center, int widthRad, int heightRad) {
        this(center.x + widthRad, center.y + heightRad, center.x - widthRad, center.y - heightRad);
    }

    public Point getMax() {
        return new Point(maxX, maxY);
    }

    public Point getMin() {
        return new Point(minX, minY);
    }

    public AABB to3dCentered(int y, int height) {
        height >>= 1;
        return to3d(y - height, y + height);
    }

    public AABB to3d(int minY, int maxY) {
        return new AABB(this.minX, minY, this.minY, this.maxX, maxY, this.maxY);
    }

    public String toString() {
        return "Centered at : " + ((maxX + minX) >> 1) + ((maxY + minY) >> 1);
    }
}
