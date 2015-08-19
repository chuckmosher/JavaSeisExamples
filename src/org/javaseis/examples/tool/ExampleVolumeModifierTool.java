package org.javaseis.examples.tool;

import org.javaseis.services.ParameterService;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.tool.VolumeToolRunner;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.array.ITraceIterator;
import beta.javaseis.parallel.IParallelContext;
import edu.mines.jtk.util.ArrayMath;

/**
 * Example tool that modifies an input volume scalar multiplication.
 * <p>
 * Parameters:
 * <p>
 * org.javaseis.examples.tool.ExampleVolumeTool.scalarValue - scalar that will
 * be multiplied with input data to produce output data
 */
public class ExampleVolumeModifierTool implements IVolumeTool {
  private static final long serialVersionUID = 1L;
  float scalarValue;

  @Override
  public void serialInit(ToolState toolState) {
    // Get the scalar multiplier
    scalarValue = toolState.getFloatParameter("scalarValue", 0f);
    toolState.log("Scalar value for multiplication: " + scalarValue);
  }

  @Override
  public void parallelInit(IParallelContext pc, ToolState toolState) {
    pc.serialPrint("Entering parallelInit: scalarValue = " + scalarValue);
  }

  @Override
  public boolean processVolume(IParallelContext pc, ToolState toolState, ISeismicVolume input,
      ISeismicVolume output) {
    // Get trace iterators for input and output
    ITraceIterator iti = input.getTraceIterator();
    ITraceIterator oti = output.getTraceIterator();
    float[] trc;
    // Loop over input traces
    while (iti.hasNext()) {
      // Get the trace and multiply by scalar
      trc = iti.next();
      ArrayMath.mul(scalarValue, trc, trc);
      // Advance the output trace iterator and store the modified trace
      oti.next();
      oti.putTrace(trc);
    }
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ToolState toolContext, ISeismicVolume output) {
    // No additional output
    return false;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ToolState toolContext) {
    // Nothing to clean up for parallel tasks
  }

  @Override
  public void serialFinish(ToolState toolContext) {
    // Nothing to clean up in serial mode
  }

  public static void main(String[] args) {
    String[] toolList = new String[2];
    toolList[0] = ExampleVolumeInputTool.class.getCanonicalName();
    toolList[1] = ExampleVolumeModifierTool.class.getCanonicalName();
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter(ToolState.INPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_SYSTEM, "/Data/Projects/SEG-ACTI");
      //parms.setParameter(ToolState.INPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.INPUT_FILE_NAME) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_NAME, "SegActiShotNo1.js");
      //parms.setParameter(ToolState.INPUT_FILE_NAME, "temp.js");
    }
    parms.setParameter("scalarValue", "2.0");
    parms.setParameter(ToolState.TASK_COUNT, "4");
    try {
      VolumeToolRunner.exec(parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }
}
