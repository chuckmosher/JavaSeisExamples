package org.javaseis.volume.test;

import java.io.File;
//import java.util.Arrays;
import java.util.Iterator;

import org.javaseis.array.IMultiArray;
import org.javaseis.array.MultiArray;
import org.javaseis.util.SeisException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JTestExampleRandomDataset {

  private ExampleRandomDataset dataset;
  private MultiArray workFrame;

  //Find out if data already exists in that folder, and delete it if it does
  @Before
  @After
  public void deleteTemporaryFolderIfItExists() {    
    String dataFullPath = ExampleRandomDataset.defaultPath;
    File dataFile = new File(dataFullPath);
    if (dataFile.exists()) {
      deleteDataFolder(dataFile);
    }
  }

  private static void deleteDataFolder(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null && files.length > 0) {
        for (File subFile : files) {
          deleteDataFolder(subFile);
        }
      }
    }
    file.delete();
  }

  //TODO replace UnsupportedOperationException with a javaseis specific exception
  // say DatasetAlreadyExistsException
  @Test(expected=UnsupportedOperationException.class)
  public void testConstructorFailsIfDataExists() {
    dataset = createDefaultData();
    dataset = createDefaultData();
  }

  @Test
  public void testNoArgConstructor() {
    dataset = createDefaultData();
    Assert.assertNotNull("Volume is null",dataset);
    Assert.assertNotNull("Seisio is null",dataset.seisio);
    Assert.assertNotNull("GridDefinition is null",dataset.gridDefinition);
  }

  @Test
  public void testPublicDeleteMethod() {
    dataset = createDefaultData();
    dataset.deleteJavaSeisData();
    try {
      dataset = new ExampleRandomDataset();
    } catch (UnsupportedOperationException e) {
      Assert.fail("Data folder still exists after delete");
    }
  }

  @Test
  public void testDatasetIsNonempty() {
    dataset = createDefaultData();
    initializeWorkFrame();
    Assert.assertTrue("Dataset is empty",datasetIsNonEmpty());
  }

  private void initializeWorkFrame() {
    long[] axisLengths = dataset.gridDefinition.getAxisLengths();
    workFrame = new MultiArray(2,float.class,
        new int[] {(int)axisLengths[0],(int)axisLengths[1]});
    workFrame.allocate();
  }
  
  private boolean datasetIsNonEmpty() {
    Iterator<int[]> frameIterator = dataset.seisio.frameIterator();
    while (frameIterator.hasNext()) {
      int[] nextIndex = frameIterator.next();
      try {
        dataset.seisio.readMultiArray(workFrame, nextIndex);
      } catch (SeisException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
      if (frameIsNotEmpty(workFrame)) {
        return true;
      }
    }
    return false; 
  }
  
  private boolean frameIsNotEmpty(IMultiArray frame) {
    //look at all the sample values until you find one that isn't zero
    int[] framesize = frame.getShape();
    int[] position = new int[framesize.length];

    for (int trace = 0 ; trace < framesize[1] ; trace++) {
      position[1] = trace;
      for (int sample = 0 ; sample < framesize[0] ; sample++) {
        position[0] = sample;
        float[] samp = new float[1];
        frame.getSample(samp,position);
        if (samp[0] != 0) {
          System.out.println(samp[0]);
          return true;
        }
      }
    }
    return false; //if every element is zero.
  }

  private ExampleRandomDataset createDefaultData() {
    return new ExampleRandomDataset();
  }
}
