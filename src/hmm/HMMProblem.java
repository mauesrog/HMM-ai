package hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import datastructures.Matrix;

public class HMMProblem {
/******************************** CONSTANTS ***********************************/
	//
	
/*************************** INSTANCE VARIABLES *******************************/
	// PUBLIC
		//
	
	// PRIVATE	
	private int timestep, totalRobots, mazeWidth, mazeHeight, colorCount[],
			mazeWalls[][], mazeColors[][];
	private ArrayList<Integer> percepts;
	private Double error, maxProb;
	private ArrayList<Matrix> forwardMessage;
	private Color possibleColors[];
	private String colorNames[];
	private ArrayList<RobotNode> robots;
	private Matrix transitionModel;
	private static JPanel mazePanel;
	private static JFrame mazeFrame;
	private RobotNode startNode;
	
/***************************** INNER CLASSES **********************************/
	private class RobotNode {
	//----------------------INSTANCE VARIABLES--------------------------------//
		// PUBLIC
			//
		
		// PROTECTED
		protected int state[], x0, y0;
		protected Double ratio; 

	//--------------------------- CONSTRUCTOR --------------------------------//
		public RobotNode(int x, int y, Double r) {
			state = new int[2];
			this.state[0] = x;
			this.state[1] = y;
			ratio = r;
			
			x0 = x;
			y0 = y;
		}
		
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ equals() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		@Override
		public boolean equals(Object other) {
			return Arrays.equals(state, ((RobotNode) other).state);
		}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ toString() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		@Override
		public String toString() {
			return "(" + this.state[0] + "," + this.state[1] + ")";
		}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~ updateStateNode() ~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		public void updateStateNode(final int moveId) {
			int x, y, offsetX = 0, offsetY = 0;
			
			switch(moveId) {
				case 1:
					offsetY++;
					break;
				case 2:
					offsetY--;
					break;
				case 3:
					offsetX++;
					break;
				case 4:
					offsetX--;
					break;
			}
			
			x = this.state[0] + offsetX;
			y = this.state[1] + offsetY;
			
			if (isSafeMove(new int[]{ x, y })) {
				this.state[0] = x;
				this.state[1] = y;
			}	
		}
		
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~ getPercept() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	public int getPercept() {
		int errorInt = (int) Math.round(error * 100), 
			remainingColors = possibleColors.length - 1,
			success = 100 - errorInt * remainingColors,
			realColor = mazeColors[this.state[0]][this.state[1]+1],
			offset, distrib[] = new int[100], x;
		
		ArrayList<Integer> totalColors = new ArrayList<Integer>(100);
		
		for (int i = 0; i < 100; i++) {
			if (i < success) {
				offset = 0;
			} else {
				offset = Math.floorDiv(i - success, errorInt) + 1;
			}
			
			totalColors.add((realColor + offset) % possibleColors.length);
		}
		
		for (int i = 0; i < 100; i++) {
			x = ThreadLocalRandom.current().nextInt(0, totalColors.size());
			distrib[i] = totalColors.get(x);
			totalColors.remove(x);
		}
		
		return distrib[ThreadLocalRandom.current().nextInt(0, 99)];
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ restoreInitialState() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	public void restoreInitialState() {
		this.state[0] = x0;
		this.state[1] = y0;
	}
		
	//------------------------- PRIVATE METHODS ------------------------------//
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ isSafeMode() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		private boolean isSafeMove(int[] successor) {
			return 0 <= successor[0] && successor[0] < mazeWidth &&
					0 <= successor[1] && successor[1] < mazeHeight &&
					(mazeWalls == null || mazeWalls[successor[0]][successor[1]] == 1);
		}
	}
	

	private class Maze extends JComponent implements ActionListener {
	//----------------------------- CONSTANTS --------------------------------//
		// PUBLIC
			//
		
		// PRIVATE
		private static final long serialVersionUID = 1L;
		
	//----------------------- INSTANCE VARIABLES -----------------------------//
		// PUBLIC
			//
		
		// PRIVATE
		private int ratio;    // ratio of real width/height to coordinate-wise
							  		// width/height
		private int borders;  // width of borders
		private JButton button, smoothButton, viterbiButton;
		private boolean started;
		private Matrix smoothDistribution, backwardMessage[];
		private Timer reconstructSteps, vitrebiSteps;
		private int t, vitrebiPath[];
		private ArrayList<Integer> moves;
		
