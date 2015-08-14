package org.javaseis.examples.parallel;

import java.util.concurrent.ExecutionException;

import beta.javaseis.distributed.ConstructDistributedArray;
import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.distributed.DistributedFrameIterator;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

/**
 * Example showing how to use the JavaSeis parallel thread model in a
 * client-server context. The top level method takes a floating point array and
 * a number of tasks as arguments
 */

public class ExampleClientServer {
  /**
   * Main method that creates and runs the JavaSeis ParallelTaskExecutor with
   * master and slave tasks
   * 
   * @param args - number of slaves to use (default 8)
   * 
   */

  public static void main(String[] args) {
    int n1 = 10;
    int n2 = 50;
    int n3 = 25;
    int ntask = 5;
    float[] data = new float[n1 * n2 * n3];
    server(3, new int[] { n1, n2, n3 }, data, ntask);
  }

  /** Shared reference to an array and attributes */
  public static float[] buf;
  public static int ndim;
  public static int[] idim;

  public static void server(int numAxes, int[] axisLengths, float[] data, int ntask) {
    // Store reference to input data and size
    buf = data;
    ndim = numAxes;
    idim = axisLengths;
    // Start the parallel task executor with the simple class provided below
    try {
      ParallelTaskExecutor.runTasks(ServerTask.class, ntask);
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    // The version of the constructor above immediately starts all the tasks
    // waits for all the tasks to finish, so nothing more is needed !
  }

  /**
   * Example ParallelTask that uses an inner class (class defined only within
   * another class). Using a static inner class lets us create it without having
   * to refer to the outer class or have it in a separate file. This allows us
   * to use "shared" memory created by the invoking task as static class arrays.
   */
  public static class ServerTask extends ParallelTask {
    /**
     * Classes that implement the ParallelTask interface must provide a run
     * method. In this method we get the "communicator" from the ParallelTask
     * superclass, and then print out the task number from each task using the
     * serialPrint method from the communicator.
     */
    @Override
    public void run() {
      // Get the communicator from the superclass
      IParallelContext pc = this.getParallelContext();
      // Construct a distributed array from the shared array
      DistributedArray da = ConstructDistributedArray.fromSharedArray(pc, ndim, idim, buf, 0);
      pc.serialPrint("Yo !");
      // Get an iterator on frames
      DistributedFrameIterator fi = new DistributedFrameIterator(da);
      float[][] frame;
      // Loop over the frames we own and reverse polarity
      while (fi.hasNext()) {
        frame = fi.next();
        System.out.println("Task " + pc.rank() + " frame " + fi.getPosition()[2]);
        for (int j = 0; j < frame.length; j++) {
          for (int i = 0; i < frame[0].length; i++) {
            frame[j][i] = -frame[j][i];
          }
        }
      }
      pc.serialPrint("Done !");
    }

  }
}
