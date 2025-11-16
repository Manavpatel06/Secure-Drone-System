package com.dronecps.sensors;
import com.dronecps.core.util.*; import com.dronecps.env.EnvironmentSim; import com.dronecps.types.Pose;
import java.util.Random;
public class GpsSensor {
  public static String key(int id){ return "gps_out_d"+id; }
  private final Blackboard bb; private final int id; private final Random rnd=new Random(42);
  public GpsSensor(Blackboard bb, int id){ this.bb=bb; this.id=id; }
  public void emit(){
    Pose p = bb.get(EnvironmentSim.KEY_POSE_PREFIX+id, Pose.class); if (p==null) return;
    Vec2 noisy = new Vec2(p.pos().x()+rnd.nextGaussian()*0.1, p.pos().y()+rnd.nextGaussian()*0.1);
    bb.putNext(key(id), new Pose(noisy, p.vel()));
  }
}
