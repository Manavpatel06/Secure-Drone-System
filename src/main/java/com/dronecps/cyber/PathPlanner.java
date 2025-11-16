package com.dronecps.cyber;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard; import com.dronecps.types.Cmd;
import java.util.*;
public class PathPlanner implements SrcComponent {
  public static final String OUT_KEY = "plan_out";
  private final Blackboard bb;
  public PathPlanner(Blackboard bb){ this.bb = bb; }
  public void sample(long t){} public void compute(long t){}
  public void commit(long t){
    @SuppressWarnings("unchecked") List<Cmd> cmds = (List<Cmd>) bb.get(TaskAllocator.OUT_KEY, List.class);
    if (cmds!=null) bb.putNext(OUT_KEY, new ArrayList<>(cmds));
  }
  public String name(){ return "PathPlanner"; }
}
