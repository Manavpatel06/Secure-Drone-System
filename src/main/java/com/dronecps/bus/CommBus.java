package com.dronecps.bus;
import com.dronecps.core.util.Blackboard; import com.dronecps.types.Cmd;
import java.util.*; import java.util.stream.Collectors;
public class CommBus {
  public static final String KEY_CMD_STREAM = "cmd_stream";       // List<Cmd>
  public static final String KEY_DELIVERIES_PREFIX = "deliveries_d"; // +id
  private final Blackboard bb; public CommBus(Blackboard bb){ this.bb=bb; }
  @SuppressWarnings("unchecked")
  public void deliver(){
    List<Cmd> cmds = (List<Cmd>) bb.get(KEY_CMD_STREAM, List.class);
    if (cmds==null) return;
    var byDrone = cmds.stream().collect(Collectors.groupingBy(Cmd::droneId));
    byDrone.forEach((id,list)-> bb.putNext(KEY_DELIVERIES_PREFIX+id, new ArrayList<>(list)));
  }
}
