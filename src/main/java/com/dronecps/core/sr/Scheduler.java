package com.dronecps.core.sr;
import java.util.ArrayList; import java.util.List;
public class Scheduler {
  private final List<SrcComponent> comps = new ArrayList<>();
  public Scheduler add(SrcComponent c){ comps.add(c); return this; }
  public void tick(long t){
    for (var c: comps) c.sample(t);
    for (var c: comps) c.compute(t);
    for (var c: comps) c.commit(t);
  }
}
