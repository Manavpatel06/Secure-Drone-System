package com.dronecps.core.util;
public record Vec2(double x, double y){
  public Vec2 add(Vec2 o){ return new Vec2(x+o.x, y+o.y); }
  public Vec2 sub(Vec2 o){ return new Vec2(x-o.x, y-o.y); }
  public Vec2 scale(double s){ return new Vec2(x*s, y*s); }
  public double norm(){ return Math.hypot(x,y); }
  public Vec2 clamp(double max){ double n=norm(); return n>max? scale(max/(n+1e-9)):this; }
  public static Vec2 zero(){ return new Vec2(0,0); }
}
