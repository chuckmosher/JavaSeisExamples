package org.javaseis.examples.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javaseis.parset.ParameterSetIO;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.services.ParameterService;
import org.javaseis.util.SeisException;

import edu.mines.jtk.util.Parameter;
import edu.mines.jtk.util.ParameterSet;
import edu.mines.jtk.util.ParameterSetFormatException;

public class ExampleFromParset {

  public static ParameterSet getDefaultParms() {
    ParameterSet parms = new ParameterSet("JsPvP Parameters");

    ParameterSet parset = parms.addParameterSet("Global Parameters");
    parset.setString(ModuleState.TASK_COUNT, "1");

    parset = parms.addParameterSet(ExampleVolumeInput.class.getCanonicalName());
    String fileSystem = System.getProperty("user.home") + "/Projects/SEG-ACTI";
    parset.setString(ModuleState.INPUT_FILE_SYSTEM, fileSystem);
    parset.setString(ModuleState.INPUT_FILE_NAME, "SegActi45Shots.js");
    parset.setString("volumeRange", "18,26,1");
    //parset.setString(ModuleState.INPUT_FILE_SYSTEM, System.getProperty("user.home"));
    //parset.setString(ModuleState.INPUT_FILE_NAME, "SegActiShotNo1.js");
    //parset.setString("volumeRange", "0,0,1");
    //parset.setString("volumeRange", "*");

    parset = parms.addParameterSet(ExampleBandPassFilter.class.getCanonicalName());
    parset.setString("flc", "0");
    parset.setString("flp", "5");
    parset.setString("fhp", "10");
    parset.setString("fhc", "20");

    parset = parms.addParameterSet(ExampleVolumeInspector.class.getCanonicalName());

    return parms;
  }

  public static void runFromParset(String[] args, ParameterSet parameters) {
    ParameterService globalParms = new ParameterService(args);

    Iterator<ParameterSet> iter = parameters.getParameterSets();
    List<String> toolList = new ArrayList<String>();
    while (iter.hasNext()) {
      ParameterSet parset = iter.next();
      String name = parset.getName();
      if (name == null || name.isEmpty()) {
        System.out.println("Skipping parameter set with no name");
        continue;
      }
      if (name.equals("Global Parameters")) {
        Iterator<Parameter> itp = parset.getParameters();
        while (itp.hasNext()) {
          Parameter p = itp.next();
          globalParms.setParameter(p.getName(), p.getString());
        }
      } else {
        toolList.add(name);
      }
    }

    // Get parameters for each tool
    ParameterService[] parms = ParameterService.allocate(toolList.size(), globalParms);
    for (int i = 0; i < parms.length; i++) {
      String toolName = toolList.get(i);
      ParameterSet parset = parameters.getParameterSet(toolName);
      Iterator<Parameter> itp = parset.getParameters();
      while (itp.hasNext()) {
        Parameter p = itp.next();
        parms[i].setParameter(p.getName(), p.getString());
      }
    }

    // Run flow
    run(globalParms, parms, toolList.toArray(new String[0]));
  }

  public static void run(ParameterService globalParms, ParameterService[] parms, String[] toolList) {
    try {
      VolumeModuleRunner.exec(globalParms, parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

  public static void printUsage() {
    System.out.println("Usage: " + ExampleFromParset.class.getCanonicalName() + " -f input-parameters.xml");
    System.out.println("   or: " + ExampleFromParset.class.getCanonicalName() + " -d"
        + "\n        to use default parameters");
  }

  public static ParameterSet getParsetFromArgs(String[] args) {
    if (args.length == 1) {
      if (args[0].equals("-d")) {
        ParameterSet parms = getDefaultParms();
        System.out.println("Parameters (default):\n" + parms);
        return parms;
      }
    } else if (args.length == 2) {
      if (args[0].equals("-f")) {
        try {
          ParameterSet parms = ParameterSetIO.readFile(args[1]);
          System.out.println("Parameters from " + args[1] + ":\n" + parms);
          return parms;
        } catch (ParameterSetFormatException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  public static void main(String[] args) {
    ParameterSet parms = getParsetFromArgs(args);
    if (parms == null) {
      printUsage();
      System.exit(1);
    }

    // Run
    runFromParset(args, parms);
  }

}
