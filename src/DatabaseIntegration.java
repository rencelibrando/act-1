import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DatabaseIntegration {
    
    // Static reference to store the OneTimeLoginSystem instance if needed for OTP verification
    private static OneTimeLoginSystem otpSystem;
    
    /**
     * Add database integration to the welcome screen
     * @param contentPanel The content panel to add the button to
     * @param otpLoginSystem The OTP login system instance for potential re-verification
     */
    public static void addDatabaseButton(GradientPanel contentPanel, OneTimeLoginSystem otpLoginSystem) {
        // Store reference to OTP system for later use if needed
        otpSystem = otpLoginSystem;
        
        // Create a button for database management
        JButton dbButton = new JButton("User Database");
        dbButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dbButton.setFocusPainted(false);
        dbButton.setBackground(Color.WHITE);
        dbButton.setForeground(new Color(34, 193, 195));
        dbButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        dbButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                dbButton.setBackground(new Color(230, 230, 230));
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                dbButton.setBackground(Color.WHITE);
            }
        });
        
        // Add action listener
        dbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the parent window (login system window)
                Window parentWindow = SwingUtilities.getWindowAncestor(dbButton);
                
                // Check if database is locked from too many failed authentication attempts
                if (UserDatabaseUI.isDatabaseLocked) {
                    // Show dialog explaining the situation
                    JOptionPane.showMessageDialog(
                        parentWindow,
                        "Database access is locked due to multiple failed authentication attempts.\n" +
                        "You must complete OTP verification again to unlock access.",
                        "Database Locked",
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    // Hide current window and redirect to OTP verification
                    if (parentWindow != null) {
                        parentWindow.dispose();
                    }
                    
                    // Create new login flow that will reset the lockout after successful verification
                    SwingUtilities.invokeLater(() -> {
                        // OneTimeLoginSystem is a JFrame based on our examination
                        OneTimeLoginSystem newLoginSystem = new OneTimeLoginSystem();
                        
                        // Display an informational message after the login window is visible
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                null, // Use null to center on screen since we don't have access to the mainFrame
                                "Please complete OTP verification to unlock database access.",
                                "Database Unlock Required",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        });
                        
                        // Reset database lock once OTP verification is completed successfully
                        UserDatabaseUI.isDatabaseLocked = false;
                    });
                    
                    return;
                }
                
                // Hide the parent window first but don't dispose it yet
                if (parentWindow != null) {
                    parentWindow.setVisible(false);
                }
                
                // Open the database UI in a new thread
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Ensure consistent Look and Feel
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
                    
                    UserDatabaseUI dbUI = new UserDatabaseUI();
                    
                    // Force update of all UI components
                    SwingUtilities.updateComponentTreeUI(dbUI);
                    
                    // If authentication is successful, the database UI will remain visible
                    // and we should dispose the parent window. Otherwise, the database UI will
                    // handle showing the welcome screen again.
                    dbUI.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            // If the parent was closed directly, do nothing more
                            if (parentWindow != null && parentWindow.isDisplayable()) {
                                parentWindow.dispose();
                            }
                        }
                    });
                    
                    // Show the database UI
                    dbUI.setVisible(true);
                });
            }
        });
        
        // Find the action panel in the content panel
        for (java.awt.Component comp : contentPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                // Check if this is the action panel (contains Logout and Exit buttons)
                if (panel.getComponentCount() >= 2 && 
                    panel.getComponent(0) instanceof JButton && 
                    panel.getComponent(1) instanceof javax.swing.Box.Filler) {
                    
                    // Add our button to the panel
                    panel.add(Box.createHorizontalStrut(20));
                    panel.add(dbButton);
                    break;
                }
            }
        }
    }
    
    /**
     * Records a login attempt in the database
     * @param username The username used for login
     * @param password The password used for login
     * @return true if login was successful, false otherwise
     */
    public static boolean recordLoginAttempt(String username, String password) {
        // Check against hardcoded credentials first
        if (username.equals("rence") && password.equals("12345")) {
            return true;
        }
        
        // Check if user exists in database
        UserDatabase.User user = UserDatabase.findUserByUsername(username);
        
        if (user != null) {
            // Verify credentials using database
            boolean success = UserDatabase.verifyCredentials(username, password);
            return success;
        }
        
        return false;
    }
    
    /**
     * Checks if a user exists in the database
     * @param username The username to check
     * @return true if user exists, false otherwise
     */
    public static boolean userExists(String username) {
        return UserDatabase.findUserByUsername(username) != null;
    }
    
    /**
     * Gets the email for a user
     * @param username The username
     * @return The email address or null if user not found
     */
    public static String getUserEmail(String username) {
        UserDatabase.User user = UserDatabase.findUserByUsername(username);
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * Close the database connection
     */
    public static void closeConnection() {
        UserDatabase.closeConnection();
    }
} 