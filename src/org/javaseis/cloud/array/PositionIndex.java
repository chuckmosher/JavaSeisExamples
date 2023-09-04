package org.javaseis.cloud.array;

import java.util.Arrays;
/**
 * Convert multi-dimensional array positions to and from a linear index
 * @author chuck@momacmo.org
 *
 */
public class PositionIndex {

  int ndim, scope;
  int[] start, end, incr, shape, counts;
  long[] mults;
  long totalCount;
  
  public PositionIndex(int[] shape, int scope ) {
    this.shape = shape;
    this.scope = scope;
    ndim = shape.length;
    start = new int[ndim];
    end = new int[ndim];
    incr = new int[ndim];
    for (int i=0; i<ndim; i++) {
      start[i] = 0;
      end[i] = shape[i] - 1;
      incr[i] = 1;
    }
    init();
  }

  public PositionIndex(int ndim, int scope, int[] start, int[] end, int[] incr, int[] shape) {
    this.ndim = ndim;
    this.scope = scope;
    this.start = start;
    this.end = end;
    this.incr = incr;
    this.shape = shape;
    init();
  }

  /**
   * Set the range of this PositionIndex
   * @param start - int[] start index along each dimension
   * @param end - int[] end index along each dimension
   * @param incr - int[] increment for each dimension
   */
  public void setRange( int[] start, int[] end, int[] incr ) {
    this.start = start;
    this.end = end;
    this.incr = incr;
    init();
  }
  
  public void getRange( int[] start, int[] end, int[] incr ) {
    System.arraycopy(this.start, 0, start, 0, ndim );
    System.arraycopy(this.end, 0, end, 0, ndim );
    System.arraycopy(this.incr, 0, incr, 0, ndim );
  }
  
  /**
   * Set the range along a particular dimension 
   * @param dim - index of the dimension
   * @param range - int[] containing start,end,incr for the dimension
   */
  public void setRange( int dim, int[] range ) {
    start[dim] = range[0];
    end[dim] = range[1];
    incr[dim] = range[2];
    init();
  }
  
  public void getRange( int dim, int[] range ) {
    range[0] = start[dim];
    range[1] = end[dim];
    range[2] = incr[dim];
  }
  
  /**
   * Initialize
   */
  public void init() {
    counts = new int[ndim];
    mults = new long[ndim];
    totalCount = 1;
    for (int i = scope; i < ndim; i++) {
      if (incr[i] == 0)
        throw new IllegalArgumentException("Increment for dimension " + i + " is zero");
      counts[i] = 1 + (end[i] - start[i]) / incr[i];
      if (counts[i] < 1)
        throw new IllegalArgumentException(
            "Illegal range for dimension " + i + ": " + start[i] + ", " + end[i] + ", " + incr[i]);
      if (i == scope)
        mults[i] = 1;
      else
        mults[i] = mults[i - 1] * counts[i - 1];
    }
    totalCount = mults[ndim-1]*shape[ndim-1];
  }

  /**
   * Convert a multi-dimensional array position to a linear index
   * @param position - int[] position in the multi-dimensional array
   * @return - long index of the position
   */
  public long positionToIndex(int[] position) {
    long index = 0;
    int ipos = 0;
    for (int i = scope; i < ndim; i++) {
      ipos = (position[i] - start[i])/incr[i];
      index += mults[i] * ipos;
    }
    return index;
  }

  /**
   * Convert a linear index to a position in a multi-dimensional array
   * @param indexValue - long index of the position
   * @param position - int[] position in the multi-dimensional array for the index
   */
  public void indexToPosition(long linearIndex, int[] position) {
    long index = linearIndex;
    int ipos = 0;
    for (int i = ndim-1; i >= scope; i--) {
      ipos = (int) (index/mults[i]);
      position[i] = incr[i]*ipos + start[i];
      index -= mults[i] * ipos;
    }
  }
  
  /**
   * Adjust a position to line in range and on increments
   * @param positionIn - input position
   * @param positionOut - output adjusted position
   * @return true if output and input are the same after adjustment
   */
  public boolean adjustPosition(int[] positionIn, int[] positionOut) {
    if (positionIn.length < ndim || positionOut.length < ndim) {
      throw new IllegalArgumentException("One or both of the supplied curpos arrays are too short");
    }
    boolean same = true;
    for (int i = 0; i < ndim; i++) {
      if (positionIn[i] < start[i]) {
        positionIn[i] = start[i];
      } else if (positionIn[i] > end[i]) {
        positionOut[i] = end[i];
      }
      positionOut[i] = start[i] + incr[i] * Math.round((positionOut[i] - start[i]) / incr[i]);
      same &= positionIn[i] == positionOut[i];
    }
    return same;
  }
  
  public long getTotalCount() {
    return totalCount;
  }
  
  public long first() {
    return positionToIndex(start);
  }
  
  public long last() {
    return positionToIndex(end);
  }
  
  public static void main( String[] args ) {
    int[] shape = new int[] { 1250, 380, 8640, 30 };
    int[] start = new int[] { 25,15,105,2 };
    int[] end = new int[] { 1249, 379, 8639, 29 };
    int[] incr = new int[] { 5,7,11,3 };
    PositionIndex p2i = new PositionIndex(4,2,start,end,incr,shape);
    System.out.println("PositionIndex: \n" + p2i.toString());
    int[] pos = new int[] { 375, 21, 4950, 27 };
    p2i.adjustPosition(pos, pos);
    System.out.println("Position " + Arrays.toString(pos));
    long index = p2i.positionToIndex(pos);
    System.out.println("Position to Index " + index);
    p2i.indexToPosition(index, pos);
    System.out.println("Index to Position " + Arrays.toString(pos));
  }
  
}
