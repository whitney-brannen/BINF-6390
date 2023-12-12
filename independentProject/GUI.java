package independentProject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class GUI extends JFrame{
	
	private static final long serialVersionUID = 5979686899134093646L;
	final String newline = "\n\n";
	
	private JFrame main = new JFrame();
	private JTextArea mainTextArea = new JTextArea();
	private JButton chooseFileButton = new JButton("Select a file");
	private final JFileChooser fileChooser = new JFileChooser( FileSystemView.getFileSystemView() );
	
	private List<String> titles = new ArrayList<>();
	private Thread backgroundParser = new Thread();
	private volatile boolean backgroundParserRunning = false;
	private volatile boolean parserCancel = false;
	private JButton loadingCancelButton = new JButton("Cancel");
	
	private JPanel mainPanel = new JPanel( new BorderLayout() );
	
	private JLabel rightTitle = new JLabel();
	private JButton depthButton = new JButton("Depth");
	private JButton qualityButton = new JButton("Quality");
	private JButton missingnessButton = new JButton("Variant Missingness");
	
	private JFrame depth = new JFrame();
	private JFrame quality = new JFrame();
	private JFrame missingness = new JFrame();
	
	private JPanel depthFilterOptionsPanel = new JPanel();
	private JPanel qualityFilterOptionsPanel = new JPanel();
	private JPanel missingnessFilterOptionsPanel = new JPanel();
	
	private CombinedVariants vcfFileStats;
	private CombinedVariants currentListOfVariants;
	
	private JFileChooser saveFileChooser = new JFileChooser( FileSystemView.getFileSystemView() );

	public GUI() {
		this.vcfFileStats = null;
		initializeHomePage();
	}

	public void initializeHomePage() {
		main.setTitle("Interactive VCF File Filter");
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setSize(700, 500);
		main.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        mainTextArea.setText(newline + "Use this program to view statistics and interactively modify filters for your VCF file!" + newline);
        mainTextArea.append("Statistics include: \n\t- read depth\n\t- variant missingness\n\t- homozygous and heterozygous genotype frequencies\n\t- reference and alternative allele frequencies" + newline);
        mainTextArea.append("This program works by filtering files by variant (individual options not yet included).\n");
        mainTextArea.append("Filter options will exclude any variants that do not fall within your specified parameters of\n\t- read depth\n\t- quality score\n\t- variant missingness" + newline);
        mainTextArea.append("Filters can be interactively edited within this program to view the changes to the overall statistics of your file." + newline);
        mainTextArea.append("After applying filters, there is an option to save the filtered file to your computer.");
        mainTextArea.setEditable(false);
        mainTextArea.setLineWrap(true);
        mainTextArea.setWrapStyleWord(true);
        mainTextArea.setMargin(new Insets(20,20,20,20));
        
        panel.add(mainTextArea);
        panel.add(chooseFileButton,BorderLayout.SOUTH);
                
        chooseFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Variant Call Format (VCF)","txt","vcf");
				fileChooser.setFileFilter(filter);
				fileChooser.setDialogTitle("Select a file");
				int selectedFile = fileChooser.showOpenDialog(null);	
				
				if ( selectedFile == JFileChooser.APPROVE_OPTION ) { 
					
					File file = fileChooser.getSelectedFile();
					try {
						chooseFileButton.setVisible(false);
						loadingPage(file);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
        });
        main.add(panel);
        main.setVisible(true);
	}
	
	public void loadingPage(File file) throws IOException {
		
		JPanel panel = new JPanel( new BorderLayout() );
		mainTextArea.setEditable(false);
		panel.add(mainTextArea,BorderLayout.CENTER);
		panel.add(loadingCancelButton,BorderLayout.SOUTH);
		main.add(panel);
		
		mainTextArea.setText("\nFile Selected: " + fileChooser.getSelectedFile().getAbsolutePath() + newline);
		
		BufferedReader reader = new BufferedReader( new FileReader( fileChooser.getSelectedFile() ));
		launchBackgroundParserThread(reader);
		
		main.setVisible(true);
		
		updateLoadingText();
		
		loadingCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					parserCancel = true;
					loadingCancelButton.setVisible(false);
					System.exit(0);
					
				} catch (Exception ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
		});
	
	}
	
	private void launchBackgroundParserThread(BufferedReader reader) {
		backgroundParser = new Thread ( new Runnable() {
			@Override
			public void run() {
				try {
					backgroundParserRunning = true;
					if ( !parserCancel ) {
						parseFile(reader);
						if ( Thread.interrupted() ) {
							mainTextArea.setText(" Operation Cancelled ");
						}
					}
					backgroundParserRunning = false;
					loadingCancelButton.setVisible(false);
				} catch (Exception e ) {
					e.printStackTrace();
					backgroundParserRunning = false;
				}
			}});
		backgroundParser.start();
	}
	
	private class FileParserWorker implements Runnable {
	    	
	    	private final BlockingQueue<String> queue;
	    	private final List<String >titles;
			private final CountDownLatch latch;
			private List<Variant >allVariantsInFile;
	    	
	    	public FileParserWorker(BlockingQueue<String> queue, List<String> titles, CountDownLatch latch,List<Variant> allVariantsInFile) {
	    		this.queue = queue;
	    		this.titles = titles;
	    		this.latch = latch;
	    		this.allVariantsInFile = allVariantsInFile;
	    	}
	    	
			@Override
			public void run() {
				
				try {
					
					latch.await();
					
					while(true) {
					
						String line = queue.take();
						
						   if (line.equals("EOF")) {
						        break;
						   }
						   
						List<Object> values = new ArrayList<>();
						Map<String,Object> variantLineInfo= new HashMap<>();
		
						// all other lines that aren't headers
						if ( !line.startsWith("##") ) {
							Object[] variantLine = line.split("\t");
							values = Arrays.asList(variantLine);
						}
						
						// create hashmap for each category for variants
						for (int x = 0; x < values.size(); x++ ) {
							String key = titles.get(x);
							Object val = values.get(x);
							variantLineInfo.put(key, val);	
						}
						
						// now create Variant instance for each line
						if (variantLineInfo.get("#CHROM") != null && !variantLineInfo.get("#CHROM").equals("#CHROM")) {
							Variant instance = new Variant(variantLineInfo);
							allVariantsInFile.add(instance);
	
						}
					}	   
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
	    }
	
	
	private void parseFile(BufferedReader reader) {
		
		int maxNumThreads = Runtime.getRuntime().availableProcessors()+1;
		
		BlockingQueue<String> queue = new LinkedBlockingQueue<>();
		List<String> titles = new CopyOnWriteArrayList<>(); 
		CountDownLatch latch = new CountDownLatch(1);
		List<Variant> allVariantsInFile = new CopyOnWriteArrayList<>();
		
		try { 
			while ( reader.ready() && !parserCancel ) {
				String line = reader.readLine();
				if (line.startsWith("#CHROM")) {
					String[] titleLine = line.split("\t");
			        titles = new ArrayList<>(Arrays.asList(titleLine));
					latch.countDown(); // block until titles is established from the file or parsing will fail
				}
			    queue.put(line);
			}
			
			List<Thread> workerThreads = new ArrayList<>();
			
			for (int i =0; i < maxNumThreads; i++) {
			    Thread workerThread = new Thread(new FileParserWorker(queue, titles, latch, allVariantsInFile));
			    workerThreads.add(workerThread);
			    workerThread.start();
			    queue.put("EOF");
			}
			
			for (Thread workerThread : workerThreads) {
				workerThread.join();
			}
			vcfFileStats = new CombinedVariants(allVariantsInFile);
			currentListOfVariants = vcfFileStats;
		} 

		catch (InterruptedException | IOException ex) {
			ex.printStackTrace();
		}
		
	}

	
	public void updateLoadingText() {
	    SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
	        @Override
	        protected Void doInBackground() throws Exception {
	        	
	            while (backgroundParserRunning && !parserCancel) {
	            	publish("Loading VCF file...\n");
	                Thread.sleep(2000);
	            }
	            return null;
	        }

	        @Override
	        protected void process(List<String> chunks) {
	            for (String chunk : chunks) {
	                mainTextArea.append(chunk);
	            }
	        }

	        @Override
	        protected void done() {
	            try {
					get();
					resultsPage();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}  
	        }
	    };

	    worker.execute();
	}

	
	public void resultsPage() {
		
		mainTextArea.setText("\t\tYour input file contains " + vcfFileStats.numVariants() + " variants.");
        Font boldFont = new Font(mainTextArea.getFont().getName(), Font.BOLD, mainTextArea.getFont().getSize());
        mainTextArea.setFont(boldFont);
        
		mainPanel.add(mainTextArea, BorderLayout.NORTH);
		
		JPanel grid = new JPanel( new GridLayout(0,2) );
		
		JTextArea leftGrid = new JTextArea();
		leftGrid.setMargin(new Insets(10, 10, 10, 10));
		leftGrid.setLineWrap(true);
		leftGrid.setWrapStyleWord(true);
		leftGrid.setEditable(false);
		leftGrid.setText( resultTextLeft() );
		
		JPanel rightGrid = new JPanel();
		rightGrid.setLayout(new GridLayout(4,1));
		resultPanelRight(rightGrid);

		grid.add(leftGrid);
		grid.add(rightGrid);
		
		mainPanel.add(grid,BorderLayout.CENTER);
		
		main.add(mainPanel);
		main.setVisible(true);
		
        depthButton.addActionListener(this::depthButtonActionPerformed);
        qualityButton.addActionListener(this::qualityButtonActionPerformed);
        missingnessButton.addActionListener(this::missingnessButtonActionPerformed);
	}
	
	   private void depthButtonActionPerformed(ActionEvent e) {
	        main.remove(mainPanel);
	        displayDepthDetails();
	    }

	    private void qualityButtonActionPerformed(ActionEvent e) {
	        main.remove(mainPanel);
	        displayQualityDetails();
	    }

	    private void missingnessButtonActionPerformed(ActionEvent e) {
	        main.remove(mainPanel);
	        displayMissingnessDetails();
	    }

	private String resultTextLeft() {
		StringBuffer text = new StringBuffer();
		DecimalFormat df = new DecimalFormat("#.###");
		text.append("Average read depth: " + df.format( currentListOfVariants.getDepth() ) + newline);
		text.append("Average quality score: " + df.format( currentListOfVariants.getQualityScore() ) + newline);
		text.append("Average Variant Missingness: " + df.format( currentListOfVariants.getVariantMissingness() ) + newline);
		text.append("Average homozygous reference (0/0) genotype frequency: " + df.format( currentListOfVariants.getHomozygousRefGenotypeFreq() ) + newline);
		text.append("Average homozygous alternative (1/1) genotype frequency: " + df.format( currentListOfVariants.getHomozygousAltGenotypeFreq() ) + newline);
		text.append("Average heterozygosity: " + df.format( currentListOfVariants.getHeterozygosity() ) + newline);
		text.append("Average reference allele frequency: " + df.format( currentListOfVariants.getRefAlleleFreq() ) + newline);
		text.append("Average alternative allele frequency: " + df.format( currentListOfVariants.getAltAlleleFreq() ) + newline);
		return text.toString();
	}
	
	private void resultPanelRight(JPanel rightGrid) {
		rightTitle.setText(" Use buttons below for more details and filtering: ");
		//rightGrid.setBackground(Color.white);
		rightGrid.add(rightTitle);
		rightGrid.add(depthButton);
		rightGrid.add(qualityButton);
		rightGrid.add(missingnessButton);
	}
	
	
	private void displayDepthDetails() {
	    depth.setSize(600, 400);
	    depth.setLocationRelativeTo(main);
	    depth.setTitle("Read Depth Details");
	    
	    JPanel depthPanel = new JPanel(new BorderLayout());
	    
	    depthFilterOptions();

	    JPanel combinedPanel = new JPanel(new GridLayout(1, 2));

	    JPanel leftPanel = new JPanel(new BorderLayout());
	    JPanel header = new JPanel();
	    header.setBackground(Color.white);
	    JTextArea title = new JTextArea("Depth values: \n");
	    title.setEditable(false);
	    header.add(title);
	    leftPanel.add(header, BorderLayout.NORTH);

	    StringBuffer depths = new StringBuffer();
	    for (Variant v : vcfFileStats.getVars()) {
	        depths.append(v.getDepth() + "\n");
	    }
	    JTextArea individualValues = new JTextArea(depths.toString());
	    JScrollPane scrollPane = new JScrollPane(individualValues);
	    scrollPane.createVerticalScrollBar();
	    leftPanel.add(scrollPane, BorderLayout.CENTER);

	    combinedPanel.add(leftPanel);
	    combinedPanel.add(depthFilterOptionsPanel); 

	    depthPanel.add(combinedPanel, BorderLayout.CENTER);
	    depth.setContentPane(depthPanel);
	    depth.revalidate();
	    depth.repaint();
	    depth.setVisible(true);
	}

	
	private void displayQualityDetails() {
		
		quality.setSize(600,400);
		quality.setLocationRelativeTo(main);
		quality.setTitle("Quality Score Details");
		JPanel qualityPanel = new JPanel ( new GridLayout(1,2) );
		
		qualityFilterOptions();
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		
		JPanel header = new JPanel();
		header.setBackground(Color.white);
		JTextArea title = new JTextArea("Quality scores: \n");
		title.setEditable(false);
		header.add(title);
		leftPanel.add(header, BorderLayout.NORTH);
		
		StringBuffer quals = new StringBuffer();
		for ( Variant v : currentListOfVariants.getVars() ) {
			quals.append(v.getQualityScore() + "\n");
		}
		JTextArea individualValues = new JTextArea(quals.toString());
		JScrollPane scrollPane = new JScrollPane(individualValues);
		scrollPane.createVerticalScrollBar();
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		qualityPanel.add(leftPanel);
		qualityPanel.add(qualityFilterOptionsPanel);
		
		quality.setContentPane(qualityPanel);
		quality.revalidate();
		quality.repaint();
		quality.setVisible(true);
	}
	
	private void displayMissingnessDetails() {
		
		missingness.setSize(600,400);
		missingness.setLocationRelativeTo(main);
		missingness.setTitle("Variant Missingness Details");
		JPanel missingnessPanel = new JPanel ( new GridLayout(1,2) );
		
		missingnessFilterOptions();
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		
		JPanel header = new JPanel();
		header.setBackground(Color.white);
		JTextArea title = new JTextArea("Missingness values: \n");
		title.setEditable(false);
		header.add(title);
		leftPanel.add(header, BorderLayout.NORTH);
		
		StringBuffer missing = new StringBuffer();
		for ( Variant v : vcfFileStats.getVars() ) {
			missing.append(v.getVariantMissingness() + "\n");
		}
		JTextArea individualValues = new JTextArea(missing.toString());
		JScrollPane scrollPane = new JScrollPane(individualValues);
		scrollPane.createVerticalScrollBar();
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		missingnessPanel.add(leftPanel);
		missingnessPanel.add(missingnessFilterOptionsPanel);
		
		missingness.setContentPane(missingnessPanel);
		missingness.revalidate();
		missingness.repaint();
		missingness.setVisible(true);
	}
	
	
	public void depthFilterOptions() {
	    depthFilterOptionsPanel.setLayout(new BorderLayout());
	    JTextArea title = new JTextArea("Apply filters for Depth");
	    title.setMargin(new Insets(10, 10, 10, 10));
	    JPanel options = new JPanel(new GridLayout(5, 1));

	    JCheckBox minimumBox = new JCheckBox("Set minimum value");
	    JCheckBox maximumBox = new JCheckBox("Set maximum value");

	    JTextField minValue = new JTextField(20);
	    JTextField maxValue = new JTextField(20);

	    JButton filterData = new JButton("Apply filters");

	    depthFilterOptionsPanel.add(title, BorderLayout.NORTH);
	    options.add(minimumBox);
	    options.add(minValue);
	    options.add(maximumBox);
	    options.add(maxValue);
	    options.add(filterData);
	    depthFilterOptionsPanel.add(options);

	    minValue.setEnabled(false);
	    maxValue.setEnabled(false);
	    filterData.setEnabled(false);

	    minimumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            minValue.setEnabled(true);
		            minValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            minValue.setEnabled(false);
		            minValue.setText(null);
		        }
			}
	    });

	    maximumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            maxValue.setEnabled(true);
		            maxValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            maxValue.setEnabled(false);
		            maxValue.setText(null);
		        }	
			}
	    });

	    filterData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        applyDepthFilters(minValue, maxValue, minimumBox, maximumBox);
		        depth.dispose();
			}	
		});
	}

	public void qualityFilterOptions() {
	    qualityFilterOptionsPanel.setLayout(new BorderLayout());
	    JTextArea title = new JTextArea("Apply filters for Quality");
	    title.setMargin(new Insets(10, 10, 10, 10));
	    JPanel options = new JPanel(new GridLayout(5, 1));

	    JCheckBox minimumBox = new JCheckBox("Set minimum value");
	    JCheckBox maximumBox = new JCheckBox("Set maximum value");

	    JTextField minValue = new JTextField(20);
	    JTextField maxValue = new JTextField(20);

	    JButton filterData = new JButton("Apply filters");

	    qualityFilterOptionsPanel.add(title, BorderLayout.NORTH);
	    options.add(minimumBox);
	    options.add(minValue);
	    options.add(maximumBox);
	    options.add(maxValue);
	    options.add(filterData);
	    qualityFilterOptionsPanel.add(options);

	    minValue.setEnabled(false);
	    maxValue.setEnabled(false);
	    filterData.setEnabled(false);

	    minimumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            minValue.setEnabled(true);
		            minValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            minValue.setEnabled(false);
		            minValue.setText(null);
		        }
			}
	    });

	    maximumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            maxValue.setEnabled(true);
		            maxValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            maxValue.setEnabled(false);
		            maxValue.setText(null);
		        }	
			}
	    });

	    filterData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        applyQualityFilters(minValue, maxValue, minimumBox, maximumBox);
		        quality.dispose();
			}	
		});
	}

	public void missingnessFilterOptions() {
	    missingnessFilterOptionsPanel.setLayout(new BorderLayout());
	    JTextArea title = new JTextArea("Apply filters for Missingness");
	    title.setMargin(new Insets(10, 10, 10, 10));
	    JPanel options = new JPanel(new GridLayout(5, 1));

	    JCheckBox minimumBox = new JCheckBox("Set minimum value");
	    JCheckBox maximumBox = new JCheckBox("Set maximum value");

	    JTextField minValue = new JTextField(20);
	    JTextField maxValue = new JTextField(20);

	    JButton filterData = new JButton("Apply filters");

	    missingnessFilterOptionsPanel.add(title, BorderLayout.NORTH);
	    options.add(minimumBox);
	    options.add(minValue);
	    options.add(maximumBox);
	    options.add(maxValue);
	    options.add(filterData);
	    missingnessFilterOptionsPanel.add(options);

	    minValue.setEnabled(false);
	    maxValue.setEnabled(false);
	    filterData.setEnabled(false);

	    minimumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            minValue.setEnabled(true);
		            minValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            minValue.setEnabled(false);
		            minValue.setText(null);
		        }
			}
	    });

	    maximumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED) {
		            maxValue.setEnabled(true);
		            maxValue.requestFocus();
		            filterData.setEnabled(true);
		        } else {
		            maxValue.setEnabled(false);
		            maxValue.setText(null);
		        }	
			}
	    });

	    filterData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        applyMissingnessFilters(minValue, maxValue, minimumBox, maximumBox);
		        missingness.dispose();
			}	
		});
	}

	public void applyDepthFilters(JTextField minValueField, JTextField maxValueField, JCheckBox minimumBox, JCheckBox maximumBox) {
	    applyFilters("depth", minValueField, maxValueField);
	    resetFilterOptions(minimumBox, minValueField, maximumBox, maxValueField);

	}

	public void applyQualityFilters(JTextField minValueField, JTextField maxValueField, JCheckBox minimumBox, JCheckBox maximumBox) {
	    applyFilters("quality", minValueField, maxValueField);
	    resetFilterOptions(minimumBox, minValueField, maximumBox, maxValueField);

	}

	public void applyMissingnessFilters(JTextField minValueField, JTextField maxValueField, JCheckBox minimumBox, JCheckBox maximumBox) {
	    applyFilters("missingness", minValueField, maxValueField);
	    resetFilterOptions(minimumBox, minValueField, maximumBox, maxValueField);

	}

	private void applyFilters(String stat, JTextField minValueField, JTextField maxValueField) {
	    try {
	        Float min = null;
	        Float max = null;

	        if (!minValueField.getText().isEmpty()) {
	            min = Float.parseFloat(minValueField.getText());
	        }
	        if (!maxValueField.getText().isEmpty()) {
	            max = Float.parseFloat(maxValueField.getText());
	        }

	        if (min != null && max != null && min > max) {
	            throw new IllegalArgumentException("Minimum value must be less than maximum value");
	        }

	        List<Variant> filteredVariants = new ArrayList<>();

	        for (Variant v : currentListOfVariants.getVars()) {
	            Double statValue = null;

	            switch (stat) {
	                case "depth":
	                    statValue = v.getDepth();
	                    break;
	                case "quality":
	                    statValue = v.getQualityScore();
	                    break;
	                case "missingness":
	                    statValue = v.getVariantMissingness();
	                    break;
	            }

	            if ((min == null || statValue >= min) && (max == null || statValue <= max)) {
	                filteredVariants.add(v);
	            }
	        }
	        currentListOfVariants = new CombinedVariants(filteredVariants);

	        updateFilteredStats();

	    } catch (NumberFormatException ex) {
	        JOptionPane.showMessageDialog(main, "Filter must be a numerical value.");
	    } catch (IllegalArgumentException ex) {
	        JOptionPane.showMessageDialog(main, ex.getMessage());
	    }
	}
	private void resetFilterOptions(JCheckBox minimumBox, JTextField minValueField,JCheckBox maximumBox, JTextField maxValueField) {
		
			minimumBox.setSelected(false);
			minValueField.setText(null);
			minValueField.setEnabled(false);
			
			maximumBox.setSelected(false);
			maxValueField.setText(null);
			maxValueField.setEnabled(false);
	}


	private void updateFilteredStats() {
		JPanel menuBarPanel = new JPanel( new BorderLayout() ); 
		
		JPanel updatedStats = new JPanel ( new GridLayout(1,2) );
		
		JPanel rightSide = new JPanel(new GridLayout(4,1));

		JTextArea titleText = new JTextArea("\nAdd more filters: \n");
		titleText.setEditable(false);
		titleText.setBackground(new Color(0,0,0,0));
		titleText.setMargin(new Insets(10,10,10,10));
		rightSide.add(titleText);
		rightSide.add(depthButton);
		rightSide.add(qualityButton);
		rightSide.add(missingnessButton);
		
		JTextArea stats = new JTextArea("Your filtered file contains " + currentListOfVariants.getVars().size() + " variants" + newline);
		stats.append( ( vcfFileStats.numVariants() - currentListOfVariants.getVars().size() + " variants have been removed." + newline + newline));
		stats.append(resultTextLeft());
		stats.setLineWrap(true);
		stats.setWrapStyleWord(true);
		stats.setMargin(new Insets(10,10,10,10));
		stats.setEditable(false);
		updatedStats.add(stats);
		updatedStats.add(rightSide);
		menuBarPanel.add( mySaveMenuBar() , BorderLayout.NORTH);
		menuBarPanel.add(updatedStats, BorderLayout.CENTER);
		
		main.add(menuBarPanel);
		
        depthButton.addActionListener(this::depthButtonActionPerformed);
        qualityButton.addActionListener(this::qualityButtonActionPerformed);
        missingnessButton.addActionListener(this::missingnessButtonActionPerformed);
		
        main.setContentPane(menuBarPanel);
		main.revalidate();
		main.repaint();
		main.setVisible(true);
	}
	
	private JMenuBar mySaveMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.GRAY);
		menuBar.setOpaque(true);
		JMenu fileMenu = new JMenu("Options");
        menuBar.add(fileMenu);

        JMenuItem saveMenuItem = new JMenuItem("Save Filtered File");
        saveMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFile();	
			}
        });
        fileMenu.add(saveMenuItem);

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exitProgram();				
			}        	
        });
        fileMenu.add(exitMenuItem);
        
		return menuBar;
	}
	
	private void saveFile() {
		saveFileChooser.setDialogTitle("Save File");
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Variant Call Format (VCF)", "vcf");
        saveFileChooser.setFileFilter(filter);
        
        int result = saveFileChooser.showSaveDialog(main);
		
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveFileChooser.getSelectedFile();
            if ( selectedFile != null ) {
	            String filePath = selectedFile.getAbsolutePath();
	            if (!filePath.toLowerCase().endsWith(".vcf")) {
	                selectedFile = new File(filePath + ".vcf");
	            }
	
	            // write new file
	            try (FileWriter writer = new FileWriter(selectedFile)) {
	   
	            	for ( String s : titles) {
	            		writer.write(s + "\t"); //headers
	            	}
	            	writer.write("\n");
	            	
	            	for ( Variant v : currentListOfVariants.getVars() ) {
	            		for ( Object o : v.getValues() ) {
	            			writer.write(o + "\t"); //values
	            		}
	            		writer.write("\n");
	            	}
	
	                JOptionPane.showMessageDialog(main, "File saved successfully!");
	                
	            } catch (IOException e) {
	                e.printStackTrace();
	                JOptionPane.showMessageDialog(main, "Error saving file");
	            }
            }
        }
	}

	private void exitProgram() {
		System.exit(0);
	}
	
	public static void main(String[] args) {
		new GUI();
	}

}
