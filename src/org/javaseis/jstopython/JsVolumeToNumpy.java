package org.javaseis.jstopython;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.javaseis.io.Seisio;
import org.javaseis.util.SeisException;

public class JsVolumeToNumpy {

  /**
   * @param args
   * @throws SeisException
   */
  public static void main(String[] args) {
    try {
      jsVolumeToNumpy(args);
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void jsVolumeToNumpy(String[] args) throws SeisException {
    String inPath = args[0];
    String outPath = args[1];
    System.out.println(
        "Convert JavaSeis Volume to Numpy 3D array" + "\n  Input Path: " + inPath + "\n  Output path: " + outPath);
    // Attempt to open input
    Seisio sio = null;
    try {
      sio = new Seisio(inPath);
      sio.open("r");
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("Could not open inPath=" + inPath, e.getCause());
    }
    long[] shape = sio.getGridDefinition().getAxisLengths();
    // Open output file and write shape

    try (FileOutputStream fos = new FileOutputStream(outPath)) {
      // Create ByteBuffer with native endian
      ByteBuffer buffer = ByteBuffer.allocate((int) (4 * shape[0] * shape[1] * shape[2] + 16));  // 4 bytes per float
      buffer.order(ByteOrder.nativeOrder());  // Set the buffer to native endian order
      
      // Write the shape (z, y, x) as integers
      buffer.putInt((int)shape[0]);
      buffer.putInt((int)shape[1]);
      buffer.putInt((int)shape[2]);
      
      // Write the 3D array of floats
      int[] pos = new int[4];
      double rms = 0;
      float[][] trcs = sio.getTraceDataArray();
      for (int k = 0; k < shape[2]; k++) {
        pos[2] = k;
        sio.readFrame(pos);
        for (int j = 0; j < shape[1]; j++) {
          for (int i = 0; i < shape[0]; i++) {
            buffer.putFloat(trcs[j][i]);
            rms += trcs[j][i] * trcs[j][i];
          }
        }
      }
      rms = Math.sqrt(rms/(shape[0]*shape[1]*shape[2]));
      // Write the RMS value
      buffer.putFloat((float)rms);
      
      // Write the buffer contents to the file
      fos.write(buffer.array());
      fos.close();  
      System.out.println("Conversion complete: rms = " + rms);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Conversion failed");
    }
  }

}
