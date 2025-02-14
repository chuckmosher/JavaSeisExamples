package org.javaseis.jstopython;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.javaseis.io.Seisio;
import org.javaseis.parset.ParameterSetIO;
import org.javaseis.util.SeisException;

import edu.mines.jtk.util.ParameterSet;

public class JsVolumeToNumpy {

  /**
   * @param args
   * @throws SeisException 
   */
  public static void main(String[] args) throws SeisException {    

    // Convert arguments to a ParameterSet object for easy access
    ParameterSet parset = ParameterSetIO.argsToParameters( args );
    // Get path name for new dataset - default to "jsCreateTest" in User.home
    String inPath = parset.getString("inPath", "none" );
    String outPath = parset.getString("outPath", "none" );
    System.out.println("Convert JavaSeis Volume to Numpy 3D array" +
        "\n  Input Path: " + inPath +
        "\n  Output path: " + outPath );
    if (inPath.compareTo("none") == 0) throw new SeisException("Parameter inPath is missing");
    if (outPath.compareTo("none") == 0) throw new SeisException("Parameter outPath is missing");
    // Attempt to open input
    Seisio sio = null;
    try {
      sio = new Seisio( inPath );
      sio.open("r");
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("Could not open inPath=" + inPath,e.getCause());
    }
    long[] shape = sio.getGridDefinition().getAxisLengths();
    // Open output file and write shape

    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outPath))) {
      dos.writeInt((int) shape[0]); // Save first dimension
      dos.writeInt((int) shape[1]); // Save second dimension
      dos.writeInt((int) shape[2]); // Save third dimension
      // Get the trace array and iterator for frames
      float[][] trc = sio.getTraceDataArray();
      Iterator<int[]> frames = sio.frameIterator();
      double sum = 0;
      // Loop over frames and write to disk
      while (frames.hasNext()) {
        frames.next();
        for (int j=0; j<shape[1]; j++) {
          for (int i=0; i<shape[0]; i++) {
            dos.writeFloat(trc[j][i]);
            sum += trc[j][i] * trc[j][i];
          }
        }
      }
      // Write RMS value as a check
      float rms = (float)(Math.sqrt(sum/(shape[0]*shape[1]*shape[2])));
      dos.writeFloat( rms );
      System.out.println("3D float array written with RMS = : " + rms);
      dos.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Conversion failed");
    }
  }

}
