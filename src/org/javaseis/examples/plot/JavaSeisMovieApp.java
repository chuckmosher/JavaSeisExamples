package org.javaseis.examples.plot;

import org.javaseis.util.SeisException;

import beta.javaseis.plot.JSMovieSource;
import beta.javaseis.plot.JavaSeisMovie;

public class JavaSeisMovieApp {
	private static final long serialVersionUID = 1L;
	public static void main( String[] args) {
	  try {
      JavaSeisMovie.displayAndWait(new JSMovieSource(args[0]));
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
	}

}
