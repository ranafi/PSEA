//TODO: fix error if new folder cant be made 1 level down

package com.rayzhang.PSEA;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.*;

import de.erichseifert.vectorgraphics2d.*;

import java.beans.*;
import java.util.Random;


public class PSEA extends JPanel {
    static int IMAGE_WIDTH = 500, IMAGE_HEIGHT = 700;
    static int SUMMARY_IMAGE_WIDTH = 1;
    static double SUMMARY_SCALE_FACTOR = 40d;
    
    final static NumberFormat formatter = new DecimalFormat("0.#######E0");
    final static PaintPanel paintPanel = new PaintPanel();
    final static PaintSummaryPanel paintSummaryPanel = new PaintSummaryPanel();
    final static JFrame paintFrame = new JFrame();
    final static JFrame paintSummaryFrame = new JFrame();
    
    static File itemsFile;
    static File setsFile;
    static int minSetSize;
    static int maxPermutations;
    static double logMaxPermutations;
    static double domainMin, domainMax, domainRange;
    static boolean boundByQ;
    static double paintLowerBound;
    static int numGeneNamesDraw;
    static int imageWidth;
    static File outputFolder;
    static String exportFormat;
    
    static boolean printGeneNames = true;
    static double fontScaler = 40d;
    static Color lineColor1 = new Color(255, 191, 0, 255);
    static Color lineColor2 = new Color(255, 191, 0, 191);
    static Color lineColor3 = new Color(255, 191, 0, 127);
    static Color lineColor4 = new Color(255, 191, 0, 63);
    /*
    static boolean isHela = true;
    static Color lineColor1 = (isHela ? new Color(191, 0, 0, 255) : new Color(0, 0, 191, 255));
    static Color lineColor2 = (isHela ? new Color(191, 0, 0, 191) : new Color(0, 0, 191, 191));
    static Color lineColor3 = (isHela ? new Color(191, 0, 0, 127) : new Color(0, 0, 191, 127));
    static Color lineColor4 = (isHela ? new Color(191, 0, 0, 63) : new Color(0, 0, 191, 63));
    static double mPhase = (isHela ? 6.002110515424685 : 0.4721988617653557); 
    static double sPhase = (isHela ? 3.5803858072173615 : 3.6926724376750655);
    static double g1Phase = (mPhase > sPhase ? ((mPhase + sPhase) / 2d) - 3.14 : (mPhase + sPhase) / 2d);
    static double g2Phase = (mPhase > sPhase ? (mPhase + sPhase) / 2d : ((mPhase + sPhase) / 2d) - 3.14);
    */
    
    JLabel step1StatusLabel;
    JLabel step2StatusLabel;
    JLabel step3StatusLabel;
    JLabel step4StatusLabel;
    JLabel step5StatusLabel;

    Random random = new Random();
    OutputGenerator outputGenerator;

    HashMap<String, Double> items = new HashMap<String, Double>();
    HashMap<String, HashSet<String>> sets = new HashMap<String, HashSet<String>>();
    TreeMap<Integer, HashSet<String>> setSizeMap = new TreeMap<Integer, HashSet<String>>();
    
    ArrayList<SortableSet> sortedSetList = new ArrayList<SortableSet>();
    public class SortableSet implements Comparable<SortableSet>{
        public String id;
        public int value;

        public SortableSet(String _id, int _value) {
            id = _id;
            value = _value;
        }

        public int compareTo(SortableSet otherSortableSet) {
            return value - otherSortableSet.value;
        }
    }
    
    public class GrangerItem implements Comparable<GrangerItem>{
        public String id;
        public double value;
        public double delta;

        public GrangerItem(String _id, double setValues, double _delta) {
            id = _id;
            value = setValues;
            delta = _delta;
        }

        public int compareTo(GrangerItem other) {
            if (delta < other.delta) return -1;
            else if (delta > other.delta) return 1;
            else {
                if (value < other.value) return -1;
                else if (value > other.value) return 1;
                else {
                    if (id.length() < other.id.length()) return -1;
                    else if (id.length() > other.id.length()) return 1;
                    else return 0;
                }
            }
        }
    }
    
    public class Vector implements Comparable<Vector>{
        String name;
        double x, y, length;
        public Vector (String _name, double _x, double _y) {
            name = _name;
            x = _x;
            y = _y;
            length = Math.sqrt(x*x + y*y);
        }
        public int compareTo (Vector other) {
            if (length < other.length) return -1;
            else if (length > other.length) return 1;
            else if (name.length() < other.name.length()) return -1;
            else if (name.length() > other.name.length()) return 1;
            else return 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 

                paintFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                paintFrame.add(paintPanel);
                paintFrame.pack();
                //resultsSummaryFrame.setVisible(true);
                paintFrame.setSize(IMAGE_WIDTH + 100, IMAGE_HEIGHT + 100);

                paintSummaryFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                paintSummaryFrame.add(paintSummaryPanel);
                paintSummaryFrame.pack();
                //paintSummaryFrame.setVisible(true);
                paintSummaryFrame.setSize(SUMMARY_IMAGE_WIDTH + 100, SUMMARY_IMAGE_WIDTH + 100);

                JFrame cagsFrame = new JFrame("Phase set enrichment analysis (PSEA)");
                cagsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                cagsFrame.add(new PSEA());
                cagsFrame.pack();
                cagsFrame.setVisible(true);
                cagsFrame.setResizable(false);
            }
        });
    }

    public PSEA() {
        final JFileChooser fileChooser = new JFileChooser();
        final ArrayList<JComponent> components = new ArrayList<JComponent>();

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
        
        // STEP 1
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step1Label = new JLabel("  STEP 1    ");
        add(step1Label, constraints);
        components.add(step1Label);
        
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel itemsFileLabel = new JLabel("Select items file:");
        add(itemsFileLabel, constraints);
        components.add(itemsFileLabel);
        
        constraints.gridy++;
        final JTextField itemsFileTextField = new JTextField("", 35);
        //final JTextField itemsFileTextField = new JTextField("/Users/rayzhang/Desktop/Gene-phase set enrichent analysis/data/bfat.txt", 35);
        add(itemsFileTextField, constraints);
        components.add(itemsFileTextField);
        
        constraints.gridx = 3;
        constraints.gridwidth = 1;
        final JButton itemsFileButton = new JButton("Browse...");
        itemsFileButton.setPreferredSize(new Dimension(itemsFileButton.getPreferredSize().width, itemsFileTextField.getPreferredSize().height));
        itemsFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fileChooser.showOpenDialog(PSEA.this) == JFileChooser.APPROVE_OPTION) itemsFileTextField.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        add(itemsFileButton, constraints);
        components.add(itemsFileButton);
        
        constraints.gridy++;
        constraints.gridx = 1;
        step1StatusLabel = new JLabel(" ");
        add(step1StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);

        
        // STEP 2
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step2Label = new JLabel("  STEP 2    ");
        add(step2Label, constraints);
        components.add(step2Label);
        
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel setsFileLabel = new JLabel("Select sets file:");
        add(setsFileLabel, constraints);
        components.add(setsFileLabel);
        
        constraints.gridy++;
        final JTextField setsFileTextField = new JTextField("", 35);
        //final JTextField setsFileTextField = new JTextField("/Users/rayzhang/Desktop/Gene-phase set enrichent analysis/data/c2.cp.v4.0.symbols.gmt", 35);
        add(setsFileTextField, constraints);
        components.add(setsFileTextField);
        
        constraints.gridx = 3;
        constraints.gridwidth = 1;
        final JButton setsFileButton = new JButton("Browse...");
        setsFileButton.setPreferredSize(new Dimension(setsFileButton.getPreferredSize().width, setsFileTextField.getPreferredSize().height));
        setsFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fileChooser.showOpenDialog(PSEA.this) == JFileChooser.APPROVE_OPTION) setsFileTextField.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        add(setsFileButton, constraints);
        components.add(setsFileButton);
        
        constraints.gridy++;
        constraints.gridx = 1;
        step2StatusLabel = new JLabel(" ");
        add(step2StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);

        
        // STEP 3
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step3Label = new JLabel("  STEP 3    ");
        add(step3Label, constraints);
        components.add(step3Label);
        
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel parametersLabel = new JLabel("Select parameters:");
        add(parametersLabel, constraints);
        components.add(parametersLabel);

        constraints.gridy++;
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout());
        final JLabel domainMinLabel = new JLabel("Domain from ");
        panel3.add(domainMinLabel);
        components.add(domainMinLabel);
        final JTextField domainMinTextField = new JTextField("0", 3);
        panel3.add(domainMinTextField);
        components.add(domainMinTextField);
        final JLabel domainMaxLabel = new JLabel(" to ");
        panel3.add(domainMaxLabel);
        components.add(domainMaxLabel);
        final JTextField domainMaxTextField = new JTextField("24", 3);
        panel3.add(domainMaxTextField);
        components.add(domainMaxTextField);
        add(panel3, constraints);
        
        constraints.gridy++;
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout());
        final JLabel minSetSizeLabel = new JLabel("Min items / set: ");
        panel1.add(minSetSizeLabel);
        components.add(minSetSizeLabel);
        final JTextField minSetSizeTextField = new JTextField("10", 3);
        panel1.add(minSetSizeTextField);
        components.add(minSetSizeTextField);
