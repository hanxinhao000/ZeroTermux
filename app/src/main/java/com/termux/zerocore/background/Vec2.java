package com.termux.zerocore.background;

/**
 * Created by qinfeng on 16/8/4.
 */

public class Vec2 {
    public float x;
    public float y;

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(Vec2 vec2) {
        this.x = vec2.x;
        this.y = vec2.y;
    }


    public  Vec2 add(Vec2 vec2) {
        return new Vec2(x + vec2.x, y + vec2.y);
    }

    public float getAngle(){
        return (float) Math.atan2(y,x);
    }

    public  Vec2 subtract(Vec2 vec2) {
        return new Vec2(x - vec2.x, y - vec2.y);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }


    public Vec2 scale(float f) {
        return new Vec2(
                x * f,
                y * f);
    }

    public Vec2 getNormalized() {
        return scale(1f / length());
    }
}
