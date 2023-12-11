/*
 * currently not reupdating after filtering?
 */
package independentProject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

	public GUI() {
		this.vcfFileStats = null;
		initializeHomePage();
	}

	public void initializeHomePage() {
		main.setTitle("title");
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setSize(700, 500);
		main.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        mainTextArea.setText("Use this program to ....\n\nWorks best if you....\n");
        mainTextArea.setEditable(false);
        mainTextArea.setMargin(new Insets(10, 10, 10, 10));
        panel.add(mainTextArea);
        
        JPanel fileSelectionPanel = new JPanel();
        fileSelectionPanel.add(chooseFileButton);
        panel.add(fileSelectionPanel, BorderLayout.PAGE_END);
                
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
		
		mainTextArea.setText("File Selected: " + fileChooser.getSelectedFile().getAbsolutePath() + "\n");
		
		BufferedReader reader = new BufferedReader( new FileReader( fileChooser.getSelectedFile() ));
		launchBackgroundParserThread(reader);
		
		main.setVisible(true);
		
		updateLoadingText();
		
		loadingCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parserCancel = true;
				mainTextArea.setText("Operation Cancelled");
				loadingCancelButton.setVisible(false);
			}
		});
	
	}
	
	public void launchBackgroundParserThread(BufferedReader reader) {
		backgroundParser = new Thread ( new Runnable() {
			@Override
			public void run() {
				try {
					backgroundParserRunning = true;
					if ( !parserCancel ) {
						parseFile(reader);
					}
					backgroundParserRunning = false;
					loadingCancelButton.setVisible(false);
				} catch (IOException | InterruptedException e ) {
					e.printStackTrace();
					backgroundParserRunning = false;
				}
			}});
		backgroundParser.start();
	}
	
	public void parseFile(BufferedReader reader) throws IOException, InterruptedException {
		
		List<String> titles = new ArrayList<>();
		List<Variant> allVariantsInFile = new ArrayList<>(); 
		
		// I plan on later changing this to be multithreaded, divide number of lines by threads and each thread reads its set of lines?
		while ( reader.ready() ) {
						
			List<Object> values = new ArrayList<>();
			Map<String,Object> variantLineInfo= new LinkedHashMap<>();
			
			String line = reader.readLine();
			
			// handle title line
			if (line.startsWith("#CHROM")) {
				String[] titleLine = line.split("\t");
				titles = Arrays.asList(titleLine);
			}
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
		// Add all instances of Variant to the CombinedVariant class for avg calculations
		vcfFileStats = new CombinedVariants(allVariantsInFile);
		currentListOfVariants = vcfFileStats;
	}
	
	public void updateLoadingText() {
	    SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
	        @Override
	        protected Void doInBackground() throws Exception {
	            while (backgroundParserRunning && !parserCancel) {
	                publish("Loading...\n");
	                Thread.sleep(1000);
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
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	        }
	    };

	    worker.execute();
	}

	
	public void resultsPage() {
		
		mainTextArea.setText("Your input file contains " + vcfFileStats.numVariants() + " variants.");
		mainTextArea.setAlignmentX(CENTER_ALIGNMENT);
		mainPanel.add(mainTextArea, BorderLayout.NORTH);
		
		JPanel grid = new JPanel( new GridLayout(0,2) );
		
		JTextArea leftGrid = new JTextArea();
		leftGrid.setMargin(new Insets(10, 10, 10, 10));
		leftGrid.setLineWrap(true);
		leftGrid.setWrapStyleWord(true);
		leftGrid.setEditable(false);
		leftGrid.setText( resultTextLeft(vcfFileStats) );
		
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

	private String resultTextLeft(CombinedVariants variants) {
		StringBuffer text = new StringBuffer();
		text.append("Average read depth: " + variants.getDepth() + newline);
		text.append("Average quality score: " + variants.getQualityScore() + newline);
		text.append("Average Variant Missingness: " + variants.getVariantMissingness() + newline);
		text.append("Average homozygous reference (0/0) genotype frequency: " + variants.getHomozygousRefGenotypeFreq() + newline);
		text.append("Average homozygous alternative (1/1) genotype frequency: " + variants.getHomozygousAltGenotypeFreq() + newline);
		text.append("Average heterozygosity: " + variants.getHeterozygosity() + newline);
		text.append("Average reference allele frequency: " + variants.getRefAlleleFreq() + newline);
		text.append("Average alternative allele frequency: " + variants.getAltAlleleFreq() + newline);
		return text.toString();
	}
	
	private void resultPanelRight(JPanel rightGrid) {
		rightTitle.setText(" Use buttons below for more details and filtering");
		rightGrid.setBackground(Color.white);
		rightGrid.add(rightTitle);
		rightGrid.add(depthButton);
		rightGrid.add(qualityButton);
		rightGrid.add(missingnessButton);
	}
	
	
	private void displayDepthDetails() {
	    depth.setSize(600, 400);
	    depth.setLocationRelativeTo(main);
	    
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
	    combinedPanel.add(depthFilterOptionsPanel); // Assuming filterOptionsPanel is defined globally

	    depthPanel.add(combinedPanel, BorderLayout.CENTER);
	    depth.setContentPane(depthPanel);
	    depth.revalidate();
	    depth.repaint();
	    depth.setVisible(true);
	}

	
	private void displayQualityDetails() {
		
		quality.setSize(600,400);
		quality.setLocationRelativeTo(main);
		JPanel qualityPanel = new JPanel ( new GridLayout(1,2) );
		quality.setTitle("Quality");
		
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
		JPanel missingnessPanel = new JPanel ( new GridLayout(1,2) );
		missingness.setTitle("Missingness");
		
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

	    minimumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            minValue.setEnabled(true);
	            minValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            minValue.setEnabled(false);
	            minValue.setText(null);
	        }
	    });

	    maximumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            maxValue.setEnabled(true);
	            maxValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            maxValue.setEnabled(false);
	            maxValue.setText(null);
	        }
	    });

	    filterData.addActionListener(e -> {
	        applyDepthFilters(minValue, maxValue, minimumBox, maximumBox);
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

	    minimumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            minValue.setEnabled(true);
	            minValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            minValue.setEnabled(false);
	            minValue.setText(null);
	        }
	    });

	    maximumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            maxValue.setEnabled(true);
	            maxValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            maxValue.setEnabled(false);
	            maxValue.setText(null);
	        }
	    });

	    filterData.addActionListener(e -> {
	        applyQualityFilters(minValue, maxValue, minimumBox, maximumBox);
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

	    minimumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            minValue.setEnabled(true);
	            minValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            minValue.setEnabled(false);
	            minValue.setText(null);
	        }
	    });

	    maximumBox.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	            maxValue.setEnabled(true);
	            maxValue.requestFocus();
	            filterData.setEnabled(true);
	        } else {
	            maxValue.setEnabled(false);
	            maxValue.setText(null);
	        }
	    });

	    filterData.addActionListener(e -> {
	        applyMissingnessFilters(minValue, maxValue, minimumBox, maximumBox);
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
		
		JPanel updatedStats = new JPanel ( new GridLayout(1,2) );
		
		JPanel rightSide = new JPanel(new GridLayout(4,0));

		JTextField titleText = new JTextField("\nRefilter\n");
		titleText.setEditable(false);
		rightSide.add(titleText);
		rightSide.add(depthButton);
		rightSide.add(qualityButton);
		rightSide.add(missingnessButton);
		rightSide.setBackground(Color.white);
		
		JTextArea stats = new JTextArea("Your filtered file contains " + currentListOfVariants.getVars().size() + " variants" + newline);
		stats.append( ( vcfFileStats.numVariants() - currentListOfVariants.getVars().size() + " variants have been removed." + newline + newline));
		stats.append(resultTextLeft(currentListOfVariants));
		stats.setLineWrap(true);
		stats.setWrapStyleWord(true);
		stats.setMargin(new Insets(10,10,10,10));
		stats.setEditable(false);
		updatedStats.add(stats);
		updatedStats.add(rightSide);
		
		main.add(updatedStats);
		
        depthButton.addActionListener(this::depthButtonActionPerformed);
        qualityButton.addActionListener(this::qualityButtonActionPerformed);
        missingnessButton.addActionListener(this::missingnessButtonActionPerformed);
		
		main.revalidate();
		main.repaint();

		main.setVisible(true);
	}

	
	public static void main(String[] args) {
		new GUI();
	}

}
