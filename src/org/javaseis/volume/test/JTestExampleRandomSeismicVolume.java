package org.javaseis.volume.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JTestExampleRandomSeismicVolume {
  
  @Before
  public void deleteTemporaryFolder() {
    if (ExampleRandomSeismicVolume.dataSetExists(
        ExampleRandomSeismicVolume.dataFullPath)) {
      
    }
    ExampleRandomSeismicVolume volume = new ExampleRandomSeismicVolume();
    volume.deleteJavaSeisData();
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

}
