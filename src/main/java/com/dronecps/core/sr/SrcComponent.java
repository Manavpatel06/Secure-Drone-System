package com.dronecps.core.sr;
public interface SrcComponent {
  void sample(long tickMillis);
  void compute(long tickMillis);
  void commit(long tickMillis);
  String name();
}
