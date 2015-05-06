package org.javaseis.examples.io;

import java.io.File;
import java.util.Arrays;

import org.javaseis.array.IMultiArray;
import org.javaseis.array.MultiArray;
import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.parset.ParameterSetIO;
import org.javaseis.util.SeisException;

import edu.mines.jtk.util.ArrayMath;
import edu.mines.jtk.util.ParameterSet;

/**
 * Example class that creates a JavaSeis CDP ordered dataset and writes random
 * numbers. No trace headers are written in this example.
 * 
 * @author Chuck Mosher for JavaSeis.org
 * 
 */
public class ExampleCreateRandomFile {

  public static void main(String[] args) throws SeisException {

    // Convert arguments to a ParameterSet object for easy access
    ParameterSet parset = ParameterSetIO.argsToParameters(args);
    // Get path name for new dataset - default to "jsCreateTest" in User.home
    String path = parset.getString("path", System.getProperty("user.home")
        + File.separator + "jsCreateTest");

    System.out
        .println("Create JavaSeis CDP dataset with random numbers\nPath: "
            + path);

    // Get the size of the dataset to be created
    int[] size = parset.getInts("size", new int[] { 250, 30, 100, 10 });
    if (size.length != 4) {
      throw new RuntimeException(
          "Wrong number of elements for size - should be 4");
    }

    System.out.println("Size: " + Arrays.toString(size));
    // Get the spacing of the dataset to be created

    double[] deltas = parset.getDoubles("deltas",
        new double[] { 4, 100, 25, 50 });
    if (deltas.length != 4) {
      throw new RuntimeException(
          "Wrong number of elements for deltas - should be 4");
    }

    long[] ldeltas = parset.getLongs("ldeltas", new long[] { 4, 4, 1, 2 });
    if (ldeltas.length != 4) {
      throw new RuntimeException(
          "Wrong number of elements for ldeltas - should be 4");
    }

    // Get the origins of the dataset to be created
    double[] origins = parset
        .getDoubles("origins", new double[] { 0, 0, 0, 0 });
    if (origins.length != 4) {
      throw new RuntimeException(
          "Wrong number of elements for origins - should be 4");
    }

    long[] lorigins = parset.getLongs("lorigins", new long[] { 0, 1, 1, 1 });
    if (lorigins.length != 4) {
      throw new RuntimeException(
          "Wrong number of elements for lorigins - should be 4");
    }

    // Make a GridDefinition
    GridDefinition grid = GridDefinition.standardGrid(GridDefinition.CDP, size,
        lorigins, ldeltas, origins, deltas);

    // Be sure that it doesn't already exist (which is an error).
    Seisio.delete(path);

    // Attempt to create
    Seisio sio = new Seisio(path, grid);
    sio.create();
    System.out.println("Created " + path);
    // Use a MultiArray to hold data for writing

    IMultiArray frm = MultiArray.factory(2, float.class, 1, size);

    float[] trc = new float[size[0]];
    // Loop and write random numbers
    int[] position = new int[4];
    for (int volume = 0; volume < size[3]; volume++) {
      System.out.println("Writing volume " + volume + " ... ");
      position[3] = volume;
      for (int frame = 0; frame < size[2]; frame++) {
        position[2] = frame;
        for (int trace = 0; trace < size[1]; trace++) {
          ArrayMath.rand(trc);
          position[1] = trace;
          frm.putTrace(trc, position);
        }
        position[0] = position[1] = 0;
        sio.writeMultiArray(frm, position );
      }
    }

    // Close and that's all, folks !
    sio.close();
    System.out.println("Write complete");
  }
}
