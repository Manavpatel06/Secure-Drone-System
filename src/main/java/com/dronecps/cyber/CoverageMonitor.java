package com.dronecps.cyber;
import com.dronecps.core.sr.SrcComponent; import com.dronecps.core.util.Blackboard;
import com.dronecps.types.*; import java.util.*;
public class CoverageMonitor implements SrcComponent {
  public static final String OUT_KEY = "cov_out";
  private final Blackboard bb; private final int w,h; private final double cell;
  private final Set<Cell> uncovered = new HashSet<>();
  public CoverageMonitor(Blackboard bb, int w, int h, double cell){
    this.bb=bb; this.w=w; this.h=h; this.cell=cell;
    for(int x=0;x<w;x++) for(int y=0;y<h;y++) uncovered.add(new Cell(x,y));
  }
  public void sample(long t){
    for(int i=0;i<20;i++){
      Pose p = bb.get("pose_out_d"+i, Pose.class);
      if (p!=null){
        int cx=(int)Math.floor(p.pos().x()/cell), cy=(int)Math.floor(p.pos().y()/cell);
        cx=Math.max(0,Math.min(w-1,cx)); cy=Math.max(0,Math.min(h-1,cy));
        uncovered.remove(new Cell(cx,cy));
      }
    }
  }
  public void compute(long t){}
  public void commit(long t){ bb.putNext(OUT_KEY, new HashSet<>(uncovered)); }
  public String name(){ return "CoverageMonitor"; }
}
