package com.termux.zerocore.background;

/**
 * Created by qinfeng on 16/8/4.
 */

public class Particle {
    Vec2     pos = new Vec2(0f,0f);;
    Vec2     startPos = new Vec2(0f,0f);
    Color4F    color = new Color4F(0f,0f,0f,1f);
    Color4F    deltaColor = new Color4F(0f,0f,0f,1f);
    float        size;
    float        deltaSize;
    float        rotation;
    float        deltaRotation;
    float        timeToLive;
    int    atlasIndex;
//    Mode A: gravity, direction, radial accel, tangential accel.
    ModeA modeA = new ModeA();

//    Mode B: radius mode.
    ModeB modeB = new ModeB();

}
