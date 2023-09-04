package org.javaseis.grid;

import org.javaseis.properties.AxisDefinition;
import org.javaseis.properties.AxisLabel;
import org.javaseis.properties.DataDomain;
import org.javaseis.properties.Units;
import org.javaseis.array.TransposeType;
import org.javaseis.grid.GridDefinition;

/**
 * Convenience class for modifying the fields of a GridDefinition
 */

public class GridDefinitionModifier {

	public static GridDefinition setAxis(GridDefinition grid, AxisDefinition axis, int index) {
		int ndim = grid.getNumDimensions();
		AxisDefinition[] axes = new AxisDefinition[ndim];
	  for (int i=0; i<ndim; i++) {
	    if (index == i)
	      axes[i] = axis;
	    else
	      axes[i] = grid.getAxis(i);
	  }
	  return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setLabel(GridDefinition grid, AxisLabel label, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setLabel(axes[i], label);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setUnits(GridDefinition grid, Units units, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setUnits(axes[i], units);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setDomain(GridDefinition grid, DataDomain domain, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setDomain(axes[i], domain);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setAxisLength(GridDefinition grid, int length, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setLength(axes[i], length);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setLogicalOrigin(GridDefinition grid, long logicalOrigin, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setLogicalOrigin(axes[i], logicalOrigin);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setLogicalDelta(GridDefinition grid, long logicalDelta, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setLogicalDelta(axes[i], logicalDelta);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setPhysicalOrigin(GridDefinition grid, double physicalOrigin, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setPhysicalOrigin(axes[i], physicalOrigin);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition setPhysicalDelta(GridDefinition grid, double physicalDelta, int index) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = grid.getAxis(i);
      if (index == i)
        axes[i] = GridAxisModifier.setPhysicalDelta(axes[i], physicalDelta);
    }
    return new GridDefinition(ndim,axes);
	}

	public static GridDefinition transpose(GridDefinition grid, TransposeType transType) {
	  int ndim = grid.getNumDimensions();
    AxisDefinition[] axes = new AxisDefinition[ndim];
	  int[] ip = transType.permutation();
	  for (int i=0; i<ndim; i++) {
	    axes[i] = grid.getAxis(ip[i]);
	  }
	  return new GridDefinition(ndim,axes);
	}
} 
