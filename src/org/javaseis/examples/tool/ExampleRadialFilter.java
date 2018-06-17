package org.javaseis.examples.tool;

import org.javaseis.grid.GridDefinition;
import org.javaseis.processing.framework.IVolumeModule;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.services.ParameterService;
import org.javaseis.util.IntervalTimer;
import org.javaseis.util.SeisException;
import org.javaseis.util.IntervalTimer.TimeType;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.fft.SeisFft3d;
import beta.javaseis.parallel.IParallelContext;
import edu.mines.jtk.util.ArrayMath;

public class ExampleRadialFilter implements IVolumeModule {

  private static final long serialVersionUID = 1L;

  private GridDefinition gridDefinition;

  private SeisFft3d fft3d;

  private float validationTolerance;

  @Override
  public void serialInit(ModuleState moduleState) throws SeisException {
    validationTolerance = moduleState.getFloatParameter("validationTolerance", 1e-6f);
  }

  @Override
  public void serialFinish(ModuleState moduleState) throws SeisException {
  }

  @Override
  public void parallelInit(IParallelContext pc, ModuleState moduleState) throws SeisException {
    moduleState.print(moduleState.toString());
    gridDefinition = moduleState.getInputState().getGridDefinition();
    int[] shape = moduleState.getInputState().getArrayShape();
    float[] pad = new float[shape.length];
    int[] sign = new int[shape.length];
    ArrayMath.fill(1, sign);
    sign[0] = -1;
    fft3d = new SeisFft3d(pc, shape, pad, sign);
  }

  @Override
  public boolean processVolume(IParallelContext pc, ModuleState moduleState, ISeismicVolume inputVolume,
      ISeismicVolume outputVolume) throws SeisException {
    // Copy the input distributed array
    fft3d.getArray().copy(inputVolume.getDistributedArray());

    // Forward Transform
    IntervalTimer timer = new IntervalTimer();
    timer.start(TimeType.COMP);
    fft3d.forward();
    double dt = timer.stop(TimeType.COMP);
    pc.masterPrint("Forward 3D FFT completed in " + dt + " sec");

    // TODO add filter

    // Inverse transform
    timer.start(TimeType.COMP);
    fft3d.inverse();
    dt = timer.stop(TimeType.COMP);
    pc.masterPrint("Inverse 3D FFT completed in " + dt + " sec");

    // Validate
    DistributedArray input = inputVolume.getDistributedArray();
    DistributedArray wk = fft3d.getArray();
    int n = input.getShape()[0];
    float[] trc = new float[n];
    float[] wtrc = new float[n];
    input.resetTraceIterator();
    wk.resetTraceIterator();
    long traceCounter = 0;
    while (input.hasNext()) {
      input.next();
      input.getTrace(trc);
      wk.next();
      wk.getTrace(wtrc);
      for (int i = 0; i < n; i++) {
        if (Math.abs(trc[i] - wtrc[i]) > validationTolerance) {
          throw new RuntimeException("Out of Range at trace " + traceCounter + " sample " + i
              + ": input " + trc[i] + " output " + wtrc[i]);
        }
      }
      traceCounter++;
    }

    outputVolume.copy(inputVolume);
    outputVolume.getDistributedArray().copy(wk);
    pc.serialPrint("Success from task " + pc.rank());
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ModuleState moduleState, ISeismicVolume output) throws SeisException {
    return false;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ModuleState moduleState) throws SeisException {
  }

  public static void main(String[] args) {
    int toolCount = 3;
    String[] toolList = new String[toolCount];
    toolList[0] = ExampleVolumeInput.class.getCanonicalName();
    toolList[1] = ExampleRadialFilter.class.getCanonicalName();
    toolList[2] = ExampleVolumeInspector.class.getCanonicalName();

    ParameterService globalParms = new ParameterService(args);
    ParameterService[] parms = ParameterService.allocate(toolCount, globalParms);
    String fileSystem = System.getProperty("user.home") + "/Projects/SEG-ACTI";
    parms[0].setParameter(ModuleState.INPUT_FILE_SYSTEM, fileSystem);
    parms[0].setParameter(ModuleState.INPUT_FILE_NAME, "SegActi45Shots.js");
    parms[0].setParameter("volumeRange", "18,26,1");
    parms[1].setParameter("validationTolerance", "5e-5f");//"1e-6f");

    globalParms.setParameter(ModuleState.TASK_COUNT, "1");
    try {
      VolumeModuleRunner.exec(globalParms, parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

}
