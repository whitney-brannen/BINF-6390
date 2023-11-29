package independentProject;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class GUI extends JFrame{
	
	private static final long serialVersionUID = 5979686899134093646L;
	
	private JTextArea mainTextArea = new JTextArea();
	private JButton chooseFileButton = new JButton("Select a file");
	private final JFileChooser fileChooser = new JFileChooser( FileSystemView.getFileSystemView() );
	
	private Thread backgroundParser = new Thread();
	private volatile boolean backgroundParserRunning = false;
	private volatile boolean parserCancel = false;
	private JButton loadingCancelButton = new JButton("Cancel");
	
	private CombinedVariants vcfFileStats; 
	

	public GUI() {
		this.vcfFileStats = null;
		initializeHomePage();
	}

	public void initializeHomePage() {
		setTitle("");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
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
        add(panel);
        setVisible(true);
	}
	
	public void loadingPage(File file) throws IOException {
		
		JPanel panel = new JPanel( new BorderLayout() );
		mainTextArea.setEditable(false);
		panel.add(mainTextArea,BorderLayout.CENTER);
		panel.add(loadingCancelButton,BorderLayout.SOUTH);
		add(panel);
		
		mainTextArea.setText("File Selected: " + fileChooser.getSelectedFile().getAbsolutePath() + "\n");
		
		BufferedReader reader = new BufferedReader( new FileReader( fileChooser.getSelectedFile() ));
		launchBackgroundParserThread(reader);
		
		loadingCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parserCancel = true;
				mainTextArea.setText("Operation Cancelled");
				loadingCancelButton.setVisible(false);
			}
		});
		
		setVisible(true);
		
		updateLoadingText();
		
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
					resultsPage();
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
		
		// I plan on later changing this to be multithreaded
		while ( reader.ready() ) {
						
			List<Object> values = new ArrayList<>();
			Map<String,Object> variantLineInfo= new HashMap<>();
			
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
		// use overall.*methodName*() for avg calcs
		vcfFileStats = new CombinedVariants(allVariantsInFile);
	}
	
	public void updateLoadingText() {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
	            @Override
	            public void run() {
	            	if ( backgroundParserRunning && !parserCancel) {
	            		mainTextArea.append("Loading...\n");
	            	}
	            	else {
	            		timer.cancel();
	            	}
	            }
	        }, 500, 1000);
	}
	
	public void resultsPage() {
		mainTextArea.setText("Your stats below\n..........\n");
	}
	
	public static void main(String[] args) {
		new GUI();

	}

}
