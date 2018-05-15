package org.javaseis.examples.tool;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;

import org.javaseis.grid.GridDefinition;
import org.javaseis.processing.framework.IVolumeModule;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.processing.module.io.VolumeInput;
import org.javaseis.services.ParameterService;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import edu.mines.jtk.mosaic.PointsView.Mark;
import beta.javaseis.array.ITraceIterator;
import beta.javaseis.distributed.DistributedArrayMosaicPlot;
import beta.javaseis.distributed.DistributedArrayPlot;
import beta.javaseis.parallel.ICollective.Operation;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.plot.PlotScatterPoints;
import beta.javaseis.plot.PointSet;

public class ExampleVolumeInspector implements IVolumeModule {
  private static final long serialVersionUID = 1L;
  private double[] smin, smax, rmin, rmax;
  GridDefinition gridDefinition;
  DistributedArrayPlot plot;

  @Override
  public void serialInit(ModuleState ts) throws SeisException {
    ts.print(ts.toString());
  }

  @Override
  public void parallelInit(IParallelContext pc, ModuleState ModuleState) throws SeisException {
    ModuleState.print(ModuleState.toString());
    gridDefinition = ModuleState.getInputState().getGridDefinition();
    smin = new double[3];
    Arrays.fill(smin, Double.MAX_VALUE);
    rmin = smin.clone();
    smax = new double[3];
    Arrays.fill(smax, -Double.MAX_VALUE);
    rmax = smax.clone();
    /*plot = new DistributedArrayPlot("VolumeInspector",
        (ISeismicVolume) ModuleState.getObject(ModuleState.OUTPUT_VOLUME));*/
  }

