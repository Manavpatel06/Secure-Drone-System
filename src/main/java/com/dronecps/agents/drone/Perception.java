package com.dronecps.agents.drone;

import com.dronecps.core.sr.SrcComponent;
import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.types.Detection;
import com.dronecps.types.Pose;

import java.util.ArrayList;
import java.util.List;

public class Perception implements SrcComponent {
    public static String outKey(int id){ return "det_out_d"+id; }

    private final Blackboard bb; private final int id;
    private List<Detection> dets = new ArrayList<>();

    public Perception(Blackboard bb, int id){ this.bb = bb; this.id = id; }

    @Override public void sample(long t){
        dets = new ArrayList<>();
        Pose pose = bb.get(PoseEstimator.outKey(id), Pose.class);
        if (pose == null) return;

        String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);
        if ("FLOOD".equals(scen)) {
            @SuppressWarnings("unchecked")
            List<?> vs = (List<?>) bb.get(EnvironmentSim.KEY_VICTIMS, List.class);
            if (vs != null) for (Object o : vs)
                if (o instanceof EnvironmentSim.Victim v && !v.served) {
                    if (pose.pos().sub(v.pos).norm() < 20.0)
                        dets.add(new Detection("victim", v.pos, 0.9));
                }
        } else if ("FIRE".equals(scen)) {
            @SuppressWarnings("unchecked")
            List<?> hs = (List<?>) bb.get(EnvironmentSim.KEY_HOTSPOTS, List.class);
            if (hs != null) for (Object o : hs)
                if (o instanceof EnvironmentSim.Hotspot h && h.alive) {
                    if (pose.pos().sub(h.c).norm() < 25.0)
                        dets.add(new Detection("fire", h.c, 0.9));
                }
        }
    }

    @Override public void compute(long t){}
    @Override public void commit(long t){ bb.putNext(outKey(id), dets); }
    @Override public String name(){ return "Perception#"+id; }
}
