package com.dronecps.agents.drone;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard;
import com.dronecps.sensors.CameraSensor; import com.dronecps.sensors.GpsSensor; import com.dronecps.sensors.HeatSensor;
import java.util.*;
public class SensorCollector implements SrcComponent {
  public static String outKey(int id){ return "col_out_d"+id; }
  private final Blackboard bb; private final int id; private Object cam,gps,heat;
  public SensorCollector(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void sample(long t){
    cam  = bb.get(CameraSensor.key(id), Object.class);
    gps  = bb.get(GpsSensor.key(id), Object.class);
    heat = bb.get(HeatSensor.key(id), Object.class);
  }
  public void compute(long t){}
  public void commit(long t){
    Map<String,Object> m=new HashMap<>(); if(cam!=null)m.put("cam",cam); if(gps!=null)m.put("gps",gps); if(heat!=null)m.put("heat",heat);
    bb.putNext(outKey(id), m);
  }
  public String name(){ return "SensorCollector#"+id; }
}
