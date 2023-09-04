package org.javaseis.cloud.array;

/**
 * Static methods to convert arrays of int/float to/from long/double
 */
public class Convert {
	/**
	 * Convert a long array to an int array
	 * @param lvals input long array
	 * @return output int array
	 */
  public static int[] longToInt( long[] lvals ) {
    int[] ivals = new int[lvals.length];
    for (int i=0; i<lvals.length; i++)
      ivals[i] = (int)lvals[i];
    return ivals;
  }
  /**
   * Convert an int array to a long array
   * @param ivals input int array
   * @return output long array
   */
  public static long[] intToLong( int[] ivals ) {
    long[] lvals = new long[ivals.length];
    for (int i=0; i<ivals.length; i++)
      lvals[i] = ivals[i];
    return lvals;
  }
  /**
   * Convert a double array to a float array
   * @param dvals input double array
   * @return output float array
   */
  public static float[] DoubleToFloat( double[] dvals ) {
    float[] fvals = new float[dvals.length];
    for (int i=0; i<dvals.length; i++)
      fvals[i] = (float)dvals[i];
    return fvals;
  }
  /**
   * Convert a float array to a double array
   * @param fvals input float array
   * @return output double array
   */
  public static double[] floatToDouble( float[] fvals ) {
    double[] dvals = new double[fvals.length];
    for (int i=0; i<fvals.length; i++)
      dvals[i] = fvals[i];
    return dvals;
  }
}
