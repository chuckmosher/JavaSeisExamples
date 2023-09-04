package org.javaseis.examples.cloud.tasktranspose;

import java.util.Arrays;

import org.javaseis.array.ElementType;
import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.time.RateTracker;
import org.javaseis.time.TimeTracker;
import org.javaseis.util.JsonUtil;
import org.javaseis.util.SeisException;

import beta.javaseis.array.IMultiArray;
import beta.javaseis.array.MultiArray;
import beta.javaseis.array.MultiArrayTraceIterator;
import beta.javaseis.array.TransposeType;
import beta.javaseis.distributed.Decomposition;
import beta.javaseis.util.Convert;
import edu.mines.jtk.util.ArrayMath;

/**
 * Serial implementation of JavaSeis Async Task Transpose Stage 1
 * <p>
 * Create tiles and write to disk
 * <p>
 * For an input 3D array on disk with dimensions n0 x n1o x n2o, this class
 * reads the input data and creates a "tiled" version of the dataset. 
 * A block decomposition is used to create tiles along n1o and n2o.
 * The padded length of the input is [n2][n1][n0]. Tiles
 * are size [p2][p1], and the tile counts are n2p = n2/p2 and n1p = n1/p1.
 * The number of tasks needed to conduct transpose Stage 1 is n2p.
 * Each task reads p2 frames from the input, starting at p2*taskIndex.
 * Data is reshaped and transposed to create the tiles, in the order:
 * <ul>
 * <li> [n2o][n1o][n0] : shape of input on disk </li>
 * <li> [n2][n1][n0] : Shape padded to a multiple of tile size </li>
 * <li> [n2p][p2][n1][n0] : Expand axis 2 </li>
 * <li> [p2][n1][n0] : Slab of p2 frames read to memory on a single task<li>
 * <li> [p2][n1p][p1][n0] : Expand axis 1 with memory reshape </li> 
 * <li> [n1p][p2][p1][n0] : Transpose [p2][n1p] to [n1p][p2] </li> 
 * <li> [n1p][p2*p1][n0] : Reshape [p2][p1] to [p2*p1] </li>
 * <li> [n2p][n1p][p2*p1][n0] : shape of output tiled dataset </li>
 * <ul>
 * The output is written as a 4D dataset to disk
 * <p>
 * @author chuck@momamco.org
 *
 */
public class TaskTransposeStage1 {
  /** Shape parameters */
  int n0, n1, n2, p1, p2, p2p1, n1p, n2p;
  /** input, padded, and tiled shapes */
  int[] shape, pshape, tshape;
  /** Path for input dataset */
  String jsInPath;
  /** Path for tiled dataset */
  String jsTilePath;
  /** Volume index in the input dataset */
  int ivol;
  /** Number of tasks and current task */
  int ntask, itask;
  /** Interval time and rate tracking */
  public boolean trackTimes = false;
  /** Size of objects moved from disk to memory */
  double frameSize, tileSize, tranSize;
  
  /** Static fields for trackers */
  static double w2mb = 4.0/(1024.0*1024.0); // Converts Words to MegaBytes (MiB)
  static String[] timerNames = {
      "init",
      "compute",
      "memory"
  };
  static String[] rateNames = {
      "input",
      "output",
      "transpose"
  };

  // Constructed objects declared as transient to suppress serialization */
  
  /** MultiArray for reshapes and transposeds */
  transient IMultiArray dma;
  /** Rate trackers and names */
  public transient TimeTracker tt;
  public transient RateTracker rt;
  
  public static TaskTransposeStage1 fromJson( String jsonString ) {
    return (TaskTransposeStage1)(JsonUtil.fromJsonString(TaskTransposeStage1.class,jsonString));
  }
  
  public String toJsonString() {
    return JsonUtil.toJsonString(this);
  }
  
