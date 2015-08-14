package org.javaseis.examples.parallel;

import java.util.concurrent.ExecutionException;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

/**
 * Example showing how the JavaSeis parallel thread model is used. The top level
 * class has a main method that expects the number of tasks to be used supplied
 * as the first argument to the program. If there are no arguments, one task is
 * used. A static inner class is used to provide the worker task.
 * 
 */
public class ExampleParallelTask {
  /**
   * Main method that creates and runs the JavaSeis ParallelTaskExecutor with
   * simple worker task that prints out the task number from each active thread.
   * 
   * @param args number of tasks to use as the first program argument (default
   *          2).
   */
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
      ParallelTaskExecutor.runTasks(SimpleParallelTask.class, ntask);
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
  public static class SimpleParallelTask extends ParallelTask {
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
      pc.serialPrint("Yo from task " + pc.rank());
    }

  }
}
