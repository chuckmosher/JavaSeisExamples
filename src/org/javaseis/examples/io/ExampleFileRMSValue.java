package org.javaseis.examples.io;

import java.io.File;
import java.util.Iterator;

import org.javaseis.io.Seisio;
import org.javaseis.parset.ParameterSetIO;
import org.javaseis.util.SeisException;

import edu.mines.jtk.util.ParameterSet;

public class ExampleFileRMSValue {

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

    System.out.println("Calculate RMS trace value of JavaSeis dataset\nPath: " + path );
    // Attempt to open
    Seisio sio = new Seisio( path );
    sio.open("r");
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
  }

}
