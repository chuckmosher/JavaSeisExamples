package org.javaseis.examples.tool;

import java.util.Arrays;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.StandAloneVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.util.IntervalTimer;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import edu.mines.jtk.util.ArrayMath;
import beta.javaseis.array.ITraceIterator;
import beta.javaseis.array.PositionIterator;
import beta.javaseis.parallel.ICollective.Operation;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ReduceScalar;

public class ExampleStandAloneInputTool extends StandAloneVolumeTool {
  private static final long serialVersionUID = 1L;
  IParallelContext pc;
  PositionIterator volPos;
  IntervalTimer compTimer, totalTimer;
  GridDefinition inputGrid;
  double compTime, waitTime, totalTime;

  public static void main(String[] args) {
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter(ToolState.INPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.INPUT_FILE_PATH) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_PATH, "temp.js");
    }
    parms.setParameter(ToolState.TASK_COUNT, "4");
    try {
      exec(parms, new ExampleStandAloneInputTool());
    } catch (SeisException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void serialInit(ToolState toolContext) {
    if ((GridDefinition) toolContext.getFlowGlobal(ToolState.INPUT_GRID) == null)
      throw new RuntimeException("No input grid defined");

  }

  @Override
  public void parallelInit(ToolState toolContext) {
    totalTimer = new IntervalTimer();
    totalTimer.start();
    pc = toolContext.getParallelContext();
    inputGrid = (GridDefinition) toolContext.getFlowGlobal(ToolState.INPUT_GRID);
    pc.masterPrint("Input Grid Definition:\n" + inputGrid);
    compTimer = new IntervalTimer();
    int nvol = (int) inputGrid.getAxisLength(3);
    int nhyp = (int) inputGrid.getAxisLength(4);
    volPos = new PositionIterator(new int[] { nvol, nhyp });
  }

  @Override
  public boolean processVolume(ToolState toolContext, ISeismicVolume input, ISeismicVolume output) {
    if (volPos.hasNext()) {
      volPos.next();
      pc.masterPrint("Process Input Volume at position: " + Arrays.toString(volPos.getPosition()));
      compTimer.start();
      ITraceIterator ti = output.getTraceIterator();
      float[] trc;
      double min = Double.MAX_VALUE;
      double max = Double.MIN_VALUE;
      while (ti.hasNext()) {
        trc = ti.next();
        min = Math.min(min, ArrayMath.min(trc));
        max = Math.max(max, ArrayMath.max(trc));
      }
      pc.masterPrint("  Min,Max values in volume: " + ReduceScalar.reduceDouble(pc, min, Operation.MIN) + ", "
          + ReduceScalar.reduceDouble(pc, max, Operation.MAX));
      compTimer.stop();
      return true;
    }
    return volPos.hasNext();
  }

  @Override
  public boolean outputVolume(ToolState toolContext, ISeismicVolume output) {
    return false;
  }

  @Override
  public void parallelFinish(ToolState toolContext) {
    compTime = ReduceScalar.reduceDouble(pc, compTimer.elapsedTime(), Operation.SUM);
    totalTimer.stop();
    totalTime = ReduceScalar.reduceDouble(pc, totalTimer.elapsedTime(), Operation.SUM);
    pc.masterPrint("Completed ExampleStandAloneInputTool:" + "\n  Computation Time: " + compTime + "\n  Total Time: "
        + totalTime);
  }

  @Override
  public void serialFinish(ToolState toolContext) {

  }

  @Override
  public void setState(IVolumeTool anotherTool) throws SeisException {
    // TODO Auto-generated method stub

  }
}
