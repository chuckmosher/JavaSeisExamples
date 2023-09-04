package org.javaseis.cloud.array;

import java.util.Arrays;
import java.util.Iterator;

import org.javaseis.grid.GridDefinition;

/**
 * Support for arbitrary position iteration for multi-dimensional arrays and
 * files. This version includes support for increments other than 1 along all
 * axes.
 *
 */
public class PositionIterator implements IPosition {

  /** Number of dimensions that will be traversed, and ndim-1 */
  private int ndim;

  /** Rank of the iterator */
  private int scope;

  /**
   * Position and shape arrays - these are references to the supplied arguments
   */
  private int[] position, shape;

  /** Start, End, and Increment */
  private int[] start, end, incr;

  /** Flag for when there is only one which means scope == ndim */
  private boolean firsttime;

  protected PositionIterator() {
    // Null constructor, be sure to call init from extending classes
    return;
  }

  /**
   * Creates a new Position from a given shape and position array
   * 
   * @param shapeIn    Input shape for the iterator
   * @param positionIn Array to use to reflect the position set by iterator
   *                   methods. A reference is retained, so any changes made
   *                   externally will affect the iterator.
   * @param startIn    Starting position for the iterator
   * @param endIn      Ending position for the iterator
   * @param incrIn     Position increment for the iterator
   * @param scopeIn    Rank of the position iterator, scope=0 iterates samples, 1
   *                   traces, 2 frames, 3 volumes, etc.
   * @throws IllegalArgumentException if any of the parameters are out of range
   */
  public PositionIterator(int[] shapeIn, int[] startIn, int[] endIn, int[] incrIn, int scopeIn, int[] positionIn) {
    init(shapeIn, startIn, endIn, incrIn, scopeIn, positionIn);
  }

  /**
   * Creates a new Position from a given shape and scope.
   * 
   * @param shapeIn Input shape for the iterator
   * @param scopeIn Rank of the position iterator, scope=0 iterates samples, 1
   *                traces, 2 frames, 3 volumes, etc.
   */
  public PositionIterator(int[] shapeIn, int scopeIn) {
    int ndimIn = shapeIn.length;
    int[] startIn = new int[ndimIn];
    int[] endIn = new int[ndimIn];
    int[] incrIn = new int[ndimIn];
    for (int i = 0; i < ndimIn; i++) {
      incrIn[i] = 1;
      endIn[i] = shapeIn[i] - 1;
    }
    init(shapeIn, startIn, endIn, incrIn, scopeIn, new int[ndimIn]);
  }

  /**
   * Creates a new Position iterator for a given shape and scope of 0 (sample
   * iterator).
   * 
   * @param shapeIn Input shape for the iterator
   */
  public PositionIterator(int[] shapeIn) {
    this(shapeIn, 0);
  }

  /**
   * Copy constructor for creating a new Position from an existing Position.
   * 
   * @param pos Position to copy
   */
  public PositionIterator(PositionIterator pos) {
    init(pos.shape, pos.start, pos.end, pos.incr, pos.scope, pos.position);
  }

  /**
   * method to set internal state
   */
  protected void init(int[] shapeIn, int[] startIn, int[] endIn, int[] incrIn, int scopeIn, int[] positionIn) {
    ndim = shapeIn.length;
    if (scopeIn > ndim) {
      throw new IllegalArgumentException("Scope must be less than the shape dimensions");
    }
    if (positionIn.length < ndim) {
      throw new IllegalArgumentException("Position dimensions are less than shape dimensions");
    }
    shape = shapeIn.clone();
    scope = scopeIn;
    position = positionIn;
    start = startIn.clone();
    end = endIn.clone();
    incr = incrIn.clone();
    reset();
  }

