package org.javaseis.examples.tool;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.javaseis.services.ParameterService;
import org.javaseis.tool.DataState;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.tool.VolumeToolRunner;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.array.ITraceIterator;
import beta.javaseis.distributed.FileSystemIOService;
import beta.javaseis.distributed.IDistributedIOService;
import beta.javaseis.parallel.ICollective.Operation;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ReduceScalar;
import beta.javaseis.parallel.UniprocessorContext;
import beta.javaseis.services.VolumePropertyService;
import edu.mines.jtk.util.ArrayMath;

/**
 * Example Volume Input Tool
 * <p>
 * Parameters:
 * <P>
 * inputFileSystem - input file system where datatsets are stored
 * <p>
 * inputFileName - file name to be opened and read
 *
 */
public class ExampleVolumeInputTool implements IVolumeTool {
  private static final long serialVersionUID = 1L;
  IDistributedIOService ipio;
  VolumePropertyService vps;
  boolean usesProperties;
  String inputFileSystem, inputFileName;

  @Override
  public void serialInit(ToolState toolState) throws SeisException {
    inputFileSystem = toolState.getParameter(ToolState.INPUT_FILE_SYSTEM);
    toolState.log("Input file system: " + inputFileSystem);
    inputFileName = toolState.getParameter(ToolState.INPUT_FILE_NAME);
    toolState.log("Input file name: " + inputFileName);
    IParallelContext upc = new UniprocessorContext();
    ipio = new FileSystemIOService(upc, inputFileSystem);
    ipio.open(inputFileName);
    toolState.log("Opened file in serial mode");
    toolState.setOutputState(new DataState(ipio, toolState.getIntParameter(ToolState.TASK_COUNT, 1)));
    ipio.close();
  }

  @Override
  public void parallelInit(IParallelContext pc, ToolState toolState) throws SeisException {
    ipio = new FileSystemIOService(pc, inputFileSystem);
    ipio.open(inputFileName);
    ISeismicVolume outputVolume = (ISeismicVolume) toolState.getObject(ToolState.OUTPUT_VOLUME);
    ipio.setSeismicVolume(outputVolume);
    ipio.reset();
    if (ipio.usesProperties()) {
      vps = (VolumePropertyService) (ipio.getPropertyService());
      pc.masterPrint("\n" + vps.listProperties() + "\n");
    }
    pc.serialPrint("Re-opened file in parallel mode");
  }

  @Override
  public boolean processVolume(IParallelContext pc, ToolState toolContext, ISeismicVolume input,
      ISeismicVolume output) {
    output.copyVolume(input);
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ToolState toolContext, ISeismicVolume output)
      throws SeisException {
    if (ipio.hasNext() == false) {
      toolContext.log("Read complete");
      ipio.close();
      return false;
    }
    pc.serialPrint("Read next volume ...");
    ipio.next();
    ipio.read();
    pc.serialPrint("Read complete for position " + Arrays.toString(ipio.getFilePosition()));
    if (ipio.usesProperties()) {
      double sx = vps.getValue("SOU_XD");
      double sy = vps.getValue("SOU_YD");
      double rx = vps.getValue("REC_XD");
      double ry = vps.getValue("REC_YD");
      pc.serialPrint("Source X " + sx + " Source Y " + sy + " Receiver X " + rx + " Receiver Y " + ry);
    }
    float[] trc = new float[output.getAxisLength(0)];
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    while (output.hasNext()) {
      output.next();
      output.getTrace(trc);
      min = Math.min(min, ArrayMath.min(trc));
      max = Math.max(max, ArrayMath.max(trc));
    }
    pc.serialPrint("  Local Min,Max values in volume: " + min + ", " + max);
    min = ReduceScalar.reduceDouble(pc, min, Operation.MIN);
    max = ReduceScalar.reduceDouble(pc, max, Operation.MAX);
    pc.masterPrint("  Global Min,Max values: " + min + ", " + max);
    return true;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ToolState toolContext) throws SeisException {
    //
  }

  @Override
  public void serialFinish(ToolState toolContext) {
    // Nothing to clean up in serial mode
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    // default serialization
    oos.writeObject(inputFileSystem);
    oos.writeObject(inputFileName);
  }

  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    // default de-serialization
    inputFileSystem = (String) ois.readObject();
    inputFileName = (String) ois.readObject();
  }

  public static void main(String[] args) {
    String[] toolList = new String[1];
    toolList[0] = ExampleVolumeInputTool.class.getCanonicalName();
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter(ToolState.INPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_SYSTEM, "/Data/Projects/SEG-ACTI");
      //parms.setParameter(ToolState.INPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.INPUT_FILE_NAME) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_NAME, "SegActiShotNo1.js");
      //parms.setParameter(ToolState.INPUT_FILE_NAME, "temp.js");
    }
    parms.setParameter(ToolState.TASK_COUNT, "4");
    try {
      VolumeToolRunner.exec(parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }
}
