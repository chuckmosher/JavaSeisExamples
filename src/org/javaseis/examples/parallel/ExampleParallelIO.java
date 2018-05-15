package org.javaseis.examples.parallel;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.javaseis.processing.framework.FileSystemIOService;
import org.javaseis.processing.framework.IDistributedIOService;
import org.javaseis.util.SeisException;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;

/**
 * This example illustrates basic parallel I/O with JavaSeis. The example starts
 * a parallel task and creates a cube of random numbers. The cube is written to
 * disk in parallel, and then read back and verified.
 * 
 */
public class ExampleParallelIO {

  public static void main(String[] args) {
    int ntask = 4;
    try {
      ParallelTaskExecutor.runTasks(ParallelIOTask.class, ntask);
    } catch (ExecutionException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static class ParallelIOTask extends ParallelTask {
    @Override
    public void run() {
      IParallelContext pc = this.getParallelContext();
      String tmpdir = System.getProperty("java.io.tmpdir");
      pc.serialPrint("Create FileSystemIOService using " + tmpdir);
      IDistributedIOService pio;
      try {
        pio = new FileSystemIOService(pc, tmpdir);
      } catch (SeisException e) {
        throw new RuntimeException(e.getCause());
      }
      int[] testShape = new int[] { 201, 201, 201, 9, 5 };
      try {
        pio.create("temp.js", testShape);
        pio.open("temp.js");
      } catch (SeisException ex) {
        throw new RuntimeException(ex);
      }
      int[] fileShape = pio.getFileShape();
      int[] shape = new int[] { fileShape[0], fileShape[1], fileShape[2] };
      DistributedArray da = new DistributedArray(pc, shape);
      Random r = new Random(12345l);
      float[] trc = new float[shape[0]];
      pio.setDataArray(da);
      pio.reset();
      int ivol = 0;
      while (pio.hasNext()) {
        pio.next();
        float val = pc.rank() + ivol++;
        da.resetTraceIterator();
        while (da.hasNext()) {
          da.next();
          da.getTrace(trc);
          for (int i = 0; i < shape[0]; i++) {
            trc[i] = r.nextFloat() + val;
          }
          da.putTrace(trc);
        }
        try {
          pc.serialPrint("Write filePosition " + Arrays.toString(pio.getFilePosition()));
          pio.write();
        } catch (SeisException ex) {
          throw new RuntimeException(ex);
        }
      }

      try {
        pio.close();
        pio = null;
        System.gc();
        pio = new FileSystemIOService(pc, tmpdir);
        pio.open("temp.js");
      } catch (SeisException ex) {
        throw new RuntimeException(ex);
      }
      r = new Random(12345l);
      pio.setDataArray(da);
      pio.reset();
      ivol = 0;
      while (pio.hasNext()) {
        pio.next();
        try {
          pc.serialPrint("Read filePosition " + Arrays.toString(pio.getFilePosition()));
          pio.read();
        } catch (SeisException ex) {
          throw new RuntimeException(ex);
        }
        float val = pc.rank() + ivol++;
        da.resetTraceIterator();
        while (da.hasNext()) {
          da.next();
          da.getTrace(trc);
          for (int i = 0; i < shape[0]; i++) {
            float expected = r.nextFloat() + val;
            if (trc[i] != expected) {
              throw new RuntimeException("Failed at file position " + Arrays.toString(pio.getFilePosition())
                  + " array position " + Arrays.toString(da.getPosition()) + ": Expected value " + expected
                  + " actual value " + trc[i] + " at sample " + i);
            }
          }
        }
      }
      try {
        pio.close();
        //pio.delete("temp.js");
      } catch (SeisException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