	//--------------------------- INNER CLASSES ------------------------------//
		
	//--------------------------- CONSTRUCTOR --------------------------------//	
		private Maze(Dimension rs) {		      
			ratio = rs.width > rs.height ? rs.width / mazeWidth :
					rs.height / mazeHeight;
			borders = 40;
			button = new JButton("Start");
			smoothButton = new JButton("Smooth");
			viterbiButton = new JButton("<");
			
			button.addActionListener(this);
			button.setActionCommand("timestep");
			
			smoothDistribution = null;
			vitrebiPath = null;
			t = -1;
			
			started = false;
			smoothButton.addActionListener(this);
			smoothButton.setEnabled(false);
			smoothButton.setActionCommand("smooth");
			
			viterbiButton.addActionListener(this);
			viterbiButton.setEnabled(false);
			viterbiButton.setActionCommand("viterbi");
			
			moves = new ArrayList<Integer>();
		}
		
	//----------------------------- OVERRIDES --------------------------------//
	//~~~~~~~~~~~~~~~~~~~~~~~~~~ paintComponent() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		@Override
		public void paintComponent(Graphics g) {
			 Graphics2D g2d = (Graphics2D) g;
		        g2d.setRenderingHint(
		            RenderingHints.KEY_ANTIALIASING,
			
		            RenderingHints.VALUE_ANTIALIAS_ON);
			
			int x, y, colorCounter[] = new int[possibleColors.length];
			Matrix distrib;
			
			for (int i = 0; i < colorCounter.length; i++) {
				colorCounter[i] = 0;
			}
			
			// draw the maze's Cartesian plane vertical lines in black
			g.setColor(Color.BLACK);
			for (int w = 0; w <= mazeWidth; w++) {				
				if (w < mazeWidth) {
					for (int h = 0; h < mazeHeight; h++) {
						x = w;
						y = h + 1;
						// draw obstacles in black as well
						if (mazeWalls != null && mazeWalls[w][h] == -1) {
							drawObstacle(x, y, g, true);
						} else {
							distrib = this.smoothDistribution == null || t < 0 ? forwardMessage.get(forwardMessage.size() - 1) : this.smoothDistribution;
							drawColoredSquare(x, y, g, possibleColors[mazeColors[x][y]], distrib);
							colorCounter[mazeColors[x][y]]++;
						}
					}
				}
				
			}
			
			// draw robots in red
			g.setColor(Color.white);
			for (int i = 0; i < robots.size(); i++) {
				drawBot(robots.get(i), g);
			}
			
			colorCount = colorCounter;
			displayStats(g);
		}
		
