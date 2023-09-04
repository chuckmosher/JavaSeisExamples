package org.javaseis.examples.cloud.tasktranspose;

import org.javaseis.util.SeisException;
import org.javaseis.grid.GridDefinition;
import org.javaseis.grid.GridUtil;
import org.javaseis.io.Seisio;
import org.javaseis.time.RateTracker;
import org.javaseis.time.TimeTracker;

import beta.javaseis.array.IMultiArray;
import beta.javaseis.array.MultiArray;
import beta.javaseis.array.TransposeType;
import beta.javaseis.distributed.Decomposition;
import beta.javaseis.util.Convert;
import edu.mines.jtk.util.ArrayMath;

/**
 * Serial implementation of JavaSeis Async Task Transpose Stage 2
 * Class WriteTiles creates a tiled version of a 3D dataset on disk.
 * This class reads the tiled data and writes a completed transpose to disk
 * Tiles are size [p2][p1], and the tiles counts are n2p = n2/p2 and n1p = n1/p1.
 * Data is reshaped and transposed to create the tiles, in the order:
 * <ul>
 * <li> [n2p][n1p][p2*p1][n0] : tiled input </li> 
 * <li> [n1p][n2p][p2*p1][n0] : Transpose n2p and n1p on input </li> 
 * <li> [n1p][n2][p1][n0] : Collapse [n2p][p2*p1] to [n2][p1] with reshape </li> 
 * <li> [n1p][p1][n2][n0] : Transpose [n2][p1] to [p1][n2] </li>
 * <li> [n1][n2][n0] : Collapse [n1p][n1] to [n1] with disk reshape </li>
 * <ul>
 * Each tasks handles a single index along outer n1 dimension.
 * The output transpose is written as a 3D dataset to disk
 * @author chuck@momamco.org
 *
 */
public class TaskTransposeStage2 {
  /** Shape parameters */
  int n0, n1, n2,  p1, p2, p2p1, n1p, n2p;
  /** Input, padded, reshaped, transposed, tiled, and output shapes */
  int[] shape, pshape, tshape, oshape;
  /** Multi-arrays used for reshaping */
  IMultiArray dma, rma, oma;
  /** Path for output dataset */
  String jsOutPath;
  /** Path for tiled dataset */
  String jsTilePath;
  /** Volume index for output */
  int ivol;
  /** Number of tasks and current task */
  int ntask, itask;
  /** Interval time and rate tracking */
  boolean trackTimes;
  TimeTracker tt;
  RateTracker rt;
  double frameSize, tileSize, tranSize;
  double w2mb = 4.0/(1024.0*1024.0);
  String[] timerNames = {
      "init",
      "compute",
      "memory"
  };
  String[] rateNames = {
      "input",
      "output",
      "transpose"
  };
  
