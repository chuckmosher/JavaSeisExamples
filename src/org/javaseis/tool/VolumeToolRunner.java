package org.javaseis.tool;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.volume.ISeismicVolume;
import org.javaseis.volume.SeismicVolume;

import beta.javaseis.distributed.Decomposition;
import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

public class VolumeToolRunner {
  List<IVolumeTool> tools;
  ToolContext[] toolContext;
  ISeismicVolume[] vol;
  int toolCount;
  IParallelContext pc;

  public VolumeToolRunner(List<IVolumeTool> toolList) {
    tools = toolList;
    toolCount = toolList.size();
    toolContext = new ToolContext[toolCount];
    vol = new ISeismicVolume[2];
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
      toolContext[i].mergeFlowMaps(toolContext[i - 1]);
      toolContext[i].putToolGlobal(ToolContext.INPUT_GRID, currentGrid);
      toolContext[i].putToolGlobal(ToolContext.OUTPUT_GRID, currentGrid);
      tools.get(i).serialInit(toolContext[i]);
      currentGrid = (GridDefinition) toolContext[i].getToolGlobal(ToolContext.OUTPUT_GRID);
      if (currentGrid == null) throw new RuntimeException("Tool did not provide an outputGridDefinition");
    }
  }

  public void serialFinish() {
    for (int i = 0; i < tools.size(); i++) {
      tools.get(i).serialFinish(toolContext[i]);
    }
  }

  public static long getShapeLength(IParallelContext pc, long[] shape) {
    int[] paddedShape = new int[] { (int) shape[0], (int) shape[2],
        (int) Decomposition.paddedLength(shape[2], pc.size()) };
    paddedShape[2] = (int) Decomposition.paddedLength(shape[2], pc.size());
    return DistributedArray.getShapeLength(3, 1, paddedShape);
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
      int ivol = 0;
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
      long maxlength = getShapeLength(pc, currentGrid.getAxisLengths());
      for (int i = 1; i < toolCount; i++) {
        toolContext[i].setParallelContext(pc);
        toolContext[i].mergeFlowMaps(toolContext[i - 1]);
        toolContext[i].putToolGlobal(ToolContext.INPUT_GRID, currentGrid);
        tools.get(i).parallelInit(toolContext[i]);
        currentGrid = (GridDefinition) toolContext[i].getToolGlobal(ToolContext.OUTPUT_GRID);
        maxlength = Math.max(maxlength, getShapeLength(pc, currentGrid.getAxisLengths()));
      }
      toolContext[0].mergeFlowMaps(toolContext[toolCount - 1]);
      vol[0] = new SeismicVolume(pc, (GridDefinition) toolContext[0].getToolGlobal(ToolContext.OUTPUT_GRID),
          maxlength);
      if (toolCount > 1) {
        vol[1] = new SeismicVolume(pc,
            (GridDefinition) toolContext[1].getToolGlobal(ToolContext.OUTPUT_GRID), maxlength);
      }
    }

    public void parallelFinish() {
      for (int i = 0; i < toolCount; i++) {
        tools.get(i).parallelFinish(toolContext[i]);
      }
    }
  }
}
