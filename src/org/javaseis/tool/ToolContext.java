package org.javaseis.tool;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;

import beta.javaseis.parallel.IParallelContext;

public class ToolContext {
	  public ParameterService parms;
	  public IParallelContext pc;
	  public GridDefinition inputGrid, outputGrid;
	  public void setParamaterService(ParameterService parmService) {
		  parms = parmService;
	  }
	  public void setParallelContext( IParallelContext parallelContext) {
		  pc = parallelContext;
	  }
	  public ParameterService getParameterService() {
		  return parms;
	  }
	  public IParallelContext getParallelContext() {
		  return pc;
	  }
	  public void setInputGrid( GridDefinition gridDefinition ) {
		  inputGrid = gridDefinition;
	  }
	  public GridDefinition getInputGrid() {
		  return inputGrid;
	  }
	  public void setOutputGrid( GridDefinition gridDefinition ) {
		  outputGrid = gridDefinition;
	  }
	  public GridDefinition getOutputGrid() {
		  return outputGrid;
	  }
  }