  /**
   * Open the input dataset and set tile sizes
   * @param path - full path to the input JavaSeis dataset
   * @param tileSize1 - Tile size for axis 1 of the 3D array
   * @param tileSize2 - tile size for axis 2 of the 3D array
   * @param track - track timing information
   * @throws SeisException on I/O errors
   */
  public TaskTransposeStage2( String inPath, String tilePath, String outPath, int tileSize1, int tileSize2, int volumeIndex, boolean track ) throws SeisException {
    trackTimes = track;
    // Set up time and rate trackers
    if (trackTimes) {
      tt = new TimeTracker(timerNames);
      tt.start("init");
      rt = new RateTracker(rateNames);
    }
    
    // Get shape parameters from input dataset 
    Seisio sio = new Seisio(inPath);
    sio.open("r");
    // Get the GridDefinition and check number of dimensions
    GridDefinition grid = sio.getGridDefinition();
    if (grid.getNumDimensions() < 3) throw new SeisException("Input must be at least a 3D array");
    shape = Convert.longToInt(grid.getAxisLengths());
    sio.close();
    ivol = volumeIndex;
    if (ivol < 0 || ivol >= shape[3]) throw new SeisException("Volume Index " + volumeIndex + " is out of range");
    // Set tile sizes
    frameSize = w2mb*shape[0]*shape[1];
    p1 = tileSize1;
    p2 = tileSize2;
    p2p1 = p2*p1;
    // Get the padded shape
    pshape = getTiledShape(p1, p2, 3, shape);
    n0 = pshape[0];
    n1 = pshape[1];
    n2 = pshape[2];
    // Set the tile counts
    n1p = n1 / p1;
    n2p = n2 / p2;
    tileSize = w2mb*p1*p2*shape[0];
    // shape for input tiles
    tshape = new int[] { n0, p2p1, n1p, n2p*shape[3] };
    tranSize = w2mb*n0*p2;
    // Output shape
    oshape = new int[] { shape[0], shape[2], shape[1], shape[3] };
    // Check the tiled dataset for matching shape
    sio = new Seisio(tilePath);
    sio.open("rw");
    int[] tmp = Convert.longToInt(sio.getGridDefinition().getAxisLengths());
    if (tmp.length != 4) 
      throw new SeisException("Tiled Dataset exists but shape does not match");
    for (int i=0; i<4; i++) {
      if (tmp[i] != tshape[i])
        throw new SeisException("Tiled Dataset exists but shape does not match");
    }
    sio.close();
    jsTilePath = new String(tilePath);
    // Create output dataset
    if (Seisio.isJavaSeis(outPath) == false) {
      GridDefinition outGrid = GridUtil.transpose(grid,TransposeType.T1324);
      sio = new Seisio(outPath,outGrid);
      sio.create();
      sio.close();
    }
    jsOutPath = new String(outPath);  
    if (trackTimes) tt.stop("init"); 
  }
  
  public TaskTransposeStage2( TaskTranspose taskTranspose ) {
    this.n0 = taskTranspose.n0;
    this.n1 = taskTranspose.n1;
    this.n2 = taskTranspose.n2;
    this.p1 = taskTranspose.p1;
    this.p2 = taskTranspose.p2;
    this.p2p1 = taskTranspose.p2p1;
    this.n1p = taskTranspose.n1p;
    this.n2p = taskTranspose.n2p;
    this.shape = ArrayMath.copy(taskTranspose.shape);
    this.pshape = ArrayMath.copy(taskTranspose.pshape);
    this.tshape = ArrayMath.copy(taskTranspose.tshape);
    this.jsOutPath = new String(taskTranspose.jsOutPath);
    this.jsTilePath = new String(taskTranspose.jsTilePath);
    this.ntask = n2p;
    this.frameSize = taskTranspose.inFrameSize;
    this.tileSize = taskTranspose.tileSize;
    this.tranSize = taskTranspose.inTranSize;
    this.trackTimes = taskTranspose.trackTimes;
    if (trackTimes) {
      this.tt = new TimeTracker(timerNames);
      this.rt = new RateTracker(rateNames);
    }
  }
  
  /**
   * Set the task number - must be between 0 and n1p-1
   * @param taskNumber
   */
  public void setTask( int taskNumber ) {
    if (taskNumber < 0 || taskNumber >= n1p)
      throw new IllegalArgumentException("Invalid task number");
    itask = taskNumber;
  }
  /**
   * Set the input volume that will be transposed by this task
   * @param volume
   */
  public void setVolume(int volume) {
    if (volume < 0 || volume >= shape[3]) throw new IllegalArgumentException("Volume out of range");
    ivol = volume;
  }
  /**
   * Initialize MultiArrays used for reshape and transpose
   */
  public void init() {
    // Allocate an array to hold n2p tiles from disk
    if (trackTimes) tt.start("memory");
    dma = MultiArray.float3D(n0, p2p1, n2p);
    if (trackTimes) tt.stop("memory");
  }
  
