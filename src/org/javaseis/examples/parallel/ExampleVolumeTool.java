package org.javaseis.examples.parallel;

import org.javaseis.services.ParameterService;
import org.javaseis.tool.IVolumeTool;
import org.javaseis.tool.StandAloneVolumeTool;
import org.javaseis.tool.ToolContext;
import org.javaseis.volume.ISeismicVolume;

public class ExampleVolumeTool extends StandAloneVolumeTool implements IVolumeTool {

  public static void main(String[] args) {
    ParameterService parms = new ParameterService(args);
    if (parms.getParameter("inputFileSystem") == "null") {
      parms.setParameter("inputFileSystem",System.getProperty("java.io.tmpdir"));
    }
    if (parms.getParameter("inputFilePath") == "null") parms.setParameter("inputFilePath","temp.js");
    exec(parms,new ExampleVolumeTool() );
  }
  
  @Override
  public boolean processVolume(ToolContext toolContext, ISeismicVolume input,
      ISeismicVolume output) {
    output.copyVolume(input);
    return true;
  } 
}
