import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
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
    
    public UserDatabaseUI() {
        setTitle("Admin Database Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
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
        searchButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
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
        addUserButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
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
        deleteUserButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
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
        
        // Add search panel to main panel
        panel.add(searchPanel, BorderLayout.NORTH);
        
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
        String searchText = searchField.getText().toLowerCase();
        String searchType = (String) searchTypeCombo.getSelectedItem();
        
        if ("Registration Date".equals(searchType)) {
            searchText = dateSearchField.getText().toLowerCase();
        }
        
        if (searchText.isEmpty()) {
            refreshUserTable();
            return;
        }
        
        userTableModel.setRowCount(0);
        List<UserDatabase.User> users = UserDatabase.getAllUsers();
        
        int rowNumber = 1;
        for (UserDatabase.User user : users) {
            boolean match = false;
            
            switch (searchType) {
                case "Username":
                    match = user.getUsername().toLowerCase().contains(searchText);
                    break;
                case "Email":
                    match = user.getEmail().toLowerCase().contains(searchText);
                    break;
                case "Full Name":
                    match = user.getFullName().toLowerCase().contains(searchText);
                    break;
                case "Registration Date":
                    String dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(user.getRegistrationDate()).toLowerCase();
                    match = dateFormat.contains(searchText);
                    break;
            }
            
            if (match) {
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UserDatabaseUI ui = new UserDatabaseUI();
            ui.setVisible(true);
        });
    }
} 