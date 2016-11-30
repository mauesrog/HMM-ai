package hmm;

import java.awt.Color;

public class HMMDriver {
	public static HMMProblem mcProblem = null;
	public static Color colors[] = null;
	public static String colorNames[] = null;
	
	public static void main(String args[]) {
		colors = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW };
		colorNames = new String[] { "Red", "Green", "Blue", "Yellow" };
		
		mcProblem = new HMMProblem(1, 6, 6, 0.04, colors, colorNames);
	}
}