	//------------------------- PRIVATE METHODS ------------------------------//
		private void displayStats(Graphics g) {
			int displayX = mazeWidth * this.ratio + this.borders + 40, displayY = this.borders, 
					displayWidth = (mazeFrame.getWidth() - 40) - displayX, 
					displayHeight = mazeHeight * this.ratio, x, y;	
			
			Matrix probs = forwardMessage.get(t > -1 ? timestep + 1 : timestep);
			Double prob, smoothProb;
			String smoothProbString; 
			
			((Graphics2D ) g).setStroke(new BasicStroke(1));
			g.drawRect(displayX, displayY, displayWidth, displayHeight);
			
			x = displayX + 10;
			y = displayY + 25;
			
			g.setFont(new Font("Times New Roman", Font.BOLD, 20));
			g.drawString("Color distribution:", x, y);
			
			button.setBounds(new Rectangle(x + 200, y, 100, 40));
			button.setLayout(null);
			this.add(button);
			
			smoothButton.setBounds(new Rectangle(x + 200, y + 85, 100, 40));
			smoothButton.setLayout(null);
			this.add(smoothButton);
			
			viterbiButton.setBounds(new Rectangle(x + 300, y, 30, 40));
			viterbiButton.setLayout(null);
			this.add(viterbiButton);
			
			g.drawString("Timestep:" + (started ? " " + timestep : ""), x + 200, y + 75);
			
			for (int color = 0; color < colorCount.length; color++) {				
				g.drawString("P("+translatePercept(color)+") = " + Math.round(((double) colorCount[color] 
						/ (mazeHeight * mazeWidth)) * 1000.0) / 1000.0, x + 15, (y += 25));
			}
			
			g.drawString("Last percept:", x, (y += 20));
			
			if (percepts.size() > 0) {
				g.setColor(possibleColors[percepts.get(timestep)]);
				g.drawString(translatePercept(percepts.get(timestep)), x + 120, y);
				g.setColor(Color.black);
			}
			
			g.drawLine(displayX, (y += 20), displayWidth + displayX, y);
			
			g.setFont(g.getFont().deriveFont(Font.PLAIN, 16));
			
			for (int w = 0; w < mazeWidth; w++) {
				for (int h = 0; h < mazeHeight; h++) {
					prob = Math.round(probs.entry(0, mapToInteger(w, h)) * 100) / 100.0;
					
					smoothProb = this.smoothDistribution != null && t > -1 ? Math.round(this.smoothDistribution.entry(0, mapToInteger(w, h)) * 100) / 100.0 : null;
					
					smoothProbString = smoothProb == null ? "" : "\tvs\t" + smoothProb; 
					
					g.drawString("P(" + w + "," + h + ") = " + prob + smoothProbString, x, (y += 20));
				}
			}
		}
		
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ drawObstacle() ~~~~~~~~~~~~~~~~~~~~~~~~~~~//
		private void drawObstacle(int x1, int y1, Graphics g, boolean fill) {
			int x = x1 * this.ratio + this.borders, 
					y = mazeHeight * this.ratio - y1 * this.ratio + this.borders;
			
			int offset = 3;
			
			if (fill) {
				g.fillRect(x + offset, y - offset, this.ratio - offset, this.ratio - offset);
			} else {
				g.drawRect(x+ offset, y - offset, this.ratio - offset, this.ratio - offset);
			}
		}
		
		
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ drawColoredSquare() ~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	private void drawColoredSquare(int x1, int y1, Graphics g, Color color, Matrix distrib) {
		Color prevColor = g.getColor(), outline;
		
		g.setColor(color);
		
		drawObstacle(x1, y1, g, true);
		
		Stroke s = ((Graphics2D ) g).getStroke(); 
			
		((Graphics2D ) g).setStroke(new BasicStroke(3));
		
		if (this.vitrebiPath != null && t > -1) {
			outline = this.vitrebiPath[t] == mapToInteger(x1, y1 - 1) ? Color.BLACK : Color.WHITE; 
		} else {
			outline = choropleth(distrib.entry(0, mapToInteger(x1, y1 - 1))); 
		}
		
		
		g.setColor(outline);
		
		drawObstacle(x1, y1, g, false);
			
		((Graphics2D ) g).setStroke(s);
		
		g.setColor(prevColor);
	}
		
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ drawBot() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//	
		private void drawBot(RobotNode bot, Graphics g2) {
			Graphics2D g = (Graphics2D) g2;
			int x = this.ratio * bot.state[0] + 40 + this.ratio / 4, 
				y = this.ratio * (mazeHeight - 1) - this.ratio * bot.state[1] +
						40 + this.ratio / 4;
			int r = (int) ((((double) this.ratio) / 2.0) * bot.ratio);
			
			g.setColor(Color.WHITE);
			g.fillOval(x, y, r, r);
			
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(2));
			
			g.drawOval(x, y, r, r);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			button.setEnabled(false);
			smoothButton.setEnabled(false);
			viterbiButton.setEnabled(false);
			
