package beta.javaseis.examples.synthetic;

import beta.javaseis.plot.PlotArray2D;
import beta.javaseis.plot.PlotArray2D.PlotArray2DParms;
import beta.javaseis.plot.PlotColorModel.ColorScale;
import beta.javaseis.synthetic.SyntheticWavelet;
import beta.javaseis.synthetic.TimeDomainSyntheticServer;
import beta.javaseis.util.IntervalTimer;

/**
 * This class illustrates use of the JavaSeis convolutional modeling package:
 * beta.javaseis.synthetic.TimeDomainSyntheticServer
 * @author Chuck Mosher for JavaSeis.org
 *
 */
public class ExampleConvolutionalModeling {
	/**
	   * Example usage of a TimeDomainSyntheticServer with java arrays used for model and output.
	   * 
	   * @param args
	   *          - number of tasks to use, default 4
	   */
	  public static void main(String[] args) {
	    // Simple case with depth = time
	    int ntask = 4;
	    // Check to see if any arguments were provided
	    if (args != null && args.length > 0) {
	      // Convert the argument to an integer if it was provided
	      ntask = Integer.parseInt(args[0]);
	    }
	    int nx = 100;
	    int ny = 100;
	    int nz = 100;
	    float dx = 10f;
	    float dz = 10f;
	    float dt = 0.002f;
	    int nt = 500;
	    float[][][] vp = new float[ny][nx][nz];
	    float[][][] vs = new float[ny][nx][nz];
	    float[][][] rho = new float[ny][nx][nz];
	    float[][][] refl = new float[ny][nx][nz];
	    float[][][] trc = new float[ny][nx][nt];
	    float[][] ht0 = new float[ny][nx];
	    // Use a gradient for p wave velocity
	    float v0 = 2000;
	    float g = 0.0f;
	    float z = 0;
	    float vpi = 0;
	    for (int k = 0; k < ny; k++) {
	      for (int j = 0; j < nx; j++) {
	        ht0[k][j] = 0.1f + 0.001f * (j + k);
	        for (int i = 0; i < nz; i++) {
	          z = dz * i;
	          vpi = v0 + g * z;
	          // Add a random component to Vp
	          vp[k][j][i] = vpi + (float) (100 * (Math.random() - 0.5));
	          // Vs approximately 0.5 Vp with random component
	          vs[k][j][i] = 0.5f * vpi + (float) (50 * (Math.random() - 0.5));
	          // Density approximately Vp/1000 with random component
	          rho[k][j][i] = 0.001f * vpi + (float) (0.1 * (Math.random() - 0.5));
	          // Big density contrast at zmax/2
	          if (i > (nz / 2 - 1))
	            rho[k][j][i] += 0.5f;
	        }
	      }
	    }
	    
	    // Create a synthetic wavelet
	    float[] wavelet = SyntheticWavelet.ricker(10, dt);
	    float wt0 = dt * (wavelet.length / 2);
	    // Initialize the server with shared arrays for elastic parameters
	    TimeDomainSyntheticServer tdss = new TimeDomainSyntheticServer(3,
	        new int[] { nz, nx, ny }, 0f, vp, vs, rho, refl, nz, dz, nt, dt, ht0,
	        wavelet, wt0, trc);
	    IntervalTimer it = new IntervalTimer();
	    it.start();
	    // Calculate reflectivity and plot the center line
	    tdss.calculateReflectivity(ntask);
	    System.out.format("Reflectivity Time for %d tasks: %-8.3f\n", ntask,
	        it.stop());
	    PlotArray2DParms p2d = new PlotArray2DParms(nz, 0, dz, nx, 0, dx);
	    p2d.setColorScale(ColorScale.PLUS_MINUS);
	    p2d.setVertical();
	    p2d.setClipRange(-0.11f, 0.11f);
	    p2d.setLabels("Reflectivity at 0 deg", "Depth", "X-Axis");
	    new PlotArray2D(p2d, refl[ny / 2]).display();
	    // Calculate synthetics and plot the center line
	    it.reset();
	    it.start();
	    tdss.calculateSynthetics(ntask);
	    System.out
	        .format("Synthetic Time for %d tasks: %-8.3f\n", ntask, it.stop());
	    p2d.setSampling(nt, 0, dt, nx, 0, dx);
	    p2d.setLabels("Synthetic at 0 deg", "Time", "X-Axis");
	    new PlotArray2D(p2d, trc[ny / 2]).display();

	    // repeat for a second angle
	    it.reset();
	    it.start();
	    tdss.setAngle(30f);
	    tdss.calculateReflectivity(ntask);
	    System.out.format("Reflectivity Time for %d tasks: %-8.3f\n", ntask,
	        it.stop());
	    p2d.setSampling(nz, 0, dz, nx, 0, dx);
	    p2d.setLabels("Reflectivity at 30 deg", "Depth", "X-Axis");
	    new PlotArray2D(p2d, refl[ny / 2]).display();
	    tdss.calculateSynthetics(ntask);
	    System.out
	        .format("Synthetic Time for %d tasks: %-8.3f\n", ntask, it.stop());
	    p2d.setSampling(nt, 0, dt, nx, 0, dx);
	    p2d.setLabels("Synthetic at 30 deg", "Time", "X-Axis");
	    new PlotArray2D(p2d, trc[ny / 2]).display();
	  }
}
