package com.dronecps.agents.drone;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard; import com.dronecps.types.Pose;
import java.util.Map;
public class PoseEstimator implements SrcComponent {
  public static String outKey(int id){ return "pose_out_d"+id; }
  private final Blackboard bb; private final int id; private Pose pose;
  public PoseEstimator(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void sample(long t){
    @SuppressWarnings("unchecked") Map<String,Object> col = (Map<String,Object>) bb.get(SensorCollector.outKey(id), Map.class);
    if (col!=null && col.get("gps") instanceof Pose p){ pose = p; }
  }
  public void compute(long t){}
  public void commit(long t){ if (pose!=null) bb.putNext(outKey(id), pose); }
  public String name(){ return "PoseEstimator#"+id; }
}
