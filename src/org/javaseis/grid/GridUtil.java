package org.javaseis.grid;

import java.util.Arrays;

import org.javaseis.properties.AxisDefinition;
import org.javaseis.properties.AxisLabel;
import org.javaseis.properties.DataDomain;
import org.javaseis.properties.DataType;
import org.javaseis.properties.Units;
import org.javaseis.util.SeisException;

import beta.javaseis.array.TransposeType;
import edu.mines.jtk.util.ParameterSet;

public class GridUtil {
  
  public static GridDefinition fromParameterSet( ParameterSet gridParms ) {
    int ndim = gridParms.getInt("DataDimensions", 0);
    long[] axisLengths = gridParms.getLongs("AxisLengths",null );
    if (axisLengths == null) throw new RuntimeException("Could not parse AxisLengths");
    String[] axisLabels = gridParms.getStrings("AxisLabels",null );
    if (axisLabels == null) throw new RuntimeException("Could not parse AxisLabels");
    String[] axisUnits = gridParms.getStrings("AxisUnits",null );
    if (axisUnits == null) throw new RuntimeException("Could not parse AxisUnits");
    String[] axisDomains = gridParms.getStrings("AxisDomains",null );
    if (axisDomains == null) throw new RuntimeException("Could not parse AxisDomains");
    long[] logicalOrigins = gridParms.getLongs("LogicalOrigins",null );
    if (logicalOrigins == null) throw new RuntimeException("Could not parse LogicalOrigins");
    long[] logicalDeltas = gridParms.getLongs("LogicalDeltas",null );
    if (logicalDeltas == null) throw new RuntimeException("Could not parse LogicalDeltas");
    double[] physicalOrigins = gridParms.getDoubles("PhysicalOrigins",null );
    if (physicalOrigins == null) throw new RuntimeException("Could not parse PhysicalOrigins");
    double[] physicalDeltas = gridParms.getDoubles("PhysicalDeltas",null );
    if (physicalDeltas == null) throw new RuntimeException("Could not parse PhysicalDeltas");
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = new AxisDefinition(getAxisLabel(axisLabels[i]), new Units(axisUnits[i]), new DataDomain(axisDomains[i]), axisLengths[i],
          logicalOrigins[i], logicalDeltas[i], physicalOrigins[i], physicalDeltas[i] );
    }
    return new GridDefinition( ndim, axes );
  }
  
  public static AxisLabel getAxisLabel( String axisName ) {
    AxisLabel label = AxisLabel.get(axisName);
    if (label != null) return label;
    return new AxisLabel( axisName, axisName );
  }
  
  public static GridDefinition standardGrid(DataType dataType, int[] lengths) {
    long[] lo = new long[4];
    long[] ld = new long[4];
    Arrays.fill(ld,1);
    double[] po = new double[4];
    double[] pd = new double[4];
    Arrays.fill(pd, 1);
    return GridDefinition.standardGrid(dataType.ordinal(), lengths, lo, ld, po, pd);
  }
  
  public static boolean gridEquals( GridDefinition g1, GridDefinition g2) {
    if (g1.getNumDimensions() != g2.getNumDimensions()) return false;
    for (int i=0; i<g1.getNumDimensions(); i++) {
      if (axisEquals( g1.getAxis(i),g2.getAxis(i) )) continue;
      return false;
    }
    return true;
  }
    
  public static boolean axisEquals(AxisDefinition a1, AxisDefinition a2) {
    if (a1.getLabel().getName().compareTo(a2.getLabel().getName()) != 0) return false;
    if (a1.getLength() != a2.getLength()) return false;
    if (a1.getDomain() != a2.getDomain()) return false;
    if (a1.getLogicalOrigin() != a2.getLogicalOrigin()) return false;
    if (a1.getLogicalDelta() != a2.getLogicalDelta()) return false;
    if (a1.getPhysicalOrigin() != a2.getPhysicalOrigin()) return false;
    if (a1.getPhysicalDelta() != a2.getPhysicalDelta()) return false;
    return true;
  }
  
  public static GridDefinition transpose(GridDefinition grid, TransposeType transType) throws SeisException {
    int[] p = transType.permutation();
    int ndim = grid.getNumDimensions();
    if (ndim != p.length) throw new SeisException("Input grid dimensions " + ndim +
        " and transpose type do not match " + transType );
    AxisDefinition[] axes = new AxisDefinition[ndim];
    for (int i=0; i<ndim; i++) {
      axes[i] = new AxisDefinition(grid.getAxis(p[i]));
    }
    return new GridDefinition(ndim,axes);
  }
  
}