			if (e.getActionCommand() == "timestep") {
				if (!started) {
					started = true;
					
					button.setText("Timestep");
					timestep = 0;
					transitionModel();
				} else {
					moves.add(ThreadLocalRandom.current().nextInt(1, 4));
					startNode.updateStateNode(moves.get(moves.size() - 1));
					timestep++;
				}
				
				updateDistribution(startNode.getPercept());
				updateMaze();
				
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	button.setEnabled(true);
				    	viterbiButton.setEnabled(true);
				    	
				    	if (percepts.size() > 1) {
				    		smoothButton.setEnabled(true);
				    	}
				    }
			    });
				
			} else if (e.getActionCommand() == "smooth") {
				reconstructSteps = new Timer(1000, this);
				reconstructSteps.setActionCommand("smoothStep");
				
				backwardMessage = backwardsAlgorithm();
				
				smoothDistribution = new Matrix(1, mazeWidth * mazeHeight, 0.0);
				
				button.setEnabled(false);
				smoothButton.setEnabled(false);
				viterbiButton.setEnabled(false);
				
				startNode.restoreInitialState();
				
				updateMaze();
				
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	reconstructSteps.start();
				    }
			    });
				
			} else if (e.getActionCommand() == "smoothStep") {
				if (++t < percepts.size()) {
					timestep = t;
					Matrix f = forwardMessage.get(t + 1), b = backwardMessage[t];
								
					for (int m = 0; m < mazeWidth * mazeHeight; m++) {
						this.smoothDistribution.setEntry(0, m, f.entry(0, m) * b.entry(m, 0));
					}
					
					normalize(this.smoothDistribution);
					
					if (t > 0) {
						startNode.updateStateNode(moves.get(t - 1));
					}
					
					updateMaze();
				} else {
					reconstructSteps.stop();
					
					SwingUtilities.invokeLater(new Runnable() {
					    public void run() {
							button.setEnabled(true);
							smoothButton.setEnabled(true);
							viterbiButton.setEnabled(true);
					    }
				    });
					
					smoothDistribution = null;
					t = -1;
				}
			} else if (e.getActionCommand() == "viterbi") {
				vitrebiSteps = new Timer(1000, this);
				vitrebiSteps.setActionCommand("vitrebiStep");
				
				
				vitrebiPath = viterbi();
				
				startNode.restoreInitialState();
				
				updateMaze();
				
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	vitrebiSteps.start();
				    }
			    });
			} else if (e.getActionCommand() == "vitrebiStep") {
				if (++t < percepts.size()) {
					timestep = t;
					if (t > 0) {
						startNode.updateStateNode(moves.get(t - 1));
					}
					
					updateMaze();
				} else {
					vitrebiSteps.stop();
					
					SwingUtilities.invokeLater(new Runnable() {
					    public void run() {
							button.setEnabled(true);
							smoothButton.setEnabled(true);
							viterbiButton.setEnabled(true);
					    }
				    });
					
					
					vitrebiPath = null;
					t = -1;
				}
			}
		}
	}

/****************************** CONSTRUCTOR ***********************************///-----------------------------------------------//
	public HMMProblem(int nrobots, int w, int h, Double e, 
			Color pc[], String cNames[]) {
		totalRobots = nrobots;
		mazeWidth = w;
		mazeHeight = h;
		possibleColors = pc;
		robots = new ArrayList<RobotNode>(totalRobots);
		
		percepts = new ArrayList<Integer>();
		error = e;
		
		transitionModel = null;
		
		this.robots.add(new RobotNode(0, 0, 1.0));
		
		startNode = this.robots.get(0);
		
		mazeColors = new int[w][h + 1];
		colorNames = cNames;
		
		forwardMessage = new ArrayList<Matrix>();
		
		
		forwardMessage.add(new Matrix(1, w * h, 1.0 / (w * h)));
		
		mazeWalls = loadMaze();
		mazePanel = new JPanel();
		
		if (mazeFrame == null) {
			mazeFrame = new JFrame();
			mazeFrame.setBackground(Color.WHITE);
		}
		
		if (mazePanel == null) {
			mazePanel = new JPanel();
			mazePanel.setBackground(Color.WHITE);
		}
		
		startMaze();
	}

/********************************** OVERRIDES *********************************/
	public RobotNode getBot(int i) {
		return this.robots.get(i);
	}
	
	public void updateMaze() {
		mazeFrame.repaint();
	}
	
	public void updateDistribution(int p) {				
		Matrix newDistrib, sensor;
				
		maxProb = Double.NEGATIVE_INFINITY;
		this.percepts.add(p);
				
		newDistrib = this.forwardMessage.get(this.forwardMessage.size() - 1)
				.matrixMultiply(transitionModel);
				
		sensor = this.sensorModel(p, newDistrib);
				
		newDistrib = newDistrib.matrixMultiply(sensor);
			
		this.normalize(newDistrib);
		
		this.forwardMessage.add(newDistrib);
	}
	
	
	public String translatePercept(int color) {
		return colorNames[color];
	}
	
