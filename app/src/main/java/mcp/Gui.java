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
import javax.swing.JComponent;
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
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class Gui {
  // GUI Components
  private JFrame frame;
  private BridgeAnimationPanel bridgePanel;
  private JPanel controlPanel;
  private JPanel modeControlPanel;
  private JPanel warningPanel;
  private JPanel notificationPanel;
  private JLabel notificationLabel;
  private Timer notificationTimer;
  private Timer notificationSlideTimer;
  private int notificationY = -100;
  private int notificationTargetY = 10;
  private boolean isOverrideMode = false;
  private boolean communicationLost = false;
  private boolean isDiagnosticMode = false;

  // Status Display Components
  private JLabel trafficSequenceLabel;
  private JLabel systemTestLabel;
  private JLabel roadLightsLabel;
  private JLabel boatLightsLabel;
  private JLabel bridgeLightsLabel;
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
  private long lastStatusTime = 0;
  private String lastWeightReading = "N/A";

  // Communication timeout timer
  private Timer communicationCheckTimer;
  private JComponent communicationLostPane;

  // Responsive sizing
  private boolean isLaptopSize = false;

  // Constructor
  public Gui() {
    SwingUtilities.invokeLater(this::createGUI);
    startCommunicationMonitor();
  }

  // Update this GUI's sender object
  public void initializeSender(Send sendObject) {
    this.mcpSendObject = sendObject;
    updateMessageLog("Send object initialized - ready for communication");
  }

  // Timer for detecting communication loss
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
        updateMessageLog("SYSTEM: Connected to ESP");
      }
    });
    communicationCheckTimer.start();
  }

  private void updateCommunicationStatus(boolean connected) {
    SwingUtilities.invokeLater(() -> {
      if (connected) {
        warningPanel.setVisible(false);
        communicationLostPane.setVisible(false);
      } else {
        warningPanel.setVisible(true);
        communicationLostPane.setVisible(true);
      }
    });
  }

  private void createGUI() {
    System.out.println("Creating GUI window");

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
    frame.getContentPane().setBackground(new Color(18, 18, 18));

    // Create main center area with left stats panel
    JPanel centerArea = new JPanel(new BorderLayout(10, 10));
    centerArea.setBackground(new Color(18, 18, 18));

    // Create wrapper panel for stats with full height background
    JPanel statsOuterPanel = new JPanel(new BorderLayout());
    statsOuterPanel.setBackground(new Color(28, 28, 30));
    statsOuterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    statsOuterPanel.add(createStatsPanel(), BorderLayout.CENTER);

    centerArea.add(statsOuterPanel, BorderLayout.WEST);
    centerArea.add(createCenterPanel(), BorderLayout.CENTER);

    JPanel rightPanel = createRightPanel();
    JPanel bottomPanel = createLogPanel();

    frame.add(centerArea, BorderLayout.CENTER);
    frame.add(rightPanel, BorderLayout.EAST);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    // Setup glass pane to block interactions when communication is lost
    communicationLostPane = (JComponent) frame.getGlassPane();
    communicationLostPane.setOpaque(false);
    communicationLostPane.addMouseListener(new java.awt.event.MouseAdapter() {
      // Consume all mouse events when glass pane is visible
    });
    communicationLostPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      // Consume all mouse motion events when glass pane is visible
    });
    communicationLostPane.addKeyListener(new java.awt.event.KeyAdapter() {
      // Consume all key events when glass pane is visible
    });
    communicationLostPane.setVisible(false);

    // Add resize listener for responsiveness
    frame.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        handleResize();
      }
    });

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    // Create notification panel after frame is visible
    createNotificationPanel();

    System.out.println("GUI window should now be visible");
  }

  private void handleResize() {
    int width = frame.getWidth();
    boolean wasLaptopSize = isLaptopSize;
    isLaptopSize = width < 1600;

    // Update notification panel width
    if (notificationPanel != null) {
      int panelWidth = width / 2;
      int panelX = (width - panelWidth) / 2;
      notificationPanel.setBounds(panelX, notificationY, panelWidth, 70);
    }

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
    statsPanel.setBackground(new Color(28, 28, 30));
    statsPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 62), 2),
            "System Status",
            0,
            0,
            new Font("Arial", Font.BOLD, 13),
            new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    int panelWidth = isLaptopSize ? 220 : 250;
    statsPanel.setPreferredSize(new Dimension(panelWidth, 0));

    // Create all status labels - vertical list
    modeLabel = createStatLabel("Mode: AUTOMATIC");
    bridgeStatusLabel = createStatLabel("Bridge: CLOSED");
    gateStatusLabel = createStatLabel("Gate: OPEN");
    sequenceStateLabel = createStatLabel("State: IDLE");
    roadDistanceLabel = createStatLabel("Road: 0 cm");
    boatDistanceLabel = createStatLabel("Boat: 0 cm");
    bridgeMovementLabel = createStatLabel("Bridge: 0 cm");
    boatClearanceLabel = createStatLabel("Clearance: 0 cm");
    manualLightsLabel = createStatLabel("Manual Lights: No");
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
    label.setFont(new Font("Arial", Font.PLAIN, 14));
    label.setForeground(new Color(180, 180, 180));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  private JPanel createCenterPanel() {
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(new Color(18, 18, 18));

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

    JLabel warningIcon = new JLabel("COMMUNICATION LOST");
    warningIcon.setFont(new Font("Arial", Font.BOLD, 72));
    warningIcon.setForeground(Color.WHITE);
    warningIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel warningLabel = new JLabel("Lost connection with ESP, attempting reconnection");
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

  private void createNotificationPanel() {
    // Create notification panel
    notificationPanel = new JPanel(new BorderLayout());
    notificationPanel.setBackground(new Color(60, 60, 62));
    notificationPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(100, 100, 102), 2),
        BorderFactory.createEmptyBorder(15, 30, 15, 30)));

    notificationLabel = new JLabel("");
    notificationLabel.setFont(new Font("Arial", Font.BOLD, 16));
    notificationLabel.setForeground(new Color(220, 220, 220));
    notificationLabel.setHorizontalAlignment(JLabel.CENTER);

    notificationPanel.add(notificationLabel, BorderLayout.CENTER);
    notificationPanel.setVisible(false);

    // Add to layered pane so it appears on top of everything
    frame.getLayeredPane().add(notificationPanel, javax.swing.JLayeredPane.MODAL_LAYER);
    int panelWidth = frame.getWidth() / 3;
    int panelX = (frame.getWidth() - panelWidth) / 2;
    notificationPanel.setBounds(panelX, notificationY, panelWidth, 70);
  }

  public void showNotification(String message) {
    SwingUtilities.invokeLater(() -> {
      // Stop any existing notification animations/timers
      if (notificationTimer != null && notificationTimer.isRunning()) {
        notificationTimer.stop();
      }
      if (notificationSlideTimer != null && notificationSlideTimer.isRunning()) {
        notificationSlideTimer.stop();
      }

      // If a notification is already visible, slide it out first
      if (notificationPanel.isVisible()) {
        slideOutNotification(() -> {
          // After sliding out, show the new notification
          displayNewNotification(message);
        });
      } else {
        // No notification visible, show immediately
        displayNewNotification(message);
      }
    });
  }

  private void displayNewNotification(String message) {
    notificationLabel.setText(message);
    notificationPanel.setVisible(true);
    notificationY = -70;

    // Update width in case frame was resized
    int panelWidth = frame.getWidth() / 3;
    int panelX = (frame.getWidth() - panelWidth) / 2;
    notificationPanel.setBounds(panelX, notificationY, panelWidth, 70);

    // Slide in animation
    notificationSlideTimer = new Timer(10, e -> {
      notificationY += 3;
      if (notificationY >= notificationTargetY) {
        notificationY = notificationTargetY;
        notificationSlideTimer.stop();

        // Start timer to hide notification after 5 seconds
        notificationTimer = new Timer(5000, evt -> {
          slideOutNotification(null);
        });
        notificationTimer.setRepeats(false);
        notificationTimer.start();
      }
      notificationPanel.setBounds(panelX, notificationY, panelWidth, 70);
    });
    notificationSlideTimer.start();
  }

  private void slideOutNotification(Runnable onComplete) {
    int panelWidth = frame.getWidth() / 3;
    int panelX = (frame.getWidth() - panelWidth) / 2;
    notificationSlideTimer = new Timer(10, e -> {
      notificationY -= 3;
      if (notificationY <= -70) {
        notificationY = -70;
        notificationSlideTimer.stop();
        notificationPanel.setVisible(false);

        if (onComplete != null) {
          onComplete.run();
        }
      }
      notificationPanel.setBounds(panelX, notificationY, panelWidth, 70);
    });
    notificationSlideTimer.start();
  }

  private JPanel createRightPanel() {
    JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    int panelWidth = isLaptopSize ? 400 : 500;
    rightPanel.setPreferredSize(new Dimension(panelWidth, 0));
    rightPanel.setBackground(new Color(28, 28, 30));

    // Create main container with vertical layout
    JPanel mainContainer = new JPanel();
    mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
    mainContainer.setBackground(new Color(28, 28, 30));
    mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

    // Customize scrollbar colors
    scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
      @Override
      protected void configureScrollBarColors() {
        this.thumbColor = new Color(130, 130, 130);
        this.trackColor = new Color(235, 236, 240);
        this.thumbHighlightColor = new Color(160, 160, 160);
        this.thumbDarkShadowColor = new Color(130, 130, 130);
        this.thumbLightShadowColor = new Color(130, 130, 130);
      }

      @Override
      protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
          return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Check if mouse is over the thumb
        java.awt.Point mousePos = c.getMousePosition();
        boolean isHovered = mousePos != null && thumbBounds.contains(mousePos);

        g2.setColor(isHovered ? new Color(160, 160, 160) : new Color(130, 130, 130));
        g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
            thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);

        g2.dispose();
      }

      @Override
      protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
      }

      @Override
      protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
      }

      private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
      }
    });

    rightPanel.add(scrollPane, BorderLayout.CENTER);

    return rightPanel;
  }

  private JPanel createModeControlPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(new Color(60, 60, 62), 2),
        "Mode Controls",
        0,
        0,
        new Font("Arial", Font.BOLD, 13),
        new Color(200, 200, 200)));
    panel.setBackground(new Color(28, 28, 30));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 10, 5, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy = 0;

    Color buttonColor = new Color(60, 60, 62);
    int buttonHeight = isLaptopSize ? 28 : 32;

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

    // Set initial states - system starts in automatic mode
    automaticModeButton.setEnabled(false);
    automaticModeButton.setText("CURRENTLY IN AUTOMATIC MODE");
    automaticModeButton.setBackground(new Color(70, 70, 72));

    overrideModeButton.setEnabled(true);
    overrideModeButton.setText("SWITCH TO OVERRIDE MODE");
    overrideModeButton.setBackground(new Color(101, 181, 109));

    panel.add(automaticModeButton, gbc);
    gbc.gridy = 1;
    panel.add(overrideModeButton, gbc);

    return panel;
  }

  private JButton createModeButton(String text, Color color, int height) {
    JButton button = new JButton(text);
    button.setBackground(color);
    button.setForeground(new Color(220, 220, 220));
    if (isLaptopSize) {
      button.setFont(new Font("Arial", Font.BOLD, 10));
    } else {
      button.setFont(new Font("Arial", Font.BOLD, 11));
    }
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(170, height));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      Color hoverColor = null;

      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          hoverColor = button.getBackground();
          // Create a subtle brightness increase (10% brighter)
          int r = Math.min(255, (int) (hoverColor.getRed() * 1.1));
          int g = Math.min(255, (int) (hoverColor.getGreen() * 1.1));
          int b = Math.min(255, (int) (hoverColor.getBlue() * 1.1));
          button.setBackground(new Color(r, g, b));
        }
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        if (button.isEnabled() && hoverColor != null) {
          button.setBackground(hoverColor);
        }
      }
    });

    return button;
  }

  private JPanel createControlPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(new Color(60, 60, 62), 2),
        "Manual Override Controls",
        0,
        0,
        new Font("Arial", Font.BOLD, 13),
        new Color(200, 200, 200)));
    panel.setBackground(new Color(28, 28, 30));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 10, 3, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    Color buttonColor = new Color(101, 181, 109);
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
          emergencyStopButton.setBackground(new Color(210, 63, 50));
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
    trafficSequenceLabel = addSectionLabel(panel, "TRAFFIC SEQUENCES - SWITCH TO OVERRIDE MODE TO ENABLE", gbc, 2);
    addControlButton(panel, "ALLOW BOAT TRAFFIC", buttonColor, "allow_boat_traffic", gbc, 3, buttonHeight);
    addControlButton(panel, "ALLOW ROAD TRAFFIC", buttonColor, "allow_road_traffic", gbc, 4, buttonHeight);

    // System Test Section
    systemTestLabel = addSectionLabel(panel, "SYSTEM TESTING - SWITCH TO OVERRIDE MODE TO ENABLE", gbc, 5);
    addControlButton(panel, "RUN FULL TEST", buttonColor, "run_full_test", gbc, 6, buttonHeight);
    addControlButton(panel, "PERFORM DIAGNOSTICS", buttonColor, "perform_diagnostics", gbc, 7, buttonHeight);
    addControlButton(panel, "RESTART ESP32", new Color(192, 57, 43), "restart", gbc, 8, buttonHeight);

    // Road Light Control Section
    roadLightsLabel = addSectionLabel(panel, "ROAD LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE", gbc, 9);
    addControlButton(panel, "RED", new Color(192, 57, 43), "road_lights_red", gbc, 10, buttonHeight);
    addControlButton(panel, "YELLOW", new Color(230, 176, 5), "road_lights_yellow", gbc, 11, buttonHeight);
    addControlButton(panel, "GREEN", buttonColor, "road_lights_green", gbc, 12, buttonHeight);

    // Boat Light Control Section
    boatLightsLabel = addSectionLabel(panel, "BOAT LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE", gbc, 13);
    addControlButton(panel, "RED", new Color(192, 57, 43), "boat_lights_red", gbc, 14, buttonHeight);
    addControlButton(panel, "GREEN", buttonColor, "boat_lights_green", gbc, 15, buttonHeight);

    // Bridge Lights Control Section
    bridgeLightsLabel = addSectionLabel(panel, "BRIDGE LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE", gbc, 16);

    gbc.gridy = 17;
    JCheckBox manualControlCheckbox = new JCheckBox("Manual Control");
    manualControlCheckbox.setFont(new Font("Arial", Font.PLAIN, 11));
    manualControlCheckbox.setBackground(new Color(28, 28, 30));
    manualControlCheckbox.setForeground(new Color(180, 180, 180));
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

    addControlButton(panel, "LIGHTS ON", new Color(230, 176, 5), "manual_bridge_lights_on", gbc, 18, buttonHeight);
    addControlButton(panel, "LIGHTS OFF", new Color(100, 100, 100), "manual_bridge_lights_off", gbc, 19, buttonHeight);

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
          button.setBackground(new Color(70, 70, 72));
        }
      }
    }

    return panel;
  }

  private JLabel addSectionLabel(JPanel panel, String text, GridBagConstraints gbc, int row) {
    gbc.gridy = row;
    JLabel label = new JLabel(text);
    if (isLaptopSize) {
      label.setFont(new Font("Arial", Font.BOLD, 9));
    } else {
      label.setFont(new Font("Arial", Font.BOLD, 11));
    }
    label.setForeground(new Color(160, 160, 160));
    label.setBorder(BorderFactory.createEmptyBorder(3, 5, 1, 5));
    panel.add(label, gbc);
    // Return the label
    return label;
  }

  private void addControlButton(JPanel panel, String text, Color color, String command,
      GridBagConstraints gbc, int row, int height) {
    gbc.gridy = row;
    JButton button = new JButton(text);
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    if (isLaptopSize) {
      button.setFont(new Font("Arial", Font.BOLD, 10));
    } else {
      button.setFont(new Font("Arial", Font.BOLD, 11));
    }
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(170, height));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.putClientProperty("command", command);
    button.putClientProperty("originalColor", color);

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        if (button.isEnabled()) {
          Color originalColor = (Color) button.getClientProperty("originalColor");
          // Create a subtle brightness increase (10% brighter)
          int r = Math.min(255, (int) (originalColor.getRed() * 1.1));
          int g = Math.min(255, (int) (originalColor.getGreen() * 1.1));
          int b = Math.min(255, (int) (originalColor.getBlue() * 1.1));
          button.setBackground(new Color(r, g, b));
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
        BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(60, 60, 62)),
        BorderFactory.createEmptyBorder(5, 10, 5, 0)));
    int logHeight = isLaptopSize ? 250 : 300;
    panel.setPreferredSize(new Dimension(0, logHeight));
    panel.setBackground(new Color(18, 18, 18));

    // Header panel with title and buttons
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(new Color(18, 18, 18));

    JLabel titleLabel = new JLabel("Message Log");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
    titleLabel.setForeground(new Color(200, 200, 200));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttonPanel.setBackground(new Color(18, 18, 18));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JButton clearButton = new JButton("Clear");
    clearButton.setFont(new Font("Arial", Font.PLAIN, 10));
    clearButton.setBackground(new Color(192, 57, 43));
    clearButton.setForeground(Color.WHITE);
    clearButton.setFocusPainted(false);
    clearButton.setBorderPainted(false);
    clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    clearButton.addActionListener(e -> clearMessageLog());

    clearButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        clearButton.setBackground(new Color(211, 63, 47));
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        clearButton.setBackground(new Color(192, 57, 43));
      }
    });

    JButton exportButton = new JButton("Export");
    exportButton.setFont(new Font("Arial", Font.PLAIN, 10));
    exportButton.setBackground(new Color(52, 152, 219));
    exportButton.setForeground(Color.WHITE);
    exportButton.setFocusPainted(false);
    exportButton.setBorderPainted(false);
    exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    exportButton.addActionListener(e -> exportMessageLog());

    exportButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseEntered(java.awt.event.MouseEvent evt) {
        exportButton.setBackground(new Color(57, 167, 241));
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        exportButton.setBackground(new Color(52, 152, 219));
      }
    });

    buttonPanel.add(clearButton);
    buttonPanel.add(exportButton);

    headerPanel.add(titleLabel, BorderLayout.WEST);
    headerPanel.add(buttonPanel, BorderLayout.EAST);

    messageLogArea = new JTextPane();
    messageLogArea.setEditable(false);
    if (isLaptopSize) {
      messageLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
    } else {
      messageLogArea.setFont(new Font("Consolas", Font.PLAIN, 14));
    }

    messageLogArea.setBackground(new Color(18, 18, 18));
    messageLogArea.setForeground(new Color(180, 180, 180));
    messageLogArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JScrollPane scrollPane = new JScrollPane(messageLogArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(null);

    // Customize scrollbar colors
    scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
      @Override
      protected void configureScrollBarColors() {
        this.thumbColor = new Color(130, 130, 130);
        this.trackColor = new Color(235, 236, 240);
        this.thumbHighlightColor = new Color(160, 160, 160);
        this.thumbDarkShadowColor = new Color(130, 130, 130);
        this.thumbLightShadowColor = new Color(130, 130, 130);
      }

      @Override
      protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
          return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Check if mouse is over the thumb
        java.awt.Point mousePos = c.getMousePosition();
        boolean isHovered = mousePos != null && thumbBounds.contains(mousePos);

        g2.setColor(isHovered ? new Color(160, 160, 160) : new Color(130, 130, 130));
        g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
            thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);

        g2.dispose();
      }

      @Override
      protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
      }

      @Override
      protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
      }

      private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
      }
    });

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
    // Update section labels
    if (enabled) {
      trafficSequenceLabel.setText("TRAFFIC SEQUENCES");
      systemTestLabel.setText("SYSTEM TESTING");
      roadLightsLabel.setText("ROAD LIGHTS");
      boatLightsLabel.setText("BOAT LIGHTS");
      bridgeLightsLabel.setText("BRIDGE LIGHTS");
    } else {
      trafficSequenceLabel.setText("TRAFFIC SEQUENCES - SWITCH TO OVERRIDE MODE TO ENABLE");
      systemTestLabel.setText("SYSTEM TESTING - SWITCH TO OVERRIDE MODE TO ENABLE");
      roadLightsLabel.setText("ROAD LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE");
      boatLightsLabel.setText("BOAT LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE");
      bridgeLightsLabel.setText("BRIDGE LIGHTS - SWITCH TO OVERRIDE MODE TO ENABLE");
    }

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
            button.setBackground(new Color(70, 70, 72));
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
            button.setBackground(new Color(70, 70, 72));
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
        bridgeStatusLabel.setForeground(new Color(46, 204, 113));
      } else if (bridgeState.equals("CLOSED")) {
        bridgeStatusLabel.setForeground(new Color(231, 76, 60));
      } else {
        bridgeStatusLabel.setForeground(new Color(150, 150, 150));
      }

      // Update gate status
      gateStatusLabel.setText("Gate: " + gateState);
      if (gateState.equals("OPEN")) {
        gateStatusLabel.setForeground(new Color(46, 204, 113));
      } else if (gateState.equals("CLOSED")) {
        gateStatusLabel.setForeground(new Color(231, 76, 60));
      } else {
        gateStatusLabel.setForeground(new Color(150, 150, 150));
      }

      // Update sequence state
      boolean wasDiagnostic = isDiagnosticMode;
      isDiagnosticMode = sequenceState.equals("DIAGNOSTIC");

      if (isLaptopSize) {
        sequenceStateLabel.setText("State: " + sequenceState);
      } else {
        sequenceStateLabel.setText("State: " + "\n" + sequenceState);
      }

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
      bridgeMovementLabel.setText("Bridge: " + bridgeMovementDistance + " cm");
      boatClearanceLabel.setText("Clearance: " + boatClearanceDistance + " cm");

      // Update manual lights status
      manualLightsLabel.setText("Manual Lights: " + manualBridgeLights);
      if (manualBridgeLights.equals("Yes")) {
        manualLightsLabel.setForeground(new Color(241, 196, 15));
      } else {
        manualLightsLabel.setForeground(new Color(180, 180, 180));
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
          queueStatusLabel.setForeground(new Color(180, 180, 180));
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
      if (Integer.parseInt(weight) > 3500 * 0.8) {
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
      automaticModeButton.setText("SWITCH TO AUTOMATIC MODE");
      overrideModeButton.setText("CURRENTLY IN OVERRIDE MODE");
      automaticModeButton.setBackground(new Color(101, 181, 109));
      overrideModeButton.setBackground(new Color(70, 70, 72));
    } else {
      automaticModeButton.setEnabled(false);
      overrideModeButton.setEnabled(true);
      automaticModeButton.setText("CURRENTLY IN AUTOMATIC MODE");
      overrideModeButton.setText("SWITCH TO OVERRIDE MODE");
      automaticModeButton.setBackground(new Color(70, 70, 72));
      overrideModeButton.setBackground(new Color(101, 181, 109));
    }
  }

  public void updateMessageLog(String message) {
    if (messageLogArea != null) {
      SwingUtilities.invokeLater(() -> {
        try {
          String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

          // Add line break after MANUAL_BRIDGE_LIGHTS value
          String processedMessage = message;
          int manualLightsIndex = message.indexOf("MANUAL_BRIDGE_LIGHTS:");
          if (manualLightsIndex != -1) {
            // Find the end of the MANUAL_BRIDGE_LIGHTS value (either YES or NO)
            int afterValue = -1;
            if (message.indexOf("MANUAL_BRIDGE_LIGHTS:YES", manualLightsIndex) != -1) {
              afterValue = manualLightsIndex + "MANUAL_BRIDGE_LIGHTS:YES".length();
            } else if (message.indexOf("MANUAL_BRIDGE_LIGHTS:NO", manualLightsIndex) != -1) {
              afterValue = manualLightsIndex + "MANUAL_BRIDGE_LIGHTS:NO".length();
            }

            // Insert line break after the value if there's more content
            if (afterValue != -1 && afterValue < message.length()) {
              // Check if next character is a pipe separator
              if (message.charAt(afterValue) == '|') {
                processedMessage = message.substring(0, afterValue) + "\n    " + message.substring(afterValue + 1);
              }
            }
          }

          String newMessage = timeStamp + " - " + processedMessage + "\n";

          StyledDocument doc = messageLogArea.getStyledDocument();

          // Change all existing text to light gray
          javax.swing.text.Style defaultStyle = messageLogArea.addStyle("Default", null);
          StyleConstants.setForeground(defaultStyle, new Color(180, 180, 180));
          doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, false);

          // Determine color based on message type
          javax.swing.text.Style messageStyle = messageLogArea.addStyle("MessageStyle", null);
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
    private double waveOffset = 0;
    private int lightPulse = 0;

    public BridgeAnimationPanel() {
      setBackground(new Color(18, 18, 18));

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

        // Continuously increment wave offset without resetting
        waveOffset += 1.0;
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
      int waterY = height / 2 + 140;

      // Draw sky gradient background
      java.awt.GradientPaint skyGradient = new java.awt.GradientPaint(
          0, 0, new Color(135, 206, 250), // Light sky blue at top
          0, waterY, new Color(176, 224, 255) // Lighter blue at horizon
      );
      g2d.setPaint(skyGradient);
      g2d.fillRect(0, 0, width, waterY);

      // Draw clouds
      drawClouds(g2d, width, waterY);

      // Draw top water layer (behind bridge)
      int topLayerColor = 0x133050;
      int totalLayers = 6;
      int layerHeight = (height - waterY) / totalLayers;

      // Draw just the first (top) layer behind the bridge
      g2d.setColor(new Color(topLayerColor));
      for (int wavePass = 0; wavePass < 3; wavePass++) {
        int yStart = waterY - 15;
        int yEnd = yStart + layerHeight + 10;

        java.awt.geom.Path2D.Double wavePath = new java.awt.geom.Path2D.Double();
        wavePath.moveTo(-10, yStart);

        double direction = 1.0;
        double waveSpeed = waveOffset * 0.5 * direction;

        for (int x = -10; x <= width + 10; x += 3) {
          double wave1 = Math.sin((x + waveSpeed * 2 + wavePass * 15) * 0.02) * 8;
          double wave2 = Math.sin((x - waveSpeed * 1.5 + wavePass * 10) * 0.03) * 5;
          double wave3 = Math.sin((x + waveSpeed + wavePass * 5) * 0.05) * 3;

          int yWave = yStart + (int) (wave1 + wave2 + wave3);
          wavePath.lineTo(x, yWave);
        }

        wavePath.lineTo(width + 10, yEnd + 5);
        wavePath.lineTo(-10, yEnd + 5);
        wavePath.closePath();

        g2d.fill(wavePath);
      }

      // Draw bridge and structures
      int liftOffset = (int) (-bridgeAngle * 2.2f);
      int bridgeWidth = 180;
      int towerWidth = 30;
      int towerHeight = 280;

      // Left tower
      g2d.setColor(new Color(50, 55, 60));
      g2d.fillRect(centerX - bridgeWidth - towerWidth, waterY - towerHeight, towerWidth, towerHeight);

      g2d.setColor(new Color(40, 45, 50));
      g2d.fillRect(centerX - bridgeWidth - towerWidth - 5, waterY - towerHeight - 10, towerWidth + 10, 10);

      g2d.setColor(new Color(25, 30, 35));
      for (int i = 0; i < 5; i++) {
        g2d.fillRect(centerX - bridgeWidth - towerWidth + 8, waterY - towerHeight + 30 + i * 45, 14, 25);
      }

      // Right tower
      g2d.setColor(new Color(50, 55, 60));
      g2d.fillRect(centerX + bridgeWidth, waterY - towerHeight, towerWidth, towerHeight);

      g2d.setColor(new Color(40, 45, 50));
      g2d.fillRect(centerX + bridgeWidth - 5, waterY - towerHeight - 10, towerWidth + 10, 10);

      g2d.setColor(new Color(25, 30, 35));
      for (int i = 0; i < 5; i++) {
        g2d.fillRect(centerX + bridgeWidth + 8, waterY - towerHeight + 30 + i * 45, 14, 25);
      }

      // Draw lift cables
      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(new Color(80, 85, 90));

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

      g2d.setColor(new Color(60, 65, 70));
      g2d.fillRect(centerX - bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2,
          bridgeWidth * 2, deckHeight);

      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(new Color(50, 55, 60));
      for (int i = centerX - bridgeWidth; i < centerX + bridgeWidth; i += 30) {
        g2d.drawLine(i, waterY - 85 + liftOffset - deckHeight / 2,
            i + 15, waterY - 85 + liftOffset + deckHeight / 2);
        g2d.drawLine(i + 15, waterY - 85 + liftOffset - deckHeight / 2,
            i + 30, waterY - 85 + liftOffset + deckHeight / 2);
      }

      g2d.setColor(new Color(35, 37, 40));
      g2d.fillRect(centerX - bridgeWidth, waterY - 85 + liftOffset - 4,
          bridgeWidth * 2, 8);

      g2d.setColor(new Color(180, 180, 180));
      for (int i = centerX - bridgeWidth; i < centerX + bridgeWidth; i += 40) {
        g2d.fillRect(i, waterY - 85 + liftOffset - 1, 20, 2);
      }

      g2d.setStroke(new BasicStroke(1));
      g2d.setColor(new Color(70, 75, 80));
      g2d.drawLine(centerX - bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2 - 3,
          centerX + bridgeWidth, waterY - 85 + liftOffset - deckHeight / 2 - 3);
      g2d.drawLine(centerX - bridgeWidth, waterY - 85 + liftOffset + deckHeight / 2 + 3,
          centerX + bridgeWidth, waterY - 85 + liftOffset + deckHeight / 2 + 3);

      // Draw bridge lights on top of deck
      drawBridgeLights(g2d, centerX, waterY, liftOffset, deckHeight);

      // Draw approach roads
      g2d.setColor(new Color(35, 37, 40));
      g2d.fillRect(-5, waterY - 89, centerX - bridgeWidth + 5, 8);
      g2d.fillRect(centerX + bridgeWidth, waterY - 89, width - (centerX + bridgeWidth), 8);

      // Draw support pillars
      g2d.setColor(new Color(45, 50, 55));
      int pillarHeight = 89 - 8;
      g2d.fillRect(-5, waterY - 81, centerX - bridgeWidth + 5, pillarHeight);
      g2d.fillRect(centerX + bridgeWidth, waterY - 81, width - (centerX + bridgeWidth), pillarHeight);

      // Road markings - only draw on the solid road sections
      g2d.setColor(new Color(180, 180, 180));

      // Left side - stop before the bridge/pillar edge
      int leftRoadEnd = centerX - bridgeWidth - 5; // Stop at pillar edge
      for (int i = 40; i < leftRoadEnd; i += 40) {
        // Only draw if the entire marking fits before the edge
        if (i + 20 <= leftRoadEnd) {
          g2d.fillRect(i, waterY - 86, 20, 2);
        }
      }

      // Right side - start after the bridge/pillar edge
      int rightRoadStart = centerX + bridgeWidth + 5; // Start after pillar edge
      for (int i = rightRoadStart + 40; i < width; i += 40) {
        g2d.fillRect(i, waterY - 86, 20, 2);
      }

      // Draw gates on both sides
      drawGate(g2d, centerX - 200, waterY, liftOffset, true);
      drawGate(g2d, centerX + 200, waterY, liftOffset, false);

      // Draw traffic lights
      drawTrafficLights(g2d, centerX, waterY);

      // Draw remaining water layers (in front of bridge)
      int[] bottomLayerColors = {
          0x183d67,
          0x1e4b7e,
          0x235995,
          0x2966ac,
          0x2e74c3
      };

      for (int layer = 1; layer < totalLayers; layer++) {
        int colorValue = bottomLayerColors[layer - 1];
        g2d.setColor(new Color(colorValue));

        for (int wavePass = 0; wavePass < 3; wavePass++) {
          int yStart = waterY + (layer * layerHeight) - 15;
          int yEnd = yStart + layerHeight + 10;

          java.awt.geom.Path2D.Double wavePath = new java.awt.geom.Path2D.Double();
          wavePath.moveTo(-10, yStart);

          double direction = (layer % 2 == 0) ? 1.0 : -1.0;
          double waveSpeed = waveOffset * 0.5 * direction;

          for (int x = -10; x <= width + 10; x += 3) {
            double wave1 = Math.sin((x + waveSpeed * 2 + layer * 30 + wavePass * 15) * 0.02) * 8;
            double wave2 = Math.sin((x - waveSpeed * 1.5 + layer * 20 + wavePass * 10) * 0.03) * 5;
            double wave3 = Math.sin((x + waveSpeed + layer * 10 + wavePass * 5) * 0.05) * 3;

            int yWave = yStart + (int) (wave1 + wave2 + wave3);
            wavePath.lineTo(x, yWave);
          }

          wavePath.lineTo(width + 10, yEnd + 5);
          wavePath.lineTo(-10, yEnd + 5);
          wavePath.closePath();

          g2d.fill(wavePath);
        }
      }
    }

    private void drawClouds(Graphics2D g2d, int width, int waterY) {
      g2d.setColor(new Color(255, 255, 255, 180));

      // Cloud positions (static for now, can animate later)
      // Calculate cloud positions based on waveOffset for gentle movement
      int cloudOffset = (int) (waveOffset * 0.2) % width;

      // Draw multiple clouds at different positions
      drawCloud(g2d, 100 + cloudOffset - width, 80, 1.0f);
      drawCloud(g2d, 350 + cloudOffset - width, 120, 0.8f);
      drawCloud(g2d, 600 + cloudOffset - width, 60, 1.2f);
      drawCloud(g2d, 850 + cloudOffset - width, 140, 0.9f);

      drawCloud(g2d, 100 + cloudOffset, 80, 1.0f);
      drawCloud(g2d, 350 + cloudOffset, 120, 0.8f);
      drawCloud(g2d, 600 + cloudOffset, 60, 1.2f);
      drawCloud(g2d, 850 + cloudOffset, 140, 0.9f);

      drawCloud(g2d, 100 + cloudOffset + width, 80, 1.0f);
      drawCloud(g2d, 350 + cloudOffset + width, 120, 0.8f);
      drawCloud(g2d, 600 + cloudOffset + width, 60, 1.2f);
      drawCloud(g2d, 850 + cloudOffset + width, 140, 0.9f);
    }

    private void drawCloud(Graphics2D g2d, int x, int y, float scale) {
      // Draw cloud using multiple overlapping circles
      int baseSize = (int) (40 * scale);

      g2d.setColor(new Color(255, 255, 255, 160));

      // Main cloud body (multiple circles)
      g2d.fillOval(x, y, baseSize, baseSize);
      g2d.fillOval(x + baseSize / 3, y - baseSize / 4, baseSize, baseSize);
      g2d.fillOval(x + baseSize * 2 / 3, y, baseSize, baseSize);
      g2d.fillOval(x + baseSize, y + baseSize / 6, baseSize, baseSize);

      // Add softer outer layer
      g2d.setColor(new Color(255, 255, 255, 100));
      g2d.fillOval(x - baseSize / 4, y + baseSize / 4, baseSize, baseSize);
      g2d.fillOval(x + baseSize * 5 / 4, y + baseSize / 4, baseSize, baseSize);
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
          g2d.setColor(new Color(40, 40, 40));
          g2d.fillOval(lightX - 5, lightY - 5, 10, 10);
        }

        g2d.setColor(new Color(30, 30, 30));
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
      drawTrafficLightPole(g2d, centerX - 285, waterY - 81, roadLight, true);
      drawTrafficLightPole(g2d, centerX + 285, waterY - 81, boatLight, false);
    }

    private void drawTrafficLightPole(Graphics2D g2d, int x, int y, String activeLight, boolean isRoadLight) {
      g2d.setColor(new Color(40, 45, 50));
      g2d.fillRect(x - 4, y - 120, 8, 120);

      g2d.setColor(new Color(30, 35, 40));
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
      g2d.setColor(active ? color : new Color(40, 40, 40));
      g2d.fillOval(x - 8, y - 8, 16, 16);

      g2d.setColor(new Color(20, 20, 20));
      g2d.setStroke(new BasicStroke(1));
      g2d.drawOval(x - 8, y - 8, 16, 16);
    }
  }
}