//        add(panel1, constraints);
        final JLabel maxPermutationsLabel = new JLabel("    Max sims / test: ");
        panel1.add(maxPermutationsLabel);
        components.add(maxPermutationsLabel);
        final JTextField maxPermutationsTextField = new JTextField("10000", 6);
        panel1.add(maxPermutationsTextField);
        components.add(maxPermutationsTextField);
        add(panel1, constraints);
        
        constraints.gridy++;
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout());
        final JLabel paintLowerBoundLabel1 = new JLabel("Save image if [");
        panel4.add(paintLowerBoundLabel1);
        components.add(paintLowerBoundLabel1);
        final JRadioButton pButton = new JRadioButton("p ");
        pButton.setSelected(false);
        final JRadioButton qButton = new JRadioButton("q");
        qButton.setSelected(true);
        final ButtonGroup pqButtonGroup = new ButtonGroup();
        pqButtonGroup.add(pButton);
        pqButtonGroup.add(qButton);
        panel4.add(pButton);
        components.add(pButton);
        panel4.add(qButton);
        components.add(qButton);
        final JLabel paintLowerBoundLabel2 = new JLabel("] value < ");
        panel4.add(paintLowerBoundLabel2);
        components.add(paintLowerBoundLabel2);
        final JTextField paintLowerBoundTextField = new JTextField("0.05", 4);
        panel4.add(paintLowerBoundTextField);
        components.add(paintLowerBoundTextField);
        add(panel4, constraints);
        
        final JCheckBox showGeneNamesCheckBox = new JCheckBox("Print genes");
        showGeneNamesCheckBox.setSelected(true);
        panel4.add(showGeneNamesCheckBox);
        components.add(showGeneNamesCheckBox);
        
        /*
        constraints.gridy++;
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout());
        final JLabel imageWidthLabel = new JLabel("Image width (px): ");
        panel5.add(imageWidthLabel);
        components.add(imageWidthLabel);
        final JTextField imageWidthTextField = new JTextField("500", 4);
        panel5.add(imageWidthTextField);
        components.add(imageWidthTextField);
        final JLabel numGeneNamesDrawLabel = new JLabel("    Max item names to draw: ");
        panel5.add(numGeneNamesDrawLabel);
        components.add(numGeneNamesDrawLabel);
        final JTextField numGeneNamesDrawTextField = new JTextField("20", 3);
        panel5.add(numGeneNamesDrawTextField);
        components.add(numGeneNamesDrawTextField);
        add(panel5, constraints);
        
        constraints.gridy++;
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout());
        final JLabel summaryImageWidthLabel = new JLabel("* Width of CDF images. Summary images also sized proportionally.");
        Font summaryImageWidthLabelFont = summaryImageWidthLabel.getFont();
        summaryImageWidthLabel.setFont(new Font(summaryImageWidthLabelFont.getFontName(), Font.ITALIC, summaryImageWidthLabelFont.getSize()));
        summaryImageWidthLabel.setForeground(Color.GRAY);
        panel6.add(summaryImageWidthLabel);
        add(panel6, constraints);
        */
        
        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.gridx = 1;
        step3StatusLabel = new JLabel(" ");
        add(step3StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);

        
        // STEP 4
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step4Label = new JLabel("  STEP 4    ");
        add(step4Label, constraints);
        components.add(step4Label);
        
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel outputFolderLabel = new JLabel("Select output folder:");
        add(outputFolderLabel, constraints);
        components.add(outputFolderLabel);
        
        constraints.gridy++;
        final JTextField outputFolderTextField = new JTextField("", 35);
        //final JTextField outputFolderTextField = new JTextField("/Users/rayzhang/Desktop/Gene-phase set enrichent analysis/temp_output2", 35);
        add(outputFolderTextField, constraints);
        components.add(outputFolderTextField);
        
        constraints.gridx = 3;
        constraints.gridwidth = 1;
        final JButton outputFolderButton = new JButton("Browse...");
        outputFolderButton.setPreferredSize(new Dimension(outputFolderButton.getPreferredSize().width, outputFolderTextField.getPreferredSize().height));
        outputFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(PSEA.this) == JFileChooser.APPROVE_OPTION) outputFolderTextField.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        add(outputFolderButton, constraints);
        components.add(outputFolderButton);

        constraints.gridy++;
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout());
        final JLabel exportFormatLabel = new JLabel("Export images as: ");
        panel7.add(exportFormatLabel);
        components.add(exportFormatLabel);
        final JRadioButton svgButton = new JRadioButton(".svg (fastest) ");
        svgButton.setSelected(true);
        final JRadioButton pdfButton = new JRadioButton(".pdf ");
        pdfButton.setSelected(false);
        final JRadioButton epsButton = new JRadioButton(".eps ");
        epsButton.setSelected(false);
        final ButtonGroup exportFormatButtonGroup = new ButtonGroup();
        exportFormatButtonGroup.add(svgButton);
        exportFormatButtonGroup.add(pdfButton);
        exportFormatButtonGroup.add(epsButton);
        panel7.add(svgButton);
        components.add(svgButton);
        panel7.add(pdfButton);
        components.add(pdfButton);
        panel7.add(epsButton);
        components.add(epsButton);
        add(panel7, constraints);
        
        constraints.gridy++;
        constraints.gridx = 1;
        step4StatusLabel = new JLabel(" ");
        add(step4StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);

        
        // STEP 5
        constraints.gridy++;
        constraints.gridx = 0;
        final JLabel step5Label = new JLabel("  STEP 5    ");
        add(step5Label, constraints);
        components.add(step5Label);
        
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        final JLabel generateOutputLabel = new JLabel("Generate output:");
        add(generateOutputLabel, constraints);
        components.add(generateOutputLabel);
        
        constraints.gridy++;
        constraints.gridx = 2;
        constraints.gridwidth = 1;
        final JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar, constraints);
        components.add(progressBar);
        
        constraints.gridx = 1;
        final JButton generateOutputButton = new JButton("START");
        generateOutputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (!progressBar.isVisible()) {
                        boolean isOK = true;
                        String text;
                        
                        try {
                            step3StatusLabel.setText(" ");
                            minSetSize = Integer.parseInt(minSetSizeTextField.getText());
                            maxPermutations = Integer.parseInt(maxPermutationsTextField.getText());
                            logMaxPermutations = -Math.log10(maxPermutations);
                            domainMin = Double.parseDouble(domainMinTextField.getText());
                            domainMax = Double.parseDouble(domainMaxTextField.getText());
                            domainRange = Math.abs(domainMax - domainMin);
                            boundByQ = qButton.isSelected();
                            paintLowerBound = Double.parseDouble(paintLowerBoundTextField.getText());
                            printGeneNames = showGeneNamesCheckBox.isSelected();
                            //numGeneNamesDraw = Integer.parseInt(numGeneNamesDrawTextField.getText());
                            //imageWidth = Integer.parseInt(imageWidthTextField.getText());
                            
                            numGeneNamesDraw = 1000;
                            imageWidth = 500;
                            if (!checkStep3()) isOK = false;
                        } catch (Exception ee) {
                            step3StatusLabel.setForeground(new Color(191, 0, 0));
                            step3StatusLabel.setText("Error: parameters not valid");
                            isOK = false;
                        }
                        
                        text = itemsFileTextField.getText();
                        if (text.equals("")) {
                            step1StatusLabel.setForeground(new Color(191, 0, 0));
                            step1StatusLabel.setText("Error: no file selected");
                            isOK = false;
                        } else {
                            step1StatusLabel.setText(" ");
                            itemsFile = new File(text);
                            if (!checkStep1()) isOK = false;
                        }
                        
                        text = setsFileTextField.getText();
                        if (text.equals("")) {
                            step2StatusLabel.setForeground(new Color(191, 0, 0));
                            step2StatusLabel.setText("Error: no file selected");
                            isOK = false;
                        } else {
                            step2StatusLabel.setText(" ");
                            setsFile = new File(text);
                            if (!checkStep2()) isOK = false;
                        }
                        
                        text = outputFolderTextField.getText();
                        if (text.equals("")) {
                            step4StatusLabel.setForeground(new Color(191, 0, 0));
                            step4StatusLabel.setText("Error: no file selected");
                            isOK = false;
                        } else {
                            step4StatusLabel.setText(" ");
                            outputFolder = new File(text);
                            if (!checkStep4()) isOK = false;
                        }
                        
                        if (svgButton.isSelected()) exportFormat = "svg";
                        else if (pdfButton.isSelected()) exportFormat = "pdf";
                        else exportFormat = "eps";
                        
                        if (isOK) {
                            for (JComponent component : components) component.setEnabled(false);
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
                        for (JComponent component : components) component.setEnabled(true);
                        generateOutputButton.setText("START");
                        progressBar.setValue(0);
                        progressBar.setVisible(false);
                    }
                } catch (Exception ex) {}
            }
        });
        add(generateOutputButton, constraints);
        
        constraints.gridy++;
        constraints.gridx = 1;
        step5StatusLabel = new JLabel(" ");
        add(step5StatusLabel, constraints);
        constraints.gridy++;
        constraints.gridx = 0;
        add(new JLabel(" "), constraints);
    }

    public boolean checkStep1() {
        items = new HashMap<String, Double>();
        
        String line = "";
        int lineNum = 0;
        try {
            if (itemsFile.isFile() && itemsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(itemsFile));
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    if (!line.trim().equals("")) {
                        String[] tokens = line.split("\t");
                        if (tokens.length != 2) {
                            step1StatusLabel.setForeground(new Color(191, 0, 0));
                            step1StatusLabel.setText("Error: " + (tokens.length > 2 ? "more" : "less") + " than 2 columns found on line " + lineNum);
                            reader.close();
                            return false;
                        } else {
                            String id = tokens[0].trim().toUpperCase();
                            double value = Double.parseDouble(tokens[1].trim());
                            if (items.containsKey(id)) {
                                step1StatusLabel.setForeground(new Color(191, 0, 0));
                                step1StatusLabel.setText("Error: duplicate item id " + id + " found on line " + lineNum);
                                reader.close();
                                return false;
                            } else items.put(id, value);
                        }
                    }
                }
                if (items.size() == 0) step1StatusLabel.setForeground(new Color(191, 0, 0));
                else step1StatusLabel.setForeground(new Color(0, 191, 0));
                step1StatusLabel.setText("Successfully loaded " + items.size() + " items");
                reader.close();
                return true;
            } else {
                step1StatusLabel.setForeground(new Color(191, 0, 0));
                step1StatusLabel.setText("Error: this is not an existing file");
                return false;
            }
        } catch (Exception ex) {
            items = new HashMap<String, Double>();
            step1StatusLabel.setForeground(new Color(191, 0, 0));
            step1StatusLabel.setText("Error: invalid data on line " + lineNum);
            return false;
        }
    }

    public boolean checkStep2() {
        sets = new HashMap<String, HashSet<String>>();
        setSizeMap = new TreeMap<Integer, HashSet<String>>();
        
        String line = "";
        int lineNum = 0;
        try {
            if (setsFile.isFile() && setsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(setsFile));
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    if (!line.trim().equals("")) {
                        String[] tokens = line.split("\t");
                        if (tokens.length >= 2 + minSetSize) {
                            String id = tokens[0].trim();
                            if (sets.containsKey(id)) {
                                step2StatusLabel.setForeground(new Color(191, 0, 0));
                                step2StatusLabel.setText("Error: duplicate set id " + id + " found on line " + lineNum);
                                reader.close();
                                return false;
                            } else {
                                HashSet<String> set = new HashSet<String>();
                                for (int i = 2; i < tokens.length; i++) {
                                    String itemId = tokens[i].trim().toUpperCase();
                                    if (items.containsKey(itemId)) set.add(itemId);
                                }
                                sets.put(id, set);
                            }
                        }
                    }
                }
                for (String key : sets.keySet()) {
                    HashSet<String> set = sets.get(key);
                    int setSize = set.size();
                    if (!setSizeMap.containsKey(setSize)) setSizeMap.put(setSize, new HashSet<String>());
                    HashSet<String> setOfSameSizedSets = setSizeMap.get(setSize);
                    setOfSameSizedSets.add(key);
                    setSizeMap.put(setSize, setOfSameSizedSets);
                }
                if (sets.size() == 0) step2StatusLabel.setForeground(new Color(191, 0, 0));
                else step2StatusLabel.setForeground(new Color(0, 191, 0));
                step2StatusLabel.setText("Successfully loaded " + sets.size() + " items");
                reader.close();
                return true;
            } else {
                step2StatusLabel.setForeground(new Color(191, 0, 0));
                step2StatusLabel.setText("Error: this is not an existing file");
                return false;
            }
        } catch (Exception ex) {
            sets = new HashMap<String, HashSet<String>>();
            step2StatusLabel.setForeground(new Color(191, 0, 0));
            step2StatusLabel.setText("Error: invalid data on line " + lineNum);
            return false;
        }
    }

    public boolean checkStep3() {
        if (minSetSize < 1) {
            step3StatusLabel.setForeground(new Color(191, 0, 0));
            step3StatusLabel.setText("Error: min items/set must be at least 1");
            return false;
        } else if (maxPermutations < 10) {
            step3StatusLabel.setForeground(new Color(191, 0, 0));
            step3StatusLabel.setText("Error: max sims/test must be at least 10");
            return false;
        } else if (paintLowerBound < 0d || paintLowerBound > 1d) {
            step3StatusLabel.setForeground(new Color(191, 0, 0));
            step3StatusLabel.setText("Error: p-value bounds must be between 0 and 1");
            return false;
        } else if (numGeneNamesDraw < 0) {
            step3StatusLabel.setForeground(new Color(191, 0, 0));
            step3StatusLabel.setText("Error: cannot draw less than 0 gene names");
            return false;
        }
        return true;
    }

    public boolean checkStep4() {
        try {
            if (!outputFolder.exists()) {
                if (outputFolder.mkdir()) return true;
                else {
                    step4StatusLabel.setForeground(new Color(0, 191, 0));
                    step4StatusLabel.setText("Error: unable to create this folder");
                    return false;
                }
            } else {
                if (outputFolder.isDirectory()) return true;
                else {
                    if (outputFolder.mkdir()) return true;
                    else {
                        step4StatusLabel.setForeground(new Color(0, 191, 0));
                        step4StatusLabel.setText("Error: unable to create this folder");
                        return false;
                    }
                }
            }
        } catch (Exception ex) {
            step4StatusLabel.setForeground(new Color(191, 0, 0));
            step4StatusLabel.setText("Error: unable to access this folder");
            return false;
        }
    }

    class OutputGenerator extends SwingWorker<Void, Void> {
        File resultFile, tempFile, bgLessThanFolder, uniLessThanFolder;
        PrintWriter writer;

        @Override
        public Void doInBackground() {
            try {
                setProgress(0);
                (bgLessThanFolder = new File(outputFolder.getAbsolutePath() + "/vsBackground")).mkdir();
                (uniLessThanFolder = new File(outputFolder.getAbsolutePath() + "/vsUniform")).mkdir();
                tempFile = new File(outputFolder.getAbsolutePath() + "/results.tmp");
                writer = new PrintWriter(new FileWriter(tempFile, true));
                writer.println("Set ID\tSet N\tKuiper p-value (vs. background)\tKuiper p-value (vs. uniform)\tVector-average magnitude\tVector-average value");
                
                TreeMap<String, Vector> vectors = new TreeMap<String, Vector>();
                TreeMap<String, Vector> vectorsUni = new TreeMap<String, Vector>();
                
                if (items.size() > minSetSize) {
                    double[] backgroundValues = new double[items.size()];
                    {
                        int ctr = 0;
                        for (Double value : items.values()) {
                            backgroundValues[ctr] = value;
                            ctr++;
                        }
                    }
                    TreeMap<Double, Double> bgPdf = getPdf(backgroundValues);
                    TreeMap<Double, Double> bgCdf = getCdf(bgPdf);
                    TreeMap<Double, Double> uniCdf = getUniCdf(bgCdf, domainMin, domainMax);

                    int setCount = 0;
                    for (int setSize : setSizeMap.keySet()) {
                        if (isCancelled()) break;
                        HashSet<String> setOfSameSizedSets = setSizeMap.get(setSize);
                        
                        ArrayList<Double> storedNullTestStatisticsSmaller = new ArrayList<Double>();
                        ArrayList<Double> storedNullTestStatisticsBigger = new ArrayList<Double>();
                        ArrayList<Double> storedNullTestStatisticsSmallerUni = new ArrayList<Double>();
                        ArrayList<Double> storedNullTestStatisticsBiggerUni = new ArrayList<Double>();

                        for (String setId : setOfSameSizedSets) {
                            if (setSize >= minSetSize) {
                                if (isCancelled()) break;
                                HashSet<String> set = sets.get(setId);
                                String[] setItemIds = new String[setSize];
                                double[] setValues = new double[setSize];
                                {
                                    int ctr = 0;
                                    for (String itemId : set) {
                                        setItemIds[ctr] = itemId;
                                        setValues[ctr] = items.get(itemId);
                                        ctr++;
                                    }
                                }
                                TreeMap<Double, Double> fgPdf = getPdf(setValues);
                                TreeMap<Double, Double> fgCdf = getCdf(fgPdf);
                                double testStatistic = getTestStatistic(fgCdf, bgCdf, false);
                                double testStatisticUni = getTestStatistic(fgCdf, uniCdf, true);

                                if (storedNullTestStatisticsSmaller.size() < 1) {
                                    double nullTestStatistic = getTestStatistic(getCdf(getPdf(sample(backgroundValues, setSize))), bgCdf, false);
                                    storedNullTestStatisticsSmaller.add(nullTestStatistic);
                                }
                                if (storedNullTestStatisticsBigger.size() < 10) {
                                    storedNullTestStatisticsBigger = storedNullTestStatisticsSmaller;
                                    for (int i = storedNullTestStatisticsBigger.size(); i < 10; i++) {
                                        if (isCancelled()) break;
                                        double nullTestStatistic = getTestStatistic(getCdf(getPdf(sample(backgroundValues, setSize))), bgCdf, false);
                                        storedNullTestStatisticsBigger.add(nullTestStatistic);
                                    }
                                    if (isCancelled()) break;
                                    Collections.sort(storedNullTestStatisticsBigger);
                                }
                                if (storedNullTestStatisticsSmallerUni.size() < 1) {
                                    double[] valuesUni = new double[setSize];
                                    for (int i = 0; i < valuesUni.length; i++) valuesUni[i] = random.nextDouble();
                                    TreeMap<Double, Double> newCdf = getCdf(getPdf(valuesUni));
                                    double nullTestStatisticUni = getTestStatistic(newCdf, getUniCdf(newCdf, 0.0, 1.0), true);
                                    storedNullTestStatisticsSmallerUni.add(nullTestStatisticUni);
                                }
                                if (storedNullTestStatisticsBiggerUni.size() < 10) {
                                    storedNullTestStatisticsBiggerUni = storedNullTestStatisticsSmallerUni;
                                    for (int i = storedNullTestStatisticsBiggerUni.size(); i < 10; i++) {
                                        if (isCancelled()) break;
                                        double[] valuesUni = new double[setSize];
                                        for (int j = 0; j < valuesUni.length; j++) valuesUni[j] = random.nextDouble();
                                        TreeMap<Double, Double> newCdf = getCdf(getPdf(valuesUni));
                                        double nullTestStatisticUni = getTestStatistic(newCdf, getUniCdf(newCdf, 0.0, 1.0), true);
                                        storedNullTestStatisticsBiggerUni.add(nullTestStatisticUni);
                                    }
                                    if (isCancelled()) break;
                                    Collections.sort(storedNullTestStatisticsBiggerUni);
                                }

                                double bestPValueSmaller = getP(testStatistic, getPrimitiveArray(storedNullTestStatisticsSmaller));
                                double bestPValueBigger = getP(testStatistic, getPrimitiveArray(storedNullTestStatisticsBigger));
                                double bestPValueSmallerUni = getP(testStatisticUni, getPrimitiveArray(storedNullTestStatisticsSmallerUni));
                                double bestPValueBiggerUni = getP(testStatisticUni, getPrimitiveArray(storedNullTestStatisticsBiggerUni));

                                while (storedNullTestStatisticsBigger.size() < maxPermutations) {
                                    if (isCancelled()) break;
                                    boolean isDone = true;
                                    
                                    double resolution = 1.0 / (double)storedNullTestStatisticsBigger.size();
                                    if (bestPValueBigger == 0d || (bestPValueBigger <= 0.2 && Math.abs(bestPValueBigger - bestPValueSmaller) >= resolution))
                                    	isDone = false;
                                    
                                    if (isDone) break;
                                    else {
                                        storedNullTestStatisticsSmaller = storedNullTestStatisticsBigger;
                                        bestPValueSmaller = bestPValueBigger;
                                        int numPermutations = Math.min(10 * storedNullTestStatisticsBigger.size(), maxPermutations);
                                        for (int i = storedNullTestStatisticsBigger.size(); i < numPermutations; i++) {
                                            if (isCancelled()) break;
                                            double nullTestStatistic = getTestStatistic(getCdf(getPdf(sample(backgroundValues, setSize))), bgCdf, false);
                                            storedNullTestStatisticsBigger.add(nullTestStatistic);
                                        }
                                        if (isCancelled()) break;
                                        Collections.sort(storedNullTestStatisticsBigger);
                                        bestPValueBigger = getP(testStatistic, getPrimitiveArray(storedNullTestStatisticsBigger));
                                    }
                                }
                                
                                while (storedNullTestStatisticsBiggerUni.size() < maxPermutations) {
                                    if (isCancelled()) break;
                                    boolean isDone = true;

                                    double resolution = 1.0 / (double)storedNullTestStatisticsBiggerUni.size();
                                    if (bestPValueBiggerUni == 0d || (bestPValueBiggerUni <= 0.2 && Math.abs(bestPValueBiggerUni - bestPValueSmallerUni) >= resolution))
                                    	isDone = false;
                                    
                                    if (isDone) break;
                                    else {
                                        storedNullTestStatisticsSmallerUni = storedNullTestStatisticsBiggerUni;
                                        bestPValueSmallerUni = bestPValueBiggerUni;
                                        int numPermutations = Math.min(10 * storedNullTestStatisticsBiggerUni.size(), maxPermutations);
                                        for (int i = storedNullTestStatisticsBiggerUni.size(); i < numPermutations; i++) {
                                            if (isCancelled()) break;
                                            double[] valuesUni = new double[setSize];
                                            for (int j = 0; j < valuesUni.length; j++) valuesUni[j] = random.nextDouble();
                                            TreeMap<Double, Double> newCdf = getCdf(getPdf(valuesUni));
                                            double nullTestStatisticUni = getTestStatistic(newCdf, getUniCdf(newCdf, 0.0, 1.0), true);
                                            storedNullTestStatisticsBiggerUni.add(nullTestStatisticUni);
                                        }
                                        if (isCancelled()) break;
                                        Collections.sort(storedNullTestStatisticsBiggerUni);
                                        bestPValueBiggerUni = getP(testStatisticUni, getPrimitiveArray(storedNullTestStatisticsBiggerUni));
                                    }
                                }
                                if (isCancelled()) break;

                                double[] vectorAverage = new double[2];
                                for (int i = 0; i < setItemIds.length; i++) {
                                    double[] point = rotatePoint(-360d*setValues[i]/(domainRange), new double[]{0d, 1d});
                                    vectorAverage[0] += point[0];
                                    vectorAverage[1] += point[1];
                                }
                                vectorAverage[0] /= (double)setItemIds.length;
                                vectorAverage[1] /= (double)setItemIds.length;
                                Vector vector = new Vector(setId, vectorAverage[0], vectorAverage[1]);
                                
                                double kuiperP = bestPValueBigger;
                                double kuiperPUni = bestPValueBiggerUni;
                                String kuiperPString = (kuiperP == 0 ? "0" : formatter.format(kuiperP));
                                String kuiperPStringUni = (kuiperPUni == 0 ? "0" : formatter.format(kuiperPUni));
                                double angle = Math.PI*2d - getRads(0, 0, vector.x, vector.y) + Math.PI/2d;
                                while (angle < 0) angle += Math.PI*2d;
                                while (angle > Math.PI*2d) angle -= Math.PI*2d;
                                angle = (angle / (Math.PI*2d)) * domainRange + domainMin;
                                writer.println(setId + "\t" + setSize + "\t" + kuiperPString + "\t" + kuiperPStringUni + "\t" + vector.length + "\t" + angle);

                                if (kuiperP < paintLowerBound) {
                                	vectors.put(vector.name, vector);
                                	
                                    ArrayList<GrangerItem> grangerItems = new ArrayList<GrangerItem>();
                                    PrintWriter writerLess1 = (kuiperP < paintLowerBound ? new PrintWriter(new FileWriter(bgLessThanFolder.getAbsolutePath() + "/" + setId + ".txt"), true) : null);
                                    if (writerLess1 != null) writerLess1.println("Item ID\tItem value\tChange in Kuiper test statistic when item removed");

                                    for (int i = 0; i < setItemIds.length; i++) {
                                        double[] setValuesLess1 = new double[setSize - 1];
                                        for (int j = 0; j < setValuesLess1.length; j++) {
                                            if (j < i) setValuesLess1[j] = setValues[j];
                                            else setValuesLess1[j] = setValues[j + 1];
                                        }
                                        TreeMap<Double, Double> fgPdfLess1 = getPdf(setValuesLess1);
                                        TreeMap<Double, Double> fgCdfLess1 = getCdf(fgPdfLess1);
                                        double testStatisticLess1 = getTestStatistic(fgCdfLess1, bgCdf, false);
                                        double delta = testStatisticLess1 - testStatistic;

                                        if (writerLess1 != null) writerLess1.println(setItemIds[i] + "\t" + setValues[i] + "\t" + delta);

                                        grangerItems.add(new GrangerItem(setItemIds[i], setValues[i], delta));
                                    }
                                    if (writerLess1 != null) {
                                    	writerLess1.flush();
                                    	writerLess1.close();
                                    }
                                    
                                    Collections.sort(grangerItems);

                                    IMAGE_WIDTH = imageWidth;
                                    IMAGE_HEIGHT = (int)(imageWidth * 1.2);
                                    paintFrame.setSize(IMAGE_WIDTH + 100, IMAGE_WIDTH + 100);
                                    paintPanel.setSize(IMAGE_WIDTH + 100, IMAGE_HEIGHT + 100);
                                    
                                    paintPanel.newR();
                                    paintPanel.r.name = setId;
                                    paintPanel.r.items = setValues.length;
                                    paintPanel.r.kuiperPString = kuiperPString;
                                    paintPanel.r.fgPdf = fgPdf;
                                    paintPanel.r.bgPdf = bgPdf;
                                    paintPanel.r.fgCdf = fgCdf;
                                    paintPanel.r.bgCdf = bgCdf;
                                    paintPanel.r.isUni = false;
                                    paintPanel.r.grangerItems = grangerItems;
                                    if (writerLess1 != null) {
                                        paintPanel.r.grangerLessThan = true;
                                        paintPanel.doPaint(bgLessThanFolder.getAbsolutePath() + "/" + setId);
                                    }
                                }

                                if (kuiperPUni < paintLowerBound) {
                                	vectorsUni.put(vector.name, vector);
                                	
                                    ArrayList<GrangerItem> grangerItems = new ArrayList<GrangerItem>();
                                    PrintWriter writerLess1 = (kuiperPUni < paintLowerBound ? new PrintWriter(new FileWriter(uniLessThanFolder.getAbsolutePath() + "/" + setId + ".txt"), true) : null);
                                    if (writerLess1 != null) writerLess1.println("Item ID\tItem value\tChange in Kuiper test statistic when item removed");

                                    for (int i = 0; i < setItemIds.length; i++) {
                                        double[] setValuesLess1 = new double[setSize - 1];
                                        for (int j = 0; j < setValuesLess1.length; j++) {
                                            if (j < i) setValuesLess1[j] = setValues[j];
                                            else setValuesLess1[j] = setValues[j + 1];
                                        }
                                        TreeMap<Double, Double> fgPdfLess1 = getPdf(setValuesLess1);
                                        TreeMap<Double, Double> fgCdfLess1 = getCdf(fgPdfLess1);
                                        double testStatisticUniLess1 = getTestStatistic(fgCdfLess1, uniCdf, true);
                                        double delta = testStatisticUniLess1 - testStatisticUni;

                                        if (writerLess1 != null) writerLess1.println(setItemIds[i] + "\t" + setValues[i] + "\t" + delta);

                                        grangerItems.add(new GrangerItem(setItemIds[i], setValues[i], delta));
                                    }
                                    if (writerLess1 != null) {
                                    	writerLess1.flush();
                                    	writerLess1.close();
                                    }

                                    Collections.sort(grangerItems);

                                    IMAGE_WIDTH = imageWidth;
                                    IMAGE_HEIGHT = (int)(imageWidth * 1.2);
                                    paintFrame.setSize(IMAGE_WIDTH + 100, IMAGE_WIDTH + 100);
                                    paintPanel.setSize(IMAGE_WIDTH + 100, IMAGE_HEIGHT + 100);
                                    
                                    paintPanel.newR();
                                    paintPanel.r.name = setId;
                                    paintPanel.r.items = setValues.length;
                                    paintPanel.r.kuiperPString = kuiperPStringUni;
                                    paintPanel.r.fgPdf = fgPdf;
                                    paintPanel.r.bgPdf = bgPdf;
                                    paintPanel.r.fgCdf = fgCdf;
                                    paintPanel.r.bgCdf = uniCdf;
                                    paintPanel.r.isUni = true;
                                    paintPanel.r.grangerItems = grangerItems;
                                    if (writerLess1 != null) {
                                        paintPanel.r.grangerLessThan = true;
                                        paintPanel.doPaint(uniLessThanFolder.getAbsolutePath() + "/" + setId);
                                    }
                                }
                            } else {
                                writer.println(setId + "\t" + setSize + "\tNA\tNA\tNA\tNA");
                            }
                            
                            setCount++;
                            setProgress((int)(((double)setCount / (double)sets.size()) * 90d));
                        }
                    }
                }
                writer.flush();
                writer.close();
                setProgress(90);
                
                resultFile = new File(outputFolder.getAbsolutePath() + "/results.txt");
                BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                ArrayList<String> setIdList = new ArrayList<String>();
                ArrayList<String> setNList = new ArrayList<String>();
                ArrayList<Double> kuiperBgList = new ArrayList<Double>();
                ArrayList<Double> kuiperUniList = new ArrayList<Double>();
                ArrayList<String> vectorMagList = new ArrayList<String>();
                ArrayList<String> vectorValList = new ArrayList<String>();
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                	String[] tokens = line.split("\t");
                	setIdList.add(tokens[0]);
                	setNList.add(tokens[1]);
                	if (tokens[2].equals("NA")) {
	                	kuiperBgList.add(-1d);
	                	kuiperUniList.add(-1d);
                	} else {
	                	kuiperBgList.add(Double.parseDouble(tokens[2]));
	                	kuiperUniList.add(Double.parseDouble(tokens[3]));
                	}
                	vectorMagList.add(tokens[4]);
                	vectorValList.add(tokens[5]);
                }
                reader.close();
                TreeMap<Double, Double> kuiperBgQMap = getQValues(kuiperBgList);
                TreeMap<Double, Double> kuiperUniQMap = getQValues(kuiperUniList);
                writer = new PrintWriter(new FileWriter(resultFile), true);
                writer.println("Set ID\tSet N\tKuiper p-value (vs. background)\tKuiper q-value (vs. background)\tKuiper p-value (vs. uniform)\tKuiper q-value (vs. uniform)\tVector-average magnitude\tVector-average value");
                for (int i = 0; i < setIdList.size(); i++) {
                	String setId = setIdList.get(i);
                	writer.print(setId);
                	writer.print("\t" + setNList.get(i));
                	if (kuiperBgList.get(i) < 0) {
	                	writer.print("\tNA\tNA\tNA\tNA");
                	} else {
                		double pBg = kuiperBgList.get(i);
                		double qBg = kuiperBgQMap.get(pBg);
                		double pUni = kuiperUniList.get(i);
                		double qUni = kuiperUniQMap.get(pUni);
	                	writer.print("\t" + pBg);
	                	writer.print("\t" + qBg);
	                	writer.print("\t" + pUni);
	                	writer.print("\t" + qUni);
	                	if (boundByQ) {
	                		if (qBg > paintLowerBound) {
		                		(new File(bgLessThanFolder.getAbsolutePath() + "/" + setId + "." + exportFormat)).delete();
		                		(new File(bgLessThanFolder.getAbsolutePath() + "/" + setId + ".txt")).delete();
		                		vectors.remove(setId);
	                		}
	                		if (qUni > paintLowerBound) {
		                		(new File(uniLessThanFolder.getAbsolutePath() + "/" + setId + "." + exportFormat)).delete();
		                		(new File(uniLessThanFolder.getAbsolutePath() + "/" + setId + ".txt")).delete();
		                		vectorsUni.remove(setId);
	                		}
	                	}
                	}
                	writer.print("\t" + vectorMagList.get(i));
                	writer.println("\t" + vectorValList.get(i));

                    setProgress((int)(((double)i / (double)setIdList.size()) * 7d) + 90);
                }
                writer.flush();
                writer.close();
                tempFile.delete();
                setProgress(98);
                
                SUMMARY_SCALE_FACTOR = (double)IMAGE_WIDTH / 500d;
                
                for (int i = 0; i < 2; i++) {
                    Graphics2D g2 = (Graphics2D) getGraphics();
                    int totalDistance = (int)(SUMMARY_SCALE_FACTOR*120d);
                    for (Vector vector : vectors.values()) {
                        int fontSize = (int)(vector.length * SUMMARY_SCALE_FACTOR * fontScaler);
                        if (fontSize > 1) {
                            g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
                            double r = totalDistance;
                            double a = getRads(0, 0, vector.x, vector.y);
                            a = Math.PI*2.0 - a;
                            Point point = getPoint(new Point(0, 0), r, a);
                            totalDistance += g2.getFontMetrics().getHeight();
                        }
                    }
                    SUMMARY_IMAGE_WIDTH = (int)(totalDistance * 2.2d);
                    paintSummaryFrame.setSize(SUMMARY_IMAGE_WIDTH + 100, SUMMARY_IMAGE_WIDTH + 100);
                    paintSummaryPanel.setSize(SUMMARY_IMAGE_WIDTH + 100, SUMMARY_IMAGE_WIDTH + 100);
                }
                paintSummaryPanel.vectors = new ArrayList<Vector>(vectors.values());
                Collections.sort(paintSummaryPanel.vectors);
                paintSummaryPanel.doPaint(outputFolder + "/vsBackground");
                setProgress(99);
                
                for (int i = 0; i < 2; i++) {
                    Graphics2D g2 = (Graphics2D) getGraphics();
                    int totalDistance = (int)(SUMMARY_SCALE_FACTOR*120d);
                    for (Vector vector : vectorsUni.values()) {
                        int fontSize = (int)(vector.length * SUMMARY_SCALE_FACTOR * fontScaler);
                        if (fontSize > 1) {
                            g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
                            double r = totalDistance;
                            double a = getRads(0, 0, vector.x, vector.y);
                            a = Math.PI*2.0 - a;
                            Point point = getPoint(new Point(0, 0), r, a);
                            totalDistance += g2.getFontMetrics().getHeight();
                        }
                    }
                    SUMMARY_IMAGE_WIDTH = (int)(totalDistance * 2.2d);
                    paintSummaryFrame.setSize(SUMMARY_IMAGE_WIDTH + 100, SUMMARY_IMAGE_WIDTH + 100);
                    paintSummaryPanel.setSize(SUMMARY_IMAGE_WIDTH + 100, SUMMARY_IMAGE_WIDTH + 100);
                }
                paintSummaryPanel.vectors = new ArrayList<Vector>(vectorsUni.values());
                Collections.sort(paintSummaryPanel.vectors);
                paintSummaryPanel.doPaint(outputFolder + "/vsUniform");
                setProgress(100);
            } catch (Exception ex) {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
                ex.printStackTrace();
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
    
    public TreeMap<Double, Double> getUniCdf(TreeMap<Double, Double> xCdf, double min, double max) {
        TreeMap<Double, Double> uniCdf = new TreeMap<Double, Double>();
        double denom = max - min;
        for (Double key : xCdf.keySet()) uniCdf.put(key, (key - min) / denom);

        //if (uniCdf.size() != xCdf.size()) System.out.println("AAA");
        return uniCdf;
    }
    
    public double[] getHist(double[] v) {
        double[] r = new double[24];
        for (double a : v) r[(int)a]++;
        for (int i = 0; i < r.length; i++) r[i] /= (double)v.length;
        return r;
    }
    
    public double getTestStatistic(TreeMap<Double, Double> xCdf, TreeMap<Double, Double> yCdf, boolean isYUni) {
        TreeSet<Double> keySet = new TreeSet<Double>();
        keySet.addAll(xCdf.keySet());
        keySet.addAll(yCdf.keySet());
        double supD = -1d, minD = 1d;
        //double n = xCdf.size(), m = yCdf.size(), sumD = 0d, sumD2 = 0d; // watson
        for (Double key : keySet) {
            Double xKey = xCdf.floorKey(key);
            double xVal = xKey != null ? xCdf.get(xKey) : 0d;
            Double yKey = yCdf.floorKey(key);
            double yVal = yKey != null ? yCdf.get(yKey) : 0d;
            double curD = xVal - yVal;
            //sumD += curD; // watson
            //sumD2 += curD * curD; // watson
            if (curD > supD) supD = curD;
            if (curD < minD) minD = curD;

            if (isYUni) {
                Double xKey2 = xCdf.lowerKey(key);
                double xVal2 = xKey2 != null ? xCdf.get(xKey2) : 0d;
                double curD2 = xVal2 - yVal;
                if (curD2 > supD) supD = curD2;
                if (curD2 < minD) minD = curD2;
            }
        }
        supD = Math.abs(supD);
        minD = Math.abs(minD);
        //double ks = Math.max(supD, minD);
        double kuiper = supD + minD;
        //double N = n + m;
        //double watson = (n * m / (N * N)) * (sumD2 - (sumD * sumD / N));
        //return new double[] {ks, kuiper, watson};
        return kuiper;
    }
    
    public TreeMap<Double, Double> getCdf(TreeMap<Double, Double> pdf) {
        TreeMap<Double, Double> cdf = new TreeMap<Double, Double>();
        //TreeMap<Double, Double> cdf = new TreeMap<Double, Double>(pdf);
        //for (Double key1 : cdf.descendingKeySet()) for (Double key2 : cdf.descendingKeySet()) if (key2 > key1) cdf.put(key2, cdf.get(key2) + cdf.get(key1));
        double sum = 0d;
        for (Double key : pdf.keySet()) {
            sum += pdf.get(key);
            cdf.put(key, sum);
        }
        if (cdf.size() != pdf.size()) System.out.println("AAA");
        return cdf;
    }

    public TreeMap<Double, Double> getPdf(double[] a) {
        double increment = 1d / (double)a.length;
        TreeMap<Double, Double> pdf = new TreeMap<Double, Double>();
        for (double v : a) {
            if (!pdf.containsKey(v)) pdf.put(v, 0d);
            pdf.put(v, pdf.get(v) + increment);
        }
        return pdf;
    }

    public double getP(double value, double[] nullDist) {
        //Arrays.sort(nullDist);
        double pLeft = 0d;
        for (int i = 0; i < nullDist.length; i++) {
            if (value < nullDist[i]) {
                pLeft = (double)(nullDist.length - i) / (double)nullDist.length;
                break;
            }
        }
        
        double pRight = 1d;
        for (int i = nullDist.length - 1; i >= 0; i--) {
            if (value > nullDist[i]) {
                pRight = (double)(nullDist.length - i - 1) / nullDist.length;
                break;
            }
        }
        
        return pLeft;
    }

    public double[] sample(double[] a, int n) {
        double[] r = new double[n];
        for (int i = 0; i < r.length; i++) r[i] = a[random.nextInt(a.length)];
        return r;
    }

    public double[] getPrimitiveArray(ArrayList<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < array.length; i++) array[i] = list.get(i);
        return array;
    }

    public static double[] rotatePoint(double angle, double[] pt) {
        double x = pt[0];
        double y = pt[1];
        double a = angle * Math.PI / 180.0;
        double cosA = Math.cos(a);
        double sinA = Math.sin(a);
        double[] pt2 = new double[2];
        pt2[0] = x * cosA - y * sinA;
        pt2[1] = x * sinA + y * cosA;
        return pt2;
    }

    public static double getRads(double x1, double y1, double x2, double y2) {
        double fullCircle = Math.PI*2;
        return (fullCircle + Math.atan2(y2 - y1, x2 - x1)) % fullCircle;
    }

    public static double getLength(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    public static Point getPoint(Point center, double r, double a) {
        int x = (int)(center.x + r * Math.cos(a));
        int y = (int)(center.y + r * Math.sin(a));
        return new Point(x, y);
    }
    
    public static TreeMap<Double, Double> getQValues(ArrayList<Double> pValueList) {
    	TreeMap<Double, Double> qValues = new TreeMap<Double, Double>();
    	TreeSet<Double> pValueSet = new TreeSet<Double>();
    	for (double value : pValueList) pValueSet.add(value);
    	double m = (double)pValueSet.size();
    	int k = 0;
    	double[] pValues = new double[pValueSet.size()];
    	double[] inners = new double[pValues.length];
    	for (double pValue : pValueSet) {
    		pValues[k] = pValue;
    		inners[k] = Math.min(pValue / ((double)(k+1)/m), 1);
    		k++;
    	}
    	int indexOfMin = -1;
    	for (int i = 0; i < inners.length; i++) {
    		double min = Double.MAX_VALUE;
    		if (indexOfMin < i) {
    			for (int j = i; j < inners.length; j++) {
    				if (inners[j] < min) {
    					indexOfMin = j;
    					min = inners[indexOfMin];
    				}
    			}
    		} else min = inners[indexOfMin];
    		qValues.put(pValues[i], min);
    	}
    	return qValues;
    }
    
    
    public static class PaintPanel extends JPanel {
        public String outputFolder = "";
        int borderL = IMAGE_WIDTH / 10, borderR = IMAGE_WIDTH / 5, borderT = IMAGE_HEIGHT / 10, borderB = IMAGE_HEIGHT / 2;
        //int thinLineSize = Math.max(1, Math.min(IMAGE_WIDTH, IMAGE_HEIGHT) / 300), thickLineSize = thinLineSize * 3;
        int thinLineSize = 1, thickLineSize = 2;
        
        public Result r;
        public class Result {
            String name;
            int items;
            String ksPString;
            String kuiperPString;
            String watsonPString;
            boolean isUni;
            TreeMap<Double, Double> fgPdf;
            TreeMap<Double, Double> bgPdf;
            TreeMap<Double, Double> fgCdf;
            TreeMap<Double, Double> bgCdf;
            ArrayList<GrangerItem> grangerItems;
            boolean grangerLessThan;
        }

        public void newR() {
            r = new Result();
            borderL = IMAGE_WIDTH / 10;
            borderR = IMAGE_WIDTH / 5;
            borderT = IMAGE_HEIGHT / 10;
            borderB = IMAGE_HEIGHT / 2;
            //thinLineSize = Math.max(1, Math.min(IMAGE_WIDTH, IMAGE_HEIGHT) / 500);
            //thickLineSize = thinLineSize * 3;
        }

        public PaintPanel() {}

        public void doPaint(String filename) {
        	ProcessingPipeline g;
        	if (exportFormat.equals("eps")) g = new EPSGraphics2D(0.0, 0.0, IMAGE_WIDTH, IMAGE_HEIGHT);
        	else if (exportFormat.equals("svg")) g = new SVGGraphics2D(0.0, 0.0, IMAGE_WIDTH, IMAGE_HEIGHT);
        	else {
        		exportFormat = "pdf";
        		g = new PDFGraphics2D(0.0, 0.0, IMAGE_WIDTH, IMAGE_HEIGHT);
        	}
        	
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.white);
            g.fillRect(0, 0, IMAGE_WIDTH*2, IMAGE_HEIGHT*2);

            if (r != null) {
                //double xMin = r.bgCdf.firstKey();
                //double xMax = r.bgCdf.lastKey();
                double xMin = domainMin;
                double xMax = domainMax;
                double xScale = (double)(IMAGE_WIDTH - borderL - borderR) / (xMax - xMin);
                double yScale = (double)(IMAGE_HEIGHT - borderT - borderB);
                
                //custom
                g.setColor(new Color(191, 191, 191, 127));
                g.fillRect(borderL + (IMAGE_WIDTH - borderR - borderL)/2, borderT, (IMAGE_WIDTH - borderR - borderL)/2, (IMAGE_HEIGHT - borderB - borderT));
                
                Double lastX = 0d, lastY = 0d;
                g.setStroke(new BasicStroke(thinLineSize));
                for (Double x : r.bgCdf.keySet()) {
                    Double y = Math.max(Math.min(r.bgPdf.get(x), 1d), 0d);
                    if (!r.isUni) {
                        g.setColor(new Color(63, 63, 191, 191));
                        g.fillRect((int)(borderL + xScale*x) - thinLineSize, (int)(IMAGE_HEIGHT - borderB - yScale*y), (int)(thinLineSize*2), (int)(yScale*y));
                    }
                    y = Math.max(Math.min(r.bgCdf.get(x), 1d), 0d);
                    g.setColor(new Color(63, 63, 191, 63));
                    if (r.isUni) {
                        g.drawLine((int)(borderL + xScale*lastX), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*y));
                    } else {
                        g.drawLine((int)(borderL + xScale*lastX), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY));
                        if (exportFormat.equals("pdf")) {
                        	g.drawLine((int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*y));
                        } else {
                        	g.drawLine((int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY - thinLineSize), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*y + thinLineSize));
                        }
                    }
                    lastX = x;
                    lastY = y;
                }
                g.drawLine((int)(borderL + xScale*lastX), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*xMax), (int)(IMAGE_HEIGHT - borderB - yScale*1d));
                
                lastX = lastY = 0d;
                g.setColor(lineColor3);
                g.setStroke(new BasicStroke(thickLineSize));
                for (Double x : r.fgCdf.keySet()) {
                    Double y = Math.max(Math.min(r.fgPdf.get(x), 1d), 0d);
                    g.setColor(lineColor2);
                    g.fillRect((int)(borderL + xScale*x) - thickLineSize, (int)(IMAGE_HEIGHT - borderB - yScale*y), (int)(thickLineSize*2), (int)(yScale*y));
                    y = Math.max(Math.min(r.fgCdf.get(x), 1d), 0d);
                    g.setColor(lineColor4);
                    if (exportFormat.equals("pdf")) {
                    	g.drawLine((int)(borderL + xScale*lastX - thinLineSize), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*x + thinLineSize), (int)(IMAGE_HEIGHT - borderB - yScale*lastY));
                    	g.drawLine((int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY - thinLineSize), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*y + thinLineSize));
                    } else {
                    	g.drawLine((int)(borderL + xScale*lastX), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY));
                    	g.drawLine((int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*lastY - thickLineSize), (int)(borderL + xScale*x), (int)(IMAGE_HEIGHT - borderB - yScale*y + thickLineSize));
                    }
                    lastX = x;
                    lastY = y;
                }
                if (exportFormat.equals("pdf"))
                	g.drawLine((int)(borderL + xScale*lastX - thinLineSize), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*xMax + thinLineSize), (int)(IMAGE_HEIGHT - borderB - yScale*1d));
                else
                	g.drawLine((int)(borderL + xScale*lastX), (int)(IMAGE_HEIGHT - borderB - yScale*lastY), (int)(borderL + xScale*xMax), (int)(IMAGE_HEIGHT - borderB - yScale*1d));

                int fontSize = 0;
                int startingDistance = IMAGE_HEIGHT/12;
                int totalDistance = startingDistance;
                int maxX = 0;
                int wordPadding = thickLineSize;
                double temp;
                for (GrangerItem grangerItem : r.grangerItems) {
                	temp = grangerItem.value;
                	grangerItem.value = grangerItem.delta;
                	grangerItem.delta = temp;
                }
                Collections.sort(r.grangerItems);
                for (GrangerItem grangerItem : r.grangerItems) {
                	temp = grangerItem.value;
                	grangerItem.value = grangerItem.delta;
                	grangerItem.delta = temp;
                }
                //if (r.grangerItems.size() <= numGeneNamesDraw) {
                if (printGeneNames) {
                    g.setStroke(new BasicStroke(thinLineSize, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, new float[]{2.0f}, 0.0f));
                    g.setColor(lineColor3);
                    for (GrangerItem grangerItem : r.grangerItems) {
                        fontSize = (int)Math.max((double)IMAGE_HEIGHT/50d + ((double)IMAGE_HEIGHT/5) *(-grangerItem.delta), (double)IMAGE_HEIGHT/100d);
                        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
                        totalDistance += g.getFontMetrics().getHeight();
                        int x = (int)(borderL + xScale*grangerItem.value);
                        if (x > maxX) totalDistance = startingDistance;
                        int x2 = x + g.getFontMetrics().stringWidth(grangerItem.id) + wordPadding;
                        if (x2 > maxX) maxX = x2;
                        int y = borderB + totalDistance;
                        g.drawLine(x, borderB, x, y);
                    }
                }
                //}

                g.setFont(new Font("Arial", Font.BOLD, Math.min(IMAGE_WIDTH, IMAGE_HEIGHT) / 20));
                g.setStroke(new BasicStroke(thickLineSize));
                g.setColor(new Color(0, 0, 0, 255));
                g.drawLine(borderL - thickLineSize, borderT, borderL - thickLineSize, IMAGE_HEIGHT - borderB);
                g.drawLine(borderL - thickLineSize, IMAGE_HEIGHT - borderB, IMAGE_WIDTH - borderR + thickLineSize, IMAGE_HEIGHT - borderB);
                //custom
                for (double i = xMin; i <= xMax; i += (xMax - xMin)) {
                    int xPos = (int)(borderL + xScale*i);
                    String text = String.valueOf(Math.round(i)); //edit round
                    g.setColor(Color.white);
                    g.fillRect(xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4), g.getFontMetrics().stringWidth(text), g.getFontMetrics().getHeight());
                    g.setColor(Color.black);
                    g.drawString(text, xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4) + g.getFontMetrics().getAscent());
                }
                
                //custom
                /*{
                	String text = "";
                	int xPos = 0;
                	text = "M";
                	xPos = (int)(borderL + xScale*mPhase);
                	g.setColor(Color.white);
                    g.fillRect(xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4), g.getFontMetrics().stringWidth(text), g.getFontMetrics().getHeight());
                    g.setColor(Color.black);
                    g.drawString(text, xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4) + g.getFontMetrics().getAscent());
                    text = "S";
                	xPos = (int)(borderL + xScale*sPhase);
                	g.setColor(Color.white);
                    g.fillRect(xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4), g.getFontMetrics().stringWidth(text), g.getFontMetrics().getHeight());
                    g.setColor(Color.black);
                    g.drawString(text, xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4) + g.getFontMetrics().getAscent());
                    text = "G1";
                	xPos = (int)(borderL + xScale*g1Phase);
                	g.setColor(Color.white);
                    g.fillRect(xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4), g.getFontMetrics().stringWidth(text), g.getFontMetrics().getHeight());
                    g.setColor(Color.black);
                    g.drawString(text, xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4) + g.getFontMetrics().getAscent());
                    text = "G2";
                	xPos = (int)(borderL + xScale*g2Phase);
                	g.setColor(Color.white);
                    g.fillRect(xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4), g.getFontMetrics().stringWidth(text), g.getFontMetrics().getHeight());
                    g.setColor(Color.black);
                    g.drawString(text, xPos - g.getFontMetrics().stringWidth(text)/2, IMAGE_HEIGHT - borderB + (thickLineSize*4) + g.getFontMetrics().getAscent());
                }*/
                
                
                for (double i = 0d; i <= 1d; i += 1d) {
                    int yPos = (int)(IMAGE_HEIGHT - borderB - yScale*i);
                    String text = String.valueOf(Math.round(i));
                    g.drawString(text, borderL - (thickLineSize*4) - g.getFontMetrics().stringWidth(text), yPos + g.getFontMetrics().getAscent()/2);
                }
                
                String title;
                title = "" + r.name;
                int newFontSize = Math.min(IMAGE_WIDTH, IMAGE_HEIGHT) / 20;
                while (g.getFontMetrics().getHeight() > borderT || g.getFontMetrics().stringWidth(title) > IMAGE_WIDTH) {
                    newFontSize--;
                    g.setFont(new Font("Arial", Font.BOLD, newFontSize));
                }
                g.drawString(title, IMAGE_WIDTH / 2 - g.getFontMetrics().stringWidth(title) / 2, borderT/2 + g.getFontMetrics().getAscent()/2);
                

                //if (r.grangerItems.size() <= numGeneNamesDraw) {
                if (printGeneNames) {
                    g.setStroke(new BasicStroke(thinLineSize, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, new float[]{2.0f}, 0.0f));
                    fontSize = 0;
                    totalDistance = startingDistance;
                    maxX = 0;
                    for (GrangerItem grangerItem : r.grangerItems) {
                        fontSize = (int)Math.max((double)IMAGE_HEIGHT/50d + ((double)IMAGE_HEIGHT/5) *(-grangerItem.delta), (double)IMAGE_HEIGHT/100d);
                        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
                        totalDistance += g.getFontMetrics().getHeight();
                        int x = (int)(borderL + xScale*grangerItem.value);
                        if (x > maxX) totalDistance = startingDistance;
                        int x2 = x + g.getFontMetrics().stringWidth(grangerItem.id) + wordPadding;
                        if (x2 > maxX) maxX = x2;
                        int y = borderB + totalDistance;
                        g.setColor(Color.white);
                        g.fillRect(x, y - g.getFontMetrics().getAscent(), g.getFontMetrics().stringWidth(grangerItem.id), g.getFontMetrics().getAscent());
                        g.setColor(Color.gray);
                        g.drawString(grangerItem.id, x, y);
                    }
                }
                //}
            }
            
            try {
	            FileOutputStream file = new FileOutputStream(filename + "." + exportFormat);
	            g.writeTo(file);
            } catch (Exception e) {e.printStackTrace();}
        }
    }
    
    
    
    
    public static class PaintSummaryPanel extends JPanel {
        int borderL = 20, borderR = 20, borderT = 20, borderB = 20;
        int thinLineSize = 5, thickLineSize = 10;
        int fontSize = 20;

        public ArrayList<Vector> vectors = new ArrayList<Vector>();
        
        public PaintSummaryPanel() {}

        public void doPaint(String filename) {
        	ProcessingPipeline g;
        	if (exportFormat.equals("eps")) g = new EPSGraphics2D(0.0, 0.0, SUMMARY_IMAGE_WIDTH, SUMMARY_IMAGE_WIDTH);
        	else if (exportFormat.equals("svg")) g = new SVGGraphics2D(0.0, 0.0, SUMMARY_IMAGE_WIDTH, SUMMARY_IMAGE_WIDTH);
        	else {
        		exportFormat = "pdf";
        		g = new PDFGraphics2D(0.0, 0.0, SUMMARY_IMAGE_WIDTH, SUMMARY_IMAGE_WIDTH);
        	}
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //g.translate(TRANS_X, TRANS_Y);
            AffineTransform originalTransform = g.getTransform();

            g.setColor(Color.white);
            g.fillRect(-SUMMARY_IMAGE_WIDTH*2, -SUMMARY_IMAGE_WIDTH*2, SUMMARY_IMAGE_WIDTH*4, SUMMARY_IMAGE_WIDTH*4);

            
            int xCenter = SUMMARY_IMAGE_WIDTH / 2;
            int yCenter = SUMMARY_IMAGE_WIDTH / 2;
            Point center = new Point(xCenter, yCenter);

            int fontSize = 0;
            int totalDistance = (int)(SUMMARY_SCALE_FACTOR*120d);
            g.setColor(lineColor1);
            g.setStroke(new BasicStroke(thinLineSize/2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, new float[]{2.0f}, 0.0f));
            for (Vector vector : vectors) {
                fontSize = (int)(vector.length * SUMMARY_SCALE_FACTOR * fontScaler);
                if (fontSize > 1) {
                    g.setFont(new Font("Arial", Font.PLAIN, fontSize));
                    double r = totalDistance;
                    double a = getRads(0, 0, vector.x, vector.y);
                    a = Math.PI*2.0 - a;
                    Point point = getPoint(center, r, a);
                    g.drawLine(xCenter, yCenter, point.x, point.y);
                    totalDistance += g.getFontMetrics().getHeight();
                }
            }

            g.setStroke(new BasicStroke(thinLineSize));
            g.setFont(new Font("Arial", Font.PLAIN, (int)(SUMMARY_SCALE_FACTOR*20d)));
            double axisRadius = SUMMARY_SCALE_FACTOR*80d;
            g.setColor(Color.white);
            g.fillOval((int)(xCenter - axisRadius), (int)(yCenter - axisRadius), (int)(axisRadius*2), (int)(axisRadius*2));
            //custom
            g.setColor(Color.LIGHT_GRAY);
            g.fillArc((int)(xCenter - axisRadius), (int)(yCenter - axisRadius), (int)(axisRadius*2), (int)(axisRadius*2), 90, 180);
            g.setColor(Color.black);
            g.drawOval((int)(xCenter - axisRadius), (int)(yCenter - axisRadius), (int)(axisRadius*2), (int)(axisRadius*2));
            
            //custom
            /*{
	            String text = "";
	            text = "M";
	            g.rotate(mPhase, xCenter, yCenter);
	            g.rotate(-mPhase, xCenter, (int)(yCenter - 30 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 30));
		        g.setTransform(originalTransform);
		        
		        text = "S";
	            g.rotate(sPhase, xCenter, yCenter);
	            g.rotate(-sPhase, xCenter, (int)(yCenter - 30 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 30));
		        g.setTransform(originalTransform);
		        
		        text = "G1";
	            g.rotate(g1Phase, xCenter, yCenter);
	            g.rotate(-g1Phase, xCenter, (int)(yCenter - 30 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 30));
		        g.setTransform(originalTransform);
		        
		        text = "G2";
	            g.rotate(g2Phase, xCenter, yCenter);
	            g.rotate(-g2Phase, xCenter, (int)(yCenter - 30 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 30));
		        g.setTransform(originalTransform);
	        }*/
            {
	            String text = "";
	            text = "0";
	            g.rotate(0, xCenter, yCenter);
	            g.rotate(0, xCenter, (int)(yCenter - 40 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 40));
		        g.setTransform(originalTransform);
		        
		        text = "6";
	            g.rotate(Math.PI/2, xCenter, yCenter);
	            g.rotate(-Math.PI/2, xCenter, (int)(yCenter - 40 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 40));
		        g.setTransform(originalTransform);
		        
		        text = "12";
	            g.rotate(Math.PI, xCenter, yCenter);
	            g.rotate(-Math.PI, xCenter, (int)(yCenter - 40 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 40));
		        g.setTransform(originalTransform);
		        
		        text = "18";
	            g.rotate(3*Math.PI/2, xCenter, yCenter);
	            g.rotate(-3*Math.PI/2, xCenter, (int)(yCenter - 40 - g.getFontMetrics().getHeight()/2));
		        g.drawString(text, xCenter - g.getFontMetrics().stringWidth(text)/2, (int)(yCenter - 40));
		        g.setTransform(originalTransform);
	        }
            g.setTransform(originalTransform);

            fontSize = 0;
            totalDistance = (int)(SUMMARY_SCALE_FACTOR*120d);
            for (Vector vector : vectors) {
                fontSize = (int)(vector.length * SUMMARY_SCALE_FACTOR * fontScaler);
                if (fontSize > 1) {
                    g.setFont(new Font("Arial", Font.PLAIN, fontSize));
                    double r = totalDistance;
                    double a = getRads(0, 0, vector.x, vector.y);
                    a = Math.PI*2.0 - a;
                    Point point = getPoint(center, r, a);
                    //custom
                    //if (vector.name.equals("REACTOME_MITOTIC_PROMETAPHASE") || vector.name.equals("KEGG_DNA_REPLICATION"))
                    //	drawCircleText(g, vector.name, center, r, a + Math.PI/2, false, Color.white, Color.black, Color.black);
                    //else
                    	drawCircleText(g, vector.name, center, r, a + Math.PI/2, false, Color.white, Color.gray, Color.gray);
                    g.setTransform(originalTransform);
                    totalDistance += g.getFontMetrics().getHeight();
                }
            }

            g.setTransform(originalTransform);
            

            try {
	            FileOutputStream file = new FileOutputStream(filename + "." + exportFormat);
	            g.writeTo(file);
            } catch (Exception e) {e.printStackTrace();}
        }

        /**
         * Draw a piece of text on a circular curve, one
         * character at a time.  This is harder than it looks...
         *
         * This method accepts many arguments: 
         *   g - a Graphics2D ready to be used to draw,
         *   st - the string to draw, 
         *   center - the center point of the circle (Point),
         *   r - the radius of the circle,
         *   a1 - the beginning angle on the circle to start, in radians
         */
        static void drawCircleText(VectorGraphics2D g, String st, Point center, double r, double a, boolean flip, Color bgColor, Color color1, Color color2) {
            {
                double curangle = a;
                Point2D c = new Point2D.Double(center.x, center.y);
                char ch[] = st.toCharArray();
                FontMetrics fm = g.getFontMetrics();
                AffineTransform xform1, cxform;
                xform1 = AffineTransform.getTranslateInstance(c.getX(),c.getY());
                g.setColor(bgColor);
                for(int i = 0; i < ch.length; i++) {
                    double cwid = (double)(getWidth(ch[i],fm));
                    if (!(ch[i] == ' ' || Character.isSpaceChar(ch[i]))) {
                        cwid = (double)(fm.charWidth(ch[i]));
                        cxform = new AffineTransform(xform1);
                        cxform.rotate(curangle, 0.0, 0.0);
                        String chstr = new String(ch, i, 1);
                        //String chstr = (flip ? new String(ch, ch.length - i - 1, 1) : new String(ch, i, 1));
                        g.setTransform(cxform);
                        //if (flip) g.rotate(Math.PI, (float)(-cwid/2) + (float)g.getFontMetrics().stringWidth(chstr)/2, (float)(-r) - g.getFontMetrics().getAscent()/2);
                        //g.drawString(chstr, (float)(-cwid/2), (float)(-r));
                        g.fillRect((int)(-cwid/2) - g.getFontMetrics().getAscent()/2, (int)(-r) - g.getFontMetrics().getAscent(), g.getFontMetrics().stringWidth(chstr) + g.getFontMetrics().getAscent(), g.getFontMetrics().getHeight());
                        //if (flip) g.rotate(Math.PI, (float)(-cwid/2) + (float)g.getFontMetrics().stringWidth(chstr)/2, (float)(-r) - g.getFontMetrics().getAscent()/2);
                    }

                    // compute advance of angle assuming cwid<<radius
                    if (i < (ch.length - 1)) {
                        double adv = cwid/2.0 + fm.getLeading() + getWidth(ch[i + 1],fm)/2.0;
                        curangle += Math.atan(adv / r);
                        //curangle += Math.sin(adv / r);

                    }
                }
            }

            {
                double curangle = a;
                Point2D c = new Point2D.Double(center.x, center.y);
                char ch[] = st.toCharArray();
                FontMetrics fm = g.getFontMetrics();
                AffineTransform xform1, cxform;
                xform1 = AffineTransform.getTranslateInstance(c.getX(),c.getY());
                g.setColor(color1);
                for(int i = 0; i < ch.length; i++) {
                    double cwid = (double)(getWidth(ch[i],fm));
                    if (!(ch[i] == ' ' || Character.isSpaceChar(ch[i]))) {
                        cwid = (double)(fm.charWidth(ch[i]));
                        cxform = new AffineTransform(xform1);
                        cxform.rotate(curangle, 0.0, 0.0);
                        String chstr = new String(ch, i, 1);
                        //String chstr = (flip ? new String(ch, ch.length - i - 1, 1) : new String(ch, i, 1));
                        g.setTransform(cxform);
                        //if (flip) g.rotate(Math.PI, (float)(-cwid/2) + (float)g.getFontMetrics().stringWidth(chstr)/2, (float)(-r) - g.getFontMetrics().getAscent()/2);
                        g.drawString(chstr, (float)(-cwid/2), (float)(-r));
                        //if (flip) g.rotate(Math.PI, (float)(-cwid/2) + (float)g.getFontMetrics().stringWidth(chstr)/2, (float)(-r) - g.getFontMetrics().getAscent()/2);
                    }

                    // compute advance of angle assuming cwid<<radius
                    if (i < (ch.length - 1)) {
                        double adv = cwid/2.0 + fm.getLeading() + getWidth(ch[i + 1],fm)/2.0;
                        curangle += Math.atan(adv / r);
                        //curangle += Math.sin(adv / r);

                    }
                    if (color2 != null) g.setColor(color2);
                }
            }
        }

        /**
         * Get the width of a given character under the
         * specified FontMetrics, interpreting all spaces as
         * en-spaces.
         */
        static int getWidth(char c, FontMetrics fm) {
            if (c == ' ' || Character.isSpaceChar(c)) {
                return fm.charWidth('n');
            }
            else {
                return fm.charWidth(c);
            }
        }
    }
}
