package org.javaseis.examples.imaging;

import java.util.Arrays;

import org.javaseis.properties.DataDomain;

import edu.mines.jtk.util.ArrayMath;
import beta.javaseis.complex.ComplexArrays;
import beta.javaseis.distributed.DistributedFrameIterator;
import beta.javaseis.fft.Fft2d;
import beta.javaseis.fft.IFFT;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.UniprocessorContext;
import beta.javaseis.plot.PlotArray2D;
import beta.javaseis.regulargrid.RegularGrid;

/**
 * Example phase shift shot record migration Illustrates usage of the
 * RegularGrid class and the JavaSeis IParallelContext interface
 * 
 * @author Chuck Mosher for JavaSeis.org
 */
public class ShotMigPhaseShift {
  /**
   * Parallel context interface - can be uniprocessor, threaded, MPI, or
   * MPI+threads (Hybrid)
   */
  IParallelContext pc;
  /** Velocities for imaging */
  float[] vels;
  /**
   * Regular grids for the input recorded data, modeled shot, and the output
   * image
   */
  RegularGrid shot, rcvr, image;
  /** image x,y size */
  int nx, ny;
  /** Maximum frequency index */
  int nf;
  /** Maximum depth */
  int nz;
  /** Depth increment */
  double dz;
  /** 2D FFT Operator */
  Fft2d f2d;
  /** Iterators for source and receiver wavefields */
  DistributedFrameIterator sdfi, rdfi;
  /** Work arrays for source and receiver wavefields and image accumulation */
  float[][] sc, rc, img, ps;
  /** Fourier Transform lengths */
  int nkx, nky;
  /** Nyquist samples */
  int nqx, nqy;
  /** FFT sampling */
  double dw, dkx, dky;

  /**
   * Initialize a Phase Shift shot migration
   * 
   * @param maxFreqIndex - Index of the maximum frequency to be imaged
   * @param maxDepthIndex - Number of depth steps
   * @param depthStep - Depth step size
   * @param recordedData - RegularGrid containing recorded data in X,Y,Frequency
   *          domain
   * @param initialSource - RegularGrid containing source at Z=0 in the
   *          X,Y,Frequency domain
   * @param outputImage - Output RegularGrid for the image in X,Y,Z domain
   * @param velocity - Interval velocity model for imaging
   */
  public ShotMigPhaseShift(float padx, float pady, int maxFreqIndex,
      int maxDepthIndex, float depthStep, RegularGrid recordedData,
      RegularGrid initialSource, RegularGrid outputImage, float[] velocity) {
    // Check a few things for consistency
    // if (recordedData.getNumDimensions() != 3 ||
    // recordedData.getGridDefinition().getAxisDomain(2) !=
    // DataDomain.FREQUENCY)
    // throw new
    // IllegalArgumentException("Input recordedData must be (X,Y,F) domain");
    if (Arrays.equals(recordedData.getLengths(), initialSource.getLengths()) == false) {
      throw new IllegalArgumentException(
          "Input recordedData and initialSource must be conformable");
    }
    // Store references to input parameters
    nf = maxFreqIndex;
    nz = maxDepthIndex;
    dz = depthStep;
    rcvr = recordedData;
    shot = initialSource;
    image = outputImage;
    vels = velocity;
    // Save the parallel context packaged with the input data
    pc = recordedData.getGrid().getParallelContext();
    // Initialize 2D FFT
    nx = rcvr.getGrid().getLength(0)/2;
    ny = rcvr.getGrid().getLength(1);
    f2d = new Fft2d(nx, ny, padx, pady, IFFT.Type.COMPLEX, IFFT.Type.COMPLEX,
        -1, -1, IFFT.Scale.SYMMETRIC, IFFT.Scale.SYMMETRIC);
    nkx = f2d.getLength(0);
    nky = f2d.getLength(1);
    nqx = nkx / 2;
    nqy = nky / 2;
    double[] deltas = rcvr.getDeltas();
    dkx = 2 * Math.PI / (nkx * deltas[0]);
    dky = 2 * Math.PI / (nky * deltas[1]);
    dw = 2 * Math.PI * deltas[2];
    // Create work arrays for FFT's, shifts, and image
    // Complex arrays require two elements per sample
    sc = new float[nky][2 * nkx];
    rc = new float[nky][2 * nkx];
    ps = new float[nky][nkx]; // Depth shift is real
    img = new float[ny][nx]; // Image is real
    // Initialize iterators for receiver and source grids
    rdfi = new DistributedFrameIterator(rcvr.getGrid());
    sdfi = new DistributedFrameIterator(shot.getGrid());
  }

