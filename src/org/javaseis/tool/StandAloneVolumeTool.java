package org.javaseis.tool;

import java.util.concurrent.ExecutionException;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicVolume;
import org.javaseis.volume.SeismicVolume;

import beta.javaseis.distributed.FileSystemIOService;
import beta.javaseis.distributed.IDistributedIOService;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;
import beta.javaseis.parallel.UniprocessorContext;

/**
 * StandAlone volume processing tool handler
 *
 * @author chuck
 *
 */
public class StandAloneVolumeTool implements IVolumeTool {

  public StandAloneVolumeTool() {
    // Need default constructor so implementors don't have to provide one
  }

  public static ToolContext toolContext;

  public static IVolumeTool tool;

  public static IDistributedIOService ipio, opio;

  public static String inputFileSystem, inputFilePath, outputFileSystem,
      outputFilePath;

  public static boolean output = false;

  public static void exec(ParameterService parms, IVolumeTool standAloneTool) {
    // Tool to be run
    tool = standAloneTool;
    // Set a uniprocessor context
    IParallelContext upc = new UniprocessorContext();
    toolContext = new ToolContext(parms);
    ipio = null;
    // Open input file if it is requested
    inputFileSystem = parms.getParameter("inputFileSystem", "null");
    if (inputFileSystem != "null") {
      ipio = new FileSystemIOService(upc, inputFileSystem);
      inputFilePath = parms.getParameter("inputFilePath");
      try {
        ipio.open(inputFilePath);
        toolContext.setInputGrid(ipio.getGridDefinition());
        ipio.close();
      } catch (SeisException ex) {
        ex.printStackTrace();
        throw new RuntimeException("Could not open inputPath: " + inputFilePath
            + "\n" + "    on inputFileSystem: " + ipio, ex.getCause());
      }

    }
    // Run the tool serial initialization step with the provided input
    // GridDefinition
    toolContext.setParameterService(parms);
    tool.serialInit(toolContext);
    // Get the output grid definition set by the tool
    GridDefinition outputGrid = toolContext.getOutputGrid();
    // Create or open output file if it was requested
    outputFileSystem = parms.getParameter("outputFileSystem", "null");
    output = false;
    // If no output specified, don't use
    if (outputGrid != null && outputFileSystem != "null") {
      output = true;
      opio = new FileSystemIOService(upc, outputFileSystem);
      outputFilePath = parms.getParameter("outputFilePath");
      String outputMode = parms.getParameter("outputMode", "create");
      // For create, make the file and then close it
      if (outputMode == "create") {
        try {
          opio.create(outputFilePath, outputGrid);
          opio.close();
        } catch (SeisException ex) {
          ex.printStackTrace();
          throw new RuntimeException("Could not create outputPath: "
              + outputFilePath + "\n" + "    on outputFileSystem: " + opio,
              ex.getCause());
        }
      }
      // Open for both open and create
      try {
        opio.open(outputFilePath);
        GridDefinition currentGrid = opio.getGridDefinition();
        opio.close();
        if (currentGrid.matches(outputGrid) == false)
          throw new RuntimeException("outputFilePath GridDefintion: "
              + outputGrid + "\n does not match toolContext GridDefinition: "
              + currentGrid);
      } catch (SeisException ex) {
        ex.printStackTrace();
        throw new RuntimeException("Could not open outputPath: "
            + outputFilePath + "\n" + "    on outputFileSystem: " + opio,
            ex.getCause());
      }
    }
    // Now run the tool handler which calls the implementor's methods
    int ntask = Integer.parseInt(parms.getParameter("threadCount", "1"));
    try {
      ParallelTaskExecutor.runTasks(StandAloneVolumeTask.class, ntask);
    } catch (ExecutionException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    // Call the implementor's serial finish method to release any global
    // resources
    tool.serialFinish(toolContext);
  }

  /**
   * StandAlone tool handler for processing regular volumes from JavaSeis
   * datasets
   *
   * @author Chuck Mosher for JavaSeis.org
   */
  public static class StandAloneVolumeTask extends ParallelTask {
    @Override
    public void run() {
      // Get the parallel context
      IParallelContext pc = this.getParallelContext();
      // Add the parallel context to the toolContext
      toolContext.setParallelContext(pc);
      // Open the input and output file systems - should have been checked by
      // main
      ipio = new FileSystemIOService(pc, inputFileSystem);
      if (output) {
        opio = new FileSystemIOService(pc, outputFileSystem);
      }
      try {
        ipio.open(inputFilePath);
        if (output) {
          opio.open(outputFilePath);
        }
      } catch (SeisException ex) {
        throw new RuntimeException(ex.getCause());
      }
      // Get the input and output grids and store in the tool context
      toolContext.setInputGrid(ipio.getGridDefinition());
      if (output) {
        toolContext.setOutputGrid(opio.getGridDefinition());
      }
      // Call the implementing method for parallel initialization
      tool.parallelInit(toolContext);
      // Create the input and output seismic volumes
      ISeismicVolume inputVolume = new SeismicVolume(pc,
          ipio.getGridDefinition());
      ipio.setDistributedArray(inputVolume.getDistributedArray());
      ISeismicVolume outputVolume = inputVolume;
      if (output) {
        outputVolume = new SeismicVolume(pc, opio.getGridDefinition());
        opio.setDistributedArray(outputVolume.getDistributedArray());
      }
      // Loop over input volumes
      while (ipio.hasNext()) {
        // Get the next input volume
        ipio.next();
        try {
          ipio.read();
        } catch (SeisException e) {
          if (pc.isMaster()) {
            e.printStackTrace();
          }
          throw new RuntimeException(e.getCause());
        }
        boolean hasOutput = tool.processVolume(toolContext, inputVolume,
            outputVolume);
        if (output && hasOutput) {
          opio.next();
          try {
            opio.write();
          } catch (SeisException e) {
            if (pc.isMaster()) {
              e.printStackTrace();
            }
            throw new RuntimeException(e.getCause());
          }
        }
      }
      if (output) {
        // Process any remaining output
        while (tool.outputVolume(toolContext, outputVolume)) {
          opio.next();
          try {
            opio.write();
          } catch (SeisException e) {
            if (pc.isMaster()) {
              e.printStackTrace();
            }
            throw new RuntimeException(e.getCause());
          }
        }
      }
      // Call the implementor's parallel finish method to release any local
      // resources
      tool.parallelFinish(toolContext);
    }
  }

  @Override
  public void serialInit(ToolContext toolContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void parallelInit(ToolContext toolContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean processVolume(ToolContext toolContext, ISeismicVolume input,
      ISeismicVolume output) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean outputVolume(ToolContext toolContext, ISeismicVolume output) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void parallelFinish(ToolContext toolContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void serialFinish(ToolContext toolContext) {
    // TODO Auto-generated method stub

  }
}
