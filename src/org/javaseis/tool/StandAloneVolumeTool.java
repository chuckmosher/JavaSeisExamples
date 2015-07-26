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
import beta.javaseis.parallel.ParallelException;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;
import beta.javaseis.parallel.UniprocessorContext;

/**
 * StandAlone volume processing tool handler
 *
 * @author chuck
 *
 */
public abstract class StandAloneVolumeTool implements IVolumeTool {

  public StandAloneVolumeTool() {
    // Need default constructor so implementors don't have to provide one
  }
  
  public static void exec(ParameterService parms, IVolumeTool tool )
      throws SeisException {
    ToolContext serialToolContext;
    String inputFileSystem, inputFilePath, outputFileSystem, outputFilePath;

    // Set a uniprocessor context
    IParallelContext upc = new UniprocessorContext();
    serialToolContext = new ToolContext(parms);
    IDistributedIOService ipio = null;
    // Open input file if it is requested
    boolean hasInput = false;
    inputFileSystem = serialToolContext.getParameter(ToolContext.INPUT_FILE_SYSTEM);
    if (inputFileSystem != "null") {
      ipio = new FileSystemIOService(upc, inputFileSystem);
      inputFilePath = serialToolContext.getParameter(ToolContext.INPUT_FILE_PATH);
      try {
        ipio.open(inputFilePath);
        serialToolContext.putFlowGlobal(ToolContext.INPUT_GRID, ipio.getGridDefinition() );
        ipio.close();
      } catch (SeisException ex) {
        ex.printStackTrace();
        throw new RuntimeException("Could not open inputPath: " + inputFilePath
            + "\n" + "    on inputFileSystem: " + ipio, ex.getCause());
      }
      hasInput = true;
    }
    serialToolContext.putFlowGlobal(ToolContext.HAS_INPUT, hasInput );
    // Run the tool serial initialization step with the provided input GridDefinition
    tool.serialInit(serialToolContext);
    // Get the output grid definition set by the tool
    GridDefinition outputGrid = (GridDefinition) serialToolContext.getFlowGlobal(ToolContext.OUTPUT_GRID);
    // Create or open output file if it was requested
    outputFileSystem = serialToolContext.getParameter(ToolContext.OUTPUT_FILE_SYSTEM);
    // If no output specified, don't use
    boolean hasOutput = false;
    if (outputGrid != null && outputFileSystem != "null") {
      IDistributedIOService opio = new FileSystemIOService(upc, outputFileSystem);
      outputFilePath = serialToolContext.getParameter(ToolContext.OUTPUT_FILE_PATH);
      String outputMode = serialToolContext.getParameter(ToolContext.OUTPUT_FILE_MODE);
      // For create, make the file and then close it
      if (outputMode == ToolContext.OUTPUT_FILE_CREATE) {
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
      hasOutput = true;
    }
    serialToolContext.putFlowGlobal(ToolContext.HAS_OUTPUT, hasOutput );
    
    // Store the tool class in the tool context
    serialToolContext.putToolGlobal(ToolContext.TOOL_CLASS, (Object)(tool.getClass()) );
    // Now run the tool handler which calls the implementor's methods
    int ntask = Integer.parseInt(serialToolContext.getParameter(ToolContext.TASK_COUNT));
    try {
      ParallelTaskExecutor.runTasks(StandAloneVolumeTask.class, ntask, (Object)serialToolContext);
    } catch (ExecutionException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    // Call the implementor's serial finish method to release any global
    // resources
    tool.serialFinish(serialToolContext);
  }
  
  public static class StandAloneVolumeTask extends ParallelTask {
    
    private ToolContext toolContext;
    private IVolumeTool tool;
    private Class<IVolumeTool> toolClass;
    public Boolean input, output;
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      // Get the parallel context
      IParallelContext pc = this.getParallelContext();
      ParallelException pe = new ParallelException(pc);
      // Add the parallel context to the toolContext
      toolContext = new ToolContext((ToolContext)super.getTaskObject());
      toolContext.setParallelContext(pc);
      toolClass = (Class<IVolumeTool>)toolContext.getToolGlobal(ToolContext.TOOL_CLASS);
      Exception ex = null;
      try {
        tool = toolClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e1) {
        ex = e1;
      }
      pe.exitOnException(ex, 1);
      // Open the input and output file systems - should have been checked by StandAloneVolumeTool.main
      ex = null;
      IDistributedIOService ipio = null;
      IDistributedIOService opio = null;
      input = (Boolean)toolContext.getFlowGlobal(ToolContext.HAS_INPUT);
      output = (Boolean)toolContext.getFlowGlobal(ToolContext.HAS_OUTPUT);
      try {
        if (input) {
          String inputFileSystem = toolContext.getParameter(ToolContext.INPUT_FILE_SYSTEM);
          ipio = new FileSystemIOService(pc,inputFileSystem);
          String inputFilePath = toolContext.getParameter(ToolContext.INPUT_FILE_PATH);
          ipio.open(inputFilePath);
        }
        if (output) {
          String outputFileSystem = toolContext.getParameter(ToolContext.OUTPUT_FILE_SYSTEM);
          opio = new FileSystemIOService(pc, outputFileSystem);
          String outputFilePath = toolContext.getParameter(ToolContext.OUTPUT_FILE_PATH);
          opio.open(outputFilePath);
        }
      } catch (SeisException e) {
        ex = e;
      }
      pe.exitOnException(ex, 1);
      // Get the input and output grids and store in the tool context
      if (input) {
        toolContext.putFlowLocal(ToolContext.INPUT_GRID, ipio.getGridDefinition());
      }
      if (output) {
        toolContext.putFlowLocal(ToolContext.OUTPUT_GRID, opio.getGridDefinition());
      }
      // Call the implementing method for parallel initialization
      tool.parallelInit(toolContext);
      // Create the input and output seismic volumes
      ISeismicVolume inputVolume = null;
      if (input) {
        inputVolume = new SeismicVolume(pc, ipio.getGridDefinition());
        ipio.setDistributedArray(inputVolume.getDistributedArray());
      }
      ISeismicVolume outputVolume = inputVolume;
      if (output) {
        outputVolume = new SeismicVolume(pc, opio.getGridDefinition());
        opio.setDistributedArray(outputVolume.getDistributedArray());
      }
      // Loop over input volumes
      if (input) {
        while (ipio.hasNext()) {
          // Get the next input volume
          ipio.next();
          // TODO: Investigate performance of ParallelException
          try {
            ipio.read();
          } catch (SeisException e) {
            ex = e;
          }
          pe.exitOnException(ex, 1);
          boolean hasOutput = tool.processVolume(toolContext, inputVolume,
              outputVolume);
          if (output && hasOutput) {
            opio.next();
            try {
              opio.write();
            } catch (SeisException e) {
              ex = e;
            }
            pe.exitOnException(ex, 1);
          }
        }
      }
      if (output) {
        // Process any remaining output
        while (tool.outputVolume(toolContext, outputVolume)) {
          if (!opio.hasNext()) {
            ex = new SeisException("Tool is attempting to output volume that is outside data context");
          }
          pe.exitOnException(ex, 1);
          opio.next();
          try {
            opio.write();
          } catch (SeisException e) {
            ex = e;
          }
          pe.exitOnException(ex, 1);
        }
      }
      // Call the implementor's parallel finish method to release any local resources
      tool.parallelFinish(toolContext);
    }
  }
}