  /**
   * Initialize the source and receiver wavefields
   */
  public void initializeWaveFields() {
    // Forward transform source and receiver and band limit to range defined by
    // initial velocity
    // Reset iterators to beginning
    rdfi.reset();
    sdfi.reset();
    float[][] sxy, rxy;
    float omega;
    // Loop over frequencies
    while (sdfi.hasNext()) {
      // Initialize work arrays to zero
      ArrayMath.fill(0, sc);
      ArrayMath.fill(0, rc);
      // Get the source and receiver arrays for this frequency
      sxy = sdfi.next();
      rxy = rdfi.next();
      // Forward transform
      f2d.forwardComplex(sxy, sc);
      f2d.forwardComplex(rxy, rc);
      // Calculate depth shift
      int ifreq = sdfi.getPosition()[2];
      applyPhaseShift(ifreq, vels[0], 0f, sc, rc);
      // inverse transform
      f2d.inverseComplex(sc, sxy);
      f2d.inverseComplex(rc, rxy);
    }
  }

  /**
   * Compute the sample shift for a given frequency, velocity and depth step
   * 
   * @param ifreq - input frequency index
   * @param v - input velocity
   * @param depthStep - input depth step
   * @param s - output depth shift in samples
   */
  public void computeDepthShift(int ifreq, float v, float depthStep, float[][] s) {
    final double EPS = 1e-12;
    // Wavenumber values
    double kx, ky, kx2, ky2, kz2;
    // K vector magnitude
    double omega = dw * ifreq;
    double wv2 = (omega * omega) / (v * v);
    for (int j = 0; j < nky; j++) {
      ky = dky * getKindex(nqy, nky, j);
      ky2 = ky * ky;
      for (int i = 0; i < nkx; i++) {
        kx = dkx * getKindex(nqx, nky, i);
        kx2 = kx * kx;
        kz2 = wv2 - kx2 - ky2;
        if (kz2 > EPS) {
          s[j][i] = (float) (depthStep * Math.sqrt(kz2));
        } else {
          s[j][i] = 0;
        }
      }
    }
  }

  /**
   * Apply phase shift operator at a given frequency, velocity and depth step
   * 
   * @param ifreq - input angular frequency
   * @param v - input velocity
   * @param depthStep - input depth step
   * @param shot - Complex wavefield array for source signature in kx and ky
   * @param rcvr - Complex wavefield array for receiver data in kx and ky
   */
  public void applyPhaseShift(int ifreq, float v, float depthStep, float[][] shot, float[][] rcvr ) {
    final double EPS = 1e-12;
    // Wavenumber values
    double kx2, ky2, kz2, shift;
    // K vector magnitude
    double omega = dw*ifreq;
    double wv2 = (omega * omega) / (v * v);
    // Wavenumber delta values squared
    double dkx2 = dkx*dkx;
    double dky2 = dky*dky;
    // Signed wavenumber index values
    int kx, ky;
    // Outer loop over Ky axis
    for (int j = 0; j < nky; j++) {
      // Get signed wavenumber index for Y
      ky = (j <= nqy ? j : j-nky);
      ky2 = dky2 * ky * ky;
      // Inner loop over Kx axis
      for (int i = 0; i < nkx; i++) {
        // Signed wavenumber index for X
        kx = (i <= nqx ? i : i-nkx);
        kx2 = dkx2* kx * kx;
        // Vertical wavenumber squared
        kz2 = wv2 - kx2 - ky2;
        // Apply shift if not evanescent
        if (kz2 > EPS) {
          shift = depthStep * Math.sqrt(kz2);
          ComplexArrays.cshift(shot[j], i, shift );
          ComplexArrays.cshift(rcvr[j], i, -shift );
        } else {
          // Zero evanescent values
          ComplexArrays.eq(shot[j], i, 0f, 0f);
          ComplexArrays.eq(rcvr[j], i, 0f, 0f);
        }
      }
    }
  }




  /**
   * Return signed symmetric wavenumber index
   * 
   * @param nyq - nyquist sample
   * @param nft - number of time/frequency samples
   * @param i - input index from 0 to nfft-1
   * @return signed index from 1-nyq to nyq
   */
  public static final int getKindex(int nyq, int nft, int i) {
    return (i <= nyq ? i : i-nft);
  }

