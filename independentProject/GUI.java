package independentProject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
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
	
	private JPanel leftPanel = new JPanel(new BorderLayout());

	private JLabel rightTitle = new JLabel();
	private JButton depthButton = new JButton("Depth");
	private JButton qualityButton = new JButton("Quality");
	private JButton missingnessButton = new JButton("Variant Missingness");
	
	private JPanel filterOptionsPanel = new JPanel();
	JCheckBox minimumBox = new JCheckBox("Set minimum value");
	JCheckBox maximumBox = new JCheckBox("Set maximum value");
	
	private JTextField minValue = new JTextField(20);
	private JTextField maxValue = new JTextField(20);
	
	private JFrame depth = new JFrame();
	
	private CombinedVariants vcfFileStats; 
	

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
		
		JPanel mainPanel = new JPanel( new BorderLayout() );
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
		rightGrid.setLayout(new BoxLayout(rightGrid, BoxLayout.Y_AXIS));
		resultPanelRight(rightGrid);

		grid.add(leftGrid);
		grid.add(rightGrid);
		
		mainPanel.add(grid,BorderLayout.CENTER);
		
		main.add(mainPanel);
		main.setVisible(true);
		
		depthButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(mainPanel);
				displayDepthDetails();		
			}
		});
		qualityButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(mainPanel);
				displayQualityDetails();
			}
		});
		missingnessButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(mainPanel);
				displayMissingnessDetails();	
			}
		});
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
		rightTitle.setText(" Use buttons below for more details");
		rightGrid.setBackground(Color.white);
		rightGrid.add(rightTitle);
		rightGrid.add(depthButton);
		rightGrid.add(qualityButton);
		rightGrid.add(missingnessButton);
	}
	
	private void displayDepthDetails() {
		
		depth.setSize(600,400);
		depth.setLocationRelativeTo(main);
		depth.setLocationRelativeTo(null);
		JPanel depthPanel = new JPanel ( new GridLayout(1,2) );
		depth.setTitle("Depth");

		filterOptions("depth");
				
		JPanel header = new JPanel();
		header.setBackground(Color.white);

		header.add(new JTextArea("Depth values: \n"));
		leftPanel.add(header, BorderLayout.NORTH);
		
		StringBuffer depths = new StringBuffer();
		for ( Variant v : vcfFileStats.getVars() ) {
			depths.append(v.getDepth() + "\n");
		}
		JTextArea individualValues = new JTextArea(depths.toString());
		JScrollPane scrollPane = new JScrollPane(individualValues);
		scrollPane.createVerticalScrollBar();
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		depthPanel.add(leftPanel);
		depthPanel.add(filterOptionsPanel);
		
		depth.add(depthPanel);
		depth.setVisible(true);
		
	}
	
	private void displayQualityDetails() {
		JPanel qualityPanel = new JPanel ( new GridLayout(1,2) );
		setTitle("Quality");
		
		filterOptions("quality");
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		
		JPanel header = new JPanel();
		header.setBackground(Color.white);
		header.add(new JButton(new AbstractAction("Back") {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(qualityPanel);
				resultsPage();	
			}
		}));
		header.add(new JTextArea("Quality scores: \n"));
		leftPanel.add(header, BorderLayout.NORTH);
		
		StringBuffer quals = new StringBuffer();
		for ( Variant v : vcfFileStats.getVars() ) {
			quals.append(v.getQualityScore() + "\n");
		}
		JTextArea individualValues = new JTextArea(quals.toString());
		JScrollPane scrollPane = new JScrollPane(individualValues);
		scrollPane.createVerticalScrollBar();
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		qualityPanel.add(leftPanel);
		qualityPanel.add(filterOptionsPanel);
		
		add(qualityPanel);
		setVisible(true);
	}
	
	private void displayMissingnessDetails() {
		JPanel missingnessPanel = new JPanel ( new GridLayout(1,2) );
		setTitle("Missingness");
		
		filterOptions("missingness");
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		
		JPanel header = new JPanel();
		header.setBackground(Color.white);
		header.add(new JButton(new AbstractAction("Back") {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(missingnessPanel);
				resultsPage();	
			}
		}));
		header.add(new JTextArea("Missingness values: \n"));
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
		missingnessPanel.add(filterOptionsPanel);
		
		add(missingnessPanel);
		setVisible(true);
	}
	
	private void filterOptions(String stat) {
		filterOptionsPanel.setLayout( new BorderLayout() );
		JTextArea title = new JTextArea("Apply filters for " + stat);
		title.setMargin(new Insets(10,10,10,10));
		JPanel options = new JPanel(new GridLayout(3,1));
		JPanel min = new JPanel ();
		JPanel max = new JPanel();
		JButton filterData = new JButton("Apply filters");
		
		filterOptionsPanel.add(title, BorderLayout.NORTH);
		min.add(minimumBox);
		min.add(minValue);
		
		max.add(maximumBox);
		max.add(maxValue);
		
		options.add(min);
		options.add(max);
		options.add(filterData);
		filterOptionsPanel.add(options);
		
		minValue.setEnabled(false);
		maxValue.setEnabled(false);
		filterData.setEnabled(false);
		
		minimumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED ) {
					minValue.setEnabled(true);
					minValue.requestFocus();
					filterData.setEnabled(true);
				}
				else {
					minValue.setEnabled(false);
					minValue.setText(null);
				}
			}	
		});
		maximumBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED ) {
					maxValue.setEnabled(true);
					maxValue.requestFocus();
					filterData.setEnabled(true);
				}
				else {
					maxValue.setEnabled(false);
					maxValue.setText(null);
				}
			}
		});
		
		filterData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Float min = null;
					Float max = null;
					if (! minValue.getText().equals(null) && ! minValue.getText().isEmpty()) {
						min = Float.parseFloat(minValue.getText());
					}
					if (! maxValue.getText().equals(null) && ! maxValue.getText().isEmpty()) {
						max = Float.parseFloat(maxValue.getText());
					}
					if ( min!=null && max!=null ) {
						if ( min > max ) {
							throw new IllegalArgumentException();
						}
					}
					
					filterData(min,max);
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(depth,"Filter must be a numerical value.");
				}
				catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(depth, "Minimum value must be less than maximum value");
				}
			}
		});
		
	}
	
	private void filterData(Float min, Float max) {
		List<Variant> filtered = new ArrayList<>();
		for ( Variant v : vcfFileStats.getVars() ) {
			double stat = v.getDepth();
		    if ((min == null || stat >= min) && (max == null || stat <= max)) {
                filtered.add(v);
		    }
		} 
		
		String updated = resultTextLeft(new CombinedVariants(filtered));
		JPanel updatedStats = new JPanel ( new GridLayout(0,2) );
		JTextArea stats = new JTextArea("Your filtered file contains " + filtered.size() + " variants" + newline);
		stats.append(updated);
		stats.setLineWrap(true);
		stats.setWrapStyleWord(true);
		stats.setMargin(new Insets(10,10,10,10));

		
		updatedStats.add(stats);
		
		main.add(updatedStats);
		main.setVisible(true);
	}

	
	public static void main(String[] args) {
		new GUI();
	}

}
