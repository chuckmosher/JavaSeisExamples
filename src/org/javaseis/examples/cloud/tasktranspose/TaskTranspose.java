package org.javaseis.examples.cloud.tasktranspose;

import java.util.Arrays;
import java.util.Collections;

import org.javaseis.cloud.array.PositionIterator;
import org.javaseis.grid.GridDefinition;
import org.javaseis.grid.GridUtil;
import org.javaseis.io.Seisio;
import org.javaseis.util.SeisException;

import beta.javaseis.array.TransposeType;
import beta.javaseis.distributed.Decomposition;
import beta.javaseis.util.Convert;

public class TaskTranspose {
  /** Shape parameters */
  int n0, n1, n2, p1, p2, p2p1, n1p, n2p;
  /** input, padded, tiled, and output shapes */
  int[] shape, pshape, tshape, oshape;
  /** Path for input dataset */
  String jsInPath;
  /** Path for tiled dataset */
  String jsTilePath;
  /** Path for output dataset */
  String jsOutPath;
  /** Object sizes */
  double inFrameSize, outFrameSize, inTranSize, outTranSize, tileSize;
  /** Track times flag */
  boolean trackTimes = false;

  // Constant to convert words to MegaBytes (MiB)
  static double w2mb = 4.0 / (1024.0 * 1024.0);

  public TaskTranspose(String inPath, String tilePath, String outPath, int tileSize1, int tileSize2, boolean track)
      throws SeisException {

    // Open the input for read only
    Seisio sio = new Seisio(inPath);
    sio.open("r");
    // Get the GridDefinition and check number of dimensions
    GridDefinition grid = sio.getGridDefinition();
    if (grid.getNumDimensions() != 4)
      throw new SeisException("Input must be a 4D array");
    jsInPath = new String(inPath);
    // Get the shape of the framework
    shape = Convert.longToInt(grid.getAxisLengths());
    sio.close();
    // Set tile sizes
    inFrameSize = w2mb * shape[0] * shape[1];
    p1 = tileSize1;
    p2 = tileSize2;
    p2p1 = p2 * p1;
    tileSize = w2mb * p2p1 * shape[0];
    // Get the padded shape
    pshape = getTiledShape(p1, p2, 3, shape);
    n0 = pshape[0];
    n1 = pshape[1];
    n2 = pshape[2];
    // Set the tile counts
    n1p = n1 / p1;
    n2p = n2 / p2;
    // Output shape with collapsed dimension on disk
    tshape = new int[] { n0, p2p1, n1p, n2p * shape[3] };
    // Check the output path for the tiled dataset
    if (Seisio.isJavaSeis(tilePath) == true) {
      sio = new Seisio(tilePath);
      sio.open("rw");
      int[] tmp = Convert.longToInt(sio.getGridDefinition().getAxisLengths());
      if (tmp.length != 4)
        throw new SeisException("Tiled Dataset exists but shape does not match");
      for (int i = 0; i < 4; i++) {
        if (tmp[i] != tshape[i])
          throw new SeisException("Tiled Dataset exists but shape does not match");
      }
    } else {
      sio = new Seisio(tilePath, GridDefinition.getDefault(4, tshape));
      sio.create();
      sio.close();
    }
    jsTilePath = new String(tilePath);

    // Output shape
    oshape = new int[] { shape[0], shape[2], shape[1], shape[3] };
    // Create output dataset
    if (Seisio.isJavaSeis(outPath) == false) {
      GridDefinition outGrid = GridUtil.transpose(grid, TransposeType.T1324);
      sio = new Seisio(outPath, outGrid);
      sio.create();
      sio.close();
    }
    jsOutPath = new String(outPath);

    trackTimes = track;
  }

  public TaskTransposeStage1 getStage1(int itask, int ivol) {
    TaskTransposeStage1 tts1 = new TaskTransposeStage1(this);
    tts1.setVolume(ivol);
    tts1.setTask(itask);
    return tts1;
  }

  public TaskTransposeStage2 getStage2(int itask, int ivol) {
    TaskTransposeStage2 tts2 = new TaskTransposeStage2(this);
    tts2.setVolume(ivol);
    tts2.setTask(itask);
    return tts2;
  }
  
  public int getStage1Tasks() {
    return n2p;
  }
  
  public int getStage2Tasks() {
    return n1p;
  }

