package org.javaseis.examples.parallel;

import java.util.concurrent.ExecutionException;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;


/**
 * Example showing how to use the JavaSeis parallel thread model in a master-slave
 * context. The top level class has a main method that expects the number of tasks 
 * to be used supplied as the first argument to the program. If there are no arguments, 
 * one task is used. A static inner class is used to provide the worker task.
 * 
 */
public class ExampleMasterSlave {
  /**
   * Main method that creates and runs the JavaSeis ParallelTaskExecutor with
   * master and slave tasks
   * 
   * @param args - number of slaves to use (default 7)
   *          
   */
  
  public static float[] data;
  
  public static void main(String[] args) {
    // Default number of slaves to one
    int nslave = 7;
    // Check to see if any arguments were provided
    if (args != null && args.length > 0) {
      // Convert the argument to an integer if it was provided
      nslave = Integer.parseInt(args[0]);
    }
    int ntask = nslave + 1;
    // Array containing class of each task
    Class<?>[] pt = new Class[ntask];
    // Set task 0 to be the master
    pt[0] = MasterTask.class;
    data = new float[ntask];
    // The rest are workers
    for (int i=1; i<ntask; i++) {
      pt[i] = SlaveTask.class;
      data[i] = i;
    }
    // Start the parallel task executor with the simple class provided below
    try {
      ParallelTaskExecutor.runTasks(pt);
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    // The version of the constructor above immediately starts all the tasks
    // waits for all the tasks to finish, so nothing more is needed !
  }

  /**
   * Example ParallelTask that uses an inner class (class defined only within
   * another class). Using a static inner class lets us create it without having
   * to refer to the outer class or have it in a separate file. This one gets
   * the parallel context from the ParallelTask superclass, and then prints out
   * the task number.
   */
  public static class SlaveTask extends ParallelTask {
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
      // Use the serialPrint method to output the rank of this task
      pc.serialPrint("Yes, Master ! My shared data = " + data[pc.rank()]);
    }

  }

  /**
   * Example ParallelTask that uses an inner class (class defined only within
   * another class). Using a static inner class lets us create it without having
   * to refer to the outer class or have it in a separate file. This one gets
   * the parallel context from the ParallelTask superclass, and then prints out
   * the task number.
   */
  public static class MasterTask extends ParallelTask {
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
      // Use the serialPrint method to output the rank of this task
      pc.serialPrint("Answer, Slaves ! My shared data = " + data[pc.rank()]);
    }

  }
}
