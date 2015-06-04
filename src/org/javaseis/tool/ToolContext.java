package org.javaseis.tool;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.parallel.IParallelContext;

/**
 * Container class for consolidating things developers would like to know about
 * the data and the tool environment
 *
 * @author Chuck Mosher for JavaSeis.org
 *
 */
public class ToolContext {
  public ParameterService parms;
  public IParallelContext pc;
  public GridDefinition inputGrid, outputGrid;
  public ISeismicVolume inputVolume, outputVolume;

  public ToolContext() {
  }

  public ToolContext(ParameterService parameterService) {
    parms = parameterService;
  }

  /**
   * Set the parameter service that will be used for communicating key,value
   * pairs
   *
   * @param parmService
   *          - parameter service for tool communication
   */
  public void setParameterService(ParameterService parmService) {
    parms = parmService;
  }

  /**
   * Define the parallel context that will be made available to tools
   *
   * @param parallelContext
   *          - parallel context that will be used by tools
   */
  public void setParallelContext(IParallelContext parallelContext) {
    pc = parallelContext;
  }

  /**
   * Return the parameter service that is available to this tool
   *
   * @return - parameter service for this tool
   */
  public ParameterService getParameterService() {
    return parms;
  }

  /**
   * Return the parallel context this tool will use
   *
   * @return - parallel context for this tool
   */
  public IParallelContext getParallelContext() {
    return pc;
  }

  /**
   * Define the input grid for this tool
   *
   * @param gridDefinition
   */
  public void setInputGrid(GridDefinition gridDefinition) {
    inputGrid = gridDefinition;
  }

  /**
   * Return the GridDefinition for the input to this tool
   *
   * @return The GridDefinition for the input to this tool
   */
  public GridDefinition getInputGrid() {
    return inputGrid;
  }

  /**
   * Set the output grid that this tool will provide
   *
   * @param gridDefinition
   */
  public void setOutputGrid(GridDefinition gridDefinition) {
    outputGrid = gridDefinition;
  }

  /**
   * Return the output grid for a tool
   *
   * @return The GridDefinition for the output from this tool
   */
  public GridDefinition getOutputGrid() {
    return outputGrid;
  }
  
  public void setInputVolume( ISeismicVolume inputSeismicVolume ) {
    inputVolume = inputSeismicVolume;
  }

  public ISeismicVolume getInputVolume() {
    return inputVolume;
  }

  
  public void setOutputVolume( ISeismicVolume outputSeismicVolume ) {
    outputVolume = outputSeismicVolume;
  }

  public ISeismicVolume getOutputVolume() {
    return outputVolume;
  }
  
  public ToolContext(ToolContext tc) {
    parms = tc.parms;
    pc = tc.pc;
    inputGrid = tc.inputGrid;
    outputGrid = tc.outputGrid;
    inputVolume = tc.inputVolume;
    outputVolume = tc.outputVolume;
  }
}