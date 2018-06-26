package org.javaseis.examples.tool;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.javaseis.processing.framework.DataState;
import org.javaseis.processing.framework.FileSystemIOService;
import org.javaseis.processing.framework.IDistributedIOService;
import org.javaseis.processing.framework.IVolumeModule;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.processing.module.io.VolumeInput;
import org.javaseis.properties.IPropertyContainer;
import org.javaseis.properties.IPropertyManager;
import org.javaseis.properties.VolumePropertyHandler;
import org.javaseis.services.ParameterService;
import org.javaseis.util.IntervalTimer;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.parallel.ICollective.Operation;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ReduceScalar;
import beta.javaseis.parallel.UniprocessorContext;
import beta.javaseis.util.StringArrays;

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
public class ExampleVolumeInput implements IVolumeModule {
  private static final long serialVersionUID = 1L;
  IDistributedIOService ipio;
  String inputFileSystem, inputFileName;
  int[] fileShape, range;
  IntervalTimer itimer;
  double readTime;
  int ivolume;
  IPropertyContainer tpc;
  IPropertyContainer vpc;

  @Override
  public void serialInit(ModuleState moduleState) throws SeisException {
    inputFileSystem = moduleState.getParameter(ModuleState.INPUT_FILE_SYSTEM);
    moduleState.print("Input file system: " + inputFileSystem);
    inputFileName = moduleState.getParameter(ModuleState.INPUT_FILE_NAME);
    moduleState.print("Input file name: " + inputFileName);
    IParallelContext upc = new UniprocessorContext();
    ipio = new FileSystemIOService(upc, inputFileSystem);
    boolean exists = ipio.exists(inputFileName);
    if (!exists) {
      throw new SeisException("File " + inputFileName + " does not exist on filesystem "
          + inputFileSystem);
    }
    ipio.info(inputFileName);
    moduleState.log("Opened file in serial mode");
    String buf = moduleState.getStringParameter("volumeRange", "*");
    moduleState.print("Requested volumeRange: " + buf);
    fileShape = ipio.getFileShape();
    if (buf.compareTo("*") == 0) {
      range = new int[] { 0, fileShape[3] - 1, 1 };
    } else {
      range = StringArrays.stringToIntArray(buf);
      if (range.length != 3 || range[0] > range[1] || range[2] < 1)
        throw new SeisException("Illegal volumeRange");
    }
    moduleState.print("Parsed volumeRange: " + Arrays.toString(range));
    DataState outputState = new DataState(ipio, moduleState.getIntParameter(ModuleState.TASK_COUNT, 1));
    IPropertyManager pm = ipio.getPropertyManager();
    if (pm.usesTraceProperties()) {
      moduleState.print(pm.getTracePropertyHandler().listProperties("Trace Properties in this file:"));
    }
    if (pm.usesVolumeProperties()) {
      moduleState.print(pm.getVolumePropertyHandler().listProperties("Volume Properties in this file:"));
    } else {
      VolumePropertyHandler vph = new VolumePropertyHandler(ipio.getFileShape() );
      pm.setVolumePropertyHandler(vph);
      moduleState.print("Created MemoryOnly VolumePropertyHandler for this flow");
    }
    outputState.setPropertyManager(pm);
    moduleState.setOutputState(outputState);
  }

  @Override
  public void parallelInit(IParallelContext pc, ModuleState moduleState) throws SeisException {
    ipio = new FileSystemIOService(pc, inputFileSystem);
    ipio.open(inputFileName);
    ipio.reset();
    pc.masterPrint("Re-opened file in parallel mode");
    itimer = new IntervalTimer();
    readTime = 0;
    ivolume = range[0];
    IPropertyManager pm = ipio.getPropertyManager();
    DataState outputState = moduleState.getOutputState();
    outputState.setPropertyManager(pm);
    if (pm.usesVolumeProperties()) {
      pm.getVolumePropertyHandler().initialize();
    } else {
      VolumePropertyHandler vph = new VolumePropertyHandler(ipio.getFileShape() );
      pm.setVolumePropertyHandler(vph);
    }
    ISeismicVolume output = outputState.getSeismicVolume();
    output.setVolumePropertyContainer(moduleState.getVolumePropertyContainer());
    if (pm.usesTraceProperties()) output.setTracePropertyContainer(moduleState.getTracePropertyContainer());
  }

  @Override
  public boolean processVolume(IParallelContext pc, ModuleState toolContext, ISeismicVolume input,
      ISeismicVolume output) throws SeisException {
    output.copy(input);
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ModuleState moduleState, ISeismicVolume output)
      throws SeisException {
    if (ivolume > range[1]) {
      return false;
    }
    int[] filePosition = new int[] { 0, 0, 0, ivolume };
    double t0 = itimer.elapsedTime();
    output.reset();
    output.setFilePosition(filePosition);
    pc.masterPrint("Read next volume at position: " + Arrays.toString(output.getFilePosition()));
    ipio.read(output);
    t0 = itimer.elapsedTime() - t0;
    pc.masterPrint("Read completed in " + t0 + " sec ");
    readTime += t0;
    ivolume += range[2];
    return true;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ModuleState toolContext) throws SeisException {
    ipio.close();
    pc.masterPrint("VolumeInput Completed, read time " + readTime + " sec ");
    double max = ReduceScalar.reduceDouble(pc, readTime, Operation.MAX);
    double total = ReduceScalar.reduceDouble(pc, readTime, Operation.SUM);
    pc.masterPrint("  Sum of read times: " + total + " sec\n  Synchronous read time: " + max + " sec");
  }

  @Override
  public void serialFinish(ModuleState toolContext) {
    // Nothing to clean up in serial mode
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    // default serialization
    oos.writeObject(inputFileSystem);
    oos.writeObject(inputFileName);
    oos.writeObject(fileShape);
    oos.writeObject(range);
  }

  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    // default de-serialization
    inputFileSystem = (String) ois.readObject();
    inputFileName = (String) ois.readObject();
    fileShape = (int[]) ois.readObject();
    range = (int[]) ois.readObject();
  }

  public static void main(String[] args) {
    String[] toolList = new String[1];
    toolList[0] = VolumeInput.class.getCanonicalName();
    //toolList[1] = VolumeDisplay.class.getCanonicalName();
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