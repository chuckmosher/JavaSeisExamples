package org.javaseis.volume.test;

import org.junit.Test;

public class JTestExampleRandomSeismicVolume {
  
  //TODO replace UnsupportedOperationException with a javaseis specific exception
  // say DatasetAlreadyExistsException
  @Test(expected=UnsupportedOperationException.class)
  public void testConstructorFailsIfDataExists() {
    ExampleRandomSeismicVolume volume1 = new ExampleRandomSeismicVolume();
    ExampleRandomSeismicVolume volume2 = new ExampleRandomSeismicVolume();
  }
  
  @Test
  public void testNoArgConstructor() {
    
  }

}
