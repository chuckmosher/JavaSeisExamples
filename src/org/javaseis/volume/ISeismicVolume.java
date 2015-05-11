package org.javaseis.volume;

import org.javaseis.grid.GridDefinition;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.regulargrid.IRegularGrid;

public interface ISeismicVolume extends IRegularGrid {
  public GridDefinition getGlobalGrid();
  public DistributedArray getDistributedArray();
  public void copyVolume( ISeismicVolume volume );
  public boolean matches( ISeismicVolume volume );
}
