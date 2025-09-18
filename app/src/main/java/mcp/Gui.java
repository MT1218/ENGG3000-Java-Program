package mcp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Gui {
  // GUI Components
  private JLabel statusLabel;
  private JButton overrideModeButton;
  private JButton automaticModeButton;
  private JPanel controlPanel;
  private boolean isOverrideMode = false;

  // Status Display Components
  private JLabel bridgeStatusLabel;
  private JLabel gateStatusLabel;
  private JLabel roadDistanceLabel;
  private JLabel boatDistanceLabel;
  private JLabel roadLightLabel;
  private JLabel boatLightLabel;
  private JTextArea messageLogArea;

  // Send object reference
  private Send mcpSendObject;

  public Gui() {
    SwingUtilities.invokeLater(() -> createGUI());
  }

  public void initializeSender(Send sendObject) {
    this.mcpSendObject = sendObject;
    // Add initialization message
    updateMessageLog("Send object initialized - ready for communication");
  }

  private void createGUI() {
    System.out.println("Creating GUI window...");

    JFrame frame = new JFrame("Bridge Control Interface");
    frame.setSize(1400, 900);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    // Create main panels
    JPanel topPanel = createTopPanel();
    JPanel centerPanel = createCenterPanel();
    JPanel rightPanel = createStatusPanel();

    frame.add(topPanel, BorderLayout.NORTH);
    frame.add(centerPanel, BorderLayout.CENTER);
    frame.add(rightPanel, BorderLayout.EAST);

    frame.setLocationRelativeTo(null); // Center the window
    frame.setVisible(true);

    System.out.println("GUI window should now be visible");
  }

  private JPanel createTopPanel() {
    JPanel topPanel = new JPanel(new FlowLayout());
    topPanel.setBorder(BorderFactory.createTitledBorder("Operation Mode"));
    topPanel.setBackground(Color.LIGHT_GRAY);

    automaticModeButton = new JButton("Switch to Automatic Mode");
    automaticModeButton.setBackground(Color.GREEN);
    automaticModeButton.setForeground(Color.WHITE);
    automaticModeButton.setFont(new Font("Arial", Font.BOLD, 14));

    overrideModeButton = new JButton("Switch to Override Mode");
    overrideModeButton.setBackground(Color.RED);
    overrideModeButton.setForeground(Color.WHITE);
    overrideModeButton.setFont(new Font("Arial", Font.BOLD, 14));

    statusLabel = new JLabel("Current Mode: AUTOMATIC");
    statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
    statusLabel.setForeground(Color.BLUE);

    // Mode switching logic
    automaticModeButton.addActionListener(
        e -> {
          if (mcpSendObject != null) {
            mcpSendObject.sendMessage("automatic_mode");
            updateMessageLog("SENT: automatic_mode");
            isOverrideMode = false;
            updateModeDisplay();
            toggleControlPanel(false);
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    overrideModeButton.addActionListener(
        e -> {
          if (mcpSendObject != null) {
            mcpSendObject.sendMessage("override_mode");
            updateMessageLog("SENT: override_mode");
            isOverrideMode = true;
            updateModeDisplay();
            toggleControlPanel(true);
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    topPanel.add(automaticModeButton);
    topPanel.add(overrideModeButton);
    topPanel.add(Box.createHorizontalStrut(20));
    topPanel.add(statusLabel);

    return topPanel;
  }

  private JPanel createCenterPanel() {
    JPanel centerPanel = new JPanel(new BorderLayout());

    // Create control panel for override commands
    controlPanel = new JPanel(new GridBagLayout());
    controlPanel.setBorder(BorderFactory.createTitledBorder("Manual Override Controls"));
    controlPanel.setBackground(Color.WHITE);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Bridge Control Section
    JLabel bridgeControlLabel = new JLabel("BRIDGE CONTROL");
    bridgeControlLabel.setFont(new Font("Arial", Font.BOLD, 16));
    bridgeControlLabel.setForeground(Color.DARK_GRAY);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    controlPanel.add(bridgeControlLabel, gbc);

    // Bridge controls
    gbc.gridwidth = 1;
    gbc.gridy = 1;

    JLabel closeBridgeLabel = new JLabel("Close Bridge:");
    gbc.gridx = 0;
    controlPanel.add(closeBridgeLabel, gbc);

    JButton closeBridgeButton = new JButton("CLOSE BRIDGE");
    closeBridgeButton.setBackground(Color.BLUE);
    closeBridgeButton.setForeground(Color.WHITE);
    gbc.gridx = 1;
    controlPanel.add(closeBridgeButton, gbc);

    JLabel openBridgeLabel = new JLabel("Open Bridge:");
    gbc.gridx = 0;
    gbc.gridy = 2;
    controlPanel.add(openBridgeLabel, gbc);

    JButton openBridgeButton = new JButton("OPEN BRIDGE");
    openBridgeButton.setBackground(Color.ORANGE);
    openBridgeButton.setForeground(Color.WHITE);
    gbc.gridx = 1;
    controlPanel.add(openBridgeButton, gbc);

    // Gate Control Section
    JSeparator separator1 = new JSeparator();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    controlPanel.add(separator1, gbc);

    JLabel gateControlLabel = new JLabel("GATE CONTROL");
    gateControlLabel.setFont(new Font("Arial", Font.BOLD, 16));
    gateControlLabel.setForeground(Color.DARK_GRAY);
    gbc.gridy = 4;
    controlPanel.add(gateControlLabel, gbc);

    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel closeGateLabel = new JLabel("Close Gate:");
    gbc.gridx = 0;
    gbc.gridy = 5;
    controlPanel.add(closeGateLabel, gbc);

    JButton closeGateButton = new JButton("CLOSE GATE");
    closeGateButton.setBackground(Color.MAGENTA);
    closeGateButton.setForeground(Color.WHITE);
    gbc.gridx = 1;
    controlPanel.add(closeGateButton, gbc);

    JLabel openGateLabel = new JLabel("Open Gate:");
    gbc.gridx = 0;
    gbc.gridy = 6;
    controlPanel.add(openGateLabel, gbc);

    JButton openGateButton = new JButton("OPEN GATE");
    openGateButton.setBackground(Color.CYAN);
    openGateButton.setForeground(Color.BLACK);
    gbc.gridx = 1;
    controlPanel.add(openGateButton, gbc);

    // Light Control Section
    JSeparator separator2 = new JSeparator();
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    controlPanel.add(separator2, gbc);

    JLabel lightControlLabel = new JLabel("LIGHT CONTROL");
    lightControlLabel.setFont(new Font("Arial", Font.BOLD, 16));
    lightControlLabel.setForeground(Color.DARK_GRAY);
    gbc.gridy = 8;
    controlPanel.add(lightControlLabel, gbc);

    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel turnLightOnLabel = new JLabel("Turn All Lights On:");
    gbc.gridx = 0;
    gbc.gridy = 9;
    controlPanel.add(turnLightOnLabel, gbc);

    JButton turnLightOnButton = new JButton("LIGHTS ON");
    turnLightOnButton.setBackground(Color.GREEN);
    turnLightOnButton.setForeground(Color.WHITE);
    gbc.gridx = 1;
    controlPanel.add(turnLightOnButton, gbc);

    JLabel turnLightOffLabel = new JLabel("Turn All Lights Off:");
    gbc.gridx = 0;
    gbc.gridy = 10;
    controlPanel.add(turnLightOffLabel, gbc);

    JButton turnLightOffButton = new JButton("LIGHTS OFF");
    turnLightOffButton.setBackground(Color.GRAY);
    turnLightOffButton.setForeground(Color.WHITE);
    gbc.gridx = 1;
    controlPanel.add(turnLightOffButton, gbc);

    // Action Listeners with correct commands
    closeBridgeButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("close_bridge");
            updateMessageLog("SENT: close_bridge");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    openBridgeButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("open_bridge");
            updateMessageLog("SENT: open_bridge");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    closeGateButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("close_gate");
            updateMessageLog("SENT: close_gate");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    openGateButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("open_gate");
            updateMessageLog("SENT: open_gate");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    turnLightOnButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("lights_on");
            updateMessageLog("SENT: lights_on");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    turnLightOffButton.addActionListener(
        e -> {
          if (isOverrideMode && mcpSendObject != null) {
            mcpSendObject.sendMessage("lights_off");
            updateMessageLog("SENT: lights_off");
          } else if (!isOverrideMode) {
            showModeWarning();
          } else {
            updateMessageLog("ERROR: Send object not initialized");
          }
        });

    // Initially disable control panel
    toggleControlPanel(false);

    centerPanel.add(controlPanel, BorderLayout.CENTER);
    return centerPanel;
  }

  private JPanel createStatusPanel() {
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(BorderFactory.createTitledBorder("System Status"));
    statusPanel.setPreferredSize(new java.awt.Dimension(700, 0));

    // Current Status Section
    JPanel currentStatusPanel = new JPanel(new GridLayout(6, 1, 5, 5));
    currentStatusPanel.setBorder(BorderFactory.createTitledBorder("Current Status"));

    bridgeStatusLabel = new JLabel("Bridge: UNKNOWN", SwingConstants.LEFT);
    bridgeStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));

    gateStatusLabel = new JLabel("Gate: UNKNOWN", SwingConstants.LEFT);
    gateStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));

    roadLightLabel = new JLabel("Road Light: UNKNOWN", SwingConstants.LEFT);
    roadLightLabel.setFont(new Font("Arial", Font.BOLD, 14));

    boatLightLabel = new JLabel("Boat Light: UNKNOWN", SwingConstants.LEFT);
    boatLightLabel.setFont(new Font("Arial", Font.BOLD, 14));

    roadDistanceLabel = new JLabel("Road Distance: 0 cm", SwingConstants.LEFT);
    roadDistanceLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    boatDistanceLabel = new JLabel("Boat Distance: 0 cm", SwingConstants.LEFT);
    boatDistanceLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    currentStatusPanel.add(bridgeStatusLabel);
    currentStatusPanel.add(gateStatusLabel);
    currentStatusPanel.add(roadLightLabel);
    currentStatusPanel.add(boatLightLabel);
    currentStatusPanel.add(roadDistanceLabel);
    currentStatusPanel.add(boatDistanceLabel);

    // Message Log Section
    JPanel messageLogPanel = new JPanel(new BorderLayout());
    messageLogPanel.setBorder(BorderFactory.createTitledBorder("Message Log"));

    messageLogArea = new JTextArea(15, 30);
    messageLogArea.setEditable(false);
    messageLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
    messageLogArea.setBackground(Color.BLACK);
    messageLogArea.setForeground(Color.GREEN);

    JScrollPane scrollPane = new JScrollPane(messageLogArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    messageLogPanel.add(scrollPane, BorderLayout.CENTER);

    statusPanel.add(currentStatusPanel, BorderLayout.NORTH);
    statusPanel.add(messageLogPanel, BorderLayout.CENTER);

    return statusPanel;
  }

  private void updateModeDisplay() {
    if (isOverrideMode) {
      statusLabel.setText("Current Mode: OVERRIDE");
      statusLabel.setForeground(Color.RED);
    } else {
      statusLabel.setText("Current Mode: AUTOMATIC");
      statusLabel.setForeground(Color.BLUE);
    }
  }

  private void toggleControlPanel(boolean enabled) {
    Component[] components = controlPanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton) {
        component.setEnabled(enabled);
      }
    }

    if (enabled) {
      controlPanel.setBackground(Color.WHITE);
    } else {
      controlPanel.setBackground(Color.LIGHT_GRAY);
    }
  }

  private void showModeWarning() {
    JOptionPane.showMessageDialog(
        null,
        "Manual controls are only available in Override Mode!\n"
            + "Please switch to Override Mode first.",
        "Mode Warning",
        JOptionPane.WARNING_MESSAGE);
  }

  // Public methods for updating status from Receive thread
  public void updateSystemStatus(
      String mode,
      String bridgeState,
      String gateState,
      String roadDistance,
      String boatDistance,
      String roadLight,
      String boatLight) {
    SwingUtilities.invokeLater(
        () -> {
          // Update bridge status with color coding
          bridgeStatusLabel.setText("Bridge: " + bridgeState);
          if (bridgeState.equals("OPEN")) {
            bridgeStatusLabel.setForeground(Color.RED);
          } else if (bridgeState.equals("CLOSED")) {
            bridgeStatusLabel.setForeground(Color.GREEN);
          } else {
            bridgeStatusLabel.setForeground(Color.GRAY);
          }

          // Update gate status with color coding
          gateStatusLabel.setText("Gate: " + gateState);
          if (gateState.equals("OPEN")) {
            gateStatusLabel.setForeground(Color.GREEN);
          } else if (gateState.equals("CLOSED")) {
            gateStatusLabel.setForeground(Color.RED);
          } else {
            gateStatusLabel.setForeground(Color.GRAY);
          }

          // Update light status with color coding
          roadLightLabel.setText("Road Light: " + roadLight);
          setLightColor(roadLightLabel, roadLight);

          boatLightLabel.setText("Boat Light: " + boatLight);
          setLightColor(boatLightLabel, boatLight);

          // Update distances
          roadDistanceLabel.setText("Road Distance: " + roadDistance + " cm");
          boatDistanceLabel.setText("Boat Distance: " + boatDistance + " cm");

          // Update mode if it has changed from ESP32
          if (!mode.equals("UNKNOWN")) {
            boolean newOverrideMode = mode.equals("OVERRIDE");
            if (newOverrideMode != isOverrideMode) {
              isOverrideMode = newOverrideMode;
              updateModeDisplay();
              toggleControlPanel(isOverrideMode);
            }
          }
        });
  }

  private void setLightColor(JLabel label, String lightColor) {
    switch (lightColor) {
      case "RED":
        label.setForeground(Color.RED);
        break;
      case "GREEN":
        label.setForeground(Color.GREEN);
        break;
      case "YELLOW":
        label.setForeground(Color.ORANGE);
        break;
      default:
        label.setForeground(Color.GRAY);
        break;
    }
  }

  public void updateMessageLog(String message) {
    if (messageLogArea != null) {
      SwingUtilities.invokeLater(
          () -> {
            try {
              String timeStamp = java.time.LocalTime.now()
                  .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
              messageLogArea.append(timeStamp + " - " + message + "\n");
              messageLogArea.setCaretPosition(messageLogArea.getDocument().getLength());
            } catch (Exception e) {
              System.err.println("Error updating message log: " + e.getMessage());
            }
          });
    } else {
      System.out.println("Message log not initialized: " + message);
    }
  }
}
