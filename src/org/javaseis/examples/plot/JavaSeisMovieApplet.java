package org.javaseis.examples.plot;

import java.io.File;

import javax.swing.JApplet;

import org.javaseis.util.SeisException;

import beta.javaseis.plot.IMovieSource;
import beta.javaseis.plot.JSMovieSource;
import beta.javaseis.plot.JavaSeisMovie;
import beta.javaseis.plot.RandomMovieSource;

public class JavaSeisMovieApplet extends JApplet {
  private static final long serialVersionUID = 1L;

  /**
   * Create the applet with a random input
   */
  public JavaSeisMovieApplet() {
    IMovieSource source = new RandomMovieSource(new int[] { 100, 100, 100 });
    JavaSeisMovie.display(source);
  }

  /**
   * Create the applet with input from a JavaSeis dataset on disk
   * @param filePath
   * @throws SeisException
   */
  public JavaSeisMovieApplet(String filePath) throws SeisException {
    IMovieSource source = new JSMovieSource(new File(filePath));
    JavaSeisMovie.display(source);
  }

}