  @Override
  public boolean processVolume(IParallelContext pc, ModuleState ModuleState, ISeismicVolume input,
      ISeismicVolume output) throws SeisException {
    int[] filePosition = ModuleState.getInputPosition();
    pc.masterPrint("\nCoordinate Ranges for volume at file position: " + Arrays.toString(filePosition)
        + "\n                     grid position: "
        + Arrays.toString(gridDefinition.indexToGrid(filePosition)));
    int[] localShape = input.getDistributedArray().getTransposeShape();
    int n1 = localShape[1];
    int n2 = localShape[2];
    int ntrc = n1 * n2;

    double[][] sxyz = new double[ntrc][3];
    double[][] rxyz = new double[ntrc][3];
    double[] sxyzMin = new double[3];
    float[] rx = new float[ntrc];
    float[] ry = new float[ntrc];
    float[] sx = new float[2];
    float[] sy = new float[2];
    Arrays.fill(sxyzMin, Double.MAX_VALUE);
    double[] sxyzMax = new double[3];
    Arrays.fill(sxyzMax, -Double.MAX_VALUE);
    double[] rxyzMin = new double[3];
    Arrays.fill(rxyzMin, Double.MAX_VALUE);
    double[] rxyzMax = new double[3];
    Arrays.fill(rxyzMax, -Double.MAX_VALUE);
    int j = 0;
    int j1, j2;
    while (input.hasNext()) {      
      int[] pos = input.next();
      input.getCoords(pos, sxyz[j], rxyz[j]);
      if (j == 0) {
        sx[0] = sx[1] = (float)sxyz[0][0];
        sy[0] = sy[1] = (float)sxyz[0][1];
      }
      rx[j] = (float)rxyz[j][0];
      ry[j] = (float)rxyz[j][1];
      j1 = j % n1;
      j2 = (j - j1) / n1;
      if ((j1 == 0 && j2 == 0) || (j1 == n1 - 1 && j2 == 0) || (j1 == 0 && j2 == n2 - 1)
          || (j1 == n1 - 1 && j2 == n2 - 1)) {
        ModuleState.print("Position " + Arrays.toString(pos) + "  Source " + Arrays.toString(sxyz[j])
            + "  Receiver " + Arrays.toString(rxyz[j]));
      }
      for (int i = 0; i < 3; i++) {
        sxyzMin[i] = Math.min(sxyz[j][i], sxyzMin[i]);
        sxyzMax[i] = Math.max(sxyz[j][i], sxyzMax[i]);
        rxyzMin[i] = Math.min(rxyz[j][i], rxyzMin[i]);
        rxyzMax[i] = Math.max(rxyz[j][i], rxyzMax[i]);
      }
      j++;
    }
    pc.serialPrint("  Local Minimum Values in volume:\n" + "  Source XYZ: " + Arrays.toString(sxyzMin)
        + "  Receiver XYZ: " + Arrays.toString(rxyzMin));
    pc.serialPrint("  Local Maximum Values in volume:\n" + "  Source XYZ: " + Arrays.toString(sxyzMax)
        + "  Receiver XYZ: " + Arrays.toString(rxyzMax));
    pc.reduceDouble(sxyzMin, 0, sxyzMin, 0, 3, Operation.MIN);
    pc.reduceDouble(sxyzMin, 0, sxyzMin, 0, 3, Operation.MIN);

    pc.serialPrint("Global Minimum Values in volume:\n" + "  Source XYZ: " + Arrays.toString(sxyzMin)
        + "  Receiver XYZ: " + Arrays.toString(rxyzMin));
    pc.serialPrint("Global Maximum Values in volume:\n" + "  Source XYZ: " + Arrays.toString(sxyzMax)
        + "  Receiver XYZ: " + Arrays.toString(rxyzMax));
    if (pc.isMaster()) {
      for (int i = 0; i < 3; i++) {
        smin[i] = Math.min(smin[i], sxyzMin[i]);
        smax[i] = Math.max(smax[i], sxyzMax[i]);
        rmin[i] = Math.min(rmin[i], rxyzMin[i]);
        rmax[i] = Math.max(rmax[i], rxyzMax[i]);
      }
    }
    output.copy(input);
    /*PlotScatterPoints psp = new PlotScatterPoints("Source at " + Arrays.toString(sxyz[0]), "X coord", "Y coord" );
    PointSet psr = new PointSet( rx, ry, Mark.CROSS, 5f, Color.BLUE  );
    PointSet pss = new PointSet( sx, sy, Mark.FILLED_SQUARE, 5f, Color.RED );
    psp.addPointSet(psr);
    psp.addPointSet(pss);
    psp.display();*/
    
    //DistributedArrayMosaicPlot.showAsModelDialog("Volume Insepctor",output);
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ModuleState ModuleState, ISeismicVolume output)
      throws SeisException {
    return false;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ModuleState ModuleState) throws SeisException {
    pc.masterPrint("\nGlobal Minimum Values in flow:\n" + "  Source XYZ: " + Arrays.toString(smin)
        + "  Receiver XYZ: " + Arrays.toString(rmin) + "\nGlobal Maximum Values in flow:\n"
        + "  Source XYZ: " + Arrays.toString(smax) + "  Receiver XYZ: " + Arrays.toString(rmax));
    //plot.close();
  }

  @Override
  public void serialFinish(ModuleState ModuleState) throws SeisException {
  }


  public static void main(String[] args) {
    String[] toolList = new String[2];
    toolList[0] = ExampleVolumeInput.class.getCanonicalName();
    toolList[1] = ExampleVolumeInspector.class.getCanonicalName();
    ParameterService globalParms = new ParameterService(args);
    ParameterService[] moduleParms = ParameterService.allocate(2);
    String fileSystem = System.getProperty("user.home") + File.separator + "Projects/SEG-ACTI";
    moduleParms[0].setParameter(ModuleState.INPUT_FILE_SYSTEM, fileSystem );
    moduleParms[0].setParameter(ModuleState.INPUT_FILE_NAME, "SegActi45Shots.js");
    globalParms.setParameter(ModuleState.TASK_COUNT, "1");
    try {
      VolumeModuleRunner.exec(globalParms, moduleParms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

}
