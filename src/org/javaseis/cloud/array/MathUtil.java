package org.javaseis.cloud.array;

public class MathUtil {
  
  /**
   * Varargs max function
   * @param ds - double values
   * @return max value
   */
  public static double max( double... ds) {
    double max = ds[0];
    for (int i = 1; i<ds.length; i++) {
      if (ds[i] > max) max = ds[i];
    }
    return max;
  }
  
  /**
   * Varargs min function
   * @param ds - double values
   * @return min value
   */
  public static double min( double... ds) {
    double min = ds[0];
    for (int i = 1; i<ds.length; i++) {
      if (ds[i] < min) min = ds[i];
    }
    return min;
  }
    
  /**
   * Varargs max function
   * @param ds - float values
   * @return max value
   */
  public static float max( float... ds) {
    float max = ds[0];
    for (int i = 1; i<ds.length; i++) {
      if (ds[i] > max) max = ds[i];
    }
    return max;
  }
    
  /**
   * Varargs min function
   * @param ds - float values
   * @return min value
   */
  public static float min( float... ds) {
    float min = ds[0];
    for (int i = 1; i<ds.length; i++) {
      if (ds[i] < min) min = ds[i];
    }
    return min;
  }
}
