package org.javaseis.examples.tool;

import org.javaseis.grid.GridDefinition;
import org.javaseis.properties.AxisDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.tool.StandAloneVolumeTool;
import org.javaseis.tool.ToolContext;
import org.javaseis.util.IntervalTimer;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.parallel.IParallelContext;

public class ExtractPWaveData extends StandAloneVolumeTool {

  int volumeCount;
  IParallelContext pc;
  IntervalTimer compTime, totalTime;

  private int componentAxis;
  private int pwaveComponentNumber;

  public static void main(String[] args) {
    ParameterService parms = new ParameterService(args);
    setParameterIfUnset(parms,"inputFileSystem","/home/wilsonmr/javaseis");
    setParameterIfUnset(parms,"inputFilePath","100-rawsyntheticdata.js");
    setParameterIfUnset(parms,"outputFileSystem","/home/wilsonmr/javaseis");
    setParameterIfUnset(parms,"outputFilePath","100a-rawsynthpwaves.js");
    exec(parms, new ExtractPWaveData());
  }

  private static void setParameterIfUnset(ParameterService parameterService,
      String parameterName, String parameterValue) {
    if (parameterService.getParameter(parameterName) == "null") {
      parameterService.setParameter(parameterName, parameterValue);
    }
  }

  @Override
  public void serialInit(ToolContext toolContext) {

    findAndRemoveComponentAxisFromGrid(toolContext);
    pwaveComponentNumber = determinePWaveHeaderValue(toolContext);
  }

  private int determinePWaveHeaderValue(ToolContext toolContext) {
    //TODO figure out a nice way to do this in general (ask the user probably)
    return 3;    
  }

  private void findAndRemoveComponentAxisFromGrid(ToolContext toolContext) {

    componentAxis = findComponentAxis(toolContext);

    if (dataIsMulticomponent(toolContext) && componentAxis > 2) {
      System.out.println("Input Data is multicomponent.  The component axis is #" + 
          (componentAxis+1));
      removeComponentAxisFromOutputGrid(toolContext);
    } else {
      if (componentAxis == -1) {
        System.out.println("Input Data is not multicomponent");
      } else {
        System.out.println("Input Data is multicomponent, "
            + "but the components differ within volumes");
      }
      toolContext.setOutputGrid(new GridDefinition(toolContext.getInputGrid()));
    }
  }

  private int findComponentAxis(ToolContext toolContext) {

    String[] AxisLabels = toolContext.getInputGrid().getAxisLabelsStrings();
    int componentAxis = -1;
    for (int axis = 0 ; axis < AxisLabels.length ; axis++) {
      if (AxisLabels[axis].equals("GEO_COMP")) {
        return axis;
      }
    }
    return componentAxis;
  }

  private boolean dataIsMulticomponent(ToolContext toolContext) {
    return (findComponentAxis(toolContext) != -1);
  }

  private void removeComponentAxisFromOutputGrid(ToolContext toolContext) {
    int componentAxis = findComponentAxis(toolContext);

    GridDefinition inputGrid = toolContext.getInputGrid();
    int outputNumDimensions = toolContext.getInputGrid().getNumDimensions() - 1;

    AxisDefinition[] outputAxes = new AxisDefinition[outputNumDimensions];
    for (int dim = 0 ; dim < outputNumDimensions ; dim++) {
      outputAxes[dim] = determineOutputAxis(inputGrid,dim,componentAxis);
    }
    toolContext.setOutputGrid(new GridDefinition(outputAxes.length,outputAxes));
  }

  private AxisDefinition determineOutputAxis(
      GridDefinition inputGrid, int dim, int axisToRemove) {

    if (dim < axisToRemove) return inputGrid.getAxis(dim);
    else return inputGrid.getAxis(dim+1);
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
    compTime.start();
    //output.copyVolume(input);
    
    //Idea: copy every volume where the GEO_COMP index is equal to
    //      pwaveComponentNumber-1.
    //      It would be better if I could figure out the 4th index, then use that to
    //      compute the value of GEO_COMP using origin + index*delta, but I can't make
    //      that work right now, so I'm just going to use the counter
    long numComponents = input.getGlobalGrid().getAxisLengths()[componentAxis];
    if (volumeCount % numComponents == pwaveComponentNumber - 1) { 
      System.out.println("Saving P-waves from volume " + volumeCount++);
      output.copyVolume(input);
      compTime.stop();
      return true;
    } else {
      System.out.println("Trashing S-waves from volume " + volumeCount++);
      compTime.stop();
      return false;
    }

  //TODO extract these musings into a coherent set of tests
  /*  {
      double[] volumePosition = new double[toolContext.inputGrid.getNumDimensions()];
      int[] pos = new int[] {0,0,0};
      input.worldCoords(pos, volumePosition);
      System.out.println(input.getNumDimensions());
      System.out.println(Arrays.toString(volumePosition));

      GridDefinition whatisthis = input.getGlobalGrid();
      System.out.println(whatisthis.getNumDimensions());
      System.out.println(Arrays.toString(whatisthis.getAxisLengths()));
      int[] position = new int[] {43,2,4,2,3};
      //This shouldn't always be true
      System.out.println(input.isPositionLocal(position));
    }
    */
  }
  
  @Override
  public boolean outputVolume(ToolContext toolContext, ISeismicVolume output) {
    return false;
  }

  @Override
  public void parallelFinish(ToolContext toolContext) {
    totalTime.stop();
    pc.masterPrint("Computation Time: " + compTime.total() + "\nTotal Time: " + totalTime.total());
  }
}
