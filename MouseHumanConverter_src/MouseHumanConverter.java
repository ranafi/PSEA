
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;


public class MouseHumanConverter extends JPanel {
    final static int IMAGE_WIDTH = 400;
    final static int IMAGE_HEIGHT = 300;
    
    File inputFile;
    File outputFolder;
    
    JLabel step1StatusLabel;
    JLabel step2StatusLabel;
    
	HashMap<String, String> mouseSymbolToGroup = new HashMap<String, String>();
	HashMap<String, String> humanSymbolToGroup = new HashMap<String, String>();
	HashMap<String, ArrayList<String>> groupToMouseSymbols = new HashMap<String, ArrayList<String>>();
	HashMap<String, ArrayList<String>> groupToHumanSymbols = new HashMap<String, ArrayList<String>>();
	
    OutputGenerator outputGenerator;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 

                JFrame frame = new JFrame("MouseHumanConverter");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new MouseHumanConverter());
                frame.pack();
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }

    public MouseHumanConverter() {
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("homologene_68_mouse_human.txt")));
    		String line;
    		while((line = reader.readLine()) != null) {
    			String[] tokens = line.split("\t");
    			String group = tokens[0];
    			String taxon = tokens[1];
    			String symbol = tokens[2];
    			if (taxon.equals("9606")) {
    				humanSymbolToGroup.put(symbol, group);
    				if(!groupToHumanSymbols.containsKey(group)) groupToHumanSymbols.put(group, new ArrayList<String>());
    				groupToHumanSymbols.get(group).add(symbol);
    				
    			} else {
    				mouseSymbolToGroup.put(symbol, group);
    				if(!groupToMouseSymbols.containsKey(group)) groupToMouseSymbols.put(group, new ArrayList<String>());
    				groupToMouseSymbols.get(group).add(symbol);
    			}
    		}
    		reader.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	
        final JFileChooser fileChooser = new JFileChooser();

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;

        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step1Label = new JLabel("STEP 1    ");
        add(step1Label, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel inputFileLabel = new JLabel("Select input file:");
        add(inputFileLabel, constraints);
        constraints.gridy++;
        final JTextField inputFileTextField = new JTextField("", 35);
        add(inputFileTextField, constraints);
        constraints.gridx = 3;
        constraints.gridwidth = 1;
        final JButton inputFileButton = new JButton("Browse...");
        inputFileButton.setPreferredSize(new Dimension(inputFileButton.getPreferredSize().width, inputFileTextField.getPreferredSize().height));
        inputFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fileChooser.showOpenDialog(MouseHumanConverter.this) == JFileChooser.APPROVE_OPTION) inputFileTextField.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        add(inputFileButton, constraints);
        constraints.gridy++;
        constraints.gridx = 1;
        step1StatusLabel = new JLabel(" ");
        add(step1StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);
        
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step2Label = new JLabel("STEP 2    ");
        add(step2Label, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel outputFolderLabel = new JLabel("Select output folder:");
        add(outputFolderLabel, constraints);
        constraints.gridy++;
        final JTextField outputFolderTextField = new JTextField("", 35);
        add(outputFolderTextField, constraints);
        constraints.gridx = 3;
        constraints.gridwidth = 1;
        final JButton outputFolderButton = new JButton("Browse...");
        outputFolderButton.setPreferredSize(new Dimension(outputFolderButton.getPreferredSize().width, outputFolderTextField.getPreferredSize().height));
        outputFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(MouseHumanConverter.this) == JFileChooser.APPROVE_OPTION) outputFolderTextField.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        add(outputFolderButton, constraints);
        constraints.gridy++;
        constraints.gridx = 1;
        step2StatusLabel = new JLabel(" ");
        add(step2StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step5Label = new JLabel("STEP 3    ");
        add(step5Label, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel generateOutputLabel = new JLabel("Generate output:");
        add(generateOutputLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 2;
        constraints.gridwidth = 1;
        final JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar, constraints);
        constraints.gridx = 1;
        final JButton generateOutputButton = new JButton("START");
        generateOutputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (!progressBar.isVisible()) {
                        boolean isOK = true;
                        String text;
                        
                        text = inputFileTextField.getText();
                        if (text.equals("")) {
                            step1StatusLabel.setForeground(new Color(191, 0, 0));
                            step1StatusLabel.setText("Error: no file selected");
                            isOK = false;
                        } else {
                            step1StatusLabel.setText(" ");
                            inputFile = new File(text);
                            if (!checkStep1()) isOK = false;
                        }
                        
                        text = outputFolderTextField.getText();
                        if (text.equals("")) {
                            step2StatusLabel.setForeground(new Color(191, 0, 0));
                            step2StatusLabel.setText("Error: no folder selected");
                            isOK = false;
                        } else {
                        	step2StatusLabel.setText(" ");
                            outputFolder = new File(text);
                            if (!checkStep2()) isOK = false;
                        }
                        
                        if (isOK) {
                            generateOutputButton.setText("ABORT");
                            progressBar.setValue(0);
                            progressBar.setVisible(true);
                            
                            outputGenerator = new OutputGenerator();
                            outputGenerator.addPropertyChangeListener(new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    if ("progress" == evt.getPropertyName()) {
                                        int progress = (Integer) evt.getNewValue();
                                        progressBar.setValue(progress);
                                        if (progress >= 100 && progressBar.isVisible()) generateOutputButton.setText("DONE");
                                    }
                                }
                            });
                            outputGenerator.execute();
                        }
                    } else {
                        try {
                            outputGenerator.cancel(true);
                            outputGenerator = null;
                        } catch (Exception ex) {}
                        generateOutputButton.setText("START");
                        progressBar.setValue(0);
                        progressBar.setVisible(false);
                    }
                } catch (Exception ex) {}
            }
        });
        add(generateOutputButton, constraints);
    }

    public boolean checkStep1() {
        if (inputFile.isFile() && inputFile.exists()) return true;
        else {
            step1StatusLabel.setForeground(new Color(191, 0, 0));
            step1StatusLabel.setText("Error: this is not an existing file");
            return false;
        }
    }

    public boolean checkStep2() {
        if (outputFolder.isDirectory() && outputFolder.exists()) return true;
        else {
            step2StatusLabel.setForeground(new Color(191, 0, 0));
            step2StatusLabel.setText("Error: this is not an existing folder");
            return false;
        }
    }

    class OutputGenerator extends SwingWorker<Void, Void> {
        PrintWriter writer;

        @Override
        public Void doInBackground() {
            try {
                setProgress(0);
                writer = new PrintWriter(new FileWriter(outputFolder.getAbsolutePath() + "/human_" + inputFile.getName()), true);
                BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                double totalLines = 0;
                while (reader.readLine() != null) totalLines++;
                reader.close();
                reader = new BufferedReader(new FileReader(inputFile));
                String line;
                double currentLine = 0;
                while ((line = reader.readLine()) != null) {
                	currentLine++;
                	String[] tokens = line.split("\t", -1);
                	String delim = "";
                	for (String token : tokens) {
                		String newToken = token;
                		if (mouseSymbolToGroup.containsKey(token)) newToken = groupToHumanSymbols.get(mouseSymbolToGroup.get(token)).get(0);
            			writer.print(delim + newToken);
            			delim = "\t";
                	}
                	writer.println();
                	setProgress((int)(100d * currentLine / totalLines));
                }
                reader.close();
                writer.flush();
                writer.close();
                setProgress(100);
            } catch (Exception e) {
            	e.printStackTrace();
        	}
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            return null;
        }


        @Override
        public void done() {
            setProgress(100);
            Toolkit.getDefaultToolkit().beep();
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }   
}