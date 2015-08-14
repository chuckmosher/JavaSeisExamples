package org.javaseis.examples.tool;

import java.util.Arrays;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.tool.DataState;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.StandAloneVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.tool.ToolState.Visibility;
import org.javaseis.util.IntervalTimer;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.array.ITraceIterator;
import beta.javaseis.array.PositionIterator;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.util.RandomRange;

public class ExampleStandAloneOutputTool implements IVolumeTool {
  private static final long serialVersionUID = 1L;
  int volumeCount;
  IParallelContext pc;
  PositionIterator volPos;
  IntervalTimer compTime, totalTime;
  RandomRange rr;

  @Override
  public void serialInit(ToolState toolContext) {
    System.out.println("\n-------- Begin ExampleStandaloneVolumeTool ------------");
    GridDefinition outputGrid = GridDefinition.getDefault(5, new int[] { 201, 201, 201, 9, 5 });
    DataState outputState = new DataState( outputGrid, toolContext.getIntParameter(ToolState.TASK_COUNT, 1) );
    toolContext.putObject(ToolState.OUTPUT_DATA_STATE, outputState, Visibility.TOOL_GLOBAL);
    System.out.println("Output Grid Definition:\n" + outputGrid);
  }

  @Override
  public void serialFinish(ToolState toolContext) {
    System.out.println("\n-------- Completed ExampleStandaloneVolumeTool --------");
  }

  @Override
  public void parallelInit(ToolState toolContext) {
    pc = toolContext.getParallelContext();
    volumeCount = 0;
    compTime = new IntervalTimer();
    totalTime = new IntervalTimer();
    volPos = new PositionIterator(new int[] { 9, 5 });
    rr = new RandomRange(666, -1, 1);
    pc.serialPrint(this.getClass().getCanonicalName() + "Completed parallelInit");
    totalTime.start();
  }

  @Override
  public boolean processVolume(ToolState toolContext, ISeismicVolume input, ISeismicVolume output) {
    return false;
  }

  @Override
  public boolean outputVolume(ToolState toolContext, ISeismicVolume output) {
    if (volPos.hasNext()) {
      volPos.next();
      pc.serialPrint("Output volume at position: " + Arrays.toString(volPos.getPosition()));
      compTime.start();
      ITraceIterator ti = output.getTraceIterator();

      float[] trc;
      while (ti.hasNext()) {
        trc = ti.next();
        rr.fill(trc);
        ti.putTrace(trc);
      }
      compTime.stop();
      return true;
    }
    return false;
  }

  @Override
  public void parallelFinish(ToolState toolContext) {
    pc.serialPrint(this.getClass().getCanonicalName() + "Completed parallelFinish");
  }

  public static void main(String[] args) {
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter(ToolState.OUTPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.OUTPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.OUTPUT_FILE_PATH) == "null") {
      parms.setParameter(ToolState.OUTPUT_FILE_PATH, "temp.js");
    }
    parms.setParameter(ToolState.OUTPUT_FILE_MODE, ToolState.OUTPUT_FILE_CREATE);
    parms.setParameter(ToolState.TASK_COUNT, "4");
    try {
      StandAloneVolumeTool.exec(parms, new ExampleStandAloneOutputTool());
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }
}
