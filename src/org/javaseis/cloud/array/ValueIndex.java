package org.javaseis.cloud.array;
/**
 * Simple container class for a value and the index where the value occurred
 * <p>
 * Used by classes that find extrema of array values.
 * @author chuck@momacmo.org
 *
 */
public class ValueIndex {
  /** Value this is returned */
  public double value;
  /** Index where the value occurred */
  public int index;
}