  public TaskTransposeStage1( TaskTranspose taskTranspose ) {
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
    this.jsInPath = new String(taskTranspose.jsInPath);
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
   * Open the input dataset and set tile sizes
   * @param path - full path to the input JavaSeis dataset
   * @param tileSize1 - Tile size for axis 1 of the 3D array
   * @param tileSize2 - tile size for axis 2 of the 3D array
   * @param track - set to true to track timing info
   * @throws SeisException on I/O errors
   */
  public TaskTransposeStage1( String inPath, String tilePath, int tileSize1, int tileSize2, int volumeIndex, boolean track ) throws SeisException {
    trackTimes = track;
    // Set up time and rate trackers
    if (trackTimes) {
      tt = new TimeTracker(timerNames);
      tt.start("init");
      rt = new RateTracker(rateNames);
    }   
    // Open the input for read only
    Seisio sio = new Seisio(inPath);
    sio.open("r");
    // Get the GridDefinition and check number of dimensions
    GridDefinition grid = sio.getGridDefinition();
    if (grid.getNumDimensions() != 4) throw new SeisException("Input must be a 4D array");
    jsInPath = new String(inPath);
    // Get the shape of the framework
    shape = Convert.longToInt(grid.getAxisLengths());
    sio.close();
    ivol = volumeIndex;
    if (ivol < 0 || ivol >= shape[3]) throw new SeisException("Volume Index " + volumeIndex + " is out of range");
    // Set tile sizes
    frameSize = w2mb*shape[0]*shape[1];
    p1 = tileSize1;
    p2 = tileSize2;
    p2p1 = p2*p1;
    tileSize = w2mb*p2p1*shape[0];
    // Get the padded shape
    pshape = TaskTranspose.getTiledShape(p1, p2, 3, shape);
    n0 = pshape[0];
    n1 = pshape[1];
    n2 = pshape[2];
    // Set the tile counts
    n1p = n1 / p1;
    n2p = n2 / p2;
    tranSize = w2mb*n0*p2p1;
    // Output shape with collapsed dimension on disk
    tshape = new int[] { n0, p2p1, n1p, n2p*shape[3] };
    // Check the output path for the tiled dataset
    if (Seisio.isJavaSeis(tilePath) == true) {
      sio = new Seisio(tilePath);
      sio.open("rw");
      int[] tmp = Convert.longToInt(sio.getGridDefinition().getAxisLengths());
      if (tmp.length != 4) 
        throw new SeisException("Tiled Dataset exists but shape does not match");
      for (int i=0; i<4; i++) {
        if (tmp[i] != tshape[i])
          throw new SeisException("Tiled Dataset exists but shape does not match");
      }
    } else {
      sio = new Seisio( tilePath, GridDefinition.getDefault(4, tshape) );
      sio.create();
      sio.close();
    }
    jsTilePath = new String(tilePath);
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
   * Set the task number for this instance
   * @param taskNumber
   */
  public void setTask( int taskNumber ) {
    if (taskNumber < 0 || taskNumber >= n2p) throw new IllegalArgumentException("taskNumber out of range");
    itask = taskNumber;
  }
  
  /**
   * Get the number of tasks needed to complete tiled output
   * @return
   */
  public int getTaskCount() {
    return n2p;
  }
  
  /**
   * Initialize MultiArrays used for reshape and transpose
   */
  public void init() {
    if (trackTimes) tt.start("init");
    // Allocate an array to hold p2 input frames in memory
    dma = MultiArray.factory(4, ElementType.FLOAT, 1, new int[] { n0, n1, p2, 1 });
    if (trackTimes) tt.stop("init");
  }
  
  /**
   * Read "p2" frames for this task from disk to memory
   * @throws SeisException on I/O errors
   */
  public void readFrames() throws SeisException {
    // Open the input dataset
    if (trackTimes) tt.start("init");
    Seisio sio = new Seisio(jsInPath);
    sio.open("r");
    if (trackTimes) tt.stop("init");
    int[] pos = new int[4];
    float[][] trcs = sio.getTraceDataArray();
    // Loop and copy the data from disk to the MultiArray
    for (int k=0; k<p2; k++) {
      // Set the volume we are reading
      pos[3] = ivol;
      // Start frame on disk for read is task number times p2
      pos[2] = p2 * itask + k;
      // Check for no more input on last task with padding
      if (pos[2] >= shape[2]) break;
      if (trackTimes) rt.start("input");
      sio.readFrame(pos);
      if (trackTimes) rt.stop("input", frameSize);
      // Position in MultiArray
      pos[3] = 0;
      pos[2] = k;
      if (trackTimes) tt.start("memory");
      dma.putFrame(trcs,pos);
      if (trackTimes) tt.stop("memory");
    }
    sio.close();
  }
  
  /**
   * Complete tile formation by transposing the outer 2 dimensions
   * and collapsing the inner 2 dimensions
   * @throws SeisException
   */
  public void writeTiles() throws SeisException {
    // Set the shape of the input to expand n1 to p1,n1p
    dma.setShape(new int[] { n0, p1, n1p, p2  });
    // Open the output dataset
    if (trackTimes) tt.start("init");
    Seisio sio = new Seisio(jsTilePath);
    sio.open("rw");
    if (trackTimes) tt.stop("init");
    // Position for the input data in memory
    int[] ipos = new int[4];
    // Position for the output data on disk
    int[] opos = new int[4];
    opos[3] = itask;
    float[][] trcs = sio.getTraceDataArray();
    // loop over n1p p1xp2 pillars in memory
    for (int k=0; k<n1p; k++) {
      if (trackTimes) rt.start("transpose");
      // Position along n1p axis
      ipos[2] = k;
      int jo = 0;
      for (int j=0; j<p2; j++) {
        // position along p2 axis
        ipos[3] = j;
        for (int i=0; i<p1; i++,jo++) {
          // Position along p1 axis
          ipos[1] = i;
          // Copy the trace from the input pillar to output frame
          dma.getTrace(trcs[jo], ipos);
        }
      }
      if (trackTimes) rt.stop("transpose",tranSize);
      if (trackTimes) rt.start("output");
      opos[2] = k;
      sio.writeFrame(opos,p2p1);
      if (trackTimes) rt.stop("output",tileSize);
    }
    sio.close();
  }
  
  /**
   * Test harness
   * @param args - args[0] = input path, args[1] = output path, args[2] = tileSize1, args[3] = tileSize2
   */
  public static void main(String[] args) {
    String path = args[0];
    String tpath = args[1];
    int tileSize1 = Integer.parseInt(args[2]);
    int tileSize2 = Integer.parseInt(args[3]);
    TaskTransposeStage1 ct = null;
    try {
      ct = new TaskTransposeStage1(path,tpath,tileSize1,tileSize2,0,true);
    } catch (SeisException e) {
      e.printStackTrace();
      System.exit(1);
    }
    int ntask = ct.getTaskCount();
    for (int itask = 0; itask <ntask; itask++) {
      ct.setTask(itask);
      ct.init();
      try {
        ct.readFrames();
        ct.writeTiles();
      } catch (SeisException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    System.out.println("Tiled output complete");
    System.out.println(ct.tt.report());
    System.out.println(ct.rt.report());
  }
  
  public static void mainTest( String[] args ) {
    int n0 = 2;
    int n1 = 4;
    int n2 = 8;
    int p1 = 2;
    int p2 = 4; 
    int p2p1 = p1*p2;
    int n1p = n1/p1;
    int n2p = n2/p2;
    float[][][] a = new float[n2][n1][n0];
    for (int k=0; k<n2; k++) {
      for (int j=0; j<n1; j++) {
        Arrays.fill(a[k][j], 100*k+j);
      }
    }
    IMultiArray ma = MultiArray.wrap(3,new int[] { n0, n1, n2 }, a);
    IMultiArray mb = MultiArray.float3D(n0, n1, p2);
    
    int[] pos = new int[4];
    float[] trc = new float[n0];
    // Loop over tasks
    for (int itask = 0; itask < n2p; itask++) {
      // Load a p2 slab into multiarray b
      for (int k=0; k<p2; k++) {
        int ka = p2*itask + k;
        for (int j=0; j<n1; j++) {
          pos[2] = ka;
          pos[1] = j;
          ma.getTrace(trc, pos);
          pos[2] = k;
          mb.putTrace(trc,pos);
        }
      }
      // Validate array mb
      MultiArrayTraceIterator<float[]> mati = new MultiArrayTraceIterator<float[]>(mb,trc);
      int[] ipos;
      while (mati.hasNext()) {
        trc = mati.next();
        ipos = mati.getPosition();
        int k = p2*itask + ipos[2];
        int j = ipos[1];
        float val = 100*k+j;
        for (int i=0; i<n0; i++) {
          if (trc[i] != val) {
            System.err.println("Mismatch at j,k " + j + ", " + k);
            System.exit(1);
          }
        }
      }
      // Expand axis 1
      IMultiArray ma1 = mb.view(4, new int[] { n0, p1, n1p, p2});
      // Validate
      mati = new MultiArrayTraceIterator<float[]>(ma1,trc);
      while (mati.hasNext()) {
        trc = mati.next();
        ipos = mati.getPosition();
        int kp2 = ipos[3];
        int k = p2*itask+kp2;
        int jn1 = ipos[2];
        int jp1 = ipos[1];
        int j = p1*jn1 + jp1;
        float val = 100*k + j;
        for (int i=0; i<n0; i++) {
          if (trc[i] != val) {
            System.err.println("Mismatch at j,k " + j + ", " + k);
            System.exit(1);
          }
        }
      }
      // Transpose p1 and n1p
      ma1.transpose(TransposeType.T1243);
      // Validate
      mati = new MultiArrayTraceIterator<float[]>(ma1,trc);
      while (mati.hasNext()) {
        trc = mati.next();
        ipos = mati.getPosition();
        int kp2 = ipos[2];
        int k = p2*itask+kp2;
        int jn1 = ipos[3];
        int jp1 = ipos[1];
        int j = p1*jn1 + jp1;
        float val = 100*k + j;
        for (int i=0; i<n0; i++) {
          if (trc[i] != val) {
            System.err.println("Mismatch at j,k " + j + ", " + k);
            System.exit(1);
          }
        }
      }
      // Collapse p1,p2 to p2p1
      IMultiArray ma2 = mb.view(4,new int[] { n0, p2p1, n1p, 1 });
      // Validate
      mati = new MultiArrayTraceIterator<float[]>(ma2,trc);
      while (mati.hasNext()) {
        trc = mati.next();
        ipos = mati.getPosition();
      }
    }   
  }
}
