package labs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class PrimeNumberGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextArea textField = new JTextArea();
    private JButton startButton = new JButton("Start");
    private JButton cancelButton = new JButton("Cancel");
    private JButton restartButton = new JButton("Enter Another Number");
    private int userInput;
    private int userThreads;
    private volatile boolean running = false;
    private volatile boolean validInputInt;
    private volatile boolean validInputThreads;
    private volatile boolean cancel = false;
    private long startTime;
    private Thread displayResults = new Thread();
    private List<Integer> primeNumbers = Collections.synchronizedList(new ArrayList<>());

    public PrimeNumberGUI() {
        initialize();

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                handleUserInput();
            }});

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel = true;
                cancelButton.setEnabled(false);
                displayPrimeNumbers(1);
         		restartButton.setVisible(true);
            }});
        
		restartButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ( displayResults.isAlive( )) {
					displayResults.interrupt();
				}
				restart();
			}});
    }

    public void initialize() {
        setTitle("Prime Number Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        textField.setWrapStyleWord(true);
        textField.setLineWrap(true);
        textField.setMargin(new Insets(10, 10, 10, 10));
        textField.setEditable(false);
        textField.setText("Use this GUI to find all of the prime numbers from 1 to your integer");
        panel.add(textField, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(textField);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(startButton);
        bottomPanel.add(cancelButton);
        bottomPanel.add(restartButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        add(panel);

        cancelButton.setEnabled(false);
        restartButton.setVisible(false);

        setVisible(true);
    }

    public void handleUserInput() {
        cancelButton.setEnabled(true);
        validInputInt = false;
        validInputThreads = false;

        while (!validInputInt) {
            String userInputString = JOptionPane.showInputDialog("Enter a large integer value");

            if (userInputString != null) {
                try {
                    userInput = Integer.parseInt(userInputString);
                    validInputInt = true;

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Invalid input. Please enter a valid integer.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                break;
            }
        }
        
        while (!validInputThreads) {
        	
        	String userInputThreads = JOptionPane.showInputDialog("How many threads would you like to use?");
        	
            if (userInputThreads != null) {
                try {
                    userThreads = Integer.parseInt(userInputThreads); 
                    if (userThreads == 0) {
                    	throw new Exception();
                    }
                    validInputThreads = true;
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Invalid input. Please enter a valid integer.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                break;
            }
        }

        try {
            calculatePrimes(userInput, userThreads);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    
    public String getProgress() {
        String yourNum = ("Your number: " + userInput + "\n");
    	if (userInput > 0) {
            double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.00;
            int howMany = primeNumbers.size();
            return (yourNum + howMany + " prime numbers found in " + timeElapsed + " seconds.");
        }
        return yourNum;
    }

    public void calculatePrimes(int userInput, int userThreads) throws InterruptedException {

        Semaphore semaphore = new Semaphore(userThreads);

        for (int i = 0; i < userThreads; i++) {
            semaphore.acquire();
            Worker worker = new Worker(userInput, semaphore);
            Thread workerThread = new Thread(worker);
            workerThread.start();
        }

        PrimeNumberWorkerManager managerWorker = new PrimeNumberWorkerManager(semaphore, userThreads);
        Thread managerThread = new Thread(managerWorker);
        managerThread.start();
        startTime = System.currentTimeMillis();
        
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running && !cancel) {
        
                	textField.setText(getProgress());
                        }
            }
        }, 500, 1000);        
        
    }
    
    public void calcuationFinished() {
    	
        cancelButton.setEnabled(false);
 		startButton.setEnabled(false);
 		displayPrimeNumbers(0);
		restartButton.setVisible(true);
		
    }
    
    public void displayPrimeNumbers(int exitCode) {
    	
        double totalElapsed = ((System.currentTimeMillis() - startTime) / 1000.00);

    	if (exitCode == 0) {
            textField.setText(primeNumbers.size() + " prime numbers found in your number " + userInput + " in " + totalElapsed + " seconds using " + userThreads + " threads.\n");
    	}
        if (exitCode == 1) {
        	textField.setText("***Warning: Job Interrupted, results may be truncated***\n");
            textField.append("Job cancelled after " + totalElapsed + " seconds. Outputting " + primeNumbers.size() + " results found.\n");
        }
    	
 		displayResults = new Thread(new Runnable() { // this can take a long time so launching on sep thread
			@Override
			public void run() {
				try {
				   for (Integer num : primeNumbers) {
			            textField.append(num.toString() + "\n");
			            if (Thread.interrupted()) { // if user wants to hit restart button before results are finished outputting
			            	textField.append(num.toString() + "\n");
			            	throw new InterruptedException();
			            }
			        }
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
 		});
 		displayResults.start();
	}
    
    public void restart() {
        dispose();
        new PrimeNumberGUI();
    }


    private class Worker implements Runnable {

        private final int inputBigInt;
        private final Semaphore semaphore;

        public Worker(int inputBigInt, Semaphore semaphore) {
            this.inputBigInt = inputBigInt;
            this.semaphore = semaphore;
        }

        static boolean isPrime(int n) {
            if (n <= 1)
                return false;

            for (int i = 2; i < n; i++)
                if (n % i == 0)
                    return false;

            return true;
        }

        @Override
        public void run() {
        	
            try {
            	running = true;
                for (int currentIteration = 0; currentIteration < inputBigInt; currentIteration++) {
                     if (isPrime(currentIteration)) {
                        primeNumbers.add(currentIteration);
                    }
                    if (Thread.interrupted() || cancel) {
                    	primeNumbers.add(currentIteration); // add current iteration to avoid concurrent modification exception?
                        throw new InterruptedException();
                    }
                }
                semaphore.release();
                running = false;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class PrimeNumberWorkerManager implements Runnable {

        private int numThreads;
        private Semaphore semaphore;

        public PrimeNumberWorkerManager(Semaphore semaphore, int numThreads) {
            this.semaphore = semaphore;
            this.numThreads = numThreads;
        }

        @Override
        public void run() {
        	
            int numAcquired = 0;
            
            while (numAcquired < numThreads) {
                try {
                    semaphore.acquire();
                    numAcquired++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            calcuationFinished();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        new PrimeNumberGUI();
    }
}
