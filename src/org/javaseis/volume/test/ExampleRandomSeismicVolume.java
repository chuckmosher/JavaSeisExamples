package org.javaseis.volume.test;

import java.io.File;

import org.javaseis.services.ParameterService;


/**
 * Create an example JavaSeis dataset that can be read in as a single SeismicVolume,
 * filled with random numbers. For testing purposes.
 * 
 * This class only makes and cleans up the dataset.
 * 
 * @author Marcus Wilson 2015
 *
 */
public class ExampleRandomSeismicVolume {

  private static ParameterService parms;
  private static String dataFolder;
  private static String dataFileName;
  private static String dataFullPath;

  // Establishes the minimum number of parameters that have to be set to 
  // make a viable javaseis volume.
  private static ParameterService generateDefaultParameters() {
    String[] noArguments = new String[0];
    parms = new ParameterService(noArguments);
    addDefaultDataLocation();

    String gridDimensions = "900 64 47";
    parms.setParameter("size",gridDimensions);

    String gridSpacings = "2 12 100";
    parms.setParameter("deltas",gridSpacings);

    return parms;
  }

  private static void addDefaultDataLocation() {
    dataFolder = System.getProperty("java.io.tmpdir");
    dataFileName = "temp.js";
    dataFullPath = "inputFileSystem" + File.separator + "inputFilePath";
    parms.setParameter("inputFileSystem",dataFolder);
    parms.setParameter("inputFilePath",dataFileName);
  }

  // Noarg constructor that uses the defaults
  public ExampleRandomSeismicVolume() {
    this(generateDefaultParameters());
  }

  private void failIfFileAlreadyExists() {
    assert dataFullPath != null;
    File datapath = new File(dataFullPath);
    if (datapath.exists()) {
      throw new UnsupportedOperationException(
          "Unable to create data.  File already exists");
    }
  }

  // Constructor that uses a parset (note numdimensions == 3)
  public ExampleRandomSeismicVolume(ParameterService inputParms) {
    parms = inputParms;
    getFilePathFromParameterService();
    failIfFileAlreadyExists();
    checkNumDimensionsIsThree();
    getAxisDefinitionsFromParameterService();
    makeGridDefinitionFromAxisDefinitions();
    createSeisIO();
    createJavaSeisData();
  }

  private void getFilePathFromParameterService() {
    // TODO Auto-generated method stub
    
  }

  private void checkNumDimensionsIsThree() {
    // TODO Auto-generated method stub
    
  }

  private void getAxisDefinitionsFromParameterService() {
    // TODO Auto-generated method stub
    
  }

  private void makeGridDefinitionFromAxisDefinitions() {
    // TODO Auto-generated method stub
    
  }

  private void createSeisIO() {
    // TODO Auto-generated method stub
    
  }

  private void createJavaSeisData() {

  }

  public void deleteJavaSeisData() {

  }


}
