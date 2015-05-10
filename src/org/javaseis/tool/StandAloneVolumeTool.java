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

public class StandAloneVolumeTool {

	public StandAloneVolumeTool() {

	}

	public static ToolContext toolContext;

	public static IVolumeTool tool;

	public static ISeismicVolume inputVolume, outputVolume;
	
	public static IDistributedIOService ipio, opio;
	
	public static String inputFileSystem, inputFilePath, outputFileSystem, outputFilePath;

	public static void exec(String[] args, IVolumeTool standAloneTool) {
		// Create a parameter service
		ParameterService parms = new ParameterService(args);
		// Set a uniprocessor context
		IParallelContext upc = new UniprocessorContext();
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
				throw new RuntimeException("Could not open inputPath: "
						+ inputFilePath + "\n" + "    on inputFileSystem: " + ipio,
						ex.getCause());
			}

		}
		// Run the tool serial initialization step with the provided input
		// GridDefinition
		toolContext = new ToolContext();
		toolContext.setParamaterService(parms);
		tool = standAloneTool;
		tool.serialInit(toolContext);
		// Get the output grid definition set by the tool
		GridDefinition outputGrid = toolContext.getOutputGrid();
		// Create or open output file if it was requested
		outputFileSystem = parms
				.getParameter("outputFileSystem", "null");
		opio = null;
		if (outputGrid != null && outputFileSystem != "null") {
			opio = new FileSystemIOService(upc, outputFileSystem);
			outputFilePath = parms.getParameter("outputFilePath");
			String outputMode = parms.getParameter("outputMode", "create");
			if (outputMode == "create") {
				try {
					opio.create(outputFilePath, outputGrid);
					opio.close();
				} catch (SeisException ex) {
					ex.printStackTrace();
					throw new RuntimeException("Could not create outputPath: "
							+ outputFilePath + "\n" + "    on outputFileSystem: "
							+ opio, ex.getCause());
				}
			}
			try {
				opio.open(outputFilePath);
				GridDefinition currentGrid = opio.getGridDefinition();
				opio.close();
				if (currentGrid.matches(outputGrid) == false) {
					throw new RuntimeException("outputFilePath GridDefintion: " + outputGrid +
					    "\n does not match toolContext GridDefinition: " + currentGrid );
				}
			} catch (SeisException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Could not open outputPath: "
						+ outputFilePath + "\n" + "    on outputFileSystem: "
						+ opio, ex.getCause());
			}
		}

		int ntask = Integer.parseInt(parms.getParameter("threadCount", "4"));
		try {
			ParallelTaskExecutor.runTasks(StandAloneTask.class, ntask);
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		tool.serialFinish(toolContext);
	}

	public static class StandAloneTask extends ParallelTask {
		@Override
		public void run() {
			IParallelContext pc = this.getParallelContext();
			toolContext.setParallelContext(pc);
			ipio = new FileSystemIOService(pc,inputFileSystem);
			opio = new FileSystemIOService(pc,outputFileSystem);
			try {
				ipio.open(inputFilePath);
				ipio.open(outputFilePath);
			} catch (SeisException ex) {
				throw new RuntimeException(ex.getCause());
			}
			toolContext.setInputGrid(ipio.getGridDefinition());
			toolContext.setOutputGrid(opio.getGridDefinition());
			tool.parallelInit(toolContext);
			ISeismicVolume inputVolume = new SeismicVolume( pc, ipio.getGridDefinition());
			ipio.setDistributedArray(inputVolume.getDistributedArray());
			ISeismicVolume outputVolume = new SeismicVolume( pc, opio.getGridDefinition());
			opio.setDistributedArray(outputVolume.getDistributedArray());
			while (ipio.hasNextVolume()) {
			  try {
          ipio.nextVolume();
        } catch (SeisException e) {
          if (pc.isMaster())
            e.printStackTrace();
          throw new RuntimeException(e.getCause());
        }
			  if (tool.processVolume(toolContext, inputVolume, outputVolume)) {
			    try {
            opio.write();
          } catch (SeisException e) {
            if (pc.isMaster())
              e.printStackTrace();
            throw new RuntimeException(e.getCause());
          }
			  }
			}
				
			while (tool.outputVolume(toolContext, outputVolume)) {
			  try {
          opio.write();
        } catch (SeisException e) {
          if (pc.isMaster())
            e.printStackTrace();
          throw new RuntimeException(e.getCause());
        }
			}
			tool.parallelFinish(toolContext);
		}
	}
}
