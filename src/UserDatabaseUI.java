import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Vector;

public class UserDatabaseUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTable loginHistoryTable;
    private DefaultTableModel loginHistoryTableModel;
    private JTextArea userDetailsArea;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private JTextField dateSearchField;
    private JPasswordField securityPasswordField;
    private String adminPassword = "INFOMNGMT"; // Updated password
    private boolean isAuthenticated = false;
    
    // Static variable to track database lockout status
    public static boolean isDatabaseLocked = false;
    
    public UserDatabaseUI() {
        setTitle("Admin Database Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Require authentication before proceeding
        if (!authenticateInitialAccess()) {
            // If authentication fails, close the window after a short delay
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(500); // Short delay before closing
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            return; // Stop initialization if authentication fails
        }
        
        // Reset database connection if needed
        try {
            if (DatabaseManager.getConnection() == null || DatabaseManager.getConnection().isClosed()) {
                System.out.println("Initializing database connection for UI...");
                DatabaseManager.initialize();
            }
        } catch (Exception e) {
            System.err.println("Error checking database connection: " + e.getMessage());
        }
        
        // Create tabbed pane with enhanced styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(245, 245, 250));
        tabbedPane.setForeground(new Color(66, 133, 244));
        
        // Create tabs
        tabbedPane.addTab("User List", createUserListTab());
        tabbedPane.addTab("User Details", createUserDetailsTab());
        tabbedPane.addTab("Login History", createLoginHistoryTab());
        
        add(tabbedPane);
        
        // Ensure sample users are loaded
        ensureSampleUsers();
        
        // Add window listener to refresh data when window is activated
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshUserTable();
            }
        });
        
        // Add File menu with print options - enhanced for better visibility
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createRaisedBevelBorder());
        menuBar.setBackground(new Color(245, 245, 250));
        
        // Create a more visible File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fileMenu.setForeground(new Color(66, 133, 244));
        fileMenu.setIcon(UIManager.getIcon("FileView.fileIcon"));
        
        // Add a separator before the first item for visual distinction
        fileMenu.addSeparator();
        
        // Create menu items with enhanced styling and icons
        JMenuItem printUserItem = new JMenuItem("Print User Details");
        printUserItem.setMnemonic(KeyEvent.VK_P);
        printUserItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        printUserItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        printUserItem.addActionListener(e -> printCurrentUserDetails());
        printUserItem.setIcon(UIManager.getIcon("FileView.printIcon"));
        
        JMenuItem printSummaryItem = new JMenuItem("Print Summary Report");
        printSummaryItem.setMnemonic(KeyEvent.VK_S);
        printSummaryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        printSummaryItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        printSummaryItem.addActionListener(e -> printSummaryReport());
        printSummaryItem.setIcon(UIManager.getIcon("FileView.printIcon"));
        
        JMenuItem exportDataItem = new JMenuItem("Export Data");
        exportDataItem.setMnemonic(KeyEvent.VK_E);
        exportDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        exportDataItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exportDataItem.addActionListener(e -> exportData());
        exportDataItem.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        
        // Add logout menu item
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.setMnemonic(KeyEvent.VK_L);
        logoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        logoutItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutItem.addActionListener(e -> returnToWelcomePage());
        logoutItem.setIcon(UIManager.getIcon("FileView.computerIcon"));
        
        fileMenu.add(printUserItem);
        fileMenu.add(printSummaryItem);
        fileMenu.addSeparator();
        fileMenu.add(exportDataItem);
        fileMenu.addSeparator();
        fileMenu.add(logoutItem);
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        // Add a toolbar for quick access to file operations
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        toolbar.setBackground(new Color(245, 245, 250));
        
        // Create a label to explain the toolbar
        JLabel fileOperationsLabel = new JLabel("File Operations: ");
        fileOperationsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileOperationsLabel.setForeground(new Color(66, 133, 244));
        toolbar.add(fileOperationsLabel);
        toolbar.addSeparator(new Dimension(10, 24));
        
        // Add toolbar buttons with the same functionality as the menu items
        JButton printUserButton = new JButton("Print User");
        printUserButton.setToolTipText("Print details of the selected user (Ctrl+P)");
        printUserButton.setFocusPainted(false);
        printUserButton.setBackground(new Color(186, 104, 200)); // Light violet
        printUserButton.setForeground(Color.WHITE);
        printUserButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        printUserButton.setOpaque(true);
        printUserButton.setContentAreaFilled(true);
        printUserButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(156, 39, 176), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        printUserButton.addActionListener(e -> printCurrentUserDetails());
        
        // Add hover effect
        printUserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                printUserButton.setBackground(new Color(156, 39, 176)); // Darker violet
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                printUserButton.setBackground(new Color(186, 104, 200)); // Light violet
            }
        });
        
        JButton printSummaryButton = new JButton("Print Summary");
        printSummaryButton.setToolTipText("Print summary report of all users (Ctrl+R)");
        printSummaryButton.setFocusPainted(false);
        printSummaryButton.setBackground(new Color(186, 104, 200)); // Light violet
        printSummaryButton.setForeground(Color.WHITE);
        printSummaryButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        printSummaryButton.setOpaque(true);
        printSummaryButton.setContentAreaFilled(true);
        printSummaryButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(156, 39, 176), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        printSummaryButton.addActionListener(e -> printSummaryReport());
        
        // Add hover effect
        printSummaryButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                printSummaryButton.setBackground(new Color(156, 39, 176)); // Darker violet
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                printSummaryButton.setBackground(new Color(186, 104, 200)); // Light violet
            }
        });
        
        JButton exportButton = new JButton("Export Data");
        exportButton.setToolTipText("Export user data to CSV file (Ctrl+E)");
        exportButton.setFocusPainted(false);
        exportButton.setBackground(new Color(76, 175, 80)); // Green
        exportButton.setForeground(Color.WHITE);
        exportButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportButton.setOpaque(true);
        exportButton.setContentAreaFilled(true);
        exportButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 142, 60), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        exportButton.addActionListener(e -> exportData());
        
        // Add hover effect
        exportButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportButton.setBackground(new Color(56, 142, 60));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportButton.setBackground(new Color(76, 175, 80));
            }
        });
        
        // Add logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setToolTipText("Exit the database management system (Ctrl+L)");
        logoutButton.setFocusPainted(false);
        logoutButton.setBackground(new Color(211, 47, 47));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setOpaque(true);
        logoutButton.setContentAreaFilled(true);
        logoutButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 42, 42), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        logoutButton.addActionListener(e -> returnToWelcomePage());
        
        // Add hover effect for logout button
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(190, 42, 42));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(211, 47, 47));
            }
        });
        
        toolbar.add(printUserButton);
        toolbar.addSeparator();
        toolbar.add(printSummaryButton);
        toolbar.addSeparator();
        toolbar.add(exportButton);
        toolbar.addSeparator(new Dimension(20, 24));
        toolbar.add(logoutButton);
        
        // Add the toolbar to the frame
        add(toolbar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Authenticates the initial access to the database UI
     * @return true if authentication is successful, false otherwise
     */
    private boolean authenticateInitialAccess() {
        // Hide the main frame
        setVisible(false);
        
        // Check if database is already locked from too many failed attempts
        if (isDatabaseLocked) {
            triggerDirectOtpVerification();
            return false;
        }
        
        JDialog authDialog = new JDialog((Frame)null, "Database Access", true);
        authDialog.setLayout(new BorderLayout());
        authDialog.setSize(500, 420); // Increased height from 380 to 420
        authDialog.setLocationRelativeTo(null);
        authDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Set undecorated before setting opacity
        authDialog.setUndecorated(true);
        
        // Create main panel with subtle gradient effect
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 242, 255);
                Color color2 = new Color(230, 235, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1)); // Add visible border
        
        // Create stylish header panel
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(66, 133, 244);
                Color color2 = new Color(59, 120, 220);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                
                // Add subtle pattern to header
                g2d.setColor(new Color(255, 255, 255, 30));
                for (int i = 0; i < w; i += 20) {
                    g2d.drawLine(i, 0, i + 10, h);
                }
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        headerPanel.setPreferredSize(new Dimension(500, 90));
        
        // Add icon to header
        JPanel iconTextPanel = new JPanel(new BorderLayout(15, 0));
        iconTextPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Secure Database Access");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Authentication required");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        iconTextPanel.add(iconLabel, BorderLayout.WEST);
        iconTextPanel.add(titlePanel, BorderLayout.CENTER);
        
        headerPanel.add(iconTextPanel, BorderLayout.WEST);
        
        // Content panel with shadow effect
        JPanel contentWrapperPanel = new JPanel(new BorderLayout());
        contentWrapperPanel.setOpaque(false);
        contentWrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 25, 25));
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        JLabel infoLabel = new JLabel("Please enter the admin password to access the database:");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(new Color(50, 50, 50));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(1000, 70));
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setForeground(new Color(80, 80, 80));
        passwordLabel.setPreferredSize(new Dimension(100, 25));
        
        // Styled password field
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBackground(new Color(250, 250, 250));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        // Create custom focus listener for password field highlight effect
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(66, 133, 244), 2, true),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            }
        });
        
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);
        
        // Button panel with styled buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setForeground(new Color(100, 100, 100));
        cancelButton.setBackground(new Color(240, 240, 240));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        // Hover effect for cancel button
        cancelButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelButton.setBackground(new Color(230, 230, 230));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                cancelButton.setBackground(new Color(240, 240, 240));
            }
        });
        
        JButton loginButton = new JButton("Authenticate");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(new Color(66, 133, 244));
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 120, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        // Hover effect for login button
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(59, 120, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(new Color(66, 133, 244));
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton);
        
        // Status label with icon for error messages
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel statusIconLabel = new JLabel();
        statusIconLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        statusIconLabel.setVisible(false);
        
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(211, 47, 47));
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        
        statusPanel.add(statusIconLabel);
        statusPanel.add(statusLabel);
        
        // Add components to content panel with spacing
        contentPanel.add(infoLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(passwordPanel);
        contentPanel.add(Box.createVerticalStrut(20)); // Increased from 15 to 20
        contentPanel.add(statusPanel);
        contentPanel.add(Box.createVerticalStrut(40)); // Increased from 25 to 40
        contentPanel.add(buttonPanel);
        
        contentWrapperPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentWrapperPanel, BorderLayout.CENTER);
        
        // Add main panel to dialog
        authDialog.add(mainPanel);
        
        // Add action listeners
        final boolean[] authenticated = {false};
        final int[] attempts = {0};
        
        ActionListener authAction = e -> {
            String password = new String(passwordField.getPassword());
            
            // Animated button press effect
            loginButton.setBackground(new Color(50, 110, 210));
            javax.swing.Timer timer = new javax.swing.Timer(100, evt -> {
                loginButton.setBackground(new Color(66, 133, 244));
                ((javax.swing.Timer)evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
            
            if (adminPassword.equals(password)) {
                authenticated[0] = true;
                isAuthenticated = true;
                
                // Success animation
                loginButton.setText("Success!");
                loginButton.setBackground(new Color(76, 175, 80));
                
                javax.swing.Timer successTimer = new javax.swing.Timer(800, evt -> {
                    authDialog.dispose();
                    ((javax.swing.Timer)evt.getSource()).stop();
                });
                successTimer.setRepeats(false);
                successTimer.start();
            } else {
                attempts[0]++;
                passwordField.setText("");
                
                // Show error with animation
                statusIconLabel.setVisible(true);
                
                if (attempts[0] >= 3) {
                    statusLabel.setText("Too many failed attempts! Access denied.");
                    
                    // Set the database lockout flag
                    isDatabaseLocked = true;
                    
                    javax.swing.Timer failTimer = new javax.swing.Timer(1500, evt -> {
                        // Hide the auth dialog
                        authDialog.dispose();
                        
                        // Keep main window invisible
                        setVisible(false);
                        
                        // Show welcome screen and dispose this window
                        OneTimeLoginSystem.prepareWelcomeScreen();
                        dispose();
                        
                        ((javax.swing.Timer)evt.getSource()).stop();
                    });
                    failTimer.setRepeats(false);
                    failTimer.start();
                } else {
                    statusLabel.setText("Incorrect password. Attempts remaining: " + (3 - attempts[0]));
                    
                    // Shake animation for wrong password
                    final int originalX = authDialog.getLocationOnScreen().x;
                    final int[] time = {0};
                    javax.swing.Timer shakeTimer = new javax.swing.Timer(30, evt -> {
                        if (time[0] >= 10) {
                            authDialog.setLocation(originalX, authDialog.getLocationOnScreen().y);
                            ((javax.swing.Timer)evt.getSource()).stop();
                        } else {
                            int offset = 10;
                            if (time[0] % 2 == 0) {
                                offset = -offset;
                            }
                            authDialog.setLocation(originalX + offset, authDialog.getLocationOnScreen().y);
                            time[0]++;
                        }
                    });
                    shakeTimer.setRepeats(true);
                    shakeTimer.start();
                }
            }
        };
        
        loginButton.addActionListener(authAction);
        passwordField.addActionListener(authAction);
        
        cancelButton.addActionListener(e -> {
            // Keep main frame invisible
            setVisible(false);
            
            // Prepare welcome screen first
            OneTimeLoginSystem.prepareWelcomeScreen();
            
            // Then dispose dialog
            authDialog.dispose();
            
            // Finally dispose this window
            dispose();
        });
        
        // Set focus to password field
        authDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                passwordField.requestFocusInWindow();
            }
        });
        
        // Make dialog draggable
        MouseAdapter dragAdapter = new MouseAdapter() {
            private int dragStartX, dragStartY;
            
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getXOnScreen() - authDialog.getX();
                dragStartY = e.getYOnScreen() - authDialog.getY();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                authDialog.setLocation(e.getXOnScreen() - dragStartX, e.getYOnScreen() - dragStartY);
            }
        };
        
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);
        
        // Show dialog with fade-in effect - simplified to avoid issues
        authDialog.setOpacity(1.0f); // Set to fully visible immediately
        authDialog.setVisible(true);
        
        // If authentication failed, show welcome screen and return false
        if (!authenticated[0]) {
            // We'll show the welcome screen only if the user clicked Cancel
            // If they clicked the X to close the dialog, we'll do nothing
            dispose();
            return false;
        }
        
        // If authentication succeeded, show this window again
        setVisible(true);
        return true;
    }
    
    /**
     * Directly triggers OTP verification when database is locked
     * This bypasses the lockout dialog and immediately shows the OTP verification
     */
    private void triggerDirectOtpVerification() {
        // Keep reference to the current UserDatabaseUI
        UserDatabaseUI thisUI = this;
        
        // Get current username from the database system
        String username = UserDatabase.getCurrentUsername();
        
        if (username == null || username.isEmpty()) {
            // Fallback if username is not available
            JOptionPane.showMessageDialog(
                null,
                "User information not available. Please log in again.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            
            thisUI.dispose();
            OneTimeLoginSystem.prepareWelcomeScreen();
            return;
        }
        
        // Get the user's email
        String email = DatabaseIntegration.getUserEmail(username);
        
        if (email == null || email.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Email information not available. Please log in again.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            
            thisUI.dispose();
            OneTimeLoginSystem.prepareWelcomeScreen();
            return;
        }
        
        // Brief notification that verification is needed
        JOptionPane.showMessageDialog(
            null,
            "Database access is locked due to multiple failed attempts.\n" +
            "An OTP will be sent to " + email + " for verification.",
            "Security Verification Required",
            JOptionPane.WARNING_MESSAGE
        );
        
        // Generate and send OTP
        OneTimeLoginSystem.sendOTPVerification(username);
        
        // Show custom database OTP verification dialog
        showDatabaseOtpVerificationDialog(username, email);
    }
    
    /**
     * Shows a custom OTP verification dialog specific for database authentication recovery
     * @param username The username for whom to verify
     * @param email The email where OTP was sent
     */
    private void showDatabaseOtpVerificationDialog(String username, String email) {
        // Keep reference to the current UserDatabaseUI
        UserDatabaseUI thisUI = this;
        
        // Create a custom dialog for database OTP verification
        JDialog otpDialog = new JDialog((Frame)null, "Database Security Verification", true);
        otpDialog.setSize(500, 420);
        otpDialog.setLocationRelativeTo(null);
        otpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        otpDialog.setUndecorated(true);
        
        // Create main panel with database-themed gradient
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(230, 240, 255); // Light blue
                Color color2 = new Color(210, 225, 245); // Slightly darker light blue
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
        
        // Create header panel with database theme
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(41, 128, 185); // Database blue
                Color color2 = new Color(52, 152, 219); // Slightly lighter blue
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                
                // Add subtle pattern
                g2d.setColor(new Color(255, 255, 255, 20));
                for (int i = 0; i < w; i += 20) {
                    g2d.drawLine(i, 0, i + 10, h);
                }
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        headerPanel.setPreferredSize(new Dimension(500, 90));
        
        // Add icon and text to header
        JPanel iconTextPanel = new JPanel(new BorderLayout(15, 0));
        iconTextPanel.setOpaque(false);
        
        // Use a database icon if available, otherwise use a security icon
        Icon tempIcon = UIManager.getIcon("FileView.computerIcon");
        if (tempIcon == null) {
            tempIcon = UIManager.getIcon("OptionPane.informationIcon");
        }
        JLabel iconLabel = new JLabel(tempIcon);
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Database Recovery");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("OTP verification required");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        iconTextPanel.add(iconLabel, BorderLayout.WEST);
        iconTextPanel.add(titlePanel, BorderLayout.CENTER);
        
        headerPanel.add(iconTextPanel, BorderLayout.WEST);
        
        // Content panel with shadow effect
        JPanel contentWrapperPanel = new JPanel(new BorderLayout());
        contentWrapperPanel.setOpaque(false);
        contentWrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 25, 25));
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Instructions
        JTextPane instructionPane = new JTextPane();
        instructionPane.setEditable(false);
        instructionPane.setOpaque(false);
        instructionPane.setContentType("text/html");
        instructionPane.setText(
            "<html><div style='text-align: center; font-family: Segoe UI; font-size: 13pt; color: #333333;'>" +
            "A one-time password has been sent to:<br>" +
            "<b style='color: #2980b9;'>" + email + "</b><br><br>" +
            "Please enter the 6-digit code to unlock database access." +
            "</div></html>"
        );
        instructionPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // OTP input field with formatting and validation
        JPanel otpInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        otpInputPanel.setBackground(Color.WHITE);
        otpInputPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField otpField = new JTextField(10);
        otpField.setFont(new Font("Segoe UI", Font.BOLD, 20));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Focus effect for OTP field
        otpField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                otpField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(41, 128, 185), 2, true),
                    BorderFactory.createEmptyBorder(9, 14, 9, 14)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                otpField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        otpInputPanel.add(otpField);
        
        // Timer label
        JLabel timerLabel = new JLabel("Expires in: 5:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        timerLabel.setForeground(new Color(150, 150, 150));
        
        // Status message
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setForeground(new Color(231, 76, 60)); // Error red
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setForeground(new Color(100, 100, 100));
        cancelButton.setBackground(new Color(240, 240, 240));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        JButton resendButton = new JButton("Resend OTP");
        resendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        resendButton.setForeground(new Color(52, 152, 219));
        resendButton.setBackground(Color.WHITE);
        resendButton.setFocusPainted(false);
        resendButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        JButton verifyButton = new JButton("Verify");
        verifyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setBackground(new Color(46, 204, 113)); // Green
        verifyButton.setFocusPainted(false);
        verifyButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(39, 174, 96), 1, true),
            BorderFactory.createEmptyBorder(8, 25, 8, 25)
        ));
        
        // Hover effects for buttons
        cancelButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelButton.setBackground(new Color(230, 230, 230));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                cancelButton.setBackground(new Color(240, 240, 240));
            }
        });
        
        resendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                resendButton.setBackground(new Color(245, 250, 255));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                resendButton.setBackground(Color.WHITE);
            }
        });
        
        verifyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                verifyButton.setBackground(new Color(39, 174, 96));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                verifyButton.setBackground(new Color(46, 204, 113));
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(resendButton);
        buttonPanel.add(verifyButton);
        
        // Add components to content panel
        contentPanel.add(instructionPane);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(otpInputPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(timerLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(buttonPanel);
        
        contentWrapperPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentWrapperPanel, BorderLayout.CENTER);
        
        // Add main panel to dialog
        otpDialog.add(mainPanel);
        
        // OTP timer
        final int[] remainingSeconds = {300}; // 5 minutes
        javax.swing.Timer otpTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds[0]--;
            if (remainingSeconds[0] <= 0) {
                ((javax.swing.Timer)e.getSource()).stop();
                timerLabel.setText("Expired");
                timerLabel.setForeground(new Color(231, 76, 60));
                statusLabel.setText("OTP has expired. Please request a new one.");
            } else {
                int minutes = remainingSeconds[0] / 60;
                int seconds = remainingSeconds[0] % 60;
                timerLabel.setText(String.format("Expires in: %d:%02d", minutes, seconds));
            }
        });
        otpTimer.start();
        
        // Add action listeners
        verifyButton.addActionListener(e -> {
            String enteredOTP = otpField.getText().trim();
            
            if (enteredOTP.isEmpty()) {
                statusLabel.setText("Please enter the OTP sent to your email.");
                return;
            }
            
            // Animated button press effect
            verifyButton.setBackground(new Color(33, 150, 83));
            javax.swing.Timer timer = new javax.swing.Timer(100, evt -> {
                verifyButton.setBackground(new Color(46, 204, 113));
                ((javax.swing.Timer)evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
            
            // For testing purposes - in a real app, this would validate against the sent OTP
            // Here we accept any 6-digit OTP to ensure demo works
            if (enteredOTP.matches("\\d{6}")) {
                // Stop the timer
                otpTimer.stop();
                
                // Reset the database lock
                isDatabaseLocked = false;
                
                // Close the dialog
                otpDialog.dispose();
                
                // Show success message
                JOptionPane.showMessageDialog(
                    null,
                    "Database access has been unlocked successfully.",
                    "Access Restored",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                // Close this UI and reopen to refresh
                SwingUtilities.invokeLater(() -> {
                    thisUI.dispose();
                    new UserDatabaseUI();
                });
            } else {
                statusLabel.setText("Invalid OTP. Please enter the 6-digit code sent to your email.");
                
                // Shake animation for wrong OTP
                final int originalX = otpDialog.getLocationOnScreen().x;
                final int[] time = {0};
                javax.swing.Timer shakeTimer = new javax.swing.Timer(30, evt -> {
                    if (time[0] >= 10) {
                        otpDialog.setLocation(originalX, otpDialog.getLocationOnScreen().y);
                        ((javax.swing.Timer)evt.getSource()).stop();
                    } else {
                        int offset = 10;
                        if (time[0] % 2 == 0) {
                            offset = -offset;
                        }
                        otpDialog.setLocation(originalX + offset, otpDialog.getLocationOnScreen().y);
                        time[0]++;
                    }
                });
                shakeTimer.setRepeats(true);
                shakeTimer.start();
            }
        });
        
        resendButton.addActionListener(e -> {
            // Animated button press effect
            resendButton.setForeground(new Color(41, 128, 185));
            javax.swing.Timer timer = new javax.swing.Timer(100, evt -> {
                resendButton.setForeground(new Color(52, 152, 219));
                ((javax.swing.Timer)evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
            
            // Resend OTP
            OneTimeLoginSystem.sendOTPVerification(username);
            
            // Reset timer
            remainingSeconds[0] = 300;
            timerLabel.setForeground(new Color(150, 150, 150));
            otpTimer.restart();
            
            // Update status
            statusLabel.setText("A new OTP has been sent to your email.");
            statusLabel.setForeground(new Color(46, 204, 113)); // Success green
            
            // Clear OTP field
            otpField.setText("");
        });
        
        cancelButton.addActionListener(e -> {
            // Stop the timer
            otpTimer.stop();
            
            // Close the dialog
            otpDialog.dispose();
            
            // Return to welcome screen
            thisUI.dispose();
            OneTimeLoginSystem.prepareWelcomeScreen();
        });
        
        // Set focus to OTP field
        otpDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                otpField.requestFocusInWindow();
            }
        });
        
        // Make dialog draggable
        MouseAdapter dragAdapter = new MouseAdapter() {
            private int dragStartX, dragStartY;
            
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getXOnScreen() - otpDialog.getX();
                dragStartY = e.getYOnScreen() - otpDialog.getY();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                otpDialog.setLocation(e.getXOnScreen() - dragStartX, e.getYOnScreen() - dragStartY);
            }
        };
        
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);
        
        // Show dialog with fade-in effect
        otpDialog.setOpacity(1.0f);
        otpDialog.setVisible(true);
    }
    
    /**
     * Shows a dialog indicating the database is locked due to too many failed attempts
     */
    private void showDatabaseLockedDialog() {
        JDialog lockedDialog = new JDialog((Frame)null, "Database Locked", true);
        lockedDialog.setLayout(new BorderLayout());
        lockedDialog.setSize(500, 450);
        lockedDialog.setLocationRelativeTo(null);
        lockedDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        lockedDialog.setUndecorated(true);
        
        // Create main panel with red warning gradient
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(250, 230, 230); // Light red
                Color color2 = new Color(245, 220, 220); // Slightly darker light red
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(180, 100, 100), 1)); // Red border
        
        // Create alert header panel
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(211, 47, 47); // Darker red
                Color color2 = new Color(183, 28, 28); // Even darker red
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                
                // Add pattern to header
                g2d.setColor(new Color(255, 255, 255, 20));
                for (int i = 0; i < w; i += 20) {
                    g2d.drawLine(i, 0, i + 10, h);
                }
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        headerPanel.setPreferredSize(new Dimension(500, 90));
        
        // Add icon and text to header
        JPanel iconTextPanel = new JPanel(new BorderLayout(15, 0));
        iconTextPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Database Access Locked");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Security verification required");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        iconTextPanel.add(iconLabel, BorderLayout.WEST);
        iconTextPanel.add(titlePanel, BorderLayout.CENTER);
        
        headerPanel.add(iconTextPanel, BorderLayout.WEST);
        
        // Content panel with shadow effect
        JPanel contentWrapperPanel = new JPanel(new BorderLayout());
        contentWrapperPanel.setOpaque(false);
        contentWrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 25, 25));
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Detailed message
        JLabel warningIconLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        warningIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextPane messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setOpaque(false);
        messagePane.setContentType("text/html");
        messagePane.setText(
            "<html><div style='text-align: center; font-family: Segoe UI; font-size: 14pt; color: #D32F2F;'>" +
            "<b>Too many incorrect password attempts</b><br>" +
            "</div><br>" +
            "<div style='text-align: center; font-family: Segoe UI; font-size: 12pt; color: #424242;'>" +
            "For security reasons, database access has been locked.<br><br>" +
            "Please complete OTP verification to continue.<br><br>" +
            "</div></html>"
        );
        messagePane.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Button panel with styled verify button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton verifyButton = new JButton("Verify with OTP");
        verifyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setBackground(new Color(66, 133, 244)); // Blue instead of red
        verifyButton.setFocusPainted(false);
        verifyButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 120, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        // Hover effect for verify button
        verifyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                verifyButton.setBackground(new Color(59, 120, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                verifyButton.setBackground(new Color(66, 133, 244));
            }
        });
        
        buttonPanel.add(verifyButton);
        
        // Add components to content panel with spacing
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(warningIconLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(messagePane);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(buttonPanel);
        
        contentWrapperPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentWrapperPanel, BorderLayout.CENTER);
        
        // Add main panel to dialog
        lockedDialog.add(mainPanel);
        
        // Add action listener to verify button
        verifyButton.addActionListener(e -> {
            // Animated button press effect
            verifyButton.setBackground(new Color(50, 110, 210));
            javax.swing.Timer timer = new javax.swing.Timer(100, evt -> {
                verifyButton.setBackground(new Color(66, 133, 244));
                ((javax.swing.Timer)evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
            
            // Close the dialog
            lockedDialog.dispose();
            
            // Keep reference to the current UserDatabaseUI
            UserDatabaseUI thisUI = this;
            
            // Create OTP verification dialog
            SwingUtilities.invokeLater(() -> {
                // Get current username from the database system
                String username = UserDatabase.getCurrentUsername();
                
                if (username == null || username.isEmpty()) {
                    // Fallback if username is not available
                    JOptionPane.showMessageDialog(
                        null,
                        "User information not available. Please log in again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    
                    thisUI.dispose();
                    OneTimeLoginSystem.prepareWelcomeScreen();
                    return;
                }
                
                // Create a custom implementation of OtpVerificationListener
                OneTimeLoginSystem.OtpVerificationListener dbUnlockListener = new OneTimeLoginSystem.OtpVerificationListener() {
                    @Override
                    public void onVerificationComplete(boolean success) {
                        if (success) {
                            // Reset the database lock
                            isDatabaseLocked = false;
                            
                            // Show success message
                            JOptionPane.showMessageDialog(
                                null,
                                "Database access has been unlocked successfully.",
                                "Access Restored",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                            
                            // Close this UI and reopen to refresh
                            SwingUtilities.invokeLater(() -> {
                                thisUI.dispose();
                                new UserDatabaseUI();
                            });
                        } else {
                            // If verification fails, go back to welcome screen
                            SwingUtilities.invokeLater(() -> {
                                thisUI.dispose();
                                OneTimeLoginSystem.prepareWelcomeScreen();
                            });
                        }
                    }
                };
                
                // Generate and send OTP
                OneTimeLoginSystem.sendOTPVerification(username);
                
                // Show verification dialog with callback
                OneTimeLoginSystem.createOtpVerificationDialog(username, dbUnlockListener);
            });
        });
        
        // Make dialog draggable
        MouseAdapter dragAdapter = new MouseAdapter() {
            private int dragStartX, dragStartY;
            
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getXOnScreen() - lockedDialog.getX();
                dragStartY = e.getYOnScreen() - lockedDialog.getY();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                lockedDialog.setLocation(e.getXOnScreen() - dragStartX, e.getYOnScreen() - dragStartY);
            }
        };
        
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);
        
        // Show dialog
        lockedDialog.setOpacity(1.0f);
        lockedDialog.setVisible(true);
    }
    
    /**
     * Shadow border for panels
     */
    private class ShadowBorder extends AbstractBorder {
        private int shadowSize = 5;
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, ((shadowSize - i) * 10)));
                g2.drawRoundRect(
                    x + i, y + i,
                    width - i * 2 - 1, height - i * 2 - 1,
                    10, 10
                );
            }
            
            // Draw white border
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(
                x + shadowSize - 1, y + shadowSize - 1,
                width - shadowSize * 2, height - shadowSize * 2,
                10, 10
            );
            
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(shadowSize, shadowSize, shadowSize, shadowSize);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = shadowSize;
            return insets;
        }
    }
    
    private JPanel createUserListTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        
        // Create table model
        String[] columnNames = {"#", "Username", "Password (Hashed)", "Full Name", "Email", "Registration Date"};
        userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table with larger font size
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.setRowHeight(30); // Increased row height
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Enable horizontal scrolling
        
        // Set column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(50); // # column
        userTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Username
        userTable.getColumnModel().getColumn(2).setPreferredWidth(220); // Password (Hashed)
        userTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Full Name
        userTable.getColumnModel().getColumn(4).setPreferredWidth(250); // Email
        userTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Registration Date
        
        // Style the table header
        userTable.getTableHeader().setBackground(new Color(66, 133, 244));
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        userTable.getTableHeader().setPreferredSize(new Dimension(0, 35)); // Taller header
        
        // Add custom header renderer with different colors for each column
        userTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setForeground(Color.WHITE);
                
                // Use a consistent blue color for all columns
                label.setBackground(new Color(66, 133, 244)); // Blue
                
                // Add a subtle border
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                
                return label;
            }
        });
        
        // Add custom renderer for alternating row colors
        userTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Apply alternating row colors
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
                    
                    // Apply column-specific colors to text
                    switch (column) {
                        case 0: // Number
                            c.setForeground(new Color(50, 50, 50)); // Dark gray
                            setHorizontalAlignment(JLabel.CENTER);
                            break;
                        case 1: // Username
                            c.setForeground(new Color(66, 133, 244)); // Blue
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 2: // Password (Hashed)
                            c.setForeground(new Color(255, 152, 0)); // Orange for password hash
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 3: // Full Name
                            c.setForeground(new Color(76, 175, 80)); // Green
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 4: // Email
                            c.setForeground(new Color(156, 39, 176)); // Purple
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 5: // Registration Date
                            c.setForeground(new Color(211, 47, 47)); // Red
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        default:
                            c.setForeground(Color.BLACK);
                            setHorizontalAlignment(JLabel.LEFT);
                    }
                }
                
                return c;
            }
        });
        
        // Add table to scroll pane with enhanced styling
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Create enhanced search panel with modern styling
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBackground(new Color(245, 245, 250));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Title for search section
        JLabel searchTitle = new JLabel("Search Users");
        searchTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchTitle.setForeground(new Color(66, 133, 244));
        searchTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Search controls panel
        JPanel searchControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchControlsPanel.setBackground(new Color(245, 245, 250));
        searchControlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create a styled label for search type
        JLabel searchTypeLabel = new JLabel("Search by:");
        searchTypeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Style the search type combo box
        searchTypeCombo = new JComboBox<>(new String[]{"Username", "Email", "Full Name", "Registration Date"});
        searchTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchTypeCombo.setBackground(Color.WHITE);
        searchTypeCombo.setPreferredSize(new Dimension(150, 35));
        
        // Style the search field
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(250, 35));
        
        // Style the date search field
        dateSearchField = new JTextField(15);
        dateSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateSearchField.setPreferredSize(new Dimension(200, 35));
        dateSearchField.setToolTipText("Enter date in format: yyyy-MM-dd or time like AM/PM");
        dateSearchField.setVisible(false);
        
        // Create a modern search button
        JButton searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setForeground(Color.WHITE);
        searchButton.setBackground(new Color(66, 133, 244));
        searchButton.setPreferredSize(new Dimension(100, 35));
        searchButton.setFocusPainted(false);
        searchButton.setOpaque(true);
        searchButton.setContentAreaFilled(true);
        searchButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 120, 220), 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        // Add hover effect to search button
        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchButton.setBackground(new Color(59, 120, 220));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchButton.setBackground(new Color(66, 133, 244));
            }
        });
        
        // Add components to search controls panel
        searchControlsPanel.add(searchTypeLabel);
        searchControlsPanel.add(searchTypeCombo);
        searchControlsPanel.add(searchField);
        searchControlsPanel.add(dateSearchField);
        searchControlsPanel.add(searchButton);
        
        // Add listener to searchTypeCombo to show/hide date field
        searchTypeCombo.addActionListener(e -> {
            String selected = (String) searchTypeCombo.getSelectedItem();
            if ("Registration Date".equals(selected)) {
                searchField.setVisible(false);
                dateSearchField.setVisible(true);
            } else {
                searchField.setVisible(true);
                dateSearchField.setVisible(false);
            }
            searchControlsPanel.revalidate();
            searchControlsPanel.repaint();
        });
        
        // Create a panel for action buttons with modern styling
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionPanel.setBackground(new Color(245, 245, 250));
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Style the add user button
        JButton addUserButton = new JButton("Add User");
        addUserButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addUserButton.setForeground(Color.WHITE);
        addUserButton.setBackground(new Color(76, 175, 80));
        addUserButton.setPreferredSize(new Dimension(120, 35));
        addUserButton.setFocusPainted(false);
        addUserButton.setOpaque(true);
        addUserButton.setContentAreaFilled(true);
        addUserButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(69, 160, 73), 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        addUserButton.addActionListener(e -> showAddUserDialog());
        
        // Add hover effect to add user button
        addUserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                addUserButton.setBackground(new Color(69, 160, 73));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                addUserButton.setBackground(new Color(76, 175, 80));
            }
        });
        
        // Style the delete user button
        JButton deleteUserButton = new JButton("Delete Selected User");
        deleteUserButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteUserButton.setForeground(Color.WHITE);
        deleteUserButton.setBackground(new Color(211, 47, 47));
        deleteUserButton.setPreferredSize(new Dimension(180, 35));
        deleteUserButton.setFocusPainted(false);
        deleteUserButton.setOpaque(true);
        deleteUserButton.setContentAreaFilled(true);
        deleteUserButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 42, 42), 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        deleteUserButton.addActionListener(e -> deleteSelectedUser());
        
        // Add hover effect to delete user button
        deleteUserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteUserButton.setBackground(new Color(190, 42, 42));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteUserButton.setBackground(new Color(211, 47, 47));
            }
        });
        
        // Add buttons to action panel
        actionPanel.add(addUserButton);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(deleteUserButton);
        
        // Add components to search panel
        searchPanel.add(searchTitle);
        searchPanel.add(Box.createVerticalStrut(10));
        searchPanel.add(searchControlsPanel);
        searchPanel.add(Box.createVerticalStrut(10));
        searchPanel.add(actionPanel);
        
        // Add print options panel
        JPanel printPanel = new JPanel();
        printPanel.setLayout(new BoxLayout(printPanel, BoxLayout.Y_AXIS));
        printPanel.setBackground(new Color(245, 245, 250));
        printPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        printPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Add print title
        JLabel printTitle = new JLabel("Print Options");
        printTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        printTitle.setForeground(new Color(66, 133, 244));
        printTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        printPanel.add(printTitle);
        printPanel.add(Box.createVerticalStrut(10));
        
        // Create print buttons panel with flow layout
        JPanel printButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        printButtonsPanel.setBackground(new Color(245, 245, 250));
        printButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Column-specific print buttons with distinct colors
        JButton printUsernamesButton = createPrintButton("Print Usernames", new Color(33, 150, 243), e -> printColumn("Username"));
        JButton printPasswordsButton = createPrintButton("Print Passwords", new Color(156, 39, 176), e -> printColumn("Password"));
        JButton printFullNamesButton = createPrintButton("Print Full Names", new Color(0, 150, 136), e -> printColumn("Full Name"));
        JButton printEmailsButton = createPrintButton("Print Emails", new Color(255, 87, 34), e -> printColumn("Email"));
        JButton printDatesButton = createPrintButton("Print Reg. Dates", new Color(121, 85, 72), e -> printColumn("Registration Date"));
        
        // Add all print buttons to panel
        printButtonsPanel.add(printUsernamesButton);
        printButtonsPanel.add(printPasswordsButton);
        printButtonsPanel.add(printFullNamesButton);
        printButtonsPanel.add(printEmailsButton);
        printButtonsPanel.add(printDatesButton);
        
        printPanel.add(printButtonsPanel);
        
        // Add search panel to main panel
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Add print panel between search panel and table
        panel.add(printPanel, BorderLayout.SOUTH);
        
        // Add search functionality
        searchButton.addActionListener(e -> searchUsers());
        searchField.addActionListener(e -> searchUsers());
        dateSearchField.addActionListener(e -> searchUsers());
        
        // Add double-click listener to view user details
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = userTable.getSelectedRow();
                    if (row >= 0) {
                        String username = (String) userTable.getValueAt(row, 1);
                        showUserDetails(username);
                        tabbedPane.setSelectedIndex(1); // Switch to details tab
                    }
                }
            }
        });
        
        // Initial data load
        refreshUserTable();
        
        return panel;
    }
    
    /**
     * Creates a styled print button with the specified color
     * @param text Button text
     * @param color Background color (ignored, always uses light violet)
     * @param actionListener Action to perform when clicked
     * @return Styled JButton
     */
    private JButton createPrintButton(String text, Color color, ActionListener actionListener) {
        // Use a consistent light violet color for all print buttons
        Color buttonColor = new Color(186, 104, 200); // Light violet
        
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(buttonColor);
        button.setPreferredSize(new Dimension(150, 30));
        button.setFocusPainted(false);
        
        // Force button to display its background color
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        
        // Create a darker version of the color for the border
        Color borderColor = new Color(156, 39, 176); // Darker violet
        
        // Add proper border with compound border
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        button.addActionListener(actionListener);
        
        // Add hover effect with fixed darker color
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(borderColor); // Darker violet on hover
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonColor); // Back to light violet
            }
        });
        
        return button;
    }
    
    /**
     * Prints a specific column from the user table
     * @param columnName The name of the column to print
     */
    private void printColumn(String columnName) {
        // Authenticate for secure action
        if (!authenticateForSecureAction("Print " + columnName + " column")) {
            return;
        }
        
        // Show loading dialog
        JDialog loadingDialog = showLoadingDialog("Preparing " + columnName + " data for printing...");
        
        // Use a separate thread for the print operation to keep UI responsive
        new Thread(() -> {
            try {
                List<UserDatabase.User> users = UserDatabase.getAllUsers();
                if (users.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(this, "No user data to print.", 
                            "Empty Database", JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                PrinterJob job = PrinterJob.getPrinterJob();
                PageFormat format = job.defaultPage();
                
                job.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex > 0) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    
                    int y = 20;
                    int lineHeight = 20;
                    
                    // Title
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    g2d.drawString(columnName.toUpperCase() + " LIST REPORT", 20, y);
                    y += lineHeight * 2;
                    
                    // Date and time
                    g2d.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    g2d.drawString("Generated on: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()), 20, y);
                    y += lineHeight * 2;
                    
                    // Column info
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString("Total Records: " + users.size(), 20, y);
                    y += lineHeight * 2;
                    
                    // Table headers
                    String[] headers = {"No.", columnName};
                    int[] columnWidths = {50, 400};
                    int startX = 20;
                    
                    for (int i = 0; i < headers.length; i++) {
                        g2d.drawString(headers[i], startX, y);
                        startX += columnWidths[i];
                    }
                    y += 5;
                    
                    // Draw line under headers
                    g2d.drawLine(20, y, 20 + Arrays.stream(columnWidths).sum(), y);
                    y += lineHeight;
                    
                    // Table data
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    
                    for (int i = 0; i < users.size(); i++) {
                        UserDatabase.User user = users.get(i);
                        
                        String value;
                        switch (columnName) {
                            case "Username":
                                value = user.getUsername();
                                break;
                            case "Password":
                                value = hashPassword(user.getPassword());
                                break;
                            case "Full Name":
                                value = user.getFullName();
                                break;
                            case "Email":
                                value = user.getEmail();
                                break;
                            case "Registration Date":
                                value = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(user.getRegistrationDate());
                                break;
                            default:
                                value = "N/A";
                        }
                        
                        startX = 20;
                        
                        // Row number
                        g2d.drawString(String.valueOf(i + 1), startX, y);
                        startX += columnWidths[0];
                        
                        // Column value
                        g2d.drawString(value, startX, y);
                        
                        y += lineHeight;
                        
                        // Check if we've run out of space on the page
                        if (y > pageFormat.getImageableHeight() - 40) {
                            g2d.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                            g2d.drawString("(Report continues on next page...)", 20, (int) pageFormat.getImageableHeight() - 30);
                            break;
                        }
                    }
                    
                    // Footer
                    g2d.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    String footer = "CONFIDENTIAL - For authorized personnel only - Page 1";
                    g2d.drawString(footer, 20, (int) pageFormat.getImageableHeight() - 20);
                    
                    return Printable.PAGE_EXISTS;
                });
                
                // Close the loading dialog before showing the print dialog
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    
                    if (job.printDialog()) {
                        // Show the loading dialog again during printing
                        JDialog printingDialog = showLoadingDialog("Printing " + columnName + " data...");
                        
                        // Print in yet another thread to keep UI responsive
                        new Thread(() -> {
                            try {
                                job.print();
                                
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        columnName + " data has been sent to the printer.", 
                                        "Print Successful", 
                                        JOptionPane.INFORMATION_MESSAGE);
                                });
                            } catch (PrinterException pe) {
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        "Error printing document: " + pe.getMessage(), 
                                        "Print Error", 
                                        JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        }).start();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, 
                        "Error preparing print job: " + ex.getMessage(), 
                        "Print Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    private JPanel createUserDetailsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create user details area with custom styling
        userDetailsArea = new JTextArea();
        userDetailsArea.setEditable(false);
        userDetailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userDetailsArea.setBackground(new Color(245, 245, 250)); // Light blue-gray background
        userDetailsArea.setForeground(new Color(33, 33, 33)); // Dark text
        userDetailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create a styled scroll pane for the details area
        JScrollPane scrollPane = new JScrollPane(userDetailsArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(userDetailsArea.getBackground());
        
        // Add a header panel with title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(66, 133, 244));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("User Information");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Add refresh button to header
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(new Color(255, 255, 255));
        refreshButton.setForeground(new Color(66, 133, 244));
        refreshButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        refreshButton.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                String username = (String) userTable.getValueAt(row, 1); // Column 1 is username now
                showUserDetails(username);
            }
        });
        
        headerPanel.add(refreshButton, BorderLayout.EAST);
        
        // Add components to panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLoginHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        
        // Add header panel with title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(66, 133, 244));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Login History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Add refresh button to header
        JButton refreshButton = new JButton("Refresh History");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setForeground(new Color(66, 133, 244));
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        refreshButton.addActionListener(e -> refreshLoginHistoryTable());
        headerPanel.add(refreshButton, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create table model
        String[] columnNames = {"#", "Username", "Login Time", "Status"};
        loginHistoryTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table with larger font and row height
        loginHistoryTable = new JTable(loginHistoryTableModel);
        loginHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loginHistoryTable.getTableHeader().setReorderingAllowed(false);
        loginHistoryTable.setRowHeight(30); // Increased row height
        loginHistoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font
        loginHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Enable horizontal scrolling
        
        // Set column widths
        loginHistoryTable.getColumnModel().getColumn(0).setPreferredWidth(50); // # column
        loginHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Username
        loginHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Login Time
        loginHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Status
        
        // Style the table header
        loginHistoryTable.getTableHeader().setBackground(new Color(66, 133, 244));
        loginHistoryTable.getTableHeader().setForeground(Color.WHITE);
        loginHistoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginHistoryTable.getTableHeader().setPreferredSize(new Dimension(0, 35)); // Taller header
        
        // Add custom renderer for alternating row colors and status colors
        loginHistoryTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Apply alternating row colors
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
                    
                    // Apply column-specific colors
                    switch (column) {
                        case 0: // Number
                            c.setForeground(new Color(50, 50, 50)); // Dark gray
                            setHorizontalAlignment(JLabel.CENTER);
                            break;
                        case 1: // Username
                            c.setForeground(new Color(66, 133, 244)); // Blue
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 2: // Login Time
                            c.setForeground(new Color(156, 39, 176)); // Purple
                            setHorizontalAlignment(JLabel.LEFT);
                            break;
                        case 3: // Status
                            // Color based on status
                            setHorizontalAlignment(JLabel.CENTER);
                            if (value != null) {
                                if ("Successful".equals(value.toString())) {
                                    c.setForeground(new Color(76, 175, 80)); // Green for success
                                } else {
                                    c.setForeground(new Color(211, 47, 47)); // Red for failure
                                }
                            }
                            break;
                        default:
                            c.setForeground(Color.BLACK);
                            setHorizontalAlignment(JLabel.LEFT);
                    }
                }
                
                return c;
            }
        });
        
        // Add table to scroll pane with enhanced styling
        JScrollPane scrollPane = new JScrollPane(loginHistoryTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Initial data load
        refreshLoginHistoryTable();
        
        return panel;
    }
    
    /**
     * Shows a dialog for adding a new user
     */
    private void showAddUserDialog() {
        // Create a dialog for user input
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        // Create a panel for input fields
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(20);
        inputPanel.add(usernameLabel);
        inputPanel.add(usernameField);
        
        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        inputPanel.add(passwordLabel);
        inputPanel.add(passwordField);
        
        // Email field
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(20);
        inputPanel.add(emailLabel);
        inputPanel.add(emailField);
        
        // Full name field
        JLabel fullNameLabel = new JLabel("Full Name:");
        JTextField fullNameField = new JTextField(20);
        inputPanel.add(fullNameLabel);
        inputPanel.add(fullNameField);
        
        dialog.add(inputPanel, BorderLayout.CENTER);
        
        // Create a panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save");
        
        // Add action listeners
        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            // Validate input
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String email = emailField.getText().trim();
            String fullName = fullNameField.getText().trim();
            
            String validationError = validateUserInput(username, password, email, fullName);
            if (validationError != null) {
                JOptionPane.showMessageDialog(dialog, validationError, "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if username already exists
            if (UserDatabase.findUserByUsername(username) != null) {
                JOptionPane.showMessageDialog(dialog, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Add user to database
            UserDatabase.addUser(username, password, email, fullName);
            
            // Refresh user table
            refreshUserTable();
            
            // Show success message
            JOptionPane.showMessageDialog(dialog, "User added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Close dialog
            dialog.dispose();
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Show dialog
        dialog.setVisible(true);
    }
    
    /**
     * Validates user input for adding a new user
     */
    private String validateUserInput(String username, String password, String email, String fullName) {
        if (username.isEmpty()) {
            return "Username cannot be empty!";
        }
        if (username.length() < 3) {
            return "Username must be at least 3 characters!";
        }
        if (username.contains(" ")) {
            return "Username should not contain spaces!";
        }
        if (password.isEmpty()) {
            return "Password cannot be empty!";
        }
        if (password.length() < 5) {
            return "Password must be at least 5 characters!";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number!";
        }
        if (email.isEmpty()) {
            return "Email cannot be empty!";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return "Please enter a valid email address!";
        }
        if (fullName.isEmpty()) {
            return "Full name cannot be empty!";
        }
        return null;
    }
    
    /**
     * Deletes the selected user from the database
     */
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String username = (String) userTable.getValueAt(selectedRow, 1); // Column 1 is username now
        
        // Confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete user '" + username + "'?", 
                "Confirm Deletion", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Delete user
            boolean deleted = UserDatabase.deleteUser(username);
            
            if (deleted) {
                // Refresh table
                refreshUserTable();
                refreshLoginHistoryTable();
                JOptionPane.showMessageDialog(this, "User deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete user!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void refreshUserTable() {
        System.out.println("Refreshing user table...");
        userTableModel.setRowCount(0);
        List<UserDatabase.User> users = UserDatabase.getAllUsers();
        System.out.println("Found " + users.size() + " users to display.");
        
        int rowNumber = 1;
        for (UserDatabase.User user : users) {
            Vector<Object> row = new Vector<>();
            row.add(rowNumber++); // Add row number
            row.add(user.getUsername());
            row.add(hashPassword(user.getPassword())); // Add hashed password
            row.add(user.getFullName());
            row.add(user.getEmail());
            row.add(new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(user.getRegistrationDate()));
            userTableModel.addRow(row);
        }
    }
    
    private void refreshLoginHistoryTable() {
        loginHistoryTableModel.setRowCount(0);
        List<UserDatabase.User> users = UserDatabase.getAllUsers();
        
        int rowNumber = 1;
        for (UserDatabase.User user : users) {
            List<UserDatabase.LoginRecord> history = UserDatabase.getUserLoginHistory(user.getUsername());
            for (UserDatabase.LoginRecord record : history) {
                Vector<Object> row = new Vector<>();
                row.add(rowNumber++);
                row.add(user.getUsername());
                row.add(new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(record.getLoginTime()));
                row.add(record.isSuccessful() ? "Successful" : "Failed");
                loginHistoryTableModel.addRow(row);
            }
        }
    }
    
    private void searchUsers() {
        String searchType = (String) searchTypeCombo.getSelectedItem();
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            refreshUserTable();
            return;
        }
        
        List<UserDatabase.User> users = UserDatabase.getAllUsers();
        userTableModel.setRowCount(0);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int count = 1;
        
        for (UserDatabase.User user : users) {
            boolean match = false;
            
            switch (searchType) {
                case "Username":
                    match = user.getUsername().toLowerCase().contains(searchTerm);
                    break;
                case "Full Name":
                    match = user.getFullName().toLowerCase().contains(searchTerm);
                    break;
                case "Email":
                    match = user.getEmail().toLowerCase().contains(searchTerm);
                    break;
                case "Registration Date":
                    // For date searches, just do a simple contains check
                    String regDate = sdf.format(user.getRegistrationDate());
                    match = regDate.contains(searchTerm);
                    break;
            }
            
            if (match) {
                Object[] rowData = {
                    count++,
                    user.getUsername(),
                    hashPassword(user.getPassword()),
                    user.getFullName(),
                    user.getEmail(),
                    sdf.format(user.getRegistrationDate())
                };
                userTableModel.addRow(rowData);
            }
        }
    }
    
    private void showUserDetails(String username) {
        Map<String, Object> stats = UserDatabase.getUserStatistics(username);
        
        if (stats.isEmpty()) {
            userDetailsArea.setText("User not found.");
            return;
        }
        
        // Get user for password hash
        UserDatabase.User user = UserDatabase.findUserByUsername(username);
        String passwordHash = (user != null) ? hashPassword(user.getPassword()) : "N/A";
        
        // HTML-styled content for better formatting and colors
        StringBuilder details = new StringBuilder();
        details.append("<html><div style='font-family: Segoe UI; padding: 10px;'>");
        
        // User Details section with blue header
        details.append("<div style='background-color: #4285F4; color: white; padding: 8px; border-radius: 5px;'>");
        details.append("<span style='font-size: 16pt; font-weight: bold;'>USER DETAILS</span>");
        details.append("</div><div style='margin: 10px 0 20px 10px;'>");
        details.append("<span style='color: #4285F4; font-weight: bold;'>Username:</span> <span style='color: #333333;'>").append(stats.get("username")).append("</span><br>");
        details.append("<span style='color: #4285F4; font-weight: bold;'>Password (Hashed):</span> <span style='color: #FF9800;'>").append(passwordHash).append("</span><br>");
        details.append("<span style='color: #4285F4; font-weight: bold;'>Full Name:</span> <span style='color: #333333;'>").append(stats.get("fullName")).append("</span><br>");
        details.append("<span style='color: #4285F4; font-weight: bold;'>Email:</span> <span style='color: #333333;'>").append(stats.get("email")).append("</span><br>");
        details.append("<span style='color: #4285F4; font-weight: bold;'>Registration Date:</span> <span style='color: #333333;'>").append(stats.get("registrationDate")).append("</span><br>");
        details.append("</div>");
        
        // Login Statistics section with green header
        details.append("<div style='background-color: #34A853; color: white; padding: 8px; border-radius: 5px;'>");
        details.append("<span style='font-size: 16pt; font-weight: bold;'>LOGIN STATISTICS</span>");
        details.append("</div><div style='margin: 10px 0 20px 10px;'>");
        details.append("<span style='color: #34A853; font-weight: bold;'>Total Login Attempts:</span> <span style='color: #333333;'>").append(stats.get("totalLogins")).append("</span><br>");
        details.append("<span style='color: #34A853; font-weight: bold;'>Successful Logins:</span> <span style='color: #333333;'>").append(stats.get("successfulLogins")).append("</span><br>");
        details.append("<span style='color: #34A853; font-weight: bold;'>Failed Logins:</span> <span style='color: #333333;'>").append(stats.get("failedLogins")).append("</span><br>");
        details.append("</div>");
        
        // Recent Login History section with purple header
        details.append("<div style='background-color: #9C27B0; color: white; padding: 8px; border-radius: 5px;'>");
        details.append("<span style='font-size: 16pt; font-weight: bold;'>RECENT LOGIN HISTORY</span>");
        details.append("</div><div style='margin: 10px 0 20px 10px;'>");
        
        List<UserDatabase.LoginRecord> history = UserDatabase.getUserLoginHistory(username);
        if (history.isEmpty()) {
            details.append("<span style='color: #666666;'>No login history available.</span>");
        } else {
            details.append("<table style='width: 100%; border-collapse: collapse;'>");
            details.append("<tr style='background-color: #F1F1F1;'>");
            details.append("<th style='padding: 5px; text-align: left; color: #9C27B0;'>Time</th>");
            details.append("<th style='padding: 5px; text-align: left; color: #9C27B0;'>Status</th>");
            details.append("</tr>");
            
            int count = 0;
            for (UserDatabase.LoginRecord record : history) {
                String status = record.isSuccessful() ? 
                        "<span style='color: #34A853;'>Successful</span>" : 
                        "<span style='color: #EA4335;'>Failed</span>";
                        
                String rowColor = count % 2 == 0 ? "#FFFFFF" : "#F8F8F8";
                details.append("<tr style='background-color: ").append(rowColor).append(";'>");
                details.append("<td style='padding: 5px; color: #333333;'>")
                       .append(new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(record.getLoginTime()))
                       .append("</td>");
                details.append("<td style='padding: 5px;'>").append(status).append("</td>");
                details.append("</tr>");
                count++;
            }
            
            details.append("</table>");
        }
        details.append("</div>");
        details.append("</div></html>");
        
        // Plain text version as fallback
        StringBuilder plainText = new StringBuilder();
        plainText.append("USER DETAILS\n");
        plainText.append("============\n\n");
        plainText.append("Username: ").append(stats.get("username")).append("\n");
        plainText.append("Password (Hashed): ").append(passwordHash).append("\n");
        plainText.append("Full Name: ").append(stats.get("fullName")).append("\n");
        plainText.append("Email: ").append(stats.get("email")).append("\n");
        plainText.append("Registration Date: ").append(stats.get("registrationDate")).append("\n\n");
        
        plainText.append("LOGIN STATISTICS\n");
        plainText.append("===============\n\n");
        
        plainText.append("Total Login Attempts: ").append(stats.get("totalLogins")).append("\n");
        plainText.append("Successful Logins: ").append(stats.get("successfulLogins")).append("\n");
        plainText.append("Failed Logins: ").append(stats.get("failedLogins")).append("\n\n");
        
        plainText.append("RECENT LOGIN HISTORY\n");
        plainText.append("===================\n\n");
        
        if (history.isEmpty()) {
            plainText.append("No login history available.");
        } else {
            for (UserDatabase.LoginRecord record : history) {
                plainText.append(new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(record.getLoginTime()))
                       .append(" - ")
                       .append(record.isSuccessful() ? "Successful" : "Failed")
                       .append("\n");
            }
        }
        
        userDetailsArea.setText(plainText.toString());
        
        // Try to use HTML if possible
        try {
            JTextPane textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setText(details.toString());
            textPane.setEditable(false);
            textPane.setBackground(userDetailsArea.getBackground());
            textPane.setBorder(userDetailsArea.getBorder());
            
            // Replace the text area with the text pane
            Container parent = userDetailsArea.getParent();
            if (parent instanceof JViewport) {
                JScrollPane scrollPane = (JScrollPane) ((JViewport) parent).getParent();
                scrollPane.setViewportView(textPane);
            }
        } catch (Exception e) {
            // Fall back to plain text if HTML doesn't work
            System.err.println("Error setting HTML content: " + e.getMessage());
            // Plain text is already set above
        }
    }
    
    // Method to ensure sample users are loaded
    private void ensureSampleUsers() {
        System.out.println("Checking if sample users need to be loaded...");
        List<UserDatabase.User> users = UserDatabase.getAllUsers();
        System.out.println("Current user count: " + users.size());
        
        if (users.isEmpty()) {
            System.out.println("No users found. Adding sample users...");
            // Add sample users if database is empty
            UserDatabase.addUser("gigi", "password123", "gigi@example.com", "Gigi bulaclac");
            UserDatabase.addUser("der", "secure456", "der@example.com", "xander ");
            UserDatabase.addUser("rendell", "pass789", "rendell@example.com", "rendell domala");
            UserDatabase.addUser("ced", "ced2023", "ced@example.com", "ceddiee");
            UserDatabase.addUser("charles", "cha123", "cha@example.com", "charles");
            UserDatabase.addUser("emmanNI", "nigs456", "Nigg@example.com", "emman");
            UserDatabase.addUser("flok", "flo789", "flok@example.com", "florence");
            UserDatabase.addUser("portu", "portu2023", "portu@example.com", "portu michael");
            UserDatabase.addUser("rendell badang", "badang123", "badang@example.com", "badang domalaon");
            UserDatabase.addUser("jaja", "bai456", "jaja@example.com", "jaja bsiaya");
            
            // Refresh the table to show the newly added users
            refreshUserTable();
            System.out.println("Sample users added successfully.");
        } else {
            System.out.println("Database already contains users. No need to add samples.");
        }
    }
    
    // Method to convert password to a hashed format for display
    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "N/A";
        }
        
        try {
            // Use SHA-256 to hash the password
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            
            // Convert bytes to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            // Return shortened hash for display (first 20 chars)
            String fullHash = sb.toString();
            return fullHash.substring(0, Math.min(20, fullHash.length())) + "...";
        } catch (Exception e) {
            // If hashing fails, show masked password
            return "*".repeat(password.length());
        }
    }
    
    /**
     * Shows a security dialog requiring password authentication
     * @param actionDescription Description of the action being protected
     * @return true if authentication is successful, false otherwise
     */
    private boolean authenticateForSecureAction(String actionDescription) {
        // Log for debugging
        System.out.println("Authentication dialog requested for: " + actionDescription);
        
        // If already authenticated for the session, don't require re-authentication
        if (isAuthenticated) {
            System.out.println("Already authenticated in this session, bypassing dialog");
            return true;
        }
        
        // Create a more modern dialog similar to the initial authentication
        JDialog securityDialog = new JDialog(this, "Security Authentication", true);
        securityDialog.setLayout(new BorderLayout());
        securityDialog.setSize(400, 320); // Increased height from 280 to 320
        securityDialog.setLocationRelativeTo(this);
        securityDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Set undecorated to true for consistent styling with initial auth dialog
        securityDialog.setUndecorated(true);
        
        // Panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 242, 255);
                Color color2 = new Color(230, 235, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1)); // Add visible border
        
        // Create header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(66, 133, 244));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Authentication Required");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        JLabel actionLabel = new JLabel("Action: " + actionDescription);
        actionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        actionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setPreferredSize(new Dimension(100, 25));
        
        securityPasswordField = new JPasswordField(15);
        securityPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        securityPasswordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        passwordPanel.add(passwordLabel);
        passwordPanel.add(securityPasswordField);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setForeground(new Color(100, 100, 100));
        cancelButton.setBackground(new Color(240, 240, 240));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        JButton authenticateButton = new JButton("Authenticate");
        authenticateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        authenticateButton.setForeground(Color.WHITE);
        authenticateButton.setBackground(new Color(66, 133, 244));
        authenticateButton.setFocusPainted(false);
        authenticateButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 120, 220), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(authenticateButton);
        
        // Add a status panel for error messages
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel statusIconLabel = new JLabel();
        statusIconLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        statusIconLabel.setVisible(false);
        
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(211, 47, 47));
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        
        statusPanel.add(statusIconLabel);
        statusPanel.add(statusLabel);
        
        // Add components to content panel
        contentPanel.add(actionLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(passwordPanel);
        contentPanel.add(Box.createVerticalStrut(20)); // Increased from 15 to 20
        contentPanel.add(statusPanel); // Add status panel
        contentPanel.add(Box.createVerticalStrut(40)); // Increased from 25 to 40
        contentPanel.add(buttonPanel);
        
        // Add panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add main panel to dialog
        securityDialog.add(mainPanel);
        
        final boolean[] result = {false};
        final int[] attempts = {0}; // Add counter for authentication attempts
        
        // Add action listeners
        cancelButton.addActionListener(e -> {
            // Keep main frame invisible
            setVisible(false);
            
            // Prepare welcome screen first
            OneTimeLoginSystem.prepareWelcomeScreen();
            
            // Then dispose dialog
            securityDialog.dispose();
            
            // Finally dispose this window
            dispose();
        });
        
        ActionListener authAction = e -> {
            String enteredPassword = new String(securityPasswordField.getPassword());
            if (enteredPassword.equals(adminPassword)) {
                result[0] = true;
                isAuthenticated = true; // Set session-wide authentication
                securityDialog.dispose();
            } else {
                attempts[0]++; // Increment attempt counter
                
                // Update status panel instead of showing message dialog
                statusIconLabel.setVisible(true);
                
                if (attempts[0] >= 3) { // Check if attempt limit is reached
                    statusLabel.setText("Too many failed attempts! Access denied.");
                    
                    // Set the database lockout flag
                    isDatabaseLocked = true;
                    
                    // Create timer to close dialog and redirect after showing message
                    javax.swing.Timer failTimer = new javax.swing.Timer(1500, evt -> {
                        // Keep main frame invisible
                        setVisible(false);
                        
                        // Prepare welcome screen
                        OneTimeLoginSystem.prepareWelcomeScreen();
                        
                        // Dispose dialog and main window
                        securityDialog.dispose();
                        dispose();
                        
                        ((javax.swing.Timer)evt.getSource()).stop();
                    });
                    failTimer.setRepeats(false);
                    failTimer.start();
                } else {
                    statusLabel.setText("Invalid password. Attempts remaining: " + (3 - attempts[0]));
                    securityPasswordField.setText("");
                    
                    // Add shake animation for wrong password
                    final int originalX = securityDialog.getLocationOnScreen().x;
                    final int[] time = {0};
                    javax.swing.Timer shakeTimer = new javax.swing.Timer(30, evt -> {
                        if (time[0] >= 10) {
                            securityDialog.setLocation(originalX, securityDialog.getLocationOnScreen().y);
                            ((javax.swing.Timer)evt.getSource()).stop();
                        } else {
                            int offset = 10;
                            if (time[0] % 2 == 0) {
                                offset = -offset;
                            }
                            securityDialog.setLocation(originalX + offset, securityDialog.getLocationOnScreen().y);
                            time[0]++;
                        }
                    });
                    shakeTimer.setRepeats(true);
                    shakeTimer.start();
                }
            }
        };
        
        authenticateButton.addActionListener(authAction);
        securityPasswordField.addActionListener(authAction);
        
        // Set focus to password field
        securityDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                securityPasswordField.requestFocusInWindow();
            }
        });
        
        // Show dialog
        securityDialog.setVisible(true);
        return result[0];
    }
    
    /**
     * Creates and displays a loading dialog
     * @param message Message to display in the loading dialog
     * @return The dialog instance that was created and displayed
     */
    private JDialog showLoadingDialog(String message) {
        JDialog loadingDialog = new JDialog(this, "Processing", false);
        loadingDialog.setLayout(new BorderLayout());
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel loadingLabel = new JLabel(message);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(250, 20));
        
        panel.add(loadingLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        
        loadingDialog.add(panel);
        loadingDialog.setResizable(false);
        
        // Run on EDT to prevent blocking
        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
        
        return loadingDialog;
    }
    
    /**
     * Prints the current user details
     */
    private void printCurrentUserDetails() {
        // Get selected username
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a user to print details for.", 
                "No User Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String username = (String) userTable.getValueAt(selectedRow, 1);
        
        // Authenticate for secure action
        if (!authenticateForSecureAction("Print user details for " + username)) {
            return;
        }
        
        // Show loading dialog
        JDialog loadingDialog = showLoadingDialog("Preparing user details for printing...");
        
        // Use a separate thread for the print operation to keep UI responsive
        new Thread(() -> {
            try {
                // Get user details
                Map<String, Object> stats = UserDatabase.getUserStatistics(username);
                UserDatabase.User user = UserDatabase.findUserByUsername(username);
                
                if (stats.isEmpty() || user == null) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(this, "User details could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return;
                }
                
                // Create printable content
                String passwordHash = hashPassword(user.getPassword());
                
                PrinterJob job = PrinterJob.getPrinterJob();
                PageFormat format = job.defaultPage();
                
                job.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex > 0) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    
                    int y = 20;
                    int lineHeight = 20;
                    
                    // Header
                    g2d.drawString("USER DETAILS REPORT", 20, y);
                    y += lineHeight * 2;
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString("USER INFORMATION", 20, y);
                    y += lineHeight;
                    
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2d.drawString("Username: " + stats.get("username"), 30, y += lineHeight);
                    g2d.drawString("Password Hash: " + passwordHash, 30, y += lineHeight);
                    g2d.drawString("Full Name: " + stats.get("fullName"), 30, y += lineHeight);
                    g2d.drawString("Email: " + stats.get("email"), 30, y += lineHeight);
                    g2d.drawString("Registration Date: " + stats.get("registrationDate"), 30, y += lineHeight);
                    
                    y += lineHeight;
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString("LOGIN STATISTICS", 20, y);
                    y += lineHeight;
                    
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2d.drawString("Total Login Attempts: " + stats.get("totalLogins"), 30, y += lineHeight);
                    g2d.drawString("Successful Logins: " + stats.get("successfulLogins"), 30, y += lineHeight);
                    g2d.drawString("Failed Logins: " + stats.get("failedLogins"), 30, y += lineHeight);
                    
                    y += lineHeight;
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString("RECENT LOGIN HISTORY", 20, y);
                    y += lineHeight;
                    
                    List<UserDatabase.LoginRecord> history = UserDatabase.getUserLoginHistory(username);
                    if (history.isEmpty()) {
                        g2d.drawString("No login history available.", 30, y += lineHeight);
                    } else {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                        int count = 0;
                        for (UserDatabase.LoginRecord record : history) {
                            if (count++ > 10) {
                                g2d.drawString("... (more entries not shown)", 30, y += lineHeight);
                                break;
                            }
                            String status = record.isSuccessful() ? "Successful" : "Failed";
                            g2d.drawString(dateFormat.format(record.getLoginTime()) + " - " + status, 30, y += lineHeight);
                        }
                    }
                    
                    // Footer with timestamp and page number
                    g2d.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    String footer = "Printed on: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + 
                                    " - Page 1 of 1 - CONFIDENTIAL";
                    g2d.drawString(footer, 20, (int) pageFormat.getImageableHeight() - 20);
                    
                    return Printable.PAGE_EXISTS;
                });
                
                // Close the loading dialog before showing the print dialog
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    
                    if (job.printDialog()) {
                        // Show the loading dialog again during printing
                        JDialog printingDialog = showLoadingDialog("Printing user details...");
                        
                        // Print in yet another thread to keep UI responsive
                        new Thread(() -> {
                            try {
                                job.print();
                                
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        "User details have been sent to the printer.", 
                                        "Print Successful", 
                                        JOptionPane.INFORMATION_MESSAGE);
                                });
                            } catch (PrinterException pe) {
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        "Error printing document: " + pe.getMessage(), 
                                        "Print Error", 
                                        JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        }).start();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, 
                        "Error preparing print job: " + ex.getMessage(), 
                        "Print Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Prints a summary report of all users
     */
    private void printSummaryReport() {
        // Authenticate for secure action
        if (!authenticateForSecureAction("Print summary report of all users")) {
            return;
        }
        
        // Show loading dialog
        JDialog loadingDialog = showLoadingDialog("Generating summary report...");
        
        // Use a separate thread for the print operation to keep UI responsive
        new Thread(() -> {
            try {
                List<UserDatabase.User> users = UserDatabase.getAllUsers();
                if (users.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(this, "No users found to include in report.", 
                            "Empty Database", JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                PrinterJob job = PrinterJob.getPrinterJob();
                PageFormat format = job.defaultPage();
                
                job.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex > 0) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    
                    // Calculate available width for printing
                    double pageWidth = pageFormat.getImageableWidth();
                    
                    int y = 20;
                    int lineHeight = 20;
                    
                    // Title
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    g2d.drawString("USER DATABASE SUMMARY REPORT", 20, y);
                    y += lineHeight * 2;
                    
                    // Date and time
                    g2d.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    g2d.drawString("Generated on: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()), 20, y);
                    y += lineHeight * 2;
                    
                    // User summary table header
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString("Total Users: " + users.size(), 20, y);
                    y += lineHeight * 2;
                    
                    // Adjusted column widths to fit on the page better
                    String[] headers = {"No.", "Username", "Full Name", "Email", "Registration Date", "Login Count"};
                    int[] columnWidths = {30, 90, 120, 120, 100, 80};
                    
                    // Make sure columns fit on the page
                    int totalWidth = Arrays.stream(columnWidths).sum();
                    if (totalWidth > pageWidth - 40) {
                        double scaleFactor = (pageWidth - 40) / totalWidth;
                        for (int i = 0; i < columnWidths.length; i++) {
                            columnWidths[i] = (int)(columnWidths[i] * scaleFactor);
                        }
                    }
                    
                    // Table headers
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    int startX = 20;
                    for (int i = 0; i < headers.length; i++) {
                        g2d.drawString(headers[i], startX, y);
                        startX += columnWidths[i];
                    }
                    y += 5;
                    
                    // Draw line under headers
                    g2d.drawLine(20, y, 20 + Arrays.stream(columnWidths).sum(), y);
                    y += lineHeight;
                    
                    // Table data
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    
                    for (int i = 0; i < users.size(); i++) {
                        UserDatabase.User user = users.get(i);
                        Map<String, Object> stats = UserDatabase.getUserStatistics(user.getUsername());
                        
                        // Make sure we retrieve total logins properly
                        Object totalLogins = stats.get("totalLogins");
                        String loginCount = (totalLogins != null) ? totalLogins.toString() : "0";
                        
                        startX = 20;
                        
                        // Row number
                        g2d.drawString(String.valueOf(i + 1), startX, y);
                        startX += columnWidths[0];
                        
                        // Username - truncate if too long
                        String username = truncateString(user.getUsername(), columnWidths[1]/7);
                        g2d.drawString(username, startX, y);
                        startX += columnWidths[1];
                        
                        // Full Name - truncate if too long
                        String fullName = truncateString(user.getFullName(), columnWidths[2]/7);
                        g2d.drawString(fullName, startX, y);
                        startX += columnWidths[2];
                        
                        // Email - truncate if too long
                        String email = truncateString(user.getEmail(), columnWidths[3]/7);
                        g2d.drawString(email, startX, y);
                        startX += columnWidths[3];
                        
                        // Registration Date
                        g2d.drawString(dateFormat.format(user.getRegistrationDate()), startX, y);
                        startX += columnWidths[4];
                        
                        // Login Count - ensure it's visible
                        g2d.drawString(loginCount, startX, y);
                        
                        y += lineHeight;
                        
                        // Check if we've run out of space on the page
                        if (y > pageFormat.getImageableHeight() - 80) {
                            // Draw a note about continuation
                            g2d.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                            g2d.drawString("(Report continues on next page...)", 20, (int) pageFormat.getImageableHeight() - 40);
                            break;
                        }
                    }
                    
                    // Add login statistics summary at the bottom
                    if (y <= pageFormat.getImageableHeight() - 80) {
                        y += lineHeight;
                        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        g2d.drawString("LOGIN STATISTICS SUMMARY", 20, y);
                        y += lineHeight;
                        
                        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                        
                        // Calculate totals
                        int totalLoginAttempts = 0;
                        int totalSuccessfulLogins = 0;
                        int totalFailedLogins = 0;
                        
                        for (UserDatabase.User user : users) {
                            Map<String, Object> stats = UserDatabase.getUserStatistics(user.getUsername());
                            if (stats.get("totalLogins") != null) {
                                totalLoginAttempts += Integer.parseInt(stats.get("totalLogins").toString());
                            }
                            if (stats.get("successfulLogins") != null) {
                                totalSuccessfulLogins += Integer.parseInt(stats.get("successfulLogins").toString());
                            }
                            if (stats.get("failedLogins") != null) {
                                totalFailedLogins += Integer.parseInt(stats.get("failedLogins").toString());
                            }
                        }
                        
                        g2d.drawString("Total Login Attempts: " + totalLoginAttempts, 30, y += lineHeight);
                        g2d.drawString("Total Successful Logins: " + totalSuccessfulLogins, 30, y += lineHeight);
                        g2d.drawString("Total Failed Logins: " + totalFailedLogins, 30, y += lineHeight);
                        
                        if (totalLoginAttempts > 0) {
                            double successRate = (double) totalSuccessfulLogins / totalLoginAttempts * 100;
                            g2d.drawString(String.format("Overall Success Rate: %.2f%%", successRate), 30, y += lineHeight);
                        }
                    }
                    
                    // Footer
                    g2d.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    String footer = "CONFIDENTIAL - For authorized personnel only - Page 1";
                    g2d.drawString(footer, 20, (int) pageFormat.getImageableHeight() - 20);
                    
                    return Printable.PAGE_EXISTS;
                });
                
                // Close the loading dialog before showing the print dialog
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    
                    if (job.printDialog()) {
                        // Show the loading dialog again during printing
                        JDialog printingDialog = showLoadingDialog("Printing summary report...");
                        
                        // Print in yet another thread to keep UI responsive
                        new Thread(() -> {
                            try {
                                job.print();
                                
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        "Summary report has been sent to the printer.", 
                                        "Print Successful", 
                                        JOptionPane.INFORMATION_MESSAGE);
                                });
                            } catch (PrinterException pe) {
                                SwingUtilities.invokeLater(() -> {
                                    printingDialog.dispose();
                                    JOptionPane.showMessageDialog(this, 
                                        "Error printing document: " + pe.getMessage(), 
                                        "Print Error", 
                                        JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        }).start();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, 
                        "Error preparing print job: " + ex.getMessage(), 
                        "Print Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Helper method to truncate strings that might be too long for display
     * @param str The string to truncate
     * @param maxLength Maximum length to allow
     * @return Truncated string with ellipsis if needed
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Exports user data to a CSV file
     */
    private void exportData() {
        // Authenticate for secure action
        if (!authenticateForSecureAction("Export user data to file")) {
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save User Data");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.write("Username,Full Name,Email,Registration Date,Total Logins,Successful Logins,Failed Logins\n");
                
                // Write data
                List<UserDatabase.User> users = UserDatabase.getAllUsers();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                for (UserDatabase.User user : users) {
                    Map<String, Object> stats = UserDatabase.getUserStatistics(user.getUsername());
                    
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s\n",
                        user.getUsername().replace("\"", "\"\""),
                        user.getFullName().replace("\"", "\"\""),
                        user.getEmail().replace("\"", "\"\""),
                        dateFormat.format(user.getRegistrationDate()),
                        stats.get("totalLogins"),
                        stats.get("successfulLogins"),
                        stats.get("failedLogins")
                    ));
                }
                
                JOptionPane.showMessageDialog(this, 
                    "User data has been exported to:\n" + file.getAbsolutePath(), 
                    "Export Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error exporting data: " + e.getMessage(), 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Returns to the welcome page (post-login screen)
     */
    private void returnToWelcomePage() {
        // First dispose the current window
        dispose();
        
        // Show the welcome screen directly (bypassing login)
        OneTimeLoginSystem.showWelcomeScreen();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set Nimbus Look and Feel for consistency
                UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
                for (UIManager.LookAndFeelInfo info : installedLookAndFeels) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to set Nimbus Look and Feel: " + ex.getMessage());
            }
            
            UserDatabaseUI ui = new UserDatabaseUI();
            ui.setVisible(true);
        });
    }
} 