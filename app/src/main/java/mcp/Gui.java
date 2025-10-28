package mcp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class Gui {
  // GUI Components
  private JFrame frame;
  private BridgeAnimationPanel bridgePanel;
  private JPanel controlPanel;
  private JPanel modeControlPanel;
  private JPanel overlayPanel;
  private JPanel warningPanel;
  private boolean isOverrideMode = false;
  private boolean communicationLost = false;
  private boolean isDiagnosticMode = false;

  // Status Display Components
  private JLabel modeLabel;
  private JLabel bridgeStatusLabel;
  private JLabel gateStatusLabel;
  private JLabel roadDistanceLabel;
  private JLabel boatDistanceLabel;
  private JLabel bridgeMovementLabel;
  private JLabel boatClearanceLabel;
  private JLabel sequenceStateLabel;
  private JLabel queueStatusLabel;
  private JLabel manualLightsLabel;
  private JLabel lastWeightLabel;
  private JTextPane messageLogArea;

  // Mode control buttons
  private JButton overrideModeButton;
  private JButton automaticModeButton;

  // Send object reference
  private Send mcpSendObject;

  // Current state
  private String currentMode = "AUTOMATIC";
  private String bridgeState = "CLOSED";
  private String gateState = "OPEN";
  private String roadLight = "RED";
  private String boatLight = "RED";
  private String sequenceState = "IDLE";
  private long lastStatusTime = 0;
  private String lastWeightReading = "N/A";

  // Communication timeout timer
  private Timer communicationCheckTimer;
  private Timer warningFlashTimer;
  private boolean warningVisible = true;

  // Responsive sizing
  private boolean isLaptopSize = false;

  public Gui() {
    SwingUtilities.invokeLater(() -> createGUI());
    startCommunicationMonitor();
  }

  public void initializeSender(Send sendObject) {
    this.mcpSendObject = sendObject;
    updateMessageLog("Send object initialized - ready for communication");
  }

  private void startCommunicationMonitor() {
    communicationCheckTimer = new Timer(1000, e -> {
      long timeSinceLastStatus = System.currentTimeMillis() - lastStatusTime;
      if (timeSinceLastStatus > 5000 && lastStatusTime > 0) {
        if (!communicationLost) {
          communicationLost = true;
          updateCommunicationStatus(false);
          updateMessageLog("WARNING: Communication lost - No status received for 5 seconds");
        }
      } else if (communicationLost && timeSinceLastStatus <= 5000) {
        communicationLost = false;
        updateCommunicationStatus(true);
        updateMessageLog("SYSTEM: Communication restored");
      }
    });
    communicationCheckTimer.start();

    // Warning flash timer - flashes the warning panel when communication is lost
    warningFlashTimer = new Timer(1000, e -> {
      if (communicationLost && warningPanel != null) {
        warningVisible = !warningVisible;
        warningPanel.setVisible(warningVisible);
        warningPanel.repaint();
      } else if (!communicationLost && warningPanel != null) {
        // Ensure warning is hidden when communication is restored
        warningPanel.setVisible(false);
        warningPanel.repaint();
      }
    });
    warningFlashTimer.start();
  }

  private void updateCommunicationStatus(boolean connected) {
    SwingUtilities.invokeLater(() -> {
      if (connected) {
        warningPanel.setVisible(false);
        warningVisible = false;
      }
    });
  }

  private void createGUI() {
    System.out.println("Creating GUI window...");

    frame = new JFrame("Bridge Control Interface");

    // Check screen size and set appropriate dimensions
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int screenWidth = screenSize.width;
    int screenHeight = screenSize.height;

    // Determine if laptop size (width < 1600)
    isLaptopSize = screenWidth < 1600;

    if (isLaptopSize) {
      frame.setSize(screenWidth - 100, screenHeight - 100);
    } else {
      frame.setSize(1600, 1000);
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout(10, 10));

    // Create main center area with left stats panel
    JPanel centerArea = new JPanel(new BorderLayout(10, 10));
    centerArea.add(createStatsPanel(), BorderLayout.WEST);
    centerArea.add(createCenterPanel(), BorderLayout.CENTER);

    JPanel rightPanel = createRightPanel();
    JPanel bottomPanel = createLogPanel();

    frame.add(centerArea, BorderLayout.CENTER);
    frame.add(rightPanel, BorderLayout.EAST);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    // Add resize listener for responsiveness
    frame.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        handleResize();
      }
    });

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    System.out.println("GUI window should now be visible");
  }

  private void handleResize() {
    int width = frame.getWidth();
    boolean wasLaptopSize = isLaptopSize;
    isLaptopSize = width < 1600;

    if (wasLaptopSize != isLaptopSize) {
      // Adjust font sizes and component sizes if needed
      SwingUtilities.invokeLater(() -> {
        adjustComponentSizes();
        frame.revalidate();
        frame.repaint();
      });
    }
  }

  private void adjustComponentSizes() {
    int fontSize = isLaptopSize ? 11 : 13;
    int buttonHeight = isLaptopSize ? 28 : 32;

    Font labelFont = new Font("Arial", Font.PLAIN, fontSize);
    Font buttonFont = new Font("Arial", Font.BOLD, fontSize);

    // Update all labels
    updateFontRecursive(controlPanel, labelFont, buttonFont, buttonHeight);
    updateFontRecursive(modeControlPanel, labelFont, buttonFont, buttonHeight);
  }

  private void updateFontRecursive(JPanel panel, Font labelFont, Font buttonFont, int buttonHeight) {
    for (Component comp : panel.getComponents()) {
      if (comp instanceof JButton) {
        comp.setFont(buttonFont);
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, buttonHeight));
      } else if (comp instanceof JLabel || comp instanceof JCheckBox) {
        comp.setFont(labelFont);
      } else if (comp instanceof JPanel) {
        updateFontRecursive((JPanel) comp, labelFont, buttonFont, buttonHeight);
      }
    }
  }

  private JPanel createStatsPanel() {
    JPanel statsPanel = new JPanel();
    statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
    statsPanel.setBackground(new Color(44, 62, 80));
    statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    int panelWidth = isLaptopSize ? 220 : 250;
    statsPanel.setPreferredSize(new Dimension(panelWidth, 0));

    // Title
    JLabel titleLabel = new JLabel("SYSTEM STATUS");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 13));
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    statsPanel.add(titleLabel);
    statsPanel.add(Box.createVerticalStrut(10));

    // Create all status labels - vertical list
    modeLabel = createStatLabel("Mode: AUTOMATIC");
    bridgeStatusLabel = createStatLabel("Bridge: CLOSED");
    gateStatusLabel = createStatLabel("Gate: OPEN");
    sequenceStateLabel = createStatLabel("State: IDLE");
    roadDistanceLabel = createStatLabel("Road: 0 cm");
    boatDistanceLabel = createStatLabel("Boat: 0 cm");
    bridgeMovementLabel = createStatLabel("Bridge Sensor: 0 cm");
    boatClearanceLabel = createStatLabel("Clearance: 0 cm");
    manualLightsLabel = createStatLabel("Manual Lights: NO");
    lastWeightLabel = createStatLabel("Last Weight: N/A");
    queueStatusLabel = createStatLabel("");

    // Add all labels vertically
    statsPanel.add(modeLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(bridgeStatusLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(gateStatusLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(sequenceStateLabel);
    statsPanel.add(Box.createVerticalStrut(8));
    statsPanel.add(roadDistanceLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(boatDistanceLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(bridgeMovementLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(boatClearanceLabel);
    statsPanel.add(Box.createVerticalStrut(8));
    statsPanel.add(manualLightsLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(lastWeightLabel);
    statsPanel.add(Box.createVerticalStrut(5));
    statsPanel.add(queueStatusLabel);

    // Add glue to push everything to top
    statsPanel.add(Box.createVerticalGlue());

    return statsPanel;
  }

  private JLabel createStatLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.PLAIN, 11));
    label.setForeground(Color.WHITE);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  private JPanel createCenterPanel() {
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(new Color(52, 73, 94));

    bridgePanel = new BridgeAnimationPanel();

    // Create warning panel for communication loss
    warningPanel = new JPanel(new BorderLayout());
    warningPanel.setOpaque(false);
    warningPanel.setVisible(false);

    JPanel warningContentPanel = new JPanel();
    warningContentPanel.setLayout(new BoxLayout(warningContentPanel, BoxLayout.Y_AXIS));
    warningContentPanel.setBackground(new Color(231, 76, 60, 220));
    warningContentPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.WHITE, 3),
        BorderFactory.createEmptyBorder(30, 50, 30, 50)));

    JLabel warningIcon = new JLabel("⚠");
    warningIcon.setFont(new Font("Arial", Font.BOLD, 72));
    warningIcon.setForeground(Color.WHITE);
    warningIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel warningLabel = new JLabel("COMMUNICATION LOST");
    warningLabel.setFont(new Font("Arial", Font.BOLD, 32));
    warningLabel.setForeground(Color.WHITE);
    warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    warningContentPanel.add(warningIcon);
    warningContentPanel.add(Box.createVerticalStrut(10));
    warningContentPanel.add(warningLabel);

    warningPanel.add(warningContentPanel, BorderLayout.CENTER);

    // Create transparent panel to center the warning
    JPanel warningCenterPanel = new JPanel(new GridBagLayout());
    warningCenterPanel.setOpaque(false);
    warningCenterPanel.add(warningContentPanel);
    warningPanel.add(warningCenterPanel, BorderLayout.CENTER);

    // Layer the panels
    JPanel layeredPanel = new JPanel();
    layeredPanel.setLayout(new OverlayLayout(layeredPanel));
    layeredPanel.add(warningPanel);
    layeredPanel.add(bridgePanel);

    centerPanel.add(layeredPanel, BorderLayout.CENTER);
    return centerPanel;
  }

  private JPanel createRightPanel() {
    JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    int panelWidth = isLaptopSize ? 400 : 500;
    rightPanel.setPreferredSize(new Dimension(panelWidth, 0));
    rightPanel.setBackground(new Color(236, 240, 241));

    // Create main container with vertical layout
    JPanel mainContainer = new JPanel();
    mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
    mainContainer.setBackground(new Color(236, 240, 241));

    modeControlPanel = createModeControlPanel();
    controlPanel = createControlPanel();

    mainContainer.add(modeControlPanel);
    mainContainer.add(Box.createVerticalStrut(10));
    mainContainer.add(controlPanel);

    // Wrap in scroll pane to prevent extending under log
    JScrollPane scrollPane = new JScrollPane(mainContainer);
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

    rightPanel.add(scrollPane, BorderLayout.CENTER);

    return rightPanel;
  }

  private JPanel createModeControlPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
        "Mode Controls",
        0,
        0,
        new Font("Arial", Font.BOLD, 13),
        new Color(52, 73, 94)));
    panel.setBackground(new Color(236, 240, 241));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 10, 5, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy = 0;

    Color buttonColor = new Color(52, 73, 94);
    int buttonHeight = isLaptopSize ? 35 : 40;

    automaticModeButton = createModeButton("SWITCH TO AUTOMATIC MODE", buttonColor, buttonHeight);
    automaticModeButton.addActionListener(e -> {
      if (!isOverrideMode)
        return;
      if (mcpSendObject != null) {
        mcpSendObject.sendMessage("automatic_mode");
        updateMessageLog("SENT: automatic_mode");
      } else {
        updateMessageLog("ERROR: Send object not initialized");
      }
    });

    overrideModeButton = createModeButton("SWITCH TO OVERRIDE MODE", buttonColor, buttonHeight);
    overrideModeButton.addActionListener(e -> {
      if (isOverrideMode)
        return;
      if (mcpSendObject != null) {
        mcpSendObject.sendMessage("override_mode");
        updateMessageLog("SENT: override_mode");
      } else {
        updateMessageLog("ERROR: Send object not initialized");
      }
    });

    panel.add(automaticModeButton, gbc);
    gbc.gridy = 1;
    panel.add(overrideModeButton, gbc);

    return panel;
  }

  private JButton createModeButton(String text, Color color, int height) {
    JButton button = new JButton(text);
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setFont(new Font("Arial", Font.BOLD, 13));
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(170, height));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      Color originalColor = button.getBackground();

      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          button.setBackground(color.brighter());
        }
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          button.setBackground(originalColor);
        }
      }
    });

    return button;
  }

  private JPanel createControlPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
        "Manual Override Controls",
        0,
        0,
        new Font("Arial", Font.BOLD, 13),
        new Color(52, 73, 94)));
    panel.setBackground(new Color(236, 240, 241));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 10, 3, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    Color buttonColor = new Color(52, 73, 94);
    int buttonHeight = isLaptopSize ? 28 : 32;

    // Emergency Controls Section
    addSectionLabel(panel, "EMERGENCY CONTROLS", gbc, 0);

    gbc.gridy = 1;
    JButton emergencyStopButton = new JButton("EMERGENCY STOP");
    emergencyStopButton.setBackground(new Color(192, 57, 43));
    emergencyStopButton.setForeground(Color.WHITE);
    emergencyStopButton.setFont(new Font("Arial", Font.BOLD, 11));
    emergencyStopButton.setFocusPainted(false);
    emergencyStopButton.setBorderPainted(false);
    emergencyStopButton.setPreferredSize(new Dimension(170, buttonHeight));
    emergencyStopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    emergencyStopButton.putClientProperty("originalColor", new Color(192, 57, 43));

    emergencyStopButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (emergencyStopButton.isEnabled()) {
          emergencyStopButton.setBackground(new Color(231, 76, 60));
        }
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (emergencyStopButton.isEnabled()) {
          emergencyStopButton.setBackground(new Color(192, 57, 43));
        }
      }
    });

    emergencyStopButton.addActionListener(e -> {
      int confirm = JOptionPane.showConfirmDialog(
          frame,
          "Activate EMERGENCY STOP?\n\nThis will:\n- Stop all motors immediately\n- Activate all warning lights" +
              "\n- Sound alarm buzzer\n- Enter diagnostic mode\n- Require manual recovery",
          "Emergency Stop Confirmation",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE);

      if (confirm == JOptionPane.YES_OPTION) {
        if (mcpSendObject != null) {
          mcpSendObject.sendMessage("emergency_stop");
          updateMessageLog("SENT: emergency_stop");
        } else {
          updateMessageLog("ERROR: Send object not initialized");
        }
      }
    });
    panel.add(emergencyStopButton, gbc);

    // Traffic Sequence Section
    addSectionLabel(panel, "TRAFFIC SEQUENCES", gbc, 2);
    addControlButton(panel, "ALLOW BOAT TRAFFIC", buttonColor, "allow_boat_traffic", gbc, 3, buttonHeight);
    addControlButton(panel, "ALLOW ROAD TRAFFIC", buttonColor, "allow_road_traffic", gbc, 4, buttonHeight);

    // System Test Section
    addSectionLabel(panel, "SYSTEM TESTING", gbc, 5);
    addControlButton(panel, "RUN FULL TEST", buttonColor, "run_full_test", gbc, 6, buttonHeight);
    addControlButton(panel, "PERFORM DIAGNOSTICS", buttonColor, "perform_diagnostics", gbc, 7, buttonHeight);
    addControlButton(panel, "RESTART ESP32", new Color(192, 57, 43), "restart", gbc, 8, buttonHeight);

    // Road Light Control Section
    addSectionLabel(panel, "ROAD LIGHTS", gbc, 9);
    addControlButton(panel, "RED", new Color(231, 76, 60), "road_lights_red", gbc, 10, buttonHeight);
    addControlButton(panel, "YELLOW", new Color(241, 196, 15), "road_lights_yellow", gbc, 11, buttonHeight);
    addControlButton(panel, "GREEN", new Color(46, 204, 113), "road_lights_green", gbc, 12, buttonHeight);

    // Boat Light Control Section
    addSectionLabel(panel, "BOAT LIGHTS", gbc, 13);
    addControlButton(panel, "RED", new Color(231, 76, 60), "boat_lights_red", gbc, 14, buttonHeight);
    addControlButton(panel, "GREEN", new Color(46, 204, 113), "boat_lights_green", gbc, 15, buttonHeight);

    // Bridge Lights Control Section
    addSectionLabel(panel, "BRIDGE LIGHTS", gbc, 16);

    gbc.gridy = 17;
    JCheckBox manualControlCheckbox = new JCheckBox("Manual Control");
    manualControlCheckbox.setFont(new Font("Arial", Font.PLAIN, 11));
    manualControlCheckbox.setBackground(new Color(236, 240, 241));
    manualControlCheckbox.addActionListener(e -> {
      if (isOverrideMode && mcpSendObject != null) {
        if (manualControlCheckbox.isSelected()) {
          mcpSendObject.sendMessage("manual_bridge_lights_true");
          updateMessageLog("SENT: manual_bridge_lights_true");
        } else {
          mcpSendObject.sendMessage("manual_bridge_lights_false");
          updateMessageLog("SENT: manual_bridge_lights_false");
        }
      }
    });
    manualControlCheckbox.setEnabled(false);
    panel.add(manualControlCheckbox, gbc);

    addControlButton(panel, "LIGHTS ON", new Color(255, 193, 7), "manual_bridge_lights_on", gbc, 18, buttonHeight);
    addControlButton(panel, "LIGHTS OFF", new Color(158, 158, 158), "manual_bridge_lights_off", gbc, 19, buttonHeight);

    // Initially disable all buttons (automatic mode) - disable directly on this
    // panel
    Component[] components = panel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton || component instanceof JCheckBox) {
        // Skip emergency stop button - it should always be enabled
        if (component instanceof JButton) {
          JButton button = (JButton) component;
          if (button.getText().equals("EMERGENCY STOP")) {
            continue;
          }
        }

        component.setEnabled(false);

        if (component instanceof JButton) {
          JButton button = (JButton) component;
          button.setBackground(new Color(150, 150, 150));
        }
      }
    }

    return panel;
  }

  private void addSectionLabel(JPanel panel, String text, GridBagConstraints gbc, int row) {
    gbc.gridy = row;
    JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.BOLD, 11));
    label.setForeground(new Color(52, 73, 94));
    label.setBorder(BorderFactory.createEmptyBorder(3, 5, 1, 5));
    panel.add(label, gbc);
  }

  private void addControlButton(JPanel panel, String text, Color color, String command,
      GridBagConstraints gbc, int row, int height) {
    gbc.gridy = row;
    JButton button = new JButton(text);
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setFont(new Font("Arial", Font.BOLD, 11));
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(170, height));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.putClientProperty("command", command);
    button.putClientProperty("originalColor", color);

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          button.setBackground(color.brighter());
        }
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          Color originalColor = (Color) button.getClientProperty("originalColor");
          button.setBackground(originalColor);
        }
      }
    });

    button.addActionListener(e -> {
      if (!button.isEnabled())
        return;
      if (isOverrideMode && mcpSendObject != null) {
        mcpSendObject.sendMessage(command);
        updateMessageLog("SENT: " + command);
      } else {
        updateMessageLog("ERROR: Send object not initialized");
      }
    });

    panel.add(button, gbc);
  }

  private JPanel createLogPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(52, 73, 94)),
        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    int logHeight = isLaptopSize ? 150 : 200;
    panel.setPreferredSize(new Dimension(0, logHeight));
    panel.setBackground(new Color(30, 30, 30));

    // Header panel with title and buttons
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(new Color(30, 30, 30));

    JLabel titleLabel = new JLabel("Message Log");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
    titleLabel.setForeground(new Color(200, 200, 200));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttonPanel.setBackground(new Color(30, 30, 30));

    JButton clearButton = new JButton("Clear");
    clearButton.setFont(new Font("Arial", Font.PLAIN, 10));
    clearButton.setBackground(new Color(192, 57, 43));
    clearButton.setForeground(Color.WHITE);
    clearButton.setFocusPainted(false);
    clearButton.setBorderPainted(false);
    clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    clearButton.addActionListener(e -> clearMessageLog());

    JButton exportButton = new JButton("Export");
    exportButton.setFont(new Font("Arial", Font.PLAIN, 10));
    exportButton.setBackground(new Color(52, 152, 219));
    exportButton.setForeground(Color.WHITE);
    exportButton.setFocusPainted(false);
    exportButton.setBorderPainted(false);
    exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    exportButton.addActionListener(e -> exportMessageLog());

    buttonPanel.add(clearButton);
    buttonPanel.add(exportButton);

    headerPanel.add(titleLabel, BorderLayout.WEST);
    headerPanel.add(buttonPanel, BorderLayout.EAST);

    messageLogArea = new JTextPane();
    messageLogArea.setEditable(false);
    messageLogArea.setFont(new Font("Consolas", Font.PLAIN, 10));
    messageLogArea.setBackground(new Color(30, 30, 30));
    messageLogArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JScrollPane scrollPane = new JScrollPane(messageLogArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(null);

    panel.add(headerPanel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  private void clearMessageLog() {
    messageLogArea.setText("");
    updateMessageLog("Message log cleared");
  }

  private void exportMessageLog() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Message Log");

    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    fileChooser.setSelectedFile(new File("bridge_log_" + timestamp + ".txt"));

    int result = fileChooser.showSaveDialog(frame);

    if (result == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(messageLogArea.getText());
        JOptionPane.showMessageDialog(frame,
            "Log exported successfully to:\n" + file.getAbsolutePath(),
            "Export Successful",
            JOptionPane.INFORMATION_MESSAGE);
        updateMessageLog("Log exported to: " + file.getName());
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(frame,
            "Error exporting log:\n" + ex.getMessage(),
            "Export Error",
            JOptionPane.ERROR_MESSAGE);
        updateMessageLog("ERROR: Failed to export log - " + ex.getMessage());
      }
    }
  }

  private void setControlPanelEnabled(boolean enabled) {
    Component[] components = controlPanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton || component instanceof JCheckBox) {
        // Skip emergency stop button - it should always be enabled
        if (component instanceof JButton) {
          JButton button = (JButton) component;
          if (button.getText().equals("EMERGENCY STOP")) {
            continue;
          }
        }

        component.setEnabled(enabled);

        if (component instanceof JButton) {
          JButton button = (JButton) component;
          if (enabled) {
            Color originalColor = (Color) button.getClientProperty("originalColor");
            if (originalColor != null) {
              button.setBackground(originalColor);
            }
          } else {
            button.setBackground(new Color(150, 150, 150));
          }
        }
      }
    }
  }

  private void updateDiagnosticModeButtons() {
    Component[] components = controlPanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton) {
        JButton button = (JButton) component;
        String command = (String) button.getClientProperty("command");

        if (isDiagnosticMode) {
          // Only enable restart and diagnostics buttons
          boolean shouldEnable = command != null &&
              (command.equals("restart") || command.equals("perform_diagnostics"));
          button.setEnabled(shouldEnable);

          if (shouldEnable) {
            Color originalColor = (Color) button.getClientProperty("originalColor");
            if (originalColor != null) {
              button.setBackground(originalColor);
            }
          } else {
            button.setBackground(new Color(150, 150, 150));
          }
        }
      }
    }
  }

  public void updateSystemStatus(String mode, String bridgeState, String gateState,
      String roadDistance, String boatDistance, String bridgeMovementDistance,
      String boatClearanceDistance, String roadLight, String boatLight,
      String bridgeLight, String manualBridgeLights, String sequenceState,
      String movementState, String queueSize, String executing) {

    SwingUtilities.invokeLater(() -> {
      lastStatusTime = System.currentTimeMillis();

      this.currentMode = mode;
      this.bridgeState = bridgeState;
      this.gateState = gateState;
      this.roadLight = roadLight;
      this.boatLight = boatLight;
      this.sequenceState = sequenceState;

      // Update mode label
      modeLabel.setText("Mode: " + mode);
      if (mode.equals("AUTOMATIC")) {
        modeLabel.setForeground(new Color(46, 204, 113));
      } else if (mode.equals("OVERRIDE")) {
        modeLabel.setForeground(new Color(231, 76, 60));
      }

      // Update bridge status
      bridgeStatusLabel.setText("Bridge: " + bridgeState);
      if (bridgeState.equals("OPEN")) {
        bridgeStatusLabel.setForeground(new Color(231, 76, 60));
      } else if (bridgeState.equals("CLOSED")) {
        bridgeStatusLabel.setForeground(new Color(46, 204, 113));
      } else {
        bridgeStatusLabel.setForeground(Color.LIGHT_GRAY);
      }

      // Update gate status
      gateStatusLabel.setText("Gate: " + gateState);
      if (gateState.equals("OPEN")) {
        gateStatusLabel.setForeground(new Color(46, 204, 113));
      } else if (gateState.equals("CLOSED")) {
        gateStatusLabel.setForeground(new Color(231, 76, 60));
      } else {
        gateStatusLabel.setForeground(Color.LIGHT_GRAY);
      }

      // Update sequence state
      boolean wasDiagnostic = isDiagnosticMode;
      isDiagnosticMode = sequenceState.equals("DIAGNOSTIC");

      sequenceStateLabel.setText("State: " + sequenceState);
      if (sequenceState.equals("DIAGNOSTIC")) {
        sequenceStateLabel.setForeground(new Color(231, 76, 60));
      } else if (sequenceState.equals("IDLE") || sequenceState.equals("CARS_PASSING")
          || sequenceState.equals("BOATS_PASSING")) {
        sequenceStateLabel.setForeground(new Color(46, 204, 113));
      } else {
        sequenceStateLabel.setForeground(new Color(241, 196, 15));
      }

      // Update distances
      roadDistanceLabel.setText("Road: " + roadDistance + " cm");
      boatDistanceLabel.setText("Boat: " + boatDistance + " cm");
      bridgeMovementLabel.setText("Bridge Sensor: " + bridgeMovementDistance + " cm");
      boatClearanceLabel.setText("Clearance: " + boatClearanceDistance + " cm");

      // Update manual lights status
      manualLightsLabel.setText("Manual Lights: " + manualBridgeLights);
      if (manualBridgeLights.equals("YES")) {
        manualLightsLabel.setForeground(new Color(241, 196, 15));
      } else {
        manualLightsLabel.setForeground(Color.WHITE);
      }

      // Update weight label
      lastWeightLabel.setText("Last Weight: " + lastWeightReading);

      // Update queue status if in override mode
      if (mode.equals("OVERRIDE") && queueSize != null && !queueSize.isEmpty()) {
        String queueText = "Queue: " + queueSize;
        if (executing != null && executing.equals("YES")) {
          queueText += " (Executing)";
          queueStatusLabel.setForeground(new Color(241, 196, 15));
        } else {
          queueStatusLabel.setForeground(Color.WHITE);
        }
        queueStatusLabel.setText(queueText);
      } else {
        queueStatusLabel.setText("");
      }

      // Update mode if it has changed
      if (!mode.equals("UNKNOWN")) {
        boolean newOverrideMode = mode.equals("OVERRIDE");
        if (newOverrideMode != isOverrideMode) {
          isOverrideMode = newOverrideMode;
          updateModeButtons();
          setControlPanelEnabled(isOverrideMode);
        }
      }

      // Check if diagnostic mode changed
      if (wasDiagnostic != isDiagnosticMode) {
        if (isDiagnosticMode && isOverrideMode) {
          updateDiagnosticModeButtons();
        } else if (!isDiagnosticMode && isOverrideMode) {
          setControlPanelEnabled(true);
        }
      }

      // Update animation
      bridgePanel.updateState(bridgeState, gateState, roadLight, boatLight);
      bridgePanel.updateBridgeLights(bridgeLight.equals("ON"));
    });
  }

  public void updateWeightReading(String weight) {
    SwingUtilities.invokeLater(() -> {
      lastWeightReading = weight;
      lastWeightLabel.setText("Last Weight: " + weight);
      if (Integer.parseInt(weight) > 2240) {
        lastWeightLabel.setForeground(new Color(231, 76, 60));
      } else {
        lastWeightLabel.setForeground(new Color(46, 204, 113));
      }
    });
  }

  private void updateModeButtons() {
    if (isOverrideMode) {
      automaticModeButton.setEnabled(true);
      overrideModeButton.setEnabled(false);
      automaticModeButton.setBackground(new Color(52, 73, 94));
      overrideModeButton.setBackground(new Color(150, 150, 150));
    } else {
      automaticModeButton.setEnabled(false);
      overrideModeButton.setEnabled(true);
      automaticModeButton.setBackground(new Color(150, 150, 150));
      overrideModeButton.setBackground(new Color(52, 73, 94));
    }
  }

  public void updateMessageLog(String message) {
    if (messageLogArea != null) {
      SwingUtilities.invokeLater(() -> {
        try {
          String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
          String newMessage = timeStamp + " - " + message + "\n";

          StyledDocument doc = messageLogArea.getStyledDocument();

          // Change all existing text to white
          Style whiteStyle = messageLogArea.addStyle("White", null);
          StyleConstants.setForeground(whiteStyle, Color.WHITE);
          doc.setCharacterAttributes(0, doc.getLength(), whiteStyle, false);

          // Determine color based on message type
          Style messageStyle = messageLogArea.addStyle("MessageStyle", null);
          if (message.startsWith("ERROR") || message.startsWith("SYSTEM: communication_lost")) {
            StyleConstants.setForeground(messageStyle, new Color(231, 76, 60));
          } else if (message.startsWith("WARNING")) {
            StyleConstants.setForeground(messageStyle, new Color(241, 196, 15));
          } else if (message.startsWith("EXECUTED") || message.startsWith("MODE CHANGE")
              || message.startsWith("SYSTEM: communication_connected")) {
            StyleConstants.setForeground(messageStyle, new Color(46, 204, 113));
          } else if (message.startsWith("INFO") || message.startsWith("SENT") || message.startsWith("RECEIVED")) {
            StyleConstants.setForeground(messageStyle, new Color(52, 152, 219));
          } else {
            StyleConstants.setForeground(messageStyle, new Color(149, 165, 166));
          }

          doc.insertString(0, newMessage, messageStyle);
          messageLogArea.setCaretPosition(0);
        } catch (BadLocationException e) {
          System.err.println("Error updating message log: " + e.getMessage());
        }
      });
    }
  }

  // Bridge Animation Panel (keeping same as before)
  class BridgeAnimationPanel extends JPanel {
    private String bridgeState = "CLOSED";
    private String gateState = "OPEN";
    private String roadLight = "RED";
    private String boatLight = "RED";
    private boolean bridgeLightsOn = false;

    private float bridgeAngle = 0f;
    private float gateAngle = 90f;
    private Timer animationTimer;
    private int waveOffset = 0;
    private int lightPulse = 0;

    public BridgeAnimationPanel() {
      setBackground(new Color(240, 242, 245));

      animationTimer = new Timer(16, e -> {
        boolean needsRepaint = false;

        float targetBridgeAngle = this.bridgeState.equals("OPEN") ? 90f : 0f;
        if (Math.abs(bridgeAngle - targetBridgeAngle) > 0.5f) {
          bridgeAngle += (targetBridgeAngle - bridgeAngle) * 0.1f;
          needsRepaint = true;
        }

        float targetGateAngle = this.gateState.equals("OPEN") ? 90f : 0f;
        if (Math.abs(gateAngle - targetGateAngle) > 0.5f) {
          gateAngle += (targetGateAngle - gateAngle) * 0.1f;
          needsRepaint = true;
        }

        waveOffset = (waveOffset + 1) % 40;
        lightPulse = (lightPulse + 1) % 60;
        needsRepaint = true;

        if (needsRepaint) {
          repaint();
        }
      });
      animationTimer.start();
    }

    public void updateState(String bridge, String gate, String road, String boat) {
      this.bridgeState = bridge;
      this.gateState = gate;
      this.roadLight = road;
      this.boatLight = boat;
    }

    public void updateBridgeLights(boolean lightsOn) {
      this.bridgeLightsOn = lightsOn;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int width = getWidth();
      int height = getHeight();
      int centerX = width / 2;
      int waterY = height / 2 + 50;

      // Draw water
      g2d.setColor(new Color(70, 90, 110));
      g2d.fillRect(0, waterY, width, height - waterY);

      // Draw animated wave pattern
      g2d.setColor(new Color(80, 100, 120));
      for (int y = waterY; y < height; y += 20) {
        for (int x = -40; x < width + 40; x += 40) {
          int offsetX = x + waveOffset;
          if (y % 40 == 0) {
            offsetX = x - waveOffset;
          }
          g2d.fillOval(offsetX, y, 30, 8);
        }
      }

      int liftOffset = (int) (-bridgeAngle * 2.2f);

      int bridgeWidth = 180;
      int towerWidth = 30;
      int towerHeight = 280;

      // Left tower
      g2d.setColor(new Color(80, 85, 90));
      g2d.fillRect(centerX - bridgeWidth - towerWidth, waterY - towerHeight, towerWidth, towerHeight);

      g2d.setColor(new Color(70, 75, 80));
      g2d.fillRect(centerX - bridgeWidth - towerWidth - 5, waterY - towerHeight - 10, towerWidth + 10, 10);

      g2d.setColor(new Color(40, 45, 50));
      for (int i = 0; i < 5; i++) {
        g2d.fillRect(centerX - bridgeWidth - towerWidth + 8, waterY - towerHeight + 30 + i * 45, 14, 25);
      }

      // Right tower
      g2d.setColor(new Color(80, 85, 90));
      g2d.fillRect(centerX + bridgeWidth, waterY - towerHeight, towerWidth, towerHeight);

      g2d.setColor(new Color(70, 75, 80));
      g2d.fillRect(centerX + bridgeWidth - 5, waterY - towerHeight - 10, towerWidth + 10, 10);

      g2d.setColor(new Color(40, 45, 50));
      for (int i = 0; i < 5; i++) {
        g2d.fillRect(centerX + bridgeWidth + 8, waterY - towerHeight + 30 + i * 45, 14, 25);
      }

      // Draw lift cables
      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(new Color(60, 65, 70));

      // Left cables
      g2d.drawLine(centerX - bridgeWidth - towerWidth + 5, waterY - towerHeight + 5,
          centerX - bridgeWidth + 10, waterY - 85 + liftOffset);
      g2d.drawLine(centerX - bridgeWidth - towerWidth + 10, waterY - towerHeight + 5,
          centerX - bridgeWidth + 30, waterY - 85 + liftOffset);
      g2d.drawLine(centerX - bridgeWidth - towerWidth + 15, waterY - towerHeight + 5,
          centerX - bridgeWidth + 50, waterY - 85 + liftOffset);
      g2d.drawLine(centerX - bridgeWidth - towerWidth + 20, waterY - towerHeight + 5,
          centerX - bridgeWidth + 70, waterY - 85 + liftOffset);

      // Right cables
      g2d.drawLine(centerX + bridgeWidth + towerWidth - 5, waterY - towerHeight + 5,
          centerX + bridgeWidth - 10, waterY - 85 + liftOffset);
      g2d.drawLine(centerX + bridgeWidth + towerWidth - 10, waterY - towerHeight + 5,
          centerX + bridgeWidth - 30, waterY - 85 + liftOffset);
      g2d.drawLine(centerX + bridgeWidth + towerWidth - 15, waterY - towerHeight + 5,
          centerX + bridgeWidth - 50, waterY - 85 + liftOffset);
      g2d.drawLine(centerX + bridgeWidth + towerWidth - 20, waterY - towerHeight + 5,
          centerX + bridgeWidth - 70, waterY - 85 + liftOffset);

      // Draw bridge deck
      int deckHeight = 18;

      g2d.setColor(new Color(90, 95, 100));
      g2d.fillRect(centerX - bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2,
          bridgeWidth * 2, deckHeight);

      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(new Color(70, 75, 80));
      for (int i = centerX - bridgeWidth; i < centerX + bridgeWidth; i += 30) {
        g2d.drawLine(i, waterY - 85 + liftOffset - deckHeight / 2,
            i + 15, waterY - 85 + liftOffset + deckHeight / 2);
        g2d.drawLine(i + 15, waterY - 85 + liftOffset - deckHeight / 2,
            i + 30, waterY - 85 + liftOffset + deckHeight / 2);
      }

      g2d.setColor(new Color(40, 42, 45));
      g2d.fillRect(centerX - bridgeWidth, waterY - 85 + liftOffset - 4,
          bridgeWidth * 2, 8);

      g2d.setColor(new Color(220, 220, 220));
      for (int i = centerX - bridgeWidth; i < centerX + bridgeWidth; i += 40) {
        g2d.fillRect(i, waterY - 85 + liftOffset - 1, 20, 2);
      }

      g2d.setStroke(new BasicStroke(1));
      g2d.setColor(new Color(100, 105, 110));
      g2d.drawLine(centerX - bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2 - 3,
          centerX + bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2 - 3);
      g2d.drawLine(centerX - bridgeWidth, waterY - 85 + liftOffset + deckHeight / 2 + 3,
          centerX + bridgeWidth, waterY - 85 + liftOffset + deckHeight / 2 + 3);

      // Draw bridge lights on top of deck
      drawBridgeLights(g2d, centerX, waterY, liftOffset, deckHeight);

      // Draw approach roads
      g2d.setColor(new Color(40, 42, 45));
      g2d.fillRect(0, waterY - 89, centerX - bridgeWidth, 8);
      g2d.fillRect(centerX + bridgeWidth, waterY - 89, width - (centerX + bridgeWidth), 8);

      // Draw support pillars
      g2d.setColor(new Color(70, 75, 80));
      int pillarHeight = 89 - 8;
      g2d.fillRect(0, waterY - 81, centerX - bridgeWidth, pillarHeight);
      g2d.fillRect(centerX + bridgeWidth, waterY - 81, width - (centerX + bridgeWidth), pillarHeight);

      // Road markings
      g2d.setColor(new Color(220, 220, 220));
      for (int i = 40; i < centerX - bridgeWidth; i += 40) {
        g2d.fillRect(i, waterY - 86, 20, 2);
      }
      for (int i = centerX + bridgeWidth + 40; i < width; i += 40) {
        g2d.fillRect(i, waterY - 86, 20, 2);
      }

      // Draw gates on both sides
      drawGate(g2d, centerX - 200, waterY, liftOffset, true);
      drawGate(g2d, centerX + 200, waterY, liftOffset, false);

      // Draw traffic lights
      drawTrafficLights(g2d, centerX, waterY);
    }

    private void drawBridgeLights(Graphics2D g2d, int centerX, int waterY, int liftOffset, int deckHeight) {
      int bridgeWidth = 180;
      int lightY = waterY - 85 + liftOffset - deckHeight / 2 - 10;

      int pulseIntensity = (int) (Math.sin(lightPulse * 0.1) * 15 + 15);

      int numLights = 8;
      int spacing = (bridgeWidth * 2) / (numLights + 1);

      for (int i = 1; i <= numLights; i++) {
        int lightX = centerX - bridgeWidth + (i * spacing);

        if (bridgeLightsOn) {
          g2d.setColor(new Color(255, 220, 100, 60 + pulseIntensity));
          g2d.fillOval(lightX - 12, lightY - 12, 24, 24);

          g2d.setColor(new Color(255, 240, 150));
          g2d.fillOval(lightX - 6, lightY - 6, 12, 12);

          g2d.setColor(new Color(255, 255, 200));
          g2d.fillOval(lightX - 3, lightY - 4, 4, 4);
        } else {
          g2d.setColor(new Color(60, 60, 60));
          g2d.fillOval(lightX - 5, lightY - 5, 10, 10);
        }

        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(lightX - 2, lightY + 5, 4, 6);
      }
    }

    private void drawGate(Graphics2D g2d, int gateX, int waterY, int liftOffset, boolean isLeftSide) {
      int gateY = waterY - 81;

      g2d.setColor(new Color(180, 60, 50));
      g2d.fillRect(gateX - 8, gateY - 40, 16, 40);

      AffineTransform old = g2d.getTransform();
      g2d.translate(gateX, gateY - 40);

      if (isLeftSide) {
        g2d.rotate(-Math.toRadians(gateAngle));
      } else {
        g2d.rotate(Math.toRadians(gateAngle));
        g2d.scale(-1, 1);
      }

      g2d.setColor(new Color(180, 60, 50));
      g2d.fillRect(0, -6, 120, 12);

      g2d.setColor(new Color(240, 240, 240));
      for (int i = 0; i < 120; i += 20) {
        g2d.fillRect(i, -6, 10, 12);
      }

      g2d.setTransform(old);
    }

    private void drawTrafficLights(Graphics2D g2d, int centerX, int waterY) {
      drawTrafficLightPole(g2d, centerX - 300, waterY - 81, roadLight, true);
      drawTrafficLightPole(g2d, centerX + 300, waterY - 81, boatLight, false);
    }

    private void drawTrafficLightPole(Graphics2D g2d, int x, int y, String activeLight, boolean isRoadLight) {
      g2d.setColor(new Color(50, 55, 60));
      g2d.fillRect(x - 4, y - 120, 8, 120);

      g2d.setColor(new Color(40, 45, 50));
      int boxHeight = isRoadLight ? 90 : 60;
      g2d.fillRect(x - 20, y - 150, 40, boxHeight);

      if (isRoadLight) {
        drawLight(g2d, x, y - 133, activeLight.equals("RED") || activeLight.equals("ALL"), Color.RED);
        drawLight(g2d, x, y - 105, activeLight.equals("YELLOW") || activeLight.equals("ALL"), new Color(255, 200, 0));
        drawLight(g2d, x, y - 77, activeLight.equals("GREEN") || activeLight.equals("ALL"), Color.GREEN);
      } else {
        drawLight(g2d, x, y - 133, activeLight.equals("RED") || activeLight.equals("ALL"), Color.RED);
        drawLight(g2d, x, y - 100, activeLight.equals("GREEN") || activeLight.equals("ALL"), Color.GREEN);
      }
    }

    private void drawLight(Graphics2D g2d, int x, int y, boolean active, Color color) {
      g2d.setColor(active ? color : new Color(60, 60, 60));
      g2d.fillOval(x - 8, y - 8, 16, 16);

      g2d.setColor(new Color(30, 30, 30));
      g2d.setStroke(new BasicStroke(1));
      g2d.drawOval(x - 8, y - 8, 16, 16);
    }
  }
}
