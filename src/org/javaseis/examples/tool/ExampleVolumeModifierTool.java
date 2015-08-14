package org.javaseis.examples.tool;

import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.volume.ISeismicVolume;

import edu.mines.jtk.util.ArrayMath;
import beta.javaseis.array.ITraceIterator;

/**
 * Example tool that modifies an input volume scalar multiplication.
 * <p>
 * Parameters:
 * <p>
 * org.javaseis.examples.tool.ExampleVolumeTool.scalarValue - scalar that will
 * be multiplied with input data to produce output data
 */
public class ExampleVolumeModifierTool implements IVolumeTool {
  float scalarValue;

  @Override
  public void serialInit(ToolState serialToolContext) {
    // Nothing to initialize in serial mode
  }

  @Override
  public void parallelInit(ToolState toolContext) {
    // Get the scalar multiplier
    scalarValue = toolContext.getFloatParameter("scalarValue", 0f);
    toolContext.log("Scalar value for multiplication: " + scalarValue);
  }

  @Override
  public boolean processVolume(ToolState toolContext, ISeismicVolume input, ISeismicVolume output) {
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
  public boolean outputVolume(ToolState toolContext, ISeismicVolume output) {
    // No additional output
    return false;
  }

  @Override
  public void parallelFinish(ToolState toolContext) {
    // Nothing to clean up for parallel tasks
  }

  @Override
  public void serialFinish(ToolState toolContext) {
    // Nothing to clean up in serial mode
  }

  public static void main(String[] args) {

  }

}
