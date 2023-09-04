package org.javaseis.time;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javaseis.util.IntervalTimer;

/**
 * Timing utility for tracking elapsed time for multiple intervals.
 * <p>
 * Allows creation of a list of timers referenced by name. Provides methods to
 * start, stop, reset, and retrieve timing for multiple steps in a program. A
 * timer named "elapsed" is created by default and tracks total elapsed time.
 * <p>
 * Example usage:
 * <p>
 * <pre>
 * TimeTracker tt = new TimeTracker();
 * tt.add("compute");
 * tt.add("io");
 * tt.start(compute);
 * ... compute operatiopns ... 
 * tt.stop("compute");
 * tt.start("io");
 * ... i/o operations ...
 * tt.stop("io");
 * System.out.println(tt.report());
 * </pre>
 *
 * @author chuck@momacmo.org
 *
 */
public class TimeTracker implements Serializable {
  private static final long serialVersionUID = 1L;
  public static String ELAPSED = "elapsed";
  
  Map <String, IntervalTimer> timers;
  
  /**
   * Initialize a set of interval timers
   * @param names - timer names
   */
  public TimeTracker(String[] names) {
    this();
    add(names);
  }
 
  /**
   * Initialize a time tracker with default set of names
   */
  public TimeTracker() {
    timers = new LinkedHashMap<String,IntervalTimer>();
    add(ELAPSED);
    start(ELAPSED);
  }
  
  /**
   * Add a timer
   * @param name - timer name
   */
  public void add(String name) {
    timers.put(name, new IntervalTimer());
  }
  
  /**
   * Add a set of timers
   * @param names - String array containing timer names
   */
  public void add(String[] names) {
    for (String name : names) {
      add(name);
    }
  }
  
  /**
   * Get a timer by name
   * @param name - timer name
   * @return - IntervalTimer for the name
   */
  public IntervalTimer get(String name) {
    return timers.get(name);
  }
  
  /**
   * Start timing an interval and return current system time. Total time
   * is accumulated for each call to start.
   * @param name - timer name
   * @return - current system time in seconds
   */
  public double start(String name) {
    return timers.get(name).start();
  }
  
  /**
   * Stop a timer and return the interval time.
   * @param name - timer name
   * @return - time in seconds since last call to start(name)
   */
  public double stop(String name) {
    return timers.get(name).stop();
  }
   
 /**
  * Return the total time accumulated for a named timer
  * @param name - timer name
  * @return - accumulated total time between "start" and "stop" calls
  */
 public double total(String name) {
   return timers.get(name).total();
 }
 
 /**
  * Reset all timers to zero
  */
 public void reset() {
   for (String name : timers.keySet()) {
     timers.get(name).reset();
   }
 }
 
 /**
  * Provide a simple report of accumulated time
  * @return - String containing report
  */
 public String report() {
   stop("elapsed");
   StringBuffer buf = new StringBuffer("Time Tracker Report: Seconds\n");
   double total = 0;
   for (String name : timers.keySet()) {
     if (name.equalsIgnoreCase(ELAPSED)) continue;
     String dispName = String.format("%12s", name);
     String dispValue = String.format("%-12.4f",timers.get(name).total());
     buf.append(dispName + ": " + dispValue + "\n");
     total += total(name);
   }
   String dispValue = String.format("%-12.4f",total(ELAPSED));
   buf.append("     elapsed: " + dispValue + "\n");
   dispValue = String.format("%-12.4f",total(ELAPSED)-total);
   buf.append("   untracked: " + dispValue);
   return buf.toString();
 }
 
}
