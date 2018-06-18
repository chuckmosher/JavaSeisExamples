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

  private float vlc;

  private float vlp;

  private float vhp;

  private float vhc;

  private float[] pad;

  private boolean doBandpass = false; 

  @Override
  public void serialInit(ModuleState moduleState) throws SeisException {
    vlp = moduleState.getFloatParameter("velLoPass", 0);
    vlc = moduleState.getFloatParameter("velLoCut", 0);
    vhc = moduleState.getFloatParameter("velHiCut", 0);
    vhp = moduleState.getFloatParameter("velHiPass", 0);
    if (doBandpass) {
      if (vlc > vlp || vlp > vhp || vhp > vhc) {
        throw new SeisException("Invalid filter specification (" + vlc + "," + vlp + "," + vhp + "," + vhc + ")");
      }
    } else {
      if (vlp > vlc || vlc > vhc || vhc > vhp) {
        throw new SeisException("Invalid filter specification (" + vlp + "," + vlc + "," + vhc + "," + vhp + ")");
      }
    }
    pad = new float[3];
    pad[0] = moduleState.getFloatParameter("pad0", 0);
    pad[1] = moduleState.getFloatParameter("pad1", 0);
    pad[2] = moduleState.getFloatParameter("pad2", 0);
  }

  @Override
  public void serialFinish(ModuleState moduleState) throws SeisException {
  }

  @Override
  public void parallelInit(IParallelContext pc, ModuleState moduleState) throws SeisException {
    moduleState.print(moduleState.toString());
    gridDefinition = moduleState.getInputState().getGridDefinition();
    pc.masterPrint(gridDefinition.toString());
    int[] shape = moduleState.getInputState().getArrayShape();
    int[] sign = new int[shape.length];
    ArrayMath.fill(1, sign);
    sign[0] = -1;
    fft3d = new SeisFft3d(pc, shape, pad, sign);
    fft3d.setTXYSampleRates(new double[] { gridDefinition.getAxisPhysicalDelta(0)/1000.0,
        gridDefinition.getAxisPhysicalDelta(1), gridDefinition.getAxisPhysicalDelta(2)});
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

    // Apply filter
    timer.start(TimeType.COMP);
    if (doBandpass) {
      applyBandpassFilter(pc);
    } else {
      applyBandstopFilter(pc);
    }
    dt = timer.stop(TimeType.COMP);
    pc.masterPrint("Apply filter completed in " + dt + " sec ");

    // Inverse transform
    timer.start(TimeType.COMP);
    fft3d.inverse();
    dt = timer.stop(TimeType.COMP);
    pc.masterPrint("Inverse 3D FFT completed in " + dt + " sec");

    // Copy to output distributed array
    outputVolume.copy(inputVolume);
    outputVolume.getDistributedArray().copy(fft3d.getArray());
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

  private void applyBandpassFilter(IParallelContext pc) {
    DistributedArray array = fft3d.getArray();
    array.resetSampleIterator();
    double[] fftpos = new double[3];
    float[] sample = new float[2];
    while (array.hasNext()) {
      array.next();
      int[] pos = array.getPosition();
      array.getSample(sample, pos);
      fft3d.getKyKxFCoordinatesForPosition(pos, fftpos);
      double k = Math.sqrt(fftpos[0]*fftpos[0] + fftpos[1]*fftpos[1]);
      double vel = k == 0 ? 0 : fftpos[2] / k;
      // Apply weighting with simple linear taper
      if (vel < vlc) {
        sample[0] = sample[1] = 0f;
      } else if (vel < vlp) {
        double weight = 1 - (vlp - vel) / (vlp - vlc);
        sample[0] *= weight;
        sample[1] *= weight;
      } else if (vel < vhp) {
        // In pass band, so nothing to do here
      } else if (vel < vhc) {
        double weight = (vhc - vel) / (vhc - vhp);
        sample[0] *= weight;
        sample[1] *= weight;
      } else { // vel >= vhc
        sample[0] = sample[1] = 0f;
      }
      array.putSample(sample, pos);
    }
  }

  private void applyBandstopFilter(IParallelContext pc) {
    DistributedArray array = fft3d.getArray();
    array.resetSampleIterator();
    double[] fftpos = new double[3];
    float[] sample = new float[2];
    while (array.hasNext()) {
      array.next();
      int[] pos = array.getPosition();
      array.getSample(sample, pos);
      fft3d.getKyKxFCoordinatesForPosition(pos, fftpos);
      double k = Math.sqrt(fftpos[0]*fftpos[0] + fftpos[1]*fftpos[1]);
      double vel = k == 0 ? 0 : fftpos[2] / k;
      // Apply weighting with simple linear taper
      if (vel < vlp) {
        // Pass, so nothing to do here
      } else if (vel < vlc) {
        double weight = (vlc - vel) / (vlc - vlp);
        sample[0] *= weight;
        sample[1] *= weight;
      } else if (vel < vhc) {
        sample[0] = sample[1] = 0f;
      } else if (vel < vhp) {
        double weight = 1 - (vhp - vel) / (vhp - vhc);
        sample[0] *= weight;
        sample[1] *= weight;
      } else { // vel >= vhp
        // Pass, so nothing to do here
      }
      array.putSample(sample, pos);
    }
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
    parms[1].setParameter("velLoPass", "500f");
    parms[1].setParameter("velLoCut", "4000f");
    parms[1].setParameter("velHiCut", "5500f");
    parms[1].setParameter("velHiPass", "6000f");
    parms[1].setParameter("pad0", "50f");
    parms[1].setParameter("pad1", "50f");
    parms[1].setParameter("pad2", "50f");

    globalParms.setParameter(ModuleState.TASK_COUNT, "1");
    try {
      VolumeModuleRunner.exec(globalParms, parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

}
