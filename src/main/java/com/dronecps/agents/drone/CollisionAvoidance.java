package com.dronecps.agents.drone;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard; import com.dronecps.types.*;
import java.util.*;
public class CollisionAvoidance implements SrcComponent {
  public static String outKey(int id){ return "safe_path_d"+id; }
  private final Blackboard bb; private final int id; private Path path;
  public CollisionAvoidance(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  @SuppressWarnings("unchecked")
  public void sample(long t){
    List<Cmd> deliveries = (List<Cmd>) bb.get("deliveries_d"+id, List.class);
    if (deliveries!=null && !deliveries.isEmpty()) path = deliveries.get(deliveries.size()-1).path();
  }
  public void compute(long t){}
  public void commit(long t){ if (path!=null) bb.putNext(outKey(id), path); }
  public String name(){ return "CollisionAvoidance#"+id; }
}
