package com.dronecps.core.util;
import java.util.HashMap; import java.util.Map; import java.util.function.Function;
public class Blackboard {
  private Map<String,Object> cur = new HashMap<>();
  private Map<String,Object> nxt = new HashMap<>();
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type){ Object v = cur.get(key); return v==null?null:(T)v; }
  public void putNext(String key, Object value){ nxt.put(key, value); }
  public <T> void updateNext(String key, Function<T,T> f, T d){
    @SuppressWarnings("unchecked") T v=(T)nxt.getOrDefault(key,d); nxt.put(key, f.apply(v));
  }
  public void commit(){ cur = nxt; nxt = new HashMap<>(); }
}
