package org.javaseis.volume;

import org.javaseis.array.ElementType;
import org.javaseis.grid.GridDefinition;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.regulargrid.IRegularGrid;

public interface ISeismicVolume extends IRegularGrid {
  public GridDefinition getGlobalGrid();
  
  public GridDefinition getLocalGrid();

  public DistributedArray getDistributedArray();

  public void copyVolume(ISeismicVolume volume);

  public boolean matches(ISeismicVolume volume);

  public int getElementCount();

  public ElementType getElementType();
  
  public long shapeLength();
  
  public void allocate(long maxLength);

}