  /**
   * Optimized phase shift application (not tested ;-) Exploits quadrant
   * symmetry of phase shift values to reduce sqrt and sin/cos computations
   * 
   * @param omega - angular frequency in radians/sec
   * @param v - input velocity
   * @param depthStep - depth step size
   * @param p - input/output 2D wavefield array of size (nky) x (2*nkx)
   */
  public void applyPhaseSymmetric(float omega, float v, float depthStep,
      float[][] p) {
    final double EPS = 1e-12;
    // Wavenumber values
    double kx, ky, kx2, ky2, kz2, shift;
    // K vector magnitude
    double wv2 = (omega * omega) / (v * v);

    // Handle ky=0 as a separate case
    ComplexArrays.cshift(p[0], 0, depthStep * Math.sqrt(wv2));
    int im = nkx - 2;
    for (int i = 1; i <= nqx - 1; i++, im--) {
      kx = dkx * i;
      kx2 = kx * kx;
      kz2 = wv2 - kx2;
      if (kz2 > EPS) {
        shift = depthStep * Math.sqrt(kz2);
        ComplexArrays.cshift(p[0], i, shift);
        ComplexArrays.cshift(p[0], im, shift);
      } else {
        p[0][i] = p[0][im] = 0;
      }
    }
    kx = dkx * nqx;
    kx2 = kx * kx;
    kz2 = wv2 - kx2;
    if (kz2 > EPS) {
      ComplexArrays.cshift(p[0], nqx, depthStep * Math.sqrt(wv2));
    } else {
      p[0][nqx] = 0;
    }

    // Loop over symmetric ky range
    int jm = nky - 2;
    for (int j = 1; j <= nqy - 1; j++, jm--) {
      ky = dky * j;
      ky2 = ky * ky;
      // Handle zero kx
      kz2 = wv2 - ky2;
      if (kz2 > EPS) {
        // Values are not evanescent
        ComplexArrays.cshift(p[j], 0, depthStep * Math.sqrt(wv2));
      } else {
        // Set evanescent values to zero
        Arrays.fill(p[j], 0f);
        Arrays.fill(p[jm], 0f);
        continue;
      }
      // Loop over symmetric kx range
      im = nkx - 2;
      for (int i = 1; i <= nqx - 1; i++, im--) {
        kx = dkx * i;
        kx2 = kx * kx;
        kz2 = wv2 - kx2 - ky2;
        if (kz2 > EPS) {
          shift = depthStep * Math.sqrt(kz2);
          ComplexArrays.cshift(p[j], i, shift);
          ComplexArrays.cshift(p[j], im, shift);
          ComplexArrays.cshift(p[jm], i, shift);
          ComplexArrays.cshift(p[jm], im, shift);
        } else {
          p[j][i] = p[jm][i] = p[j][im] = p[jm][im] = 0;
        }
      }
      // Now handle Nyquist kx
      kx = dkx * nqx;
      kx2 = kx * kx;
      kz2 = wv2 - kx2 - ky2;
      if (kz2 > EPS) {
        shift = depthStep * Math.sqrt(kz2);
        ComplexArrays.cshift(p[j], nqx, shift);
        ComplexArrays.cshift(p[jm], nqx, shift);
      } else {
        p[j][nqx] = p[jm][nqx] = 0;
      }
    }

    // Separate case for ky=nyquist

    ky = dky * nqy;
    ky2 = kx * kx;
    kz2 = wv2 - ky2;
    if (kz2 > EPS) {
      ComplexArrays.cshift(p[nqy], 0, depthStep * Math.sqrt(wv2));
    } else {
      Arrays.fill(p[nqy], 0);
      return;
    }
    im = nkx - 2;
    for (int i = 1; i <= nqx - 1; i++, im--) {
      kx = dkx * i;
      kx2 = kx * kx;
      kz2 = wv2 - kx2 - ky2;
      if (kz2 > EPS) {
        shift = depthStep * Math.sqrt(kz2);
        ComplexArrays.cshift(p[nqy], i, shift);
        ComplexArrays.cshift(p[nqy], im, shift);
      } else {
        p[nqy][i] = p[nqy][im] = 0;
      }
    }
    kx = dkx * nqx;
    kx2 = kx * kx;
    kz2 = wv2 - kx2 - ky2;
    if (kz2 > EPS) {
      ComplexArrays.cshift(p[0], nqx, depthStep * Math.sqrt(wv2));
    } else {
      p[0][nqx] = 0;
    }
  }

  /**
   * Debug test harness
   * 
   * @param args
   */
  public static void main(String[] args) {
    int nx = 32;
    int ny = 32;
    int nt = 128;
    double dx = 10;
    double dy = 10;
    double dt = 0.01;
    int nf = 33;
    double df = 1 / (dt * nt);
    int nz = 4;
    float dz = 10;
    int[] shape = new int[] { 2*nx, ny, nf };
    double[] deltas = new double[] { dx, dy, df };
    IParallelContext pc = new UniprocessorContext();
    RegularGrid shot = new RegularGrid(shape, deltas, pc);
    RegularGrid rcvr = new RegularGrid(shape, deltas, pc);
    int[] zshape = new int[] { nx, ny, nz };
    double[] zdelta = new double[] { dx, dy, dz };
    RegularGrid image = new RegularGrid(zshape, zdelta, pc);
    float[] vels = new float[nz];
    Arrays.fill(vels, 1000f);
    ShotMigPhaseShift smps = new ShotMigPhaseShift(0, 0, nf, nz, dz, rcvr,
        shot, image, vels);
    float[][] ds = new float[ny][nx];
    for (int i = 1; i <= nf; i+=8) {
      smps.computeDepthShift(i, 1000, 100, ds);
      PlotArray2D p2d = new PlotArray2D(ds);
      p2d.display();
      p2d.next();
    }
  }
}
