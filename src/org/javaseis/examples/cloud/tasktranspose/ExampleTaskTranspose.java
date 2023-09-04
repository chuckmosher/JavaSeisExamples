package org.javaseis.examples.cloud.tasktranspose;

import java.util.concurrent.ExecutionException;

import org.javaseis.array.ElementType;

import beta.javaseis.array.TransposeType;
import beta.javaseis.array.IMultiArray;
import beta.javaseis.array.MultiArray;
import beta.javaseis.distributed.Decomposition;
import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelTask;
import beta.javaseis.parallel.ParallelTaskExecutor;
import beta.javaseis.parallel.UniprocessorContext;


/**
 * Example showing usage of the task based transpose tools to transpose volumes
 * in a 3D dataset
 */
public class ExampleTaskTranspose {
  
  public static void main(String[] args) {
    
  }

  int n0, n1, n2, p, p1, p2, n1p, n2p;
  int[] shape, tshape;
  IMultiArray ma;
  
  public ExampleTaskTranspose( int[] shapeIn, int tileSize ) {
      shape = shapeIn.clone();
      p = p1 = p2 = tileSize;
      int[] dtypes = new int[] { Decomposition.NONE, Decomposition.BLOCK, Decomposition.BLOCK };
      tshape = getTransposeShape(p, 3, shape, dtypes );
      n0 = tshape[0];
      n1 = tshape[1];
      n2 = tshape[2];
      n1p = n1/p;
      n2p = n2/p;
      ma = MultiArray.float3D(n0, p, n1p);
    }
  
  /**
   * Read the 
   * @param path
   */
  public void read(String path) {
    
  }
  
  public static int[] getTransposeShape(int size, int ndim,
      int[] lengths, int[] decompTypes) {
    int[] padShape = new int[ndim];
    for (int i = 0; i < ndim; i++) {
      if (decompTypes[i] != Decomposition.NONE)
        padShape[i] = (int) Decomposition.paddedLength(lengths[i], size);
      else
        padShape[i] = lengths[i];
    }
    return padShape;
  }
  }