  /**
   * Calculate padded size to support transpose operations
   * 
   * @param size1 - tile size along axis 1
   * @param size2 - tile size along axis 2
   * @param ndim  - number of dimensions
   * @param shape - shape of the input dataset
   * @return shape padding for transposes
   */
  public static int[] getTiledShape(int size1, int size2, int ndim, int[] shape) {
    int[] padShape = new int[ndim];
    padShape[0] = shape[0];
    padShape[1] = (int) Decomposition.paddedLength(shape[1], size1);
    padShape[2] = (int) Decomposition.paddedLength(shape[2], size2);
    for (int i = 3; i < ndim; i++) {
      padShape[i] = shape[i];
    }
    return padShape;
  }
  
  public static void main( String[] args ) {
    try {
      mainTest();
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public static void mainTest() throws SeisException {
    String inPath = "/tmp/input.js";
    String tilePath = "/tmp/tiles.js";
    String outPath = "/tmp/trans.js";
    int ndim = 4;
    int[] shape = new int[] { 100, 225, 225, 1 };
    Seisio sio = new Seisio (inPath, GridDefinition.getDefault(ndim, shape));
    sio.create();
    float[][] trcs = sio.getTraceDataArray();
    PositionIterator pos = new PositionIterator( shape, 2 );
    while (pos.hasNext()) {
      int[] ipos = pos.next();
      for (int j=0; j<shape[1]; j++) {
        float val = 1000*ipos[2] + j + ipos[3];
        Arrays.fill(trcs[j], val);
      }
      sio.writeFrame(ipos,shape[1]);
    }
    sio.close();
    String[] args = new String[] { inPath, tilePath, outPath, "14", "14", "0" };
    mainArgs(args);
    sio = new Seisio(outPath);
    sio.open("r");
    shape = Convert.longToInt(sio.getGridDefinition().getAxisLengths());
    pos = new PositionIterator( shape, 2 );
    trcs = sio.getTraceDataArray();
    while (pos.hasNext()) {
      int[] ipos = pos.next();
      sio.readFrame(ipos);
      for (int j=0; j<shape[1]; j++) {
        ipos[1] = j;
        float val = 1000*ipos[1] + ipos[2] + ipos[3];
        for (int i=0; i<shape[0]; i++) {
          if (trcs[j][i] != val) {
            throw new SeisException("Mismatch at pos = " + Arrays.toString(ipos) + " expected " + val + " got " + trcs[j][i]);
          }
        }
      }
      
    }
  }

  public static void mainArgs(String[] args) {
    int tileSize1 = Integer.parseInt(args[3]);
    int tileSize2 = Integer.parseInt(args[4]);
    int ivol = Integer.parseInt(args[5]);
    TaskTranspose ttp = null;
    try {
      ttp = new TaskTranspose(args[0], args[1], args[2], tileSize1, tileSize2, true);
      int ntask = ttp.getStage1Tasks();
      Integer[] tasks = new Integer[ntask];
      for (int i=0; i<ntask; i++) {
        tasks[i] = i;
      }
      Collections.shuffle(Arrays.asList(tasks));
      System.out.println("\nBegin Stage 1\n");
      for (int itask : tasks) {
        System.out.println("Task " + itask);
        TaskTransposeStage1 tts1 = ttp.getStage1(itask, ivol);
        tts1.init();
        tts1.readFrames();
        System.out.println("Stage1 readFrames complete");
        tts1.writeTiles();
        System.out.println("Stage1 writeTiles complete");
        tts1.tt.report();
        tts1.rt.report();
      }
      System.out.println("Stage 1 Complete ivol " + ivol);
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("\nBegin Stage 2\n");
    try {
      int ntask = ttp.getStage2Tasks();
      Integer[] tasks = new Integer[ntask];
      for (int i=0; i<ntask; i++) {
        tasks[i] = i;
      }
      Collections.shuffle(Arrays.asList(tasks));
      for (int itask : tasks) {
        System.out.println("Task " + itask);
        TaskTransposeStage2 tts2 = ttp.getStage2(itask, ivol);
        tts2.init();
        tts2.readTiles();
        System.out.println("Stage2 readTiles complete");
        tts2.writeTranspose();
        System.out.println("Stage2 writeTranspose complete");
      }
      System.out.println("Stage 2 Complete ivol " + ivol);
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
