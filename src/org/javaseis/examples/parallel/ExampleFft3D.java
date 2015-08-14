package org.javaseis.examples.parallel;

import java.util.concurrent.ExecutionException;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.fft.SeisFft3d;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;
import edu.mines.jtk.util.ArrayMath;

public class ExampleFft3D {

  public static void main(String[] args) {
    // Default number of tasks to one
    int ntask = 8;
    // Check to see if any arguments were provided
    if (args != null && args.length > 0) {
      // Convert the argument to an integer if it was provided
      ntask = Integer.parseInt(args[0]);
    }
    // Start the parallel task executor with the simple class provided below
    try {
      ParallelTaskExecutor.runTasks(ParallelFft3D.class, ntask);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static class ParallelFft3D extends ParallelTask {
    @Override
    public void run() {

      // Get the parallel context for this task
      IParallelContext pc = this.getParallelContext();
      pc.serialPrint("Yo from task " + pc.rank());
      // Set a shape for the 3D array
      int[] shape = new int[] { 250, 112, 36 };
      // Create an N-Dimensional FFT with N=3
      SeisFft3d fft3d = new SeisFft3d(pc, shape, new float[] { 0, 0, 0 }, new int[] { -1, 1, 1 });
      // Get the shape of the data in the transform domain
      // Create a distributed array with the right shape for the transform
      DistributedArray input = fft3d.getArray();
      // Reshape to the input shape
      input.setShape(shape);
      // Set the inernal DA iterator to be a trace iterator
      input.resetTraceIterator();

      // Allocate an array long enough to hold a trace
      int n = input.getShape()[0];
      float[] trc = new float[n];

      // Loop over all traces in the input
      while (input.hasNext()) {
        // Step to the next trace
        input.next();
        // Fill array with random numbers
        ArrayMath.rand(trc);
        input.putTrace(trc);
      }

      // Copy the input to a work array
      DistributedArray wk = new DistributedArray(input);

      // Forward Transform
      fft3d.forward();

      // Inverse transform
      fft3d.inverse();

      // Validate
      float[] wtrc = new float[n];
      wk.resetTraceIterator();
      input.resetTraceIterator();
      while (wk.hasNext()) {
        input.next();
        input.getTrace(trc);
        wk.next();
        wk.getTrace(wtrc);
        for (int i = 0; i < n; i++) {
          if (Math.abs(trc[i] - wtrc[i]) > 1e-6f)
            throw new RuntimeException("Out of Range");
        }
      }
      pc.serialPrint("Success from task " + pc.rank());
    }
  }
}
