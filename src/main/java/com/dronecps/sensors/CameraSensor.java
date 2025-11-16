package com.dronecps.sensors;
import com.dronecps.core.util.Blackboard;
public class CameraSensor {
  public static String key(int id){ return "cam_out_d"+id; }
  private final Blackboard bb; private final int id; private int fid=0;
  public CameraSensor(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void emit(){ bb.putNext(key(id), new FrameMeta(fid++, 320, 240)); }
  public record FrameMeta(int id, int w, int h){}
}
