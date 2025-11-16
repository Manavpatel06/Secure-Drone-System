package com.dronecps.cyber;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard; import com.dronecps.types.Cmd;
import java.util.*;
public class CommandDispatcher implements SrcComponent {
  public static final String OUT_KEY = "cmd_stream";
  private final Blackboard bb; private List<Cmd> last = new ArrayList<>();
  public CommandDispatcher(Blackboard bb){ this.bb=bb; }
  @SuppressWarnings("unchecked")
  public void sample(long t){ List<Cmd> plan = (List<Cmd>) bb.get(PathPlanner.OUT_KEY, List.class); if (plan!=null) last = plan; }
  public void compute(long t){} public void commit(long t){ bb.putNext(OUT_KEY, new ArrayList<>(last)); }
  public String name(){ return "CommandDispatcher"; }
}
