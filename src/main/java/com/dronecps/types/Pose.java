package com.dronecps.types;
import com.dronecps.core.util.Vec2;
public record Pose(Vec2 pos, Vec2 vel){ public static Pose zero(){ return new Pose(new Vec2(0,0), new Vec2(0,0)); } }
