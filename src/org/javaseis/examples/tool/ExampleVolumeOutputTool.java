package org.javaseis.examples.tool;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.javaseis.grid.BinGrid;
import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.services.ParameterService;
import org.javaseis.tool.DataState;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.ToolState;
import org.javaseis.tool.VolumeToolRunner;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.distributed.FileSystemIOService;
import beta.javaseis.distributed.IDistributedIOService;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.UniprocessorContext;

/**
 * Example tool that multiplies an input volume by a scalar Parameters:
 * org.javaseis.examples.tool.ExampleVolumeTool.scalarValue - scalar that will
 * be multiplied with input data to produce output data
 */
public class ExampleVolumeOutputTool implements IVolumeTool {
  private static final long serialVersionUID = 1L;
  IDistributedIOService opio;
  String outputFileSystem;
  String outputFileName;

  @Override
  public void serialInit(ToolState toolState) throws SeisException {
    toolState.getParameter(ToolState.OUTPUT_FILE_SYSTEM);
    outputFileSystem = toolState.getParameter(ToolState.OUTPUT_FILE_SYSTEM);
    toolState.log("Output file system: " + outputFileSystem);
    outputFileName = toolState.getParameter(ToolState.OUTPUT_FILE_NAME);
    toolState.log("Output file name: " + outputFileName);
    IParallelContext upc = new UniprocessorContext();
    opio = new FileSystemIOService(upc, outputFileSystem);
    if (opio.exists(outputFileName)) throw new SeisException("Cannot create file - file already exists");
    GridDefinition grid = toolState.getInputState().gridDefinition;
    BinGrid binGrid = toolState.getInputState().binGrid;
    opio.create(outputFileName, grid, binGrid);
    toolState.log("Created file in serial mode");
    toolState.setOutputState(new DataState(opio, toolState.getIntParameter(ToolState.TASK_COUNT, 1)));
    opio.close();
  }

  @Override
  public void parallelInit(IParallelContext pc, ToolState toolState) throws SeisException {
    opio = new FileSystemIOService(pc, outputFileSystem);
    opio.open(outputFileName);
    ISeismicVolume inputVolume = (ISeismicVolume) toolState.getObject(ToolState.INPUT_VOLUME);
    opio.setSeismicVolume(inputVolume);
    opio.reset();
    pc.serialPrint("New file opened in parallel mode");
  }

  @Override
  public boolean processVolume(IParallelContext pc, ToolState toolState, ISeismicVolume input,
      ISeismicVolume output) throws SeisException {
    if (!opio.hasNext())
      throw new SeisException("Output data mismatch - requested position exceeds output file size");
    opio.next();
    opio.write();
    return true;
  }

  @Override
  public boolean outputVolume(IParallelContext pc, ToolState toolContext, ISeismicVolume output)
      throws SeisException {
    return false;
  }

  @Override
  public void parallelFinish(IParallelContext pc, ToolState toolContext) throws SeisException {
    opio.close();
  }

  @Override
  public void serialFinish(ToolState toolContext) {
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
    String[] toolList = new String[3];
    toolList[0] = ExampleVolumeInputTool.class.getCanonicalName();
    toolList[1] = ExampleVolumeModifierTool.class.getCanonicalName();
    toolList[2] = ExampleVolumeOutputTool.class.getCanonicalName();
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter(ToolState.INPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_SYSTEM, "/Data/Projects/SEG-ACTI");
      //parms.setParameter(ToolState.INPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.INPUT_FILE_NAME) == "null") {
      parms.setParameter(ToolState.INPUT_FILE_NAME, "SegActiShotNo1.js");
      //parms.setParameter(ToolState.INPUT_FILE_PATH, "temp.js");
    }
    parms.setParameter("scalarValue", "2.0");
    parms.setParameter(ToolState.TASK_COUNT, "1");

    if (parms.getParameter(ToolState.OUTPUT_FILE_SYSTEM) == "null") {
      parms.setParameter(ToolState.OUTPUT_FILE_SYSTEM, System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter(ToolState.OUTPUT_FILE_NAME) == "null") {
      String path = System.getProperty("java.io.tmpdir") + "/temp.js";
      if (Seisio.isJavaSeis(path)) {
        Seisio.delete(path);
      }
      parms.setParameter(ToolState.OUTPUT_FILE_NAME, "temp.js");
    }
    try {
      VolumeToolRunner.exec(parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }
}
