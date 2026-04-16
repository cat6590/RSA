package com.ricedotwho.rsa.utils;

import net.minecraft.core.Direction;

import java.awt.*;

public class MutableBox2D extends Box2D {

    public MutableBox2D(Point point1, Point point2) {
        super(point1, point2);
    }

    public MutableBox2D(int x1, int y1, int x2, int y2) {
        super(x1, y1, x2, y2);
    }

    public MutableBox2D(Point center, int widthRad, int heightRad) {
        super(center, widthRad, heightRad);
    }

    public void set(Point p1, Point p2) {
        set(p1.x, p1.y, p2.x, p2.y);
    }

    public void set(int x1, int y1, int x2, int y2) {
        this.maxX = Math.max(x1, x2);
        this.minX = Math.min(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.minY = Math.min(y1, y2);
    }

    public void setCentered(Point center, int widthRad, int heightRad) {
        set(
                center.x + widthRad,
                center.y + heightRad,
                center.x - widthRad,
                center.y - heightRad
        );
    }

    public void setCentered(int centerX, int centerY, int widthRad, int heightRad) {
        set(
                centerX + widthRad,
                centerY + heightRad,
                centerX - widthRad,
                centerY - heightRad
        );
    }

    public Point getMax(Point out) {
        out.x = maxX;
        out.y = maxY;
        return out;
    }

    public Point getMin(Point out) {
        out.x = minX;
        out.y = minY;
        return out;
    }

    public void scale(int scale) {
        if (scale < 0) return;
        this.minX *= scale;
        this.maxX *= scale;
        this.minY *= scale;
        this.maxY *= scale;
    }

    public void expand(int dx, int dy) {
        minX -= dx;
        minY -= dy;
        maxX += dx;
        maxY += dy;
    }

    public void move(int dx, int dy) {
        minX += dx;
        minY += dy;
        maxX += dx;
        maxY += dy;
    }

    public void moveTowards(Direction direction, int amount) {
        switch (direction) {
            case NORTH -> move(0, -amount);
            case SOUTH -> move(0, amount);
            case WEST  -> move(-amount, 0);
            case EAST  -> move(amount, 0);
        }
    }

    public void expandTowards(Direction direction, int amount) {
        switch (direction) {
            case NORTH -> minY -= amount;
            case SOUTH -> maxY += amount;
            case WEST  -> minX -= amount;
            case EAST  -> maxX += amount;
        }
    }

}
