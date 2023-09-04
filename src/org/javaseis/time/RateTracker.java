package org.javaseis.time;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javaseis.util.IntervalTimer;
import org.javaseis.util.JsonUtil;

/**
 * Tracks interval times and "amounts" to allow for rate calculations
 * for multiple sections of a program.
 * 
 * @author chuck@momacmo.org
 *
 */
public class RateTracker implements Serializable {
  private static final long serialVersionUID = 1L;
  Map <String, IntervalTimer> timers;
  Map <String, Double> amounts;
  
  /**
   * Initialize an empty rate tracker
   */
  public RateTracker() {
    timers = new LinkedHashMap<String,IntervalTimer>();
    amounts = new LinkedHashMap<String,Double>();
  }
  
  /**
   * Initialize a rate tracker with a set of names
   * @param names - String array containing rate tracker names
   */
  public RateTracker(String[] names) {
    this();
    for (String name : names) {
      add(name);
    }
  }
  
  /**
   * Add a rate tracker by name
   * @param name - name of the tracker
   */
  public void add(String name) {
    timers.put(name, new IntervalTimer());
    amounts.put(name, Double.valueOf(0));
  }
  
  /**
   * Get the interval timer for a named tracker
   * @param name - tracker name
   * @return - IntervalTimer for the name
   */
  public IntervalTimer getTime(String name) {
    return timers.get(name);
  } 
  
  /**
   * Get the current value of the amount
   * @param name - tracker name
   * @return - accumulated amount
   */
  public Double getAmount(String name) {
    return amounts.get(name);
  }
  
  /**
   * Start a rate tracker for an interval
   * @param name - tracker name
   * @return - System time in seconds
   */
  public double start(String name) {
    return timers.get(name).start();
  }
    
  /**
   * Stop a rate tracker for an interval
   * @param name - tracker name
   * @param amount - amount to accumulate
   * @return - elapsed time since the last stop(name) call
   */
  public double stop(String name, double amount) {
    Double current = amounts.get(name) + amount;
    amounts.put(name,current);
    return timers.get(name).stop();
  }
  
  /**
   * Return the current rate for a named tracker
   * @param name - tracker name
   * @return - current accumulated amount/time
   */
  public double getRate( String name ) {
    double time = total(name);
    if (time <= Float.MIN_VALUE) return 0;
    return getAmount(name)/time;
  }
   
 /**
  * Return the current total time accumulated
  * @param name - tracker name
  * @return - accumulated time in seconds for the tracker
  */
 public double total(String name) {
   return timers.get(name).total();
 }
 
 /**
  * Return a simple report of interval name, time, amount, and rate
  * @return - String with report
  */
 public String report() {
   StringBuffer buf = new StringBuffer("Rate Tracker Report: Amount/sec\n");
   buf.append("        Name        Time      Amount        Rate\n");
   for (String name : timers.keySet()) {
     String dispName = String.format("%12s", name);
     String dispValue = String.format("%12.4f",timers.get(name).total());
     String amountValue = String.format("%12.5g",amounts.get(name));
     String rateValue = String.format("%12.5g",getRate(name));
     buf.append(dispName + dispValue + amountValue + rateValue + "\n");
     if (name.equalsIgnoreCase("elapsed")) continue;
   }
   return buf.toString();
 }
 
 public static String report( String rateTrackerJson ) {
   return (String)(JsonUtil.fromJsonString(RateTracker.class, rateTrackerJson));
 }
 
}
