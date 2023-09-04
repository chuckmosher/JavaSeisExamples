package org.javaseis.cloud.array;

import java.util.Iterator;

public interface IPosition extends Iterable<int[]>, Iterator<int[]> {

  /**
   * Part of the Iterator interface.
   * <p>
   * Return true if this Position has not reached the end of the array
   * for FORWARD direction, or has not reached the beginning of the array for
   * REVERSE direction.
   */
  public boolean hasNext();

  /**
   * Part of Iterator interface.
   * <p>
   * Determines the next position[] for this Position and loads this
   * information into the array supplied at the time this Iterator was
   * constructed.
   * 
   * @return reference to the int array containing the next position.
   */
  public int[] next();

  /**
   * Extension of the Iterator interface.
   * <p>
   * Return true if there is a position available that is previous to the current position
   */
  public boolean hasPrevious();

  /**
   * Extension of the Iterator interface.
   * <p>
   * Determines the previous position[] for this IPosition and loads this
   * information into the array supplied at the time this Iterator was
   * constructed.
   * 
   * @return reference to the int array containing the next position.
   */
  public int[] previous();
  
  /**
   * Set the position to the first location in the range of this IPosition
   * @return
   */
  public int[] first();
  
  /**
   * Set the position to the last location in the range of this IPosition
   * @return
   */
  public int[] last();
  
  /** 
   * Return the start position for this IPosition
   * @return start position
   */
  public int[] getStart();
  
  /** 
   * Return the ending position for this IPosition
   * @return ending position
   */
  public int[] getEnd();
  
  /**
   * Return the increments for this IPosition
   */
  public int[] getIncr();
  
  /**
   * Reset this IPosition to the initialization state
   */
  public void reset();

  /**
   * Set the position for this IPosition
   */
  public int[] setPosition(int[] positionIn);
  
  /**
   * Set the position along a particular dimension for this IPosition
   */
  public int[] setPosition( int dim, int positionIn );

  /**
   * Return a REFERENCE to the current position
   * 
   * @return a REFERENCE to the position array used by this IPosition
   */
  public int[] getPosition();

  /**
   * Return a COPY of the current position
   * 
   * @return a COPY of the position array used by this IPosition
   */
  public int[] getPositionCopy();

  /**
   * Return the shape array used by this Position
   * 
   * @return copy of the shape array used by this IPosition
   */
  public int[] getShape();

  /**
   * Return the scope of this IPosition
   */
  public int getScope();

  /**
   * Change the range of position indices covered by this iterator
   * @param startIn - array of start positions for each axis
   * @param endIn - array of ending positions for each axis
   * @param incrIn - array of increments for each axis
   * @throws IllegalArgumentException on dimension mismatch or invalid ranges
   */
  public void setRange(int[] startIn, int endIn[], int[] incrIn);
  
  /**
   * Return the range of position indices covered by this iterator
   * @param startIn - array of start positions for each axis
   * @param endIn - array of ending positions for each axis
   * @param incrIn - array of increments for each axis
   * @throws IllegalArgumentException on dimension mismatch or invalid ranges
   */
  public void getRange(int[] startIn, int endIn[], int[] incrIn);

  /**
   * Set the range for a particular dimension
   * @param range - input int[] start, end, increment for the dimension
   * @param idim - dimension for the range
   */
  public void setRange(int[] range, int idim);

  /**
   * Return the range for a particular dimension
   * @param range - output int[] start, end, increment values for the dimension
   * @param idim - dimension for the requested range
   */
  public void getRange(int[] range, int idim);

  /**
   * Set the internal position to lie within the range of this Position
   * @param positionIn position to be adjusted
   */
  public void adjustPosition(int[] positionIn);

  /**
   * Adjust a position to lie within the range of this Position. If the supplied position is valid for
   * this Position, it is returned unchanged. If any value is less than the Position range, it is set to
   * the start value for that axis. If it is greater, it is set to the end value. If a position value is
   * not on an increment, it is set to the next lower valid position.
   * @param positionIn input position
   * @param positionOut output position
   * @return false if the input position was out of range
   */
  public boolean adjustPosition(int[] positionIn, int[] positionOut);

  /**
   * Convert the current position to a long index of all elements in the range
   * @return long index of the current position
   */
  public long positionToIndex();
  
  /**
   * Return the count of locations included in this IPosition for a particular dimension
   * @param idim dimension
   * @return count of locations for that dimension
   */
  public int getCount(int idim);

  /**
   * Return a count of all elements that will be traversed
   * @return long element count
   */
  public long getTotalCount();
}