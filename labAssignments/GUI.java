package labs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class GUI extends JFrame {
	
	public static String[] SHORT_NAMES = 
		{ "A","R", "N", "D", "C", "Q", "E", "G",  "H", "I", "L", "K", "M", "F", "P", "S", "T", "W", "Y", "V" };
	
	public static String[] FULL_NAMES = 
		{"Alanine","Arginine", "Asparagine","Aspartic Acid", "Cysteine","Glutamine",  "Glutamic Acid","Glycine" ,"Histidine",
		"Isoleucine","Leucine",  "Lysine", "Methionine","Phenylalanine", "Proline","Serine","Threonine","Tryptophan","Tyrosine", "Valine"};
	
	private JLabel timerCountDown = new JLabel("Time Remaining: 30 seconds | ");
	private JLabel scoreLabel = new JLabel("Score: 0/0");
	private JTextArea textField = new JTextArea();
	private JButton startButton = new JButton("Start");
	private JButton cancelButton = new JButton("Cancel");
	private JTextField userAnswer = new JTextField();
	private Random rand = new Random();
	private int correct = 0;
	private int total = 0;
	private String answer;
	private int num;
			
	public GUI() {
		
		initialize();
		
		startButton.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e) {
				startButton.setEnabled(false);
				cancelButton.setEnabled(true);
				startGame();
			}
		});
		
		cancelButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				startButton.setEnabled(true);
				cancelButton.setEnabled(false);	
				endGame();
			}
		});
		
	}
	
	public void initialize() {
		
		setTitle("Amino Acid Quiz");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500,300);
		setLocationRelativeTo(null);
		JPanel panel = new JPanel(new BorderLayout());
		
		textField.setWrapStyleWord(true); 
        textField.setLineWrap(true);
		textField.setText(introText());
		textField.setMargin(new Insets(10,10,10,10));
		panel.add(textField, BorderLayout.CENTER);
		add(panel);
				
		JPanel topPanel = new JPanel();
		topPanel.add(timerCountDown);
		topPanel.add(scoreLabel);
		panel.add(topPanel,BorderLayout.NORTH);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(startButton);
		bottomPanel.add(cancelButton);
		panel.add(bottomPanel, BorderLayout.SOUTH);
		
		cancelButton.setEnabled(false);
		
		add(userAnswer, BorderLayout.AFTER_LAST_LINE);
		userAnswer.setVisible(false);
		
		setVisible(true);
	}
	
	public String introText() {
		
		StringBuilder intro = new StringBuilder();
		intro.append("\nWelcome to the Amino Acid Quiz!\n\n");
		intro.append("Rules of the Game:\n\n- You will be shown the full name of the amino acid and asked to answer with the single letter code.\n");
		intro.append("- The game will last for 30 seconds.");
		intro.append("\n\nGood Luck!");
		String introString = intro.toString();
		
		return introString;
	}
	
	private void startGame() {
		
		userAnswer.setVisible(true);
		userAnswer.requestFocus();
		scoreLabel.setText("Score: " + correct + " out of " + total );
		new Thread (new Timer() ).start();
		nextQuestion();
	
	}	
	
	private void nextQuestion() {
	    num = rand.nextInt(FULL_NAMES.length);
	    textField.setText("\n\n\n\nAmino Acid: " + FULL_NAMES[num]);
	    
	    userAnswer.addKeyListener(new KeyListener() {
	        @Override
	        public void keyTyped(KeyEvent e) {
	            // Not used in this context
	        }

	        @Override
	        public void keyPressed(KeyEvent e) {
	            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	                
	            	answer = userAnswer.getText();
	                
	                if (!answer.isBlank()) {
	                	
		        		if ( answer.equalsIgnoreCase(SHORT_NAMES[num]) ) {
		        			correct++; 
		        		}
		        		total++;	
		        			
		        		scoreLabel.setText("Score: " + correct + " out of " + total );
		        			
		        		userAnswer.setText("");
		        		
		        		nextQuestion();
	                }  
	            }
	        }

	        @Override
	        public void keyReleased(KeyEvent e) {
	            // Not used in this context
	        }
	    });
	}
	
	private class Timer implements Runnable {
		
		private long start = System.currentTimeMillis();
        private long duration = 30 * 1000; // 30 seconds
        private long end = start + duration;
        
		@Override
		public void run() {
			
			while (System.currentTimeMillis() <= end) {
				try {
					long currentTime = System.currentTimeMillis();
					long elapsed = currentTime - start;
					long remaining = duration - elapsed;
					updateTimerCountDown(remaining);
				}
				catch ( Exception ex ) {
					if ( Thread.interrupted() ) {
						break;
					}
					else {
						ex.printStackTrace();
					}
				}
			}
		}	
	}
	
	private void updateTimerCountDown(long remainingTime) {
		
		timerCountDown.setText(remainingTime/1000 + " seconds remaining | ");
		if ( remainingTime == (long) 0 ) {
			timerCountDown.setText("Time is up! ");
			gameOverScreen();
		}
	}
	
	private void gameOverScreen() {
		DecimalFormat df_obj = new DecimalFormat("##.#");
		userAnswer.setVisible(false);
		double score;
		if (total == 0){
			score = (double)0;
		}
		else {
			score = ((double)correct/ (double)total)*100;
		}
		textField.setText("\n\n\nGame over\n\nYour score: " + df_obj.format(score) + "%");
		correct = 0;
		total = 0;
		startButton.setText("Play Again");
		startButton.setEnabled(true);
		cancelButton.setEnabled(false);
	}
	
	private void endGame() {
		new GUI(); //this is wrong
	}
	
	public static void main(String[] args) {
		
		new GUI();

	}

}
