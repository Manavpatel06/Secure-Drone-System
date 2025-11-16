package com.dronecps.sensors;
import com.dronecps.core.util.Blackboard; import java.util.Random;
public class HeatSensor {
  public static String key(int id){ return "heat_out_d"+id; }
  private final Blackboard bb; private final int id; private final Random rnd=new Random(5);
  public HeatSensor(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void emit(){ double temp = 20 + 10*rnd.nextDouble(); bb.putNext(key(id), temp); }
}
