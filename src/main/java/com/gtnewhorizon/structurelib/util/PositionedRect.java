package com.gtnewhorizon.structurelib.util;

import org.joml.Vector4i;

public class PositionedRect extends Vector4i {

    public PositionedRect(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public int getLeft() {
        return x;
    }

    public int getTop() {
        return y;
    }

    public int getRight() {
        return x + z;
    }

    public int getBottom() {
        return y + w;
    }

    public int getWidth() {
        return z;
    }

    public int getHeight() {
        return w;
    }

    public boolean contains(int x, int y) {
        return x >= getLeft() && x < getRight() && y >= getTop() && y < getBottom();
    }

    public boolean contains(PositionedRect rect) {
        return getLeft() <= rect.getLeft() && getTop() <= rect.getTop()
                && getRight() >= rect.getRight()
                && getBottom() >= rect.getBottom();
    }

    public boolean intersects(PositionedRect rect) {
        return getLeft() < rect.getRight() && getRight() > rect.getLeft()
                && getTop() < rect.getBottom()
                && getBottom() > rect.getTop();
    }

}
