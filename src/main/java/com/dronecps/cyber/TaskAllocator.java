package com.dronecps.cyber;

import com.dronecps.core.sr.SrcComponent;
import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.types.*;

import java.util.*;

public class TaskAllocator implements SrcComponent {
    public static final String OUT_KEY = "assign_out";
    private final Blackboard bb; private final int n;
    private final Random rnd = new Random();

    public TaskAllocator(Blackboard bb, int n){ this.bb=bb; this.n=n; }

    @Override public void sample(long t) {}
    @Override public void compute(long t) {}

    @Override public void commit(long t){
        String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);

        // 1) Collect targets (alive hotspots or unserved victims)
        List<Vec2> targets = new ArrayList<>();
        if ("FIRE".equals(scen)) {
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Hotspot> hs = (List<EnvironmentSim.Hotspot>) bb.get(EnvironmentSim.KEY_HOTSPOTS, List.class);
            if (hs != null) for (var h : hs) if (h.alive) targets.add(h.c);
        } else {
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Victim> vs = (List<EnvironmentSim.Victim>) bb.get(EnvironmentSim.KEY_VICTIMS, List.class);
            if (vs != null) for (var v : vs) if (!v.served) targets.add(v.pos);
        }

        // 2) Get current drone positions
        Vec2[] dronePos = new Vec2[n];
        for (int i=0;i<n;i++){
            Pose p = bb.get("pose_out_d"+i, Pose.class);   // estimator output
            if (p == null) {  // fallback to env pose if estimator absent
                @SuppressWarnings("unchecked")
                Pose envPose = bb.get(EnvironmentSim.KEY_POSE_PREFIX + i, Pose.class);
                dronePos[i] = envPose != null ? envPose.pos() : new Vec2(0,0);
            } else dronePos[i] = p.pos();
        }

        // 3) Greedy nearest-drone assignment to targets
        boolean[] taken = new boolean[n];
        List<Cmd> cmds = new ArrayList<>();
        for (Vec2 target : targets) {
            int bestId = -1; double bestDist = Double.POSITIVE_INFINITY;
            for (int i=0;i<n;i++) if (!taken[i]) {
                double d = dronePos[i].sub(target).norm();
                if (d < bestDist) { bestDist = d; bestId = i; }
            }
            if (bestId >= 0) {
                taken[bestId] = true;
                cmds.add(new Cmd(bestId, new Path(List.of(target))));
            }
            if (allTaken(taken)) break;
        }

        // 4) Any remaining drones → patrol (random waypoint far enough to move)
        for (int i=0;i<n;i++){
            if (!taken[i]) {
                Vec2 wp = new Vec2(8 + rnd.nextDouble()*72, 8 + rnd.nextDouble()*44);
                cmds.add(new Cmd(i, new Path(List.of(wp))));
            }
        }

        bb.putNext(OUT_KEY, cmds);
    }

    private boolean allTaken(boolean[] a){ for (boolean b : a) if (!b) return false; return true; }
    @Override public String name(){ return "TaskAllocator"; }
}
