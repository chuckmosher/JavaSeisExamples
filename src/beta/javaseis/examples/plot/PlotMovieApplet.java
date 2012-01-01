package beta.javaseis.examples.plot;

import javax.swing.JApplet;

import beta.javaseis.plot.IMovieSource;
import beta.javaseis.plot.PlotMovie;
import beta.javaseis.plot.RandomMovieSource;

public class PlotMovieApplet extends JApplet {
	private static final long serialVersionUID = 1L;

	/**
	 * Create the applet.
	 */
	public PlotMovieApplet() {
	    IMovieSource source = new RandomMovieSource(new int[] { 100, 100, 100 });
	    PlotMovie.display(source);
	}

}
