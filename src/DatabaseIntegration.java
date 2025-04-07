import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DatabaseIntegration {
    
    /**
     * Adds a database management button to the content panel of the OneTimeLoginSystem
     * @param contentPanel The content panel to add the button to
     */
    public static void addDatabaseButton(GradientPanel contentPanel) {
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
                // Open the database UI in a new thread
                SwingUtilities.invokeLater(() -> {
                    UserDatabaseUI dbUI = new UserDatabaseUI();
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