/******************************* PUBLIC METHODS *******************************/
	//
	
/**************************** PRIVATE METHODS *********************************/
//----------------------------- loadMaze() -----------------------------------//
	private int[][] loadMaze() {
		int[][] maze = new int[this.mazeWidth][this.mazeHeight];
		Random random = new Random();
		
		int midpointWidth = Math.floorDiv(this.mazeWidth, 2);
		int lowerQuartileWidth = Math.floorDiv(midpointWidth, 2);
		int upperQuartileWidth = midpointWidth + lowerQuartileWidth;
		
		for (int h = 0; h <= this.mazeHeight; h++) {
			for (int w = 0; w < this.mazeWidth; w++) {
				mazeColors[w][h] = ThreadLocalRandom.current().nextInt(0, possibleColors.length);
				
				if (h < this.mazeHeight) {
					if (h == 0) {
						if (w <= lowerQuartileWidth || w >= upperQuartileWidth)
							maze[w][h] = 1;
					} else if (h < this.mazeHeight - 1) {
						if (w == lowerQuartileWidth || w == upperQuartileWidth)
							maze[w][h] = 1;
					} else {
						if (w <= upperQuartileWidth)
							maze[w][h] = 1;
					}
				}
			}
		}
		
		for (int w = 0; w < this.mazeWidth; w++) {
			for (int h = 0; h < this.mazeHeight; h++) {
				if (maze[w][h] != 1) {
					maze[w][h] = (random.nextBoolean()) ? 1 : -1;
				}
			}
		}
		
		return null;
	}
	
