package org.javaseis.volume.test;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JTestExampleRandomSeismicVolume {
  
  //Find out if data already exists in that folder, and delete it if it does
  @Before
  @After
  public void deleteTemporaryFolderIfItExists() {    
    String dataFullPath = ExampleRandomSeismicVolume.defaultPath;
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
    ExampleRandomSeismicVolume volume1 = new ExampleRandomSeismicVolume();
    ExampleRandomSeismicVolume volume2 = new ExampleRandomSeismicVolume();
  }
  
  @Test
  public void testNoArgConstructor() {
    ExampleRandomSeismicVolume volume = new ExampleRandomSeismicVolume();
    Assert.assertNotNull("Volume is null",volume);
    Assert.assertNotNull("Seisio is null",volume.seisio);
    Assert.assertNotNull("GridDefinition is null",volume.gridDefinition);
  }
  
  @Test
  public void testPublicDeleteMethod() {
    ExampleRandomSeismicVolume volume = new ExampleRandomSeismicVolume();
    volume.deleteJavaSeisData();
    try {
    volume = new ExampleRandomSeismicVolume();
    } catch (UnsupportedOperationException e) {
      Assert.fail("Data folder still exists after delete");
    }
  }
}
