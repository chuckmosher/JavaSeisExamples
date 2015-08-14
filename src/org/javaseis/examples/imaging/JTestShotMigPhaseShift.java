package org.javaseis.examples.imaging;

import static org.junit.Assert.*;

import org.junit.Test;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.UniprocessorContext;
import beta.javaseis.regulargrid.RegularGrid;

public class JTestShotMigPhaseShift {

  @Test
  public void test() {
    int nx = 32;
    int ny = 30;
    int nf = 4;
    double dx = 25;
    double dy = 50;
    double df = 10;
    int nz = 4;
    double dz = 10;
    int[] shape = new int[] { nf, ny, 2 * nx };
    double[] deltas = new double[] { df, dy, dx };
    IParallelContext pc = new UniprocessorContext();
    RegularGrid shot = new RegularGrid(shape, deltas, pc);
    RegularGrid rcvr = new RegularGrid(shape, deltas, pc);
    int[] zshape = new int[] { nz, nx, ny };
    double[] zdelta = new double[] { dz, dx, dy };
    RegularGrid image = new RegularGrid(zshape, zdelta, pc);

  }

}