//----------------------------- startMaze() ----------------------------------//
	private void startMaze() {
		Dimension realMazeSize = this.mazeHeight > this.mazeWidth ?
				new Dimension(620 / this.mazeHeight * this.mazeWidth, 620) :
				new Dimension(620, 620 / this.mazeWidth * this.mazeHeight);
				
		Dimension realSize = new Dimension(realMazeSize.width + 80 + 400,
				realMazeSize.height + 80);
		
		mazeFrame.add(mazePanel, BorderLayout.CENTER);
		
		mazeFrame.setPreferredSize(realSize);
		mazeFrame.setBounds(0, 0, realSize.width, realSize.height);
		
		mazeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mazeFrame.setLocationRelativeTo(null);
	    
		mazePanel.setPreferredSize(realMazeSize);
		
		Maze mazePanelCompontent = new Maze(realMazeSize);
		
		LayoutManager overlay = new OverlayLayout(mazePanel);
		mazePanel.setLayout(overlay);
		
		mazePanel.add(mazePanelCompontent);
		
		mazeFrame.pack();
		
		mazeFrame.setVisible(true);
		mazePanel.setVisible(true);
		
		mazeFrame.setTitle("Hidden Markov Models and First-Order Logic");
	}
	
	private Matrix sensorModel(int p, Matrix distrib) {
		Double success = 1.0 - error;
		Matrix sensor = new Matrix(mazeWidth * mazeHeight, mazeWidth * mazeHeight, 0.0);
		
		int i;
		
		for (int x = 0; x < mazeWidth; x++) {
			for (int y = 0; y < mazeHeight; y++) {
				i = this.mapToInteger(x, y);
				
				sensor.setEntry(i, i, this.mazeColors[x][y+1] == p ? success : error);  
			}
		}
		
		return sensor; 
	}
	
	private void transitionModel() {
		Double transition[][] = new Double[mazeWidth * mazeHeight][mazeWidth * mazeHeight],
				transitionProb = 0.0; 
		int i, j;
		
		for (int x1 = 0; x1 < mazeWidth; x1++) {
			for (int y1 = 0; y1 < mazeHeight; y1++) {
				i = this.mapToInteger(x1, y1);
						
				for (int x2 = 0; x2 < mazeWidth; x2++) {
					for (int y2 = 0; y2 < mazeHeight; y2++) {
						j = this.mapToInteger(x2, y2);
						
						if (i == j) {
							if ((x1 == 0 && y1 == 0) || (x1 == 0 && y1 == mazeHeight - 1) || (x1 == mazeWidth-1 && y1 == 0) || (x1 == mazeWidth -1 && y1 == mazeHeight - 1)) {
								transitionProb = 0.50;
							} else if (x1 == 0 || x1 == mazeWidth - 1 || y1 == 0 || y1 == mazeHeight - 1) {
								transitionProb = 0.25;
							} else {
								transitionProb = 0.0;
							}
						} else {
							if (Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1 && Math.abs(y1 - y2) != Math.abs(x1 - x2)) {
								transitionProb = 0.25; 
							} else {
								transitionProb = 0.00;
							}
						}
						
						transition[i][j] = transitionProb;
					}
				}   
			}
		}
		
		transitionModel = new Matrix(transition.length, transition[0].length, transition);
	}
	
	private Matrix[] backwardsAlgorithm() {
		Matrix result[] = new Matrix[this.percepts.size()], sensor;
		
		result[this.percepts.size() - 1] = new Matrix(mazeWidth * mazeHeight, 1, 1.0);
		
		for (int t = this.percepts.size() - 2; t >= 0; t--) {
			sensor = this.sensorModel(this.percepts.get(t+1),
					this.forwardMessage.get(t+1));
			result[t] = this.transitionModel.matrixMultiply(sensor)
					.matrixMultiply(result[t+1]);
		}
		
		return result;
	}
	
	public void normalize(Matrix newDistrib) {
		Double norm = 0.0;
		
		for (int n = 0; n < newDistrib.n; n++) {
			norm += newDistrib.entry(0, n);
		}
		
		
		newDistrib.divideRow(0, newDistrib.row(0).norm());
		
		for (int n = 0; n < newDistrib.n; n++) {
			if (newDistrib.entry(0, n) > maxProb) {
				maxProb = newDistrib.entry(0, n);
			}
		}
	}
	
	private int[] viterbi() {
		int K = mazeWidth * mazeHeight,
				T = this.percepts.size(), maxJ = 0;
		
		Matrix T1 = new Matrix(K, this.percepts.size(), 0.0),
				T2 = new Matrix(K, this.percepts.size(), 0.0),
				sensor = this.sensorModel(this.percepts.get(0), 
						this.forwardMessage.get(0)).diagonalMatrix();
		int Z[] = new int[T];
		
		Double v, max, val, val2;
		
		for (int i = 0; i < K; i++) {
			T1.setEntry(i, 0, sensor.entry(i, i));
		}
		
		for (int i = 1; i < T; i++) {
			sensor = this.sensorModel(this.percepts.get(i), this.forwardMessage.get(i));
			
			for (int j = 0; j < K; j++) {
				T1.setEntry(j, i, Double.NEGATIVE_INFINITY);
				max = Double.NEGATIVE_INFINITY;
				
				for (int k = 0; k < K; k++) {
					val = T1.entry(k, i - 1);
					v = val * this.transitionModel.entry(k, j);
					val2 = Math.log(val * v);
					
					v *= sensor.entry(j, j);
					
					T1.setEntry(j, i, Math.max(T1.entry(j, i), v));
					
					if (val2 > max) {
						max = val2;
						maxJ = k;
					}
				}
				
				T2.setEntry(j, i, (double) maxJ);
			}
		}
		
		max = Double.NEGATIVE_INFINITY;
		
		for (int k = 0; k < K; k++) {
			val = T1.entry(k, T - 1);
			
			if (val > max) {
				max = val;
				Z[T - 1] = k;
			}
		}
		
		for (int i = T - 1; i > 0; i--) {
			Z[i-1] = (int) Math.floor(T2.entry(Z[i], i));
		}
		
		return Z;
	}
	
	private int mapToInteger(int x, int y) {
		return this.mazeHeight * x + y;
	}
	
	private Color choropleth(Double v) {
		final int N = 16;
		List<Color> clut = new ArrayList<>(N);
				
		for (float i = 0; i < N; i++) {
            clut.add(Color.getHSBColor(1, 0, 1 - (i / N)));
        }
		
		return clut.get((int) (v * N - 1));
	}
}