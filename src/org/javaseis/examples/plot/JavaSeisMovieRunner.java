package org.javaseis.examples.plot;

import org.javaseis.util.SeisException;

public class JavaSeisMovieRunner {

  public static void showMovie(String pathToDataset) {
    try {
      new JavaSeisMovieApplet(pathToDataset);
    } catch (SeisException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    //String pathToDataset = "/home/wilsonmr/javaseis/inputpwaves.VID";
    //String pathToDataset = "/home/wilsonmr/javaseis/100-rawsyntheticdata.js";
    String pathToDataset = "/home/wilsonmr/javaseis/100a-rawsynthpwaves.js";
    //String pathToDataset = "/home/wilsonmr/javaseis/seg_salt_vrms.VEL";
    //String pathToDataset = "/home/wilsonmr/javaseis/941cwtmigt_pp.js";	 

    if (args.length > 0) {
      pathToDataset = args[0];
    }
    JavaSeisMovieRunner.showMovie(pathToDataset);
  }
}
