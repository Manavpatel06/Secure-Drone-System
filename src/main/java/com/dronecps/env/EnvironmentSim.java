package com.dronecps.env;

import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.types.Pose;

import java.util.*;

/** 2D plant with wind, obstacles and a random FIRE or FLOOD scenario per run. */
public class EnvironmentSim {
    public enum Scenario { FIRE, FLOOD }

    public static final String KEY_POSE_PREFIX = "pose_d";     // +id -> Pose
    public static final String KEY_OBS         = "obstacles";   // List<Circle>
    public static final String KEY_SCENARIO    = "scenario";    // String "FIRE"/"FLOOD"
    public static final String KEY_VICTIMS     = "victims";     // List<Victim>
    public static final String KEY_HOTSPOTS    = "hotspots";    // List<Hotspot>

    /** Static obstacles. */
    public record Circle(Vec2 c, double r) {}

    /** FLOOD target. Mutable served flag updated at runtime. */
    public static class Victim {
        public Vec2 pos; public boolean served;
        public Victim(Vec2 pos){ this.pos = pos; this.served = false; }
    }

    /** FIRE target. Mutable alive flag. */
    public static class Hotspot {
        public Vec2 c; public double r; public boolean alive;
        public Hotspot(Vec2 c, double r){ this.c = c; this.r = r; this.alive = true; }
    }

    private final Blackboard bb;
    private final int numDrones;
    private final Map<Integer, Pose> state = new HashMap<>();
    private final List<Circle> obstacles = new ArrayList<>();
    private final Vec2 wind = new Vec2(0.2, 0.0);

    private final Scenario scenario;
    private final List<Victim>  victims  = new ArrayList<>();
    private final List<Hotspot> hotspots = new ArrayList<>();

    public EnvironmentSim(Blackboard bb, int numDrones){
        this.bb = bb; this.numDrones = numDrones;

        // Seed with current time so every run is different
        long seed = System.nanoTime() ^ Double.doubleToLongBits(Math.random());
        Random rnd = new Random(seed);

        // Base station: all drones start here (same place)
        Vec2 base = new Vec2(10, 10);
        for (int i=0; i<numDrones; i++) state.put(i, new Pose(base, Vec2.zero()));

        // Two fixed example obstacles (you can randomize too if you want)
        obstacles.add(new Circle(new Vec2(25,25), 8));
        obstacles.add(new Circle(new Vec2(60,40), 6));

        // Random scenario per run
        this.scenario = rnd.nextBoolean() ? Scenario.FIRE : Scenario.FLOOD;

        if (scenario == Scenario.FLOOD) {
            // 3–5 victims at random positions
            int k = 3 + rnd.nextInt(3);
            for (int i=0; i<k; i++) {
                victims.add(new Victim(new Vec2(8 + rnd.nextDouble()*72, 8 + rnd.nextDouble()*44)));
            }
        } else {
            // 2–4 heat hotspots at random positions and radii
            int k = 2 + rnd.nextInt(3);
            for (int i=0; i<k; i++) {
                hotspots.add(new Hotspot(new Vec2(12 + rnd.nextDouble()*68, 12 + rnd.nextDouble()*40),
                        4.5 + rnd.nextDouble()*4.0));
            }
        }

        System.out.println("Scenario = " + scenario + "  (seed=" + seed + ")");
    }

    public Scenario scenario(){ return scenario; }
    public List<Victim> victims(){ return victims; }
    public List<Hotspot> hotspots(){ return hotspots; }
    public List<Circle> obstacles(){ return obstacles; }

    /** Integrate plant and publish environment snapshot to the blackboard. */
    public void step(Map<Integer, Vec2> velocityCmds, double dt){
        for (int i=0; i<numDrones; i++){
            Pose p = state.get(i);
            Vec2 vCmd = velocityCmds.getOrDefault(i, Vec2.zero()).clamp(4.0);
            Vec2 v = vCmd.add(wind.scale(0.2));
            Vec2 newPos = p.pos().add(v.scale(dt));
            state.put(i, new Pose(newPos, v));
            bb.putNext(KEY_POSE_PREFIX + i, state.get(i));
        }
        bb.putNext(KEY_OBS, obstacles);
        bb.putNext(KEY_SCENARIO, scenario.name());
        bb.putNext(KEY_VICTIMS,  new ArrayList<>(victims));
        bb.putNext(KEY_HOTSPOTS, new ArrayList<>(hotspots));
    }
}
