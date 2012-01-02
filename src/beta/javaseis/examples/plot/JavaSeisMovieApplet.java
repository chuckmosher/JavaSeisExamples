package beta.javaseis.examples.plot;

import javax.swing.JApplet;

import beta.javaseis.plot.IMovieSource;
import beta.javaseis.plot.JavaSeisMovie;
import beta.javaseis.plot.RandomMovieSource;

public class JavaSeisMovieApplet extends JApplet {
	private static final long serialVersionUID = 1L;

	/**
	 * Create the applet.
	 */
	public JavaSeisMovieApplet() {
	    IMovieSource source = new RandomMovieSource(new int[] { 100, 100, 100 });
	    JavaSeisMovie.display(source);
	}

}
