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
import java.awt.geom.AffineTransform;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
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
  private JLabel statusLabel;
  private JButton overrideModeButton;
  private JButton automaticModeButton;
  private JPanel controlPanel;
  private boolean isOverrideMode = false;

  // Status Display Components in top bar
  private JLabel bridgeStatusLabel;
  private JLabel gateStatusLabel;
  private JLabel roadDistanceLabel;
  private JLabel boatDistanceLabel;
  private JTextPane messageLogArea;

  // Send object reference
  private Send mcpSendObject;

  // Current state
  private String currentMode = "AUTOMATIC";
  private String bridgeState = "CLOSED";
  private String gateState = "OPEN";
  private String roadLight = "RED";
  private String boatLight = "RED";

  public Gui() {
    SwingUtilities.invokeLater(() -> createGUI());
  }

  public void initializeSender(Send sendObject) {
    this.mcpSendObject = sendObject;
    updateMessageLog("Send object initialized - ready for communication");
  }

  private void createGUI() {
    System.out.println("Creating GUI window...");

    frame = new JFrame("Bridge Control Interface");
    frame.setSize(1600, 1000);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout(10, 10));
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

    JPanel topPanel = createTopPanel();
    JPanel centerPanel = createCenterPanel();
    JPanel rightPanel = createRightPanel();
    JPanel bottomPanel = createLogPanel();

    frame.add(topPanel, BorderLayout.NORTH);
    frame.add(centerPanel, BorderLayout.CENTER);
    frame.add(rightPanel, BorderLayout.EAST);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    System.out.println("GUI window should now be visible");
  }

  private JPanel createTopPanel() {
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBackground(new Color(44, 62, 80));
    topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

    // Left side - mode status
    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
    leftPanel.setBackground(new Color(44, 62, 80));

    statusLabel = new JLabel("Current Mode: AUTOMATIC");
    statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
    statusLabel.setForeground(Color.WHITE);
    leftPanel.add(statusLabel);

    // Add separator
    JLabel separator1 = new JLabel(" | ");
    separator1.setFont(new Font("Arial", Font.BOLD, 18));
    separator1.setForeground(new Color(149, 165, 166));
    leftPanel.add(separator1);

    // System status labels
    bridgeStatusLabel = createTopStatusLabel("Bridge: CLOSED");
    leftPanel.add(bridgeStatusLabel);

    gateStatusLabel = createTopStatusLabel("Gate: OPEN");
    leftPanel.add(gateStatusLabel);

    roadDistanceLabel = createTopStatusLabel("Road: 0 cm");
    leftPanel.add(roadDistanceLabel);

    boatDistanceLabel = createTopStatusLabel("Boat: 0 cm");
    leftPanel.add(boatDistanceLabel);

    // Right side - mode buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBackground(new Color(44, 62, 80));

    automaticModeButton = new JButton("Switch to Automatic Mode");
    automaticModeButton.setBackground(new Color(46, 204, 113, 128));
    automaticModeButton.setForeground(Color.WHITE);
    automaticModeButton.setFont(new Font("Arial", Font.BOLD, 16));
    automaticModeButton.setFocusPainted(false);
    automaticModeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    overrideModeButton = new JButton("Switch to Override Mode");
    overrideModeButton.setBackground(new Color(231, 76, 60));
    overrideModeButton.setForeground(Color.WHITE);
    overrideModeButton.setFont(new Font("Arial", Font.BOLD, 16));
    overrideModeButton.setFocusPainted(false);
    overrideModeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    // Mode switching logic
    automaticModeButton.addActionListener(e -> {
      if (!isOverrideMode) {
        return;
      }
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

    overrideModeButton.addActionListener(e -> {
      if (isOverrideMode) {
        return;
      }
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

    // Hover effects
    automaticModeButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (!isOverrideMode)
          return;
        automaticModeButton.setBackground(new Color(52, 231, 128));
        automaticModeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)));
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (!isOverrideMode) {
          automaticModeButton.setBackground(new Color(46, 204, 113, 128));
        } else {
          automaticModeButton.setBackground(new Color(46, 204, 113));
        }
        automaticModeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
      }
    });

    overrideModeButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (isOverrideMode)
          return;
        overrideModeButton.setBackground(new Color(243, 104, 88));
        overrideModeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)));
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (isOverrideMode) {
          overrideModeButton.setBackground(new Color(231, 76, 60, 128));
        } else {
          overrideModeButton.setBackground(new Color(231, 76, 60));
        }
        overrideModeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
      }
    });

    buttonPanel.add(automaticModeButton);
    buttonPanel.add(Box.createHorizontalStrut(10));
    buttonPanel.add(overrideModeButton);

    topPanel.add(leftPanel, BorderLayout.WEST);
    topPanel.add(buttonPanel, BorderLayout.EAST);

    return topPanel;
  }

  private JLabel createTopStatusLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.PLAIN, 15));
    label.setForeground(Color.WHITE);
    label.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
    return label;
  }

  private JPanel createCenterPanel() {
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(new Color(52, 73, 94));

    bridgePanel = new BridgeAnimationPanel();
    bridgePanel.setPreferredSize(new Dimension(1000, 700));

    centerPanel.add(bridgePanel, BorderLayout.CENTER);
    return centerPanel;
  }

  private JPanel createRightPanel() {
    JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    rightPanel.setPreferredSize(new Dimension(500, 0));
    rightPanel.setBackground(new Color(236, 240, 241));

    controlPanel = createControlPanel();

    rightPanel.add(controlPanel, BorderLayout.CENTER);

    return rightPanel;
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

    // Traffic Sequence Section
    addSectionLabel(panel, "TRAFFIC SEQUENCES", gbc, 0);
    addControlButton(panel, "ALLOW BOAT TRAFFIC", new Color(41, 128, 185), "allow_boat_traffic", gbc, 1, null);
    addControlButton(panel, "ALLOW ROAD TRAFFIC", new Color(41, 128, 185), "allow_road_traffic", gbc, 2, null);

    // Bridge Control Section
    addSectionLabel(panel, "BRIDGE CONTROL", gbc, 3);
    addControlButton(panel, "OPEN BRIDGE", new Color(52, 73, 94), "open_bridge", gbc, 4, "OPEN");
    addControlButton(panel, "CLOSE BRIDGE", new Color(52, 73, 94), "close_bridge", gbc, 5, "CLOSED");

    // Gate Control Section
    addSectionLabel(panel, "GATE CONTROL", gbc, 6);
    addControlButton(panel, "OPEN GATE", new Color(52, 73, 94), "open_gate", gbc, 7, "OPEN");
    addControlButton(panel, "CLOSE GATE", new Color(52, 73, 94), "close_gate", gbc, 8, "CLOSED");

    // Road Light Control Section
    addSectionLabel(panel, "ROAD LIGHTS", gbc, 9);
    addControlButton(panel, "RED", new Color(231, 76, 60), "road_lights_red", gbc, 10, null);
    addControlButton(panel, "YELLOW", new Color(241, 196, 15), "road_lights_yellow", gbc, 11, null);
    addControlButton(panel, "GREEN", new Color(46, 204, 113), "road_lights_green", gbc, 12, null);

    // Boat Light Control Section
    addSectionLabel(panel, "BOAT LIGHTS", gbc, 13);
    addControlButton(panel, "RED", new Color(231, 76, 60), "boat_lights_red", gbc, 14, null);
    addControlButton(panel, "GREEN", new Color(46, 204, 113), "boat_lights_green", gbc, 15, null);

    // Bridge Lights Control Section
    addSectionLabel(panel, "BRIDGE LIGHTS", gbc, 16);

    gbc.gridy = 17;
    JCheckBox manualControlCheckbox = new JCheckBox("Manual Control");
    manualControlCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
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

    addControlButton(panel, "LIGHTS ON", new Color(255, 193, 7), "manual_bridge_lights_on", gbc, 18, null);
    addControlButton(panel, "LIGHTS OFF", new Color(158, 158, 158), "manual_bridge_lights_off", gbc, 19, null);

    // Initially disable all buttons
    Component[] components = panel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton || component instanceof JCheckBox) {
        component.setEnabled(false);
      }
    }

    return panel;
  }

  private void addSectionLabel(JPanel panel, String text, GridBagConstraints gbc, int row) {
    gbc.gridy = row;
    JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.BOLD, 12));
    label.setForeground(new Color(52, 73, 94));
    label.setBorder(BorderFactory.createEmptyBorder(3, 5, 1, 5));
    panel.add(label, gbc);
  }

  private void addControlButton(JPanel panel, String text, Color color, String command, GridBagConstraints gbc,
      int row, String targetState) {
    gbc.gridy = row;
    JButton button = new JButton(text);
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setFont(new Font("Arial", Font.BOLD, 12));
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(170, 32));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.putClientProperty("targetState", targetState);
    button.putClientProperty("command", command);

    button.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(color.darker(), 1, true),
        BorderFactory.createEmptyBorder(4, 12, 4, 12)));

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (button.isEnabled() && shouldButtonBeEnabled(button)) {
          button.setBackground(new Color(93, 109, 126));
        }
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (button.isEnabled() && shouldButtonBeEnabled(button)) {
          button.setBackground(color);
        } else if (!shouldButtonBeEnabled(button)) {
          button.setBackground(new Color(150, 150, 150));
        }
      }
    });

    button.addActionListener(e -> {
      if (!shouldButtonBeEnabled(button)) {
        return;
      }
      if (isOverrideMode && mcpSendObject != null) {
        mcpSendObject.sendMessage(command);
        updateMessageLog("SENT: " + command);
      } else if (!isOverrideMode) {
        showModeWarning();
      } else {
        updateMessageLog("ERROR: Send object not initialized");
      }
    });

    panel.add(button, gbc);
  }

  private boolean shouldButtonBeEnabled(JButton button) {
    String targetState = (String) button.getClientProperty("targetState");
    if (targetState == null) {
      return true;
    }

    String command = (String) button.getClientProperty("command");
    if (command.contains("bridge")) {
      return !bridgeState.equals(targetState);
    } else if (command.contains("gate")) {
      return !gateState.equals(targetState);
    }
    return true;
  }

  private void updateButtonStates() {
    Component[] components = controlPanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton) {
        JButton button = (JButton) component;
        boolean shouldEnable = shouldButtonBeEnabled(button);
        if (!shouldEnable) {
          button.setBackground(new Color(150, 150, 150));
        } else if (isOverrideMode) {
          button.setBackground(new Color(52, 73, 94));
        }
      }
    }
  }

  private JPanel createLogPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(52, 73, 94)),
        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    panel.setPreferredSize(new Dimension(0, 200));
    panel.setBackground(new Color(30, 30, 30));

    // Title label
    JLabel titleLabel = new JLabel("Message Log");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
    titleLabel.setForeground(new Color(200, 200, 200));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    messageLogArea = new JTextPane();
    messageLogArea.setEditable(false);
    messageLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
    messageLogArea.setBackground(new Color(30, 30, 30));
    messageLogArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JScrollPane scrollPane = new JScrollPane(messageLogArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 40));
    scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
      @Override
      protected void configureScrollBarColors() {
        this.thumbColor = new Color(80, 80, 80);
        this.trackColor = new Color(40, 40, 40);
      }
    });

    panel.add(titleLabel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  private void updateModeDisplay() {
    if (isOverrideMode) {
      statusLabel.setText("Current Mode: OVERRIDE");
      statusLabel.setForeground(new Color(231, 76, 60));
      overrideModeButton.setBackground(new Color(231, 76, 60, 128));
      automaticModeButton.setBackground(new Color(46, 204, 113));
    } else {
      statusLabel.setText("Current Mode: AUTOMATIC");
      statusLabel.setForeground(new Color(46, 204, 113));
      automaticModeButton.setBackground(new Color(46, 204, 113, 128));
      overrideModeButton.setBackground(new Color(231, 76, 60));
    }
  }

  private void toggleControlPanel(boolean enabled) {
    Component[] components = controlPanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton || component instanceof JCheckBox) {
        component.setEnabled(enabled);
      }
    }

    if (enabled) {
      controlPanel.setBackground(new Color(236, 240, 241));
      updateButtonStates();
    } else {
      controlPanel.setBackground(new Color(220, 220, 220));
    }
  }

  private void showModeWarning() {
    JOptionPane.showMessageDialog(
        frame,
        "Manual controls are only available in Override Mode!\n"
            + "Please switch to Override Mode first.",
        "Mode Warning",
        JOptionPane.WARNING_MESSAGE);
  }

  public void updateSystemStatus(String mode, String bridgeState, String gateState,
      String roadDistance, String boatDistance,
      String roadLight, String boatLight, String bridgeLight) {
    SwingUtilities.invokeLater(() -> {
      this.currentMode = mode;
      this.bridgeState = bridgeState;
      this.gateState = gateState;
      this.roadLight = roadLight;
      this.boatLight = boatLight;

      // Update bridge status with color coding
      bridgeStatusLabel.setText("Bridge: " + bridgeState);
      if (bridgeState.equals("OPEN")) {
        bridgeStatusLabel.setForeground(new Color(231, 76, 60));
      } else if (bridgeState.equals("CLOSED")) {
        bridgeStatusLabel.setForeground(new Color(46, 204, 113));
      } else {
        bridgeStatusLabel.setForeground(Color.LIGHT_GRAY);
      }

      // Update gate status with color coding
      gateStatusLabel.setText("Gate: " + gateState);
      if (gateState.equals("OPEN")) {
        gateStatusLabel.setForeground(new Color(46, 204, 113));
      } else if (gateState.equals("CLOSED")) {
        gateStatusLabel.setForeground(new Color(231, 76, 60));
      } else {
        gateStatusLabel.setForeground(Color.LIGHT_GRAY);
      }

      // Update distances
      roadDistanceLabel.setText("Road: " + roadDistance + " cm");
      boatDistanceLabel.setText("Boat: " + boatDistance + " cm");

      // Update mode if it has changed from ESP32
      if (!mode.equals("UNKNOWN")) {
        boolean newOverrideMode = mode.equals("OVERRIDE");
        if (newOverrideMode != isOverrideMode) {
          isOverrideMode = newOverrideMode;
          updateModeDisplay();
          toggleControlPanel(isOverrideMode);
        }
      }

      // Update animation
      bridgePanel.updateState(bridgeState, gateState, roadLight, boatLight);

      // Update bridge lights
      bridgePanel.updateBridgeLights(bridgeLight.equals("ON"));

      // Update button states
      updateButtonStates();
    });
  }

  public void updateMessageLog(String message) {
    if (messageLogArea != null) {
      SwingUtilities.invokeLater(() -> {
        try {
          String timeStamp = java.time.LocalTime.now()
              .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
          String newMessage = timeStamp + " - " + message + "\n";

          StyledDocument doc = messageLogArea.getStyledDocument();

          // Change all existing text to white
          Style whiteStyle = messageLogArea.addStyle("White", null);
          StyleConstants.setForeground(whiteStyle, Color.WHITE);
          doc.setCharacterAttributes(0, doc.getLength(), whiteStyle, false);

          // Insert new message at the top with green color
          Style greenStyle = messageLogArea.addStyle("Green", null);
          StyleConstants.setForeground(greenStyle, new Color(46, 204, 113));
          doc.insertString(0, newMessage, greenStyle);

          // Keep scroll at top
          messageLogArea.setCaretPosition(0);
        } catch (BadLocationException e) {
          System.err.println("Error updating message log: " + e.getMessage());
        }
      });
    } else {
      System.out.println("Message log not initialized: " + message);
    }
  }

  // Bridge Animation Panel
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

      // Calculate pulse effect
      int pulseIntensity = (int) (Math.sin(lightPulse * 0.1) * 15 + 15);

      // Draw 8 lights evenly spaced across the bridge
      int numLights = 8;
      int spacing = (bridgeWidth * 2) / (numLights + 1);

      for (int i = 1; i <= numLights; i++) {
        int lightX = centerX - bridgeWidth + (i * spacing);

        if (bridgeLightsOn) {
          // Draw glow effect when lights are on
          g2d.setColor(new Color(255, 220, 100, 60 + pulseIntensity));
          g2d.fillOval(lightX - 12, lightY - 12, 24, 24);

          // Draw bright light
          g2d.setColor(new Color(255, 240, 150));
          g2d.fillOval(lightX - 6, lightY - 6, 12, 12);

          // Draw highlight
          g2d.setColor(new Color(255, 255, 200));
          g2d.fillOval(lightX - 3, lightY - 4, 4, 4);
        } else {
          // Draw dark/off light
          g2d.setColor(new Color(60, 60, 60));
          g2d.fillOval(lightX - 5, lightY - 5, 10, 10);
        }

        // Draw light fixture/mount
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
        drawLight(g2d, x, y - 133, activeLight.equals("RED"), Color.RED);
        drawLight(g2d, x, y - 105, activeLight.equals("YELLOW"), new Color(255, 200, 0));
        drawLight(g2d, x, y - 77, activeLight.equals("GREEN"), Color.GREEN);
      } else {
        drawLight(g2d, x, y - 133, activeLight.equals("RED"), Color.RED);
        drawLight(g2d, x, y - 100, activeLight.equals("GREEN"), Color.GREEN);
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
