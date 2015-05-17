package org.javaseis.examples.tool;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.tool.StandAloneVolumeTool;
import org.javaseis.tool.ToolContext;
import org.javaseis.util.IntervalTimer;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.parallel.IParallelContext;

public class ExampleVolumeTool extends StandAloneVolumeTool {

  int volumeCount;
  IParallelContext pc;
  IntervalTimer compTime, totalTime;

  public static void main(String[] args) {
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter("inputFileSystem") == "null") {
      parms.setParameter("inputFileSystem", System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter("inputFilePath") == "null") {
      parms.setParameter("inputFilePath", "temp.js");
    }
    exec(parms, new ExampleVolumeTool());
  }
  
  @Override
  public void serialInit(ToolContext toolContext) {
    toolContext.setOutputGrid(new GridDefinition(toolContext.getInputGrid()));
  }

  @Override
  public void parallelInit(ToolContext toolContext) {
    volumeCount = 0;
    pc = toolContext.getParallelContext();
    pc.masterPrint("Input Grid Definition:\n" + toolContext.getInputGrid());
    pc.masterPrint("Output Grid Definition:\n" + toolContext.getOutputGrid());
    compTime = new IntervalTimer();
    totalTime = new IntervalTimer();
    totalTime.start();
  }

  @Override
  public boolean processVolume(ToolContext toolContext, ISeismicVolume input, ISeismicVolume output) {
    System.out.println("Process volume " + volumeCount++);
    compTime.start();
    output.copyVolume(input);
    compTime.stop();
    return true;
  }

  @Override
  public void parallelFinish(ToolContext toolContext) {
    totalTime.stop();
    pc.masterPrint("Computation Time: " + compTime.total() + "\nTotal Time: " + totalTime.total());
  }
}
