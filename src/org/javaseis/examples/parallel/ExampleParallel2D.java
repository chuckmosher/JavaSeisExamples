package org.javaseis.examples.parallel;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.javaseis.array.ElementType;

import beta.javaseis.array.TransposeType;
import beta.javaseis.distributed.Decomposition;
import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;


/**
 * Example showing how to use a 3D array to support a 2D distributed array.
 * 
 */
public class ExampleParallel2D {
  
  public static void main(String[] args) {
    // Default number of tasks to one
    int ntask = 2;
    // Check to see if any arguments were provided
    if (args != null && args.length > 0) {
      // Convert the argument to an integer if it was provided
      ntask = Integer.parseInt(args[0]);
    }
    // Start the parallel task executor with the simple class provided below
    try {
      ParallelTaskExecutor.runTasks(Parallel2DTask.class, ntask);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

 
  public static class Parallel2DTask extends ParallelTask {
    @Override
    public void run() {
      // Get the communicator from the superclass
      IParallelContext pc = this.getParallelContext();
      // Use the serialPrint method to output the rank of this task
      pc.serialPrint("Yo from task " + pc.rank());
      int[] shape = new int[] { 1, 113, 255 };
      int[] dtypes = new int[] { Decomposition.NONE, Decomposition.BLOCK, Decomposition.BLOCK };
      int[] tshape = DistributedArray.getTransposeShape(pc, 3, shape, dtypes );
      //int[] tshape = shape.clone();
      DistributedArray da3d = new DistributedArray(pc, ElementType.FLOAT, tshape, dtypes );
      int n0 = tshape[0];
      int n1 = tshape[1];
      int n2 = tshape[2];
      int[] pos = new int[3];
      float[] trc = new float[tshape[0]];
      da3d.resetTraceIterator();
      int i,j;
      float val;
      while (da3d.hasNext()) {
        da3d.next();
        pos = da3d.getPosition();
        j = pos[2];
        i = pos[1];
        val = (float)(n1*j + i);
        trc[0] = val;
        da3d.putTrace(trc);
      }
      da3d.transpose(TransposeType.T132);
      da3d.resetTraceIterator();
      while (da3d.hasNext()) {
        da3d.next();
        pos = da3d.getPosition();
        i = pos[2];
        j = pos[1];
        val = (float)(n1*j + i);
        da3d.getTrace(trc);
        assertEquals("Match failed at i,j " + i + "," + j,val,trc[0],1e-6);
      }
      pc.serialPrint("Transpose Test Completed");
    }
  }

}
