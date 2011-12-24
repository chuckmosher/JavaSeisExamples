package beta.javaseis.examples.io;

import java.io.File;
import java.util.Iterator;

import beta.javaseis.grid.GridDefinition;
import beta.javaseis.io.Seisio;
import beta.javaseis.parset.ParameterSetIO;
import beta.javaseis.util.SeisException;

import edu.mines.jtk.util.ParameterSet;

public class ExampleCompute2DFFT {

  /**
   * @param args
   * @throws SeisException
   */
  public static void main(String[] args) throws SeisException {

    // Convert arguments to a ParameterSet object for easy access
    ParameterSet parset = ParameterSetIO.argsToParameters( args );
    // Get path name for new dataset - default to "jsCreateTest" in User.home
    String path = parset.getString("path", System.getProperty("user.home") + File.separator +
        "jsCreateTest" );

    System.out.println("Calculate 2D FFT for frames in a JavaSeis dataset\nPath: " + path );
    // Attempt to open
    Seisio sio = new Seisio( path );
    sio.open("r");
    // Get the grid for the dataset
    GridDefinition grid = sio.getGridDefinition();
    // Get the frame dimensions
    int n0 = (int)grid.getAxisLength(0);
    int n1 = (int)grid.getAxisLength(1);
    //SeisFft2d f2d = new SeisFft2d( n0, n1, 0f, 0f );
    // Get the trace array and iterator for frames
    float[][] trc = sio.getTraceDataArray();
    Iterator<int[]> frames = sio.frameIterator();
    double rms = 0.0;
    double sum = 0;
    // Loop over frames and calculate RMS value
    while (frames.hasNext()) {
      frames.next();
      int ntrc = sio.getTracesInFrame();
      for (int j=0; j<ntrc; j++) {
        for (int i=0; i<trc[j].length; i++) {
            sum = sum + 1;
            rms = rms + (double)(trc[j][i]*trc[j][i]);
        }
      }
    }
    rms = Math.sqrt(rms/sum);
    System.out.println("RMS Value of dataset = " + rms );

    frames = sio.frameIterator();
    rms = 0.0;
    sum = 0;
    // Loop over frames and calculate RMS value
    while (frames.hasNext()) {
      frames.next();
      int ntrc = sio.getTracesInFrame();
      for (int j=0; j<ntrc; j++) {
        for (int i=0; i<trc[j].length; i++) {
            sum = sum + 1;
            rms = rms + (double)(trc[j][i]*trc[j][i]);
        }
      }
    }
    rms = Math.sqrt(rms/sum);
    System.out.println("RMS Value of dataset = " + rms );
  }

}
