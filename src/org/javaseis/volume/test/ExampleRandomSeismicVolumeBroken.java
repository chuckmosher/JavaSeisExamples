package org.javaseis.volume.test;

import java.io.File;
import java.util.Arrays;

import org.javaseis.io.Seisio;
import org.javaseis.grid.GridDefinition;
import org.javaseis.properties.AxisDefinition;
import org.javaseis.properties.AxisLabel;
import org.javaseis.properties.DataDomain;
import org.javaseis.util.SeisException;
import org.javaseis.properties.Units;

import edu.mines.jtk.util.ParameterSet;


/**
 * Create an example JavaSeis data set that can be read in as a single SeismicVolume,
 * filled with random numbers. For testing purposes.
 * 
 * This class only makes and cleans up the data set.
 * 
 * @author Marcus Wilson 2015
 *
 */
public class ExampleRandomSeismicVolumeBroken {

  //TODO I'm making this way harder than it has to be.  I'm going to make a difference
  // one that just takes a GridDefinition.

  private static final int DEFAULT_NUM_DIMENSIONS = 5;
  private double[] defaultPhysicalDeltas = new double[] {2, 12, 100, 1, 1};
  private double[] defaultPhysicalOrigins = new double[] {0, 0, 0, 1, 1};
  private long[] defaultLogicalDeltas = new long[] {1, 1, 1, 1, 1};
  private long[] defaultLogicalOrigins = new long[] {0, 1, 1, 1, 1};
  private long[] defaultGridDimensions = new long[] {900, 64, 47, 9, 4};

  private String dataFullPath;
  private ParameterSet parameterSet;

  // Noarg constructor that uses the defaults
  public ExampleRandomSeismicVolumeBroken() {
    this(new ParameterSet());
  }

  // Establishes the minimum number of parameters that have to be set to 
  // make a viable javaseis volume.
  private static ParameterSet generateDefaultParameters() {
    return new ParameterSet();
  }

  private String DefaultDataLocation() {
    String dataFolder = System.getProperty("java.io.tmpdir");
    String dataFileName = "temp.js";
    String dataFullPath = "inputFileSystem" + File.separator + "inputFilePath";
    parameterSet.setString("inputFileSystem",dataFolder);
    parameterSet.setString("inputFilePath",dataFileName);
    return dataFullPath;
  }

  // Constructor that uses a ParameterSet
  public ExampleRandomSeismicVolumeBroken(ParameterSet inputParms) {
    parameterSet = inputParms;
    dataFullPath = getFilePathFromParameterSet();
    failIfFileAlreadyExists();
    AxisDefinition[] axes = setDefaultAxes(parameterSet);
    GridDefinition gridDefinition = makeGridDefinitionFromAxisDefinitions(axes);
    createSeisIO(gridDefinition);
    createJavaSeisData();
  }

  private String getFilePathFromParameterSet() {
    String dataFolder = parameterSet.getString("inputFileSystem", null);
    String dataFileName = parameterSet.getString("inputFilePath",null);
    return dataFolder + File.separator + dataFileName;
  }

  private void failIfFileAlreadyExists() {
    assert dataFullPath != null;
    File datapath = new File(dataFullPath);
    if (datapath.exists()) {
      throw new UnsupportedOperationException(
          "Unable to create data.  File already exists");
    }
  }

  private AxisDefinition[] setDefaultAxes(ParameterSet parameterSet) {

    AxisLabel[] labels = defaultAxisLabels();
    Units[] units = defaultUnits();
    DataDomain[] domains = defaultDomains();
    long[] gridSize = parameterSet.getLongs("size", defaultGridDimensions);
    long[] lorigins = parameterSet.getLongs("lorigins", defaultLogicalOrigins);
    long[] ldeltas = parameterSet.getLongs("ldeltas", defaultLogicalDeltas);
    double[] porigins = parameterSet.getDoubles("origins", defaultPhysicalOrigins);
    double[] pdeltas = parameterSet.getDoubles("deltas",defaultPhysicalDeltas);

    int numDimensions = checkDimensions();

    AxisDefinition[] axes = new AxisDefinition[numDimensions];
    for (int k = 0 ; k < numDimensions ; k++) {
      axes[k] = new AxisDefinition(labels[k],
          units[k],
          domains[k],
          gridSize[k],
          lorigins[k],
          ldeltas[k],
          porigins[k],
          pdeltas[k]);
    }
    return axes;
  }

  private AxisLabel[] defaultAxisLabels() {
    return AxisLabel.getDefault(DEFAULT_NUM_DIMENSIONS);
  }

  private Units[] defaultUnits() {
    return new Units[] {
        Units.MS,
        Units.M,
        Units.M,
        Units.NULL,
        Units.NULL};
  }

  private DataDomain[] defaultDomains() {
    return new DataDomain[] {
        DataDomain.TIME,
        DataDomain.SPACE,
        DataDomain.SPACE,
        DataDomain.NULL,
        DataDomain.NULL};
  }

  private int checkDimensions() {
    //TODO fix later.  Check all argument arrays have same length
    return DEFAULT_NUM_DIMENSIONS;
  }

  private String[] convertObjectArrayToStringArray(Object[] objectArray) {
    return convertObjectArrayToStringArray(objectArray,objectArray.length);
  }

  private String[] convertObjectArrayToStringArray(Object[] objectArray,int size) {
    if (size > objectArray.length)
      throw new ArrayIndexOutOfBoundsException("Requested size exceeds array size");
    return Arrays.copyOf(objectArray,size,String[].class);
  }

  private GridDefinition makeGridDefinitionFromAxisDefinitions(AxisDefinition[] axes) {
    return new GridDefinition(axes.length,axes);   
  }

  private void createSeisIO(GridDefinition grid) {
    try {
      Seisio sio = new Seisio(dataFullPath,grid);
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // TODO Auto-generated method stub

  }

  private void createJavaSeisData() {

  }

  public void deleteJavaSeisData() {

  }


}
