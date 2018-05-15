package org.javaseis.examples.tool;

import java.io.Serializable;

import org.javaseis.processing.framework.AFrameModule;
import org.javaseis.processing.framework.ActiveParm;
import org.javaseis.processing.framework.ModuleState;
import org.javaseis.processing.framework.VolumeModuleRunner;
import org.javaseis.processing.module.display.VolumeDisplay;
import org.javaseis.processing.module.io.VolumeInput;
import org.javaseis.services.ParameterService;
import org.javaseis.util.SeisException;
import org.javaseis.volume.ISeismicFrame;

import beta.javaseis.sigproc.BandpassFilter;

public class ExampleBandPassFilter extends AFrameModule {
  private static final long serialVersionUID = 1L;
  BandpassFilter bpf;
  int nsamp, ntrace;
  float dt;
  float[] fpass;
  ActiveParm filter;

  @Override
  public void checkState(ModuleState moduleState) throws SeisException {
    fpass[0] = moduleState.getFloatParameter("loCut", 0);
    fpass[1] = moduleState.getFloatParameter("loPass", 0);
    fpass[2] = moduleState.getFloatParameter("hiPass", 0);
    fpass[3] = moduleState.getFloatParameter("hiCut", 0);
    filter = new ActiveParm("BandPassFilter.filter",this);
    filter.setValue(fpass);
    nsamp = (int) moduleState.getInputState().getGridDefinition().getAxisLength(0);
    ntrace = (int) moduleState.getInputState().getGridDefinition().getAxisLength(0);
    dt = (float) moduleState.getInputState().getGridDefinition().getAxisPhysicalDelta(0);
    moduleState.log("Initialized");
  }

  @Override
  public void initialize(ModuleState moduleState) {
    bpf = new BandpassFilter( nsamp, dt, 50 );
    bpf.setFilter(fpass);
  }

  @Override
  public void processFrame(ModuleState moduleState, ISeismicFrame input, ISeismicFrame output) {
    moduleState.log("Filter Frame");
    int ntrc = input.getTraceCount();
    float[][] in = input.getFrame();
    float[][] out = output.getFrame();
    for (int j=0; j<ntrc; j++) {
      bpf.apply(in[j], out[j]);
    }
    output.putFrame(out);
  }

  @Override
  public void finish(ModuleState moduleState) {
    moduleState.log("Completed");
  }
  
  @Override
  public void updateState( ActiveParm parm, Serializable newValue ) {
    if (parm.getName() == "BandPassFilter.range") {
      bpf.setFilter((float[])newValue);
    }
  }
  
  public static void main(String[] args) {
    int toolCount = 3;
    String[] toolList = new String[toolCount];
    toolList[0] = ExampleVolumeInput.class.getCanonicalName();
    toolList[1] = ExampleBandPassFilter.class.getCanonicalName();
    toolList[2] = ExampleVolumeInspector.class.getCanonicalName();

    ParameterService globalParms = new ParameterService(args);
    ParameterService[] parms = ParameterService.allocate(toolCount, globalParms);
    String fileSystem = System.getProperty("user.home") + "/Projects/SEG-ACTI";
    parms[0].setParameter(ModuleState.INPUT_FILE_SYSTEM, fileSystem);
    parms[0].setParameter(ModuleState.INPUT_FILE_NAME, "SegActi45Shots.js");
    parms[0].setParameter("volumeRange", "18,26,1");
    parms[1].setParameter("flc", "0");
    parms[1].setParameter("flp", "5"); 
    parms[1].setParameter("fhp", "10"); 
    parms[1].setParameter("fhc", "20");    

    globalParms.setParameter(ModuleState.TASK_COUNT, "1");
    try {
      VolumeModuleRunner.exec(globalParms, parms, toolList);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

}
