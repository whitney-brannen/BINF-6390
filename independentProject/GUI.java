package independentProject;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class GUI extends JFrame{

	final JFileChooser fileChooser = new JFileChooser();
	
	private static final long serialVersionUID = 5979686899134093646L;

	public GUI() {
		initialize();
	}

	public void initialize() {
		setTitle("");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        setVisible(true);
	}
	
	public static void main(String[] args) {
		new GUI();

	}

}