  /**
   * Read tiles for this task from disk to memory with transpose of
   * outer two dimensions on input
   * @throws SeisException on I/O errors
   */
  public void readTiles() throws SeisException {
    // Open the input dataset
    if (trackTimes) tt.start("init");
    Seisio sio = new Seisio(jsTilePath);
    sio.open("r");
    if (trackTimes) tt.stop("init");
    int[] pos = new int[4];
    float[][] trcs = sio.getTraceDataArray();
    dma.setShape(new int[] { n0, p2p1, n2p } );
    // loop and read tiles transposing the outer two dimensions
    for (int k=0; k<n2p; k++) {
      pos[2] = itask;
      pos[3] = n2p*ivol + k;
      if (trackTimes) rt.start("input");
      sio.readFrame(pos);
      if (trackTimes) rt.stop("input",tileSize);
      // Copy frame to the MultiArray
      pos[2] = k;
      if (trackTimes) tt.start("memory");
      dma.putFrame(trcs, pos);
      if (trackTimes) tt.stop("memory");
    }
  }
  
  /**
   * Complete transpose transposing the outer 2 dimensions
   * and collapsing the inner 2 dimensions
   * @throws SeisException
   */
  public void writeTranspose() throws SeisException {
    // Reshape tiles to expand p1 and p2
    dma.setShape(new int[] { n0, p1, n2 } );
    // Transpose to bring n2 to axis 1
    if (trackTimes) rt.start("transpose");
    dma.transpose(TransposeType.T132);
    if (trackTimes) rt.stop("transpose", tranSize);
    // Open the output dataset
    if (trackTimes) tt.start("init");
    Seisio sio = new Seisio(jsOutPath);
    sio.open("rw");
    if (trackTimes) tt.stop("init");
    int[] pos = new int[4];
    float[][] trcs = sio.getTraceDataArray();
    // loop and copy traces to the output array
    pos[3] = ivol;
    for (int k=0; k<p1; k++) {
      pos[2] = k;
      if (trackTimes) tt.start("memory");
      // Loop and swap p1 and n2 index
      for (int j=0; j<shape[2]; j++) {
        pos[1] = j;
        dma.getTrace(trcs[j], pos);        
      }
      if (trackTimes) tt.stop("memory");
      // Write p1 transposed frames to disk
      pos[1] = 0;
      pos[2] = p1*itask + k;
      // Check to see if we're in the "padded zone"
      if (pos[2] < shape[2]) {
        if (trackTimes) rt.start("output");
        sio.writeFrame(pos,shape[2]);
        if (trackTimes) rt.stop("output",frameSize);
      }
    }
    sio.close();
  }
    
  /**
   * Get the number of tasks needed to complete tiled output
   * @return
   */
  public int getTaskCount() {
    return n1p;
  }
  
  public static int[] getTiledShape(int size1, int size2, int ndim,
      int[] lengths) {
    int[] padShape = new int[ndim];
    padShape[0] = lengths[0];
    padShape[1] = (int) Decomposition.paddedLength(lengths[1], size1);
    padShape[2] = (int) Decomposition.paddedLength(lengths[2], size2);
    for (int i = 3; i < ndim; i++) {      
      padShape[i] = lengths[i];
    }
    return padShape;
  }
  
  public static void main(String[] args) {
    String path = args[0];
    String tpath = args[1];
    String opath = args[2];
    int tileSize1 = Integer.parseInt(args[3]);
    int tileSize2 = Integer.parseInt(args[4]);
    TaskTransposeStage2 tt2 = null;
    try {
      tt2 = new TaskTransposeStage2(path,tpath, opath, tileSize1, tileSize2, 0, true);
    } catch (SeisException e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    int ntask = tt2.getTaskCount();
    for (int itask = 0; itask < ntask; itask++) {
      tt2.setTask(itask);
      tt2.init();
      try {
        tt2.readTiles();
        tt2.writeTranspose();
      } catch (SeisException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    
    System.out.println("Transpose complete");
    System.out.println(tt2.tt.report());
    System.out.println(tt2.rt.report());
  }
}
