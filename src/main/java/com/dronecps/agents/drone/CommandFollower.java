package com.dronecps.agents.drone;

import com.dronecps.core.sr.SrcComponent;
import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.types.Path;
import com.dronecps.types.Pose;

import java.util.List;

public class CommandFollower implements SrcComponent {
    public static String outKey(int id){ return "motor_cmd_d"+id; }
    public static String velKey(int id){ return "plant_vel_d"+id; }

    private final Blackboard bb; private final int id; private Vec2 v = Vec2.zero();

    public CommandFollower(Blackboard bb, int id){ this.bb=bb; this.id=id; }

    @Override public void sample(long t){
        Path safe = bb.get(CollisionAvoidance.outKey(id), Path.class);
        Pose p = bb.get(PoseEstimator.outKey(id), Pose.class);
        if (safe!=null && p!=null && !safe.waypoints().isEmpty()){
            var goal = safe.waypoints().get(0);
            Vec2 err = new Vec2(goal.x()-p.pos().x(), goal.y()-p.pos().y());

            // Base speed by scenario
            String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);
            double base = "FIRE".equals(scen) ? 3.0 : 2.0;   // faster for fire response
            // Precision approach near the goal
            double dist = err.norm();
            double approach = (dist < 3.0) ? 0.7 : base;

            v = err.clamp(approach);
        } else {
            v = Vec2.zero();
        }
    }

    @Override public void compute(long t){}
    @Override public void commit(long t){
        bb.putNext(velKey(id), v); // for plant integration
        bb.putNext(outKey(id), List.of(v.x(), v.y(), 0.0, 0.0));
    }
    @Override public String name(){ return "CommandFollower#"+id; }
}
