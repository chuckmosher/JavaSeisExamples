package org.javaseis.tool;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.volume.ISeismicVolume;
import org.javaseis.volume.SeismicVolume;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

public class VolumeToolRunner {
  List<IVolumeTool> tools;
  ToolContext[] toolContext;
  ISeismicVolume[] inputVolume;
  ISeismicVolume[] outputVolume;
  int toolCount;
  IParallelContext pc;

  public VolumeToolRunner(List<IVolumeTool> toolList) {
    tools = toolList;
    toolCount = toolList.size();
    toolContext = new ToolContext[toolCount];
    inputVolume = new ISeismicVolume[toolCount];
    outputVolume = new ISeismicVolume[toolCount];
  }

  public static void exec(ParameterService parms, List<IVolumeTool> toolList) {
    VolumeToolRunner vtr = new VolumeToolRunner(toolList);
    vtr.serialInit(parms);

    // Now run the tool handler which calls the implementor's methods
    int ntask = Integer.parseInt(parms.getParameter("threadCount", "1"));
    try {
      ParallelTaskExecutor.runTasks(VolumeRunnerTask.class, ntask);
    } catch (ExecutionException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

  }

  public void serialInit(ParameterService parms) {
    IVolumeTool tool0 = tools.get(0);
    toolContext[0] = new ToolContext(parms);
    tool0.serialInit(toolContext[0]);
    GridDefinition currentGrid = (GridDefinition) toolContext[0].getToolGlobal(ToolContext.OUTPUT_GRID);
    if (currentGrid == null)
      throw new RuntimeException("First tool did not provide an outputGridDefinition");
    for (int i = 1; i < tools.size(); i++) {
      toolContext[i] = new ToolContext(parms);
      toolContext[i].mergeFlowMaps(toolContext[i-1]);
      toolContext[i].putToolGlobal(ToolContext.INPUT_GRID, currentGrid );
      toolContext[i].putToolGlobal(ToolContext.OUTPUT_GRID, currentGrid );
      tools.get(i).serialInit(toolContext[i]);
      currentGrid = (GridDefinition) toolContext[i].getToolGlobal(ToolContext.OUTPUT_GRID);
      if (currentGrid == null)
        throw new RuntimeException("Tool did not provide an outputGridDefinition");
    }
  }

  public void serialFinish() {
    for (int i = 0; i < tools.size(); i++) {
      tools.get(i).serialFinish(toolContext[i]);
    }
  }

  public class VolumeRunnerTask extends ParallelTask {

    @Override
    public void run() {
      parallelInit();
      parallelProcess();
      parallelFinish();
    }

    public void parallelProcess() {// Create the input and output seismic volumes

      int inputToolIndex = 0;
      while (inputToolIndex < toolCount) {

        for (int i = inputToolIndex; i < toolCount; i++) {

        }
      }
    }

    public void parallelInit() {
      IParallelContext pc = this.getParallelContext();
      toolContext[0].setParallelContext(pc);
      tools.get(0).parallelInit(toolContext[0]);
      GridDefinition currentGrid = (GridDefinition) toolContext[0].getToolGlobal(ToolContext.OUTPUT_GRID);
      inputVolume[0] = null;
      outputVolume[0] = new SeismicVolume(pc,currentGrid);
      for (int i = 1; i < toolCount; i++) {
        toolContext[i].setParallelContext(pc);
        toolContext[i].mergeFlowMaps(toolContext[i-1]);
        toolContext[i].putToolGlobal(ToolContext.INPUT_GRID, currentGrid );
        inputVolume[i] = outputVolume[i-1];
        tools.get(i).parallelInit(toolContext[i]);
        currentGrid = (GridDefinition) toolContext[i].getToolGlobal(ToolContext.OUTPUT_GRID);
      }
      toolContext[0].mergeFlowMaps(toolContext[toolCount-1]);
    }

    public void parallelFinish() {
      for (int i = 0; i < toolCount; i++) {
        tools.get(i).parallelFinish(toolContext[i]);
      }
    }
  }
}