  /**
   * Part of the Iterator interface.
   * <p>
   * Return true if this Position has not reached the end of the interation range
   */
  public boolean hasNext() {
    if (firsttime) {
      firsttime = false;
      return true;
    }
    for (int i = scope; i < ndim; i++) {
      if (position[i] <= end[i] - incr[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Part of Iterator interface.
   * <p>
   * Determines the next position[] for this Position and loads this information
   * into the array supplied at the time this Iterator was constructed.
   * 
   * @return reference to the int array containing the next position.
   */
  public int[] next() {
    if (scope == ndim) {
      return position;
    }

    position[scope] += incr[scope];
    int i = scope;
    while (i < ndim - 1) {
      if (position[i] <= end[i]) {
        break;
      }
      position[i] = start[i];
      i++;
      position[i] += incr[i];
    }
    if (position[ndim - 1] > end[ndim - 1]) {
      // // make sure to get the last record
      // if (position[ndim-1] > end[ndim-1] + incr[ndim-1])
      throw new IllegalStateException("Call to next() when hasNext() is false");
    }
    return position;
  }

  /**
   * Extension of the Iterator interface.
   * <p>
   * Return true if there is a position available that is previous to the current
   * position
   */
  public boolean hasPrevious() {
    for (int i = scope; i < ndim; i++) {
      if (position[i] >= start[i] + incr[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extension of the Iterator interface.
   * <p>
   * Determines the next position[] for this Position and loads this information
   * into the array supplied at the time this Iterator was constructed.
   * 
   * @return reference to the int array containing the next position.
   */
  public int[] previous() {
    position[scope] -= incr[scope];
    int i = scope;
    while (i < ndim - 1) {
      if (position[i] >= start[i]) {
        break;
      }
      position[i] = end[i];
      i++;
      position[i] -= incr[i];
    }
    if (position[ndim - 1] < start[ndim - 1]) {
      throw new IllegalStateException("Call to previous() when hasPrevious() is false");
    }
    return position;
  }

  /**
   * Reset this Position to the first element
   */
  @Override
  public void reset() {
    firsttime = true;
    System.arraycopy(start, 0, position, 0, ndim);
    position[scope] -= incr[scope];
  }

  /**
   * Set a new starting position for this Position
   */
  @Override
  public int[] setPosition(int[] positionIn) {
    adjustPosition(positionIn);
    return position;
  }

  /**
   * Return a REFERENCE to the current position
   * 
   * @return a REFERENCE to the position array used by this Position
   */
  @Override
  public int[] getPosition() {
    return position;
  }

  /**
   * Return a COPY of the current position
   * 
   * @return a COPY of the position array used by this Position
   */
  @Override
  public int[] getPositionCopy() {
    return position.clone();
  }

  /**
   * Return the shape array used by this Position
   * 
   * @return copy of the shape array used by this Position
   */
  @Override
  public int[] getShape() {
    return shape.clone();
  }

  @Override
  public int[] getStart() {
    return start.clone();
  }

  @Override
  public int[] getEnd() {
    return end;
  }

  @Override
  public int[] getIncr() {
    return incr;
  }

  /**
   * Return the scope of this Position
   */
  @Override
  public int getScope() {
    return scope;
  }

  /**
   * Change the range of position indices covered by this iterator
   * 
   * @param startIn - array of start positions for each axis
   * @param endIn   - array of ending positions for each axis
   * @param incrIn  - array of increments for each axis
   * @throws IllegalArgumentException on dimension mismatch or invalid ranges
   */
  @Override
  public void setRange(int[] startIn, int endIn[], int[] incrIn) {
    if (startIn.length < ndim || endIn.length < ndim || incrIn.length < ndim) {
      throw new IllegalArgumentException("Dimensions of positions do not match the shape");
    }
    for (int i=0; i<ndim; i++) {
      start[i] = adjustPosition( startIn[i], i);
      end[i] = adjustPosition( endIn[i], i);
      incr[i] = (incrIn[i] > 0 ? incrIn[i] : 1);
    }
  }

  
  /**
   * Change the range of position indices covered by this iterator. The current
   * position is set to the first value in the new range.
   * @param dim - dimension to change
   * @param range - int[] start, end, and increment for the dimension
   */
  @Override
  public void setRange(int[] range, int dim) {
    if (dim < 0 || dim >= ndim) 
      throw new IllegalArgumentException("Dimension out of range");
    start[dim] = adjustPosition(range[0],dim);
    end[dim] = adjustPosition(range[1],dim);
    incr[dim] = (range[2] > 0 ? range[2] : 1);
    // if position is out of range, reset to start or end
    if (position[dim] < start[dim]) position[dim] = start[dim];
    if (position[dim] > end[dim]) position[dim] = end[dim];
  }

  /**
   * Return the count of the number of locations for a specified dimension
   * 
   * @param idim - 1 + (end-start)/incr for the specified dimension
   * @return
   */
  @Override
  public int getCount(int idim) {
    return 1 + (end[idim] - start[idim]) / incr[idim];
  }

  /**
   * Return the total number of locations for the iteration range
   * 
   * @return count of all locations for this iterator
   */
  @Override
  public long getTotalCount() {
    long count = getCount(0);
    for (int i = 1; i < ndim; i++) {
      count *= getCount(i);
    }
    return count;
  }

  /**
   * Optional method in the Iterator interface -- not implemented.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Optional method Iterator.remove() is not implemented");
  }

  /**
   * Set the internal position to lie within the range of this Position
   * 
   * @param positionIn position to be adjusted
   */
  @Override
  public void adjustPosition(int[] positionIn) {
    adjustPosition(positionIn, position);
  }

  /**
   * Adjust a position to lie within the range of this Position. If the supplied
   * position is valid for this Position, it is returned unchanged. If any value
   * is less than the Position range, it is set to the start value for that axis.
   * If it is greater, it is set to the end value. If a position value is not on
   * an increment, it is set to the next lower valid position.
   * 
   * @param positionIn  input position
   * @param positionOut output position
   * @return false if the input position was out of range
   */
  @Override
  public boolean adjustPosition(int[] positionIn, int[] positionOut) {
    if (positionIn.length < ndim || positionOut.length < ndim) {
      throw new IllegalArgumentException("One or both of the supplied position arrays are too short");
    }
    boolean same = true;
    for (int i = 0; i < ndim; i++) {
      positionOut[i] = adjustPosition( positionIn[i], i );
      same &= positionIn[i] == positionOut[i];
    }
    return same;
  }

  public int adjustPosition(int ipos, int idim) {
    if (ipos < start[idim]) {
      return start[idim];
    } else if (ipos > end[idim]) {
      return end[idim];
    } else if ((ipos - start[idim]) % incr[idim] == 0) {
      return ipos;
    }
    int ival = start[idim] + (ipos - start[idim]) / incr[idim];
    if (ival < start[idim]) {
      ival = start[idim];
    } else if (ival > end[idim]) {
      ival = end[idim];
    }
    return ival;
  }

  public long positionToIndex() {
    long index = (position[0] - start[0]) / incr[0];
    long mult = shape[0];
    long j = 0;
    for (int i = 1; i < ndim; i++) {
      j = (position[i] - start[i]) / incr[i];
      index += mult * j;
      mult *= shape[i];
    }
    return index;
  }

  @Override
  public Iterator<int[]> iterator() {
    return new PositionIterator(this);
  }

  @Override
  public String toString() {
    return org.javaseis.util.JsonUtil.toJsonString(this);
  }

  @Override
  public int[] setPosition(int dim, int ipos) {
    if (ipos < 0 || ipos >= shape[ndim])
      throw new IllegalArgumentException("Position index " + ipos + " is out of range for dimension " + dim);
    position[dim] = ipos;
    return position;
  }

  @Override
  public void getRange(int[] startIn, int[] endIn, int[] incrIn) {
    System.arraycopy(start, 0, startIn, 0, ndim);
    System.arraycopy(end, 0, endIn, 0, ndim);
    System.arraycopy(incr, 0, incrIn, 0, ndim);
  }

  @Override
  public void getRange( int[] range, int dim) {
    range[0] = start[dim];
    range[1] = end[dim];
    range[2] = incr[dim];
  }

  @Override
  public int[] first() {
    System.arraycopy(start, 0, position, 0, ndim);
    return position;
  }

  @Override
  public int[] last() {
    System.arraycopy(end, 0, position, 0, ndim);
    return position;
  }

  /**
   * Static methods for convenience
   */

  /**
   * Check a position for conformance with a GridDefinition
   * 
   * @param grid     - input GridDefintion
   * @param position - Position to be checked
   * @return
   */
  public static boolean checkPosition(GridDefinition grid, int[] position) {
    return checkPosition(grid.getNumDimensions(), grid.getAxisLengths(), position);
  }

  public static boolean checkPosition(int ndim, long shape[], int[] position) {
    if (position.length != ndim || shape.length != ndim) {
      return false;
    }
    for (int i = 0; i < ndim; i++) {
      if (position[i] < 0 || position[i] >= shape[i]) {
        return false;
      }
    }
    return true;
  }

  public static boolean checkPosition(int ndim, int shape[], int[] position) {
    if (position.length != ndim || shape.length != ndim) {
      return false;
    }
    for (int i = 0; i < ndim; i++) {
      if (position[i] < 0 || position[i] >= shape[i]) {
        return false;
      }
    }
    return true;
  }

  public static boolean checkPosition(int ndim, int[] shape, int[] start, int[] inc, int[] position) {
    if (position.length != ndim || shape.length != ndim) {
      return false;
    }
    for (int i = 0; i < ndim; i++) {
      if (start[i] < 0 || start[i] >= shape[i]) {
        return false;
      }
      if (inc[i] == 0) {
        return false;
      }
      if (position[i] < start[i] || position[i] >= shape[i]) {
        return false;
      }
      if ((position[i] - start[i]) % inc[i] != 0) {
        return false;
      }
    }
    return true;
  }

  public static boolean adjustPosition(int ndim, int[] shape, int[] start, int[] inc, int[] position) {
    if (position.length != ndim || shape.length != ndim) {
      return false;
    }
    for (int i = 0; i < ndim; i++) {
      if (start[i] < 0 || start[i] >= shape[i]) {
        return false;
      }
      if (inc[i] == 0) {
        return false;
      }
      if (position[i] < start[i] || position[i] >= shape[i]) {
        position[i] = start[i];
      }
      position[i] = start[i] + inc[i] * ((position[i] - start[i]) / inc[i]);
    }
    return true;
  }

  /**
   * Main method for testing
   * 
   * @param args
   */
  public static void main(String[] args) {
    int[] shape = new int[] { 625, 4, 3, 2 };
    PositionIterator pos = new PositionIterator(shape, 2);
    while (pos.hasNext()) {
      int[] ipos = pos.next();
      System.out.println(Arrays.toString(ipos));
    }
  }

}
