package com.dronecps.types;
import com.dronecps.core.util.Vec2;
public record Detection(String kind, Vec2 location, double score){}
