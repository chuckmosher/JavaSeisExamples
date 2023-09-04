package org.javaseis.examples.plot;

import beta.javaseis.array.IMultiArray;
import beta.javaseis.plot.AbstractMovieSource;

public class MultiArrayMovieSource extends AbstractMovieSource {

IMultiArray ma;
  
  public MultiArrayMovieSource( IMultiArray a ) {
    super( a.getShape() );
    ma = a;
  }
  
  @Override
  public void getFrame(int[] pos, float[][] buf) {
    ma.getFrame(buf, pos);
  }


}
