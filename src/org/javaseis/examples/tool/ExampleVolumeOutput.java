package org.javaseis.examples.tool;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.javaseis.grid.BinGrid;
import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.processing.framework.DataState;
import org.javaseis.processing.framework.FileSystemIOService;
import org.javaseis.processing.framework.IVolumeModule;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.processing.module.io.VolumeInput;
import org.javaseis.processing.module.io.VolumeOutput;
import org.javaseis.processing.module.io.VolumeOutput.OutputMode;
import org.javaseis.properties.DataDefinition;
import org.javaseis.properties.DataFormat;
import org.javaseis.properties.DataType;
import org.javaseis.services.ParameterService;
import org.javaseis.util.IntervalTimer;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ReduceScalar;
import beta.javaseis.parallel.UniprocessorContext;
import beta.javaseis.parallel.ICollective.Operation;

/**
 * Example tool that multiplies an input volume by a scalar Parameters:
 * org.javaseis.examples.tool.ExampleVolumeTool.scalarValue - scalar that will
 * be multiplied with input data to produce output data
 */
public class ExampleVolumeOutput implements IVolumeModule {
  private static final long serialVersionUID = 1L;
  org.javaseis.processing.framework.IDistributedIOService opio;
  String outputFileSystem;
  String outputFileName;
  IntervalTimer itimer;
  double writeTime;

  /** Mode for output data set */
  public enum OutputMode {
    /** File exists, overwrite any existing data */
    WRITE, /** File exists, only add data that does not already exist */
    UPDATE, /** Create a new file, fail if file already exists */
    CREATE, /** Delete existing file or create a new one */
    DELETE
  }

  OutputMode mode;

  @Override
  public void serialInit(ModuleState moduleState) throws SeisException {
    moduleState.getParameter(ModuleState.OUTPUT_FILE_SYSTEM);
    outputFileSystem = moduleState.getParameter(ModuleState.OUTPUT_FILE_SYSTEM);
    moduleState.log("Output file system: " + outputFileSystem);
    outputFileName = moduleState.getParameter(ModuleState.OUTPUT_FILE_NAME);
    moduleState.log("Output file name: " + outputFileName);
    String modeString = moduleState.getStringParameter("mode", "WRITE");
    moduleState.print("Output file mode: " + modeString);
    try {
      mode = OutputMode.valueOf(modeString);
    } catch (Exception e) {
      throw new SeisException("Unrecognized mode string: " + modeString + "\n  Use one of: "
          + Arrays.toString(OutputMode.values()), e.getCause());
    }
    IParallelContext upc = new UniprocessorContext();
    opio = new FileSystemIOService(upc, outputFileSystem);
    DataState inputState = moduleState.getInputState();
    boolean exists = opio.exists(outputFileName);
    if (mode == OutputMode.DELETE) {
      if (exists) {
        opio.delete(outputFileName);
        moduleState.print("Output mode DELETE: existing file was deleted");
      }
      exists = false;
    }
    if (mode == OutputMode.CREATE || mode == OutputMode.DELETE
        || (mode == OutputMode.WRITE && exists == false)) {
      if (exists) throw new SeisException("Output mode is CREATE but file already exists");
      String dataFormatString = moduleState.getStringParameter("dataFormat",
          DataFormat.COMPRESSED_INT16.getName());
      DataFormat dataFormat = DataFormat.parse(dataFormatString);
      opio.setDataDefinition(new DataDefinition(DataType.CUSTOM, dataFormat, ByteOrder.nativeOrder()));
      if (inputState.usesTraceProperties()) {
        opio.create(outputFileName, inputState.getGridDefinition(), inputState.getBinGrid(),
            inputState.getPropertyManager());
      } else {
        opio.create(outputFileName, inputState.getGridDefinition(), inputState.getBinGrid(), null, null,
            inputState.getPropertyManager().getVolumePropertyHandler());
      }
      moduleState.print("Output mode " + mode + ": New file was created");
      opio.info(outputFileName);
    }
    if (mode == OutputMode.UPDATE || (mode == OutputMode.WRITE && exists == true)) {
      if (!exists) throw new SeisException("File does not exist for Output Mode " + mode);
      opio.info(outputFileName);
      if (opio.getGridDefinition().matches(inputState.getGridDefinition()) == false)
        throw new SeisException("File exists but GridDefinition does not match output from flow");
      moduleState.print("Output mode " + mode + ": Existing file was opened for write");
    }
    opio.close();
  }

  @Override
  public void parallelInit(IParallelContext pc, ModuleState moduleState) throws SeisException {
    opio = new FileSystemIOService(pc, outputFileSystem);
    opio.open(outputFileName);
    ISeismicVolume inputVolume = moduleState.getOutputState().getSeismicVolume();
    ISeismicVolume outputVolume = moduleState.getOutputState().getSeismicVolume();
    //outputVolume.setTracePropertyContainer(inputVolume.getTracePropertyContainer());
    itimer = new IntervalTimer();
    writeTime = 0;
  }

  @Override
  public boolean processVolume(IParallelContext pc, ModuleState moduleState, ISeismicVolume input,
      ISeismicVolume output) throws SeisException {
    pc.masterPrint("Begin write for position:  " + Arrays.toString(input.getFilePosition()));
    double t0 = itimer.elapsedTime();
    opio.write(input);
    t0 = itimer.elapsedTime() - t0;
    pc.masterPrint("Write completed in " + t0 + " sec ");
    writeTime += t0;
    output.copy(input);
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ModuleState toolContext, ISeismicVolume output)
      throws SeisException {
    return false;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ModuleState toolContext) throws SeisException {
    pc.masterPrint("VolumeOutput Completed, write time " + writeTime + " sec ");
    double max = ReduceScalar.reduceDouble(pc, writeTime, Operation.MAX);
    double total = ReduceScalar.reduceDouble(pc, writeTime, Operation.SUM);
    pc.masterPrint("  Sum of write times: " + total + " sec\n  Synchronous write time: " + max + " sec");
    opio.close();
  }

  @Override
  public void serialFinish(ModuleState toolContext) {
    // Nothing to clean up in serial mode
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    // default serialization
    oos.writeObject(outputFileSystem);
    oos.writeObject(outputFileName);
  }

  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    // default de-serialization
    outputFileSystem = (String) ois.readObject();
    outputFileName = (String) ois.readObject();
  }

  public static void main(String[] args) {
    String[] toolList = new String[2];
    toolList[0] = VolumeInput.class.getCanonicalName();
    toolList[1] = VolumeOutput.class.getCanonicalName();
    ParameterService globalParms = new ParameterService(args);
    ParameterService[] parms = ParameterService.allocate(2);
    String fileSystem = System.getProperty("user.home") + File.separator + "Projects/SEG-ACTI";
    parms[0].setParameter(ModuleState.INPUT_FILE_SYSTEM, fileSystem);
    parms[0].setParameter(ModuleState.INPUT_FILE_NAME, "ShotMigStk.js");
    parms[1].setParameter(ModuleState.OUTPUT_FILE_SYSTEM, fileSystem);
    parms[1].setParameter(ModuleState.OUTPUT_FILE_NAME, "ShotMigStkFloat.js");
    parms[1].setParameter("dataFormat", "FLOAT");
    parms[1].setParameter("mode", "DELETE");
    globalParms.setParameter(ModuleState.TASK_COUNT, "1");
    try {
      VolumeModuleRunner.exec(globalParms, parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }
}