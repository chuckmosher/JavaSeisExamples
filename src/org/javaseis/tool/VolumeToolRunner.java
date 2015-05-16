package org.javaseis.tool;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javaseis.services.ParameterService;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

public class VolumeToolRunner {
  List<IVolumeTool> tools;
  ToolContext[] toolContext;
  int toolCount;
  IParallelContext pc;

  public VolumeToolRunner(List<IVolumeTool> toolList) {
    tools = toolList;
    toolCount = toolList.size();
    toolContext = new ToolContext[toolCount];
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
    if (toolContext[0].getOutputGrid() == null)
      throw new RuntimeException("First tool did not provide an outputGridDefinition");
    for (int i = 1; i < tools.size(); i++) {
      toolContext[i] = new ToolContext(parms);
      toolContext[i].setInputGrid(toolContext[i - 1].getOutputGrid());
      toolContext[i].setOutputGrid(toolContext[i].getInputGrid());
      tools.get(i).serialInit(toolContext[i]);
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
      long[] maxLength = new long[2];
      long inputLength, outputLength;
      for (int i = 0; i < toolCount; i++) {

      }
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
      for (int i = 0; i < toolCount; i++) {
        toolContext[i].setParallelContext(pc);
        tools.get(i).parallelInit(toolContext[i]);
      }
    }

    public void parallelFinish() {
      for (int i = 0; i < toolCount; i++) {
        tools.get(i).parallelFinish(toolContext[i]);
      }
    }
  }
}
