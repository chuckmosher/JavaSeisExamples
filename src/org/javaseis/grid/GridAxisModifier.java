package org.javaseis.grid;

import org.javaseis.properties.AxisDefinition;
import org.javaseis.properties.AxisLabel;
import org.javaseis.properties.DataDomain;
import org.javaseis.properties.Units;

/**
 * Convenience class for modifying individual fields of an AxisDefinition
 */
public class GridAxisModifier {
	  
  public static AxisDefinition setLabel( AxisDefinition axis, AxisLabel label ) {
	  return new AxisDefinition( label, axis.getUnits(), axis.getDomain(), 
	      axis.getLength(), axis.getLogicalOrigin(), axis.getLogicalDelta(), axis.getPhysicalOrigin(),
	      axis.getPhysicalDelta() );
  }
  
  public static AxisDefinition setUnits( AxisDefinition axis, Units units ) {
    return new AxisDefinition( axis.getLabel(), units, axis.getDomain(), 
        axis.getLength(), axis.getLogicalOrigin(), axis.getLogicalDelta(), axis.getPhysicalOrigin(),
        axis.getPhysicalDelta() );
  }
  
  public static AxisDefinition setDomain( AxisDefinition axis, DataDomain domain ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), domain, 
        axis.getLength(), axis.getLogicalOrigin(), axis.getLogicalDelta(), axis.getPhysicalOrigin(),
        axis.getPhysicalDelta() );
  }
		  
  public static AxisDefinition setLength( AxisDefinition axis, long length ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), axis.getDomain(), 
        length, axis.getLogicalOrigin(), axis.getLogicalDelta(), axis.getPhysicalOrigin(),
        axis.getPhysicalDelta() );
  }	
  
  public static AxisDefinition setLogicalOrigin( AxisDefinition axis, long logicalOrigin ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), axis.getDomain(), 
        axis.getLength(), logicalOrigin, axis.getLogicalDelta(), axis.getPhysicalOrigin(),
        axis.getPhysicalDelta() );
  }
    
  public static AxisDefinition setLogicalDelta( AxisDefinition axis, long logicalDelta ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), axis.getDomain(), 
        axis.getLength(), axis.getLogicalOrigin(), logicalDelta, axis.getPhysicalOrigin(),
        axis.getPhysicalDelta() );
  }	
  
  public static AxisDefinition setPhysicalOrigin( AxisDefinition axis, double physicalOrigin ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), axis.getDomain(), 
        axis.getLength(), axis.getLogicalOrigin(), axis.getLogicalDelta(), physicalOrigin,
        axis.getPhysicalDelta() );
  }
    
  public static AxisDefinition setPhysicalDelta( AxisDefinition axis, double physicalDelta ) {
    return new AxisDefinition( axis.getLabel(), axis.getUnits(), axis.getDomain(), 
        axis.getLength(), axis.getLogicalOrigin(), axis.getLogicalDelta(), axis.getPhysicalOrigin(),
        physicalDelta );
  }
  
}