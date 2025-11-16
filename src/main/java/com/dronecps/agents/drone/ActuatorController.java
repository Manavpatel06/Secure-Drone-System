package com.dronecps.agents.drone;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.*; import java.util.List;
public class ActuatorController implements SrcComponent {
  public static String outKey(int id){ return "act_out_d"+id; }
  private final Blackboard bb; private final int id; private Vec2 v = Vec2.zero();
  public ActuatorController(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void sample(long t){ Vec2 maybe = bb.get(CommandFollower.velKey(id), Vec2.class); v = (maybe==null?Vec2.zero():maybe); }
  public void compute(long t){}
  public void commit(long t){ bb.putNext(outKey(id), List.of(v.x(), v.y(), 0.0, 0.0)); }
  public String name(){ return "ActuatorController#"+id; }
}
