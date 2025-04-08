import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.security.SecureRandom;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.util.HashSet;
import java.util.Set;
import java.net.URL;

public class OneTimeLoginSystem {
    // Configuration constants
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_DURATION = 300; // 5 minutes in seconds
    private static final int ANIMATION_STEPS = 30;
    private static final int SHAKE_DISTANCE = 10;
    private static final int SHAKE_DURATION = 10;
    private static final float ALPHA_STEP = 0.05f;
    
    // UI constants
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font STATUS_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    // Predefined username and password for authentication
    private static final String VALID_USERNAME = "rence";
    private static final String VALID_PASSWORD = "12345";
    private static final String VALID_EMAIL = "clarencemanlolo@gmail.com";
    
    // Interface for OTP verification callbacks
    public interface OtpVerificationListener {
        void onVerificationComplete(boolean success);
    }
    
    private String generatedOTP;
    private long otpExpiryTime;
    private GradientPanel otpPanel;
    private JTextField otpField;
    private JLabel otpStatusLabel;
    private JLabel timerLabel;
    private Timer otpTimer;
    // Flag to ensure login can be used only once
    private boolean loginUsed = false;

    private JFrame mainFrame;
    private GradientPanel loginPanel;
    private GradientPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel containerPanel;

    // Login field components
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JLabel statusLabel;

    private Set<String> usedCredentials;
    private String currentUser;
    private String currentEmail;
    private String currentOTP;

    static {
        try {
            // Use system look and feel instead of Nimbus for better button compatibility
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Set custom button colors that will override the Look and Feel
            UIManager.put("Button.background", Color.WHITE);
            UIManager.put("Button.foreground", new Color(66, 133, 244));
            UIManager.put("Button.select", new Color(230, 230, 230));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OneTimeLoginSystem() {
        // Initialize the main frame
        mainFrame = new JFrame("One-Time Login System");
        mainFrame.setSize(500, 400);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        // Create card layout to switch between panels
        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);

        // Create and add panels
        createLoginPanel();
        createOtpPanel();
        createContentPanel();
        containerPanel.add(loginPanel, "login");
        containerPanel.add(otpPanel, "otp");
        containerPanel.add(contentPanel, "content");

        mainFrame.add(containerPanel);
        
        // Add window listener for cleanup
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupTimers();
            }
        });
        
        // Show the frame by default unless it will be shown later
        mainFrame.setVisible(true);

        usedCredentials = new HashSet<>();
    }

    private void createOtpPanel() {
        otpPanel = new GradientPanel(new Color(255, 153, 102), new Color(255, 94, 98));
        otpPanel.setLayout(new BoxLayout(otpPanel, BoxLayout.Y_AXIS));
        otpPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("OTP Verification");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);

        // OTP Input Field with validation
        JPanel otpInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        otpInputPanel.setOpaque(false);
        otpField = new JTextField(6);
        otpField.setFont(NORMAL_FONT);
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setBorder(new RoundedCornerBorder(15));
        
        // Add input validation for OTP field
        otpField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { validateOTPInput(); }
            public void removeUpdate(DocumentEvent e) { validateOTPInput(); }
            public void insertUpdate(DocumentEvent e) { validateOTPInput(); }
        });
        
        // Add keyboard shortcut for OTP verification
        Action verifyAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOtpVerification();
            }
        };
        otpField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "verify");
        otpField.getActionMap().put("verify", verifyAction);
        
        otpInputPanel.add(otpField);

        // Timer Label
        timerLabel = new JLabel("Time remaining: 5:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setFont(NORMAL_FONT);
        timerLabel.setForeground(Color.WHITE);

        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);

        JButton submitButton = new JButton("Verify OTP");
        JButton resendButton = new JButton("Resend OTP");

        // Style buttons similar to login
        styleButton(submitButton, Color.WHITE, new Color(76, 175, 80));
        styleButton(resendButton, Color.WHITE, new Color(239, 108, 0));

        buttonPanel.add(submitButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(resendButton);

        // Status Label
        otpStatusLabel = new JLabel(" ");
        otpStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        otpStatusLabel.setFont(STATUS_FONT);
        otpStatusLabel.setForeground(Color.YELLOW);

        // Add components to panel
        otpPanel.add(titleLabel);
        otpPanel.add(Box.createVerticalStrut(30));
        otpPanel.add(otpInputPanel);
        otpPanel.add(Box.createVerticalStrut(10));
        otpPanel.add(timerLabel);
        otpPanel.add(Box.createVerticalStrut(20));
        otpPanel.add(buttonPanel);
        otpPanel.add(Box.createVerticalStrut(10));
        otpPanel.add(otpStatusLabel);

        // Submit OTP Action
        submitButton.addActionListener(e -> handleOtpVerification());

        // Resend OTP Action
        resendButton.addActionListener(e -> resendOTP());
    }

    private void validateOTPInput() {
        String text = otpField.getText();
        if (!text.isEmpty()) {
            // Only allow digits and common OTP characters
            text = text.replaceAll("[^0-9]", "").trim();
            
            // If we've modified the text, update the field
            if (!text.equals(otpField.getText())) {
                otpField.setText(text);
                otpField.setCaretPosition(text.length());
            }
            
            // Show live validation feedback
            if (text.length() > 0) {
                if (text.equals(generatedOTP)) {
                    otpStatusLabel.setText("✓ OTP looks good!");
                    otpStatusLabel.setForeground(new Color(76, 175, 80)); // Green
                } else if (text.length() == OTP_LENGTH) {
                    otpStatusLabel.setText("⚠ Check your OTP carefully");
                    otpStatusLabel.setForeground(Color.YELLOW);
                } else {
                    otpStatusLabel.setText(" "); // Clear status
                }
            } else {
                otpStatusLabel.setText(" "); // Clear status
            }
        }
    }

    private String generateOTP() {
        // Use SecureRandom for better security
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        // Get the OTP as a string and trim to remove any whitespace
        String otpStr = otp.toString().trim();
        
        // Debug log the generated OTP
        System.out.println("Generated OTP: " + otpStr);
        
        // Save the OTP to a class variable for debugging
        currentOTP = otpStr;
        
        return otpStr;
    }

    private void sendOTP(String otp) {
        // For real implementation, integrate email/SMS API here
        // Mock implementation
        System.out.println("Mock OTP sent: " + otp);
        JOptionPane.showMessageDialog(mainFrame,
                "Mock OTP sent: " + otp,
                "OTP Sent",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void startOtpTimer() {
        if (otpTimer != null && otpTimer.isRunning()) {
            otpTimer.stop();
        }

        final int[] remainingTime = {OTP_VALIDITY_DURATION};
        otpTimer = new Timer(1000, e -> {
            remainingTime[0]--;
            if (remainingTime[0] <= 0) {
                otpTimer.stop();
                timerLabel.setText("Time remaining: 0:00");
                otpStatusLabel.setText("OTP has expired. Please request a new one.");
                // Don't set generatedOTP to null here as it can cause validation issues
                // We'll check expiry time separately during verification
            } else {
                int minutes = remainingTime[0] / 60;
                int seconds = remainingTime[0] % 60;
                timerLabel.setText(String.format("Time remaining: %d:%02d", minutes, seconds));
            }
        });
        otpTimer.start();
    }

    private void styleButton(JButton button, Color bg, Color fg) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        addHoverEffect(button, bg, bg.darker());
    }
    
    private void createLoginPanel() {
        // Create a gradient panel for an aesthetic login page
        loginPanel = new GradientPanel(new Color(66, 133, 244), new Color(219, 233, 245));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        loginPanel.setAlpha(1f); // fully visible

        JLabel titleLabel = new JLabel("System Login");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);

        // Username Panel
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setOpaque(false);
        JLabel userLabel = new JLabel("Username: ");
        userLabel.setFont(NORMAL_FONT);
        userLabel.setForeground(Color.WHITE);
        usernameField = new JTextField(15);
        usernameField.setFont(NORMAL_FONT);
        usernameField.setBorder(new RoundedCornerBorder(15));
        usernameField.setToolTipText("Enter your username");
        userPanel.add(userLabel);
        userPanel.add(usernameField);

        // Password Panel
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.setOpaque(false);
        JLabel passLabel = new JLabel("Password: ");
        passLabel.setFont(NORMAL_FONT);
        passLabel.setForeground(Color.WHITE);
        passwordField = new JPasswordField(15);
        passwordField.setFont(NORMAL_FONT);
        passwordField.setBorder(new RoundedCornerBorder(15));
        passwordField.setToolTipText("Enter your password");
        passPanel.add(passLabel);
        passPanel.add(passwordField);

        // Email Panel
        JPanel emailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        emailPanel.setOpaque(false);
        JLabel emailLabel = new JLabel("Email: ");
        emailLabel.setFont(NORMAL_FONT);
        emailLabel.setForeground(Color.WHITE);
        emailField = new JTextField(15);
        emailField.setFont(NORMAL_FONT);
        emailField.setBorder(new RoundedCornerBorder(15));
        emailField.setToolTipText("Enter your email address");
        emailPanel.add(emailLabel);
        emailPanel.add(emailField);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setFocusPainted(false);
        loginButton.setBackground(Color.WHITE);
        loginButton.setForeground(new Color(66, 133, 244));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(loginButton, Color.WHITE, new Color(230, 230, 230));
        buttonPanel.add(loginButton);

        // Status label for feedback
        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setVisible(true);
        statusLabel.setOpaque(false);
        statusLabel.setMaximumSize(new Dimension(400, 30));
        statusLabel.setPreferredSize(new Dimension(400, 30));

        // Add components to the login panel
        loginPanel.add(titleLabel);
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(userPanel);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(passPanel);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(emailPanel);
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(buttonPanel);
        loginPanel.add(Box.createVerticalStrut(20));
        loginPanel.add(statusLabel);

        // Login button action listener with additional validation and animations
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Briefly change button color on click for visual feedback
                loginButton.setBackground(new Color(200, 200, 200));
                Timer tmpTimer = new Timer(100, ev -> loginButton.setBackground(Color.WHITE));
                tmpTimer.setRepeats(false);
                tmpTimer.start();
                
                // Handle login first
                handleLogin();
                
                // Only clear fields and reset if we're going back to login panel
                if (!loginUsed) {
                    usernameField.setText("");
                    passwordField.setText("");
                    emailField.setText("");
                    otpField.setText("");
                    otpStatusLabel.setText(" ");
                    generatedOTP = null;
                    if (otpTimer != null) otpTimer.stop();
                    animateTransition(contentPanel, loginPanel);
                }
            }
        });

        // Add keyboard shortcut for login
        Action loginAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        };
        passwordField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "login");
        passwordField.getActionMap().put("login", loginAction);
    }

    private void createContentPanel() {
        contentPanel = new GradientPanel(new Color(34, 193, 195), new Color(45, 253, 221));
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setOpaque(false);
        
        JLabel welcomeLabel = new JLabel("Welcome to the Portal");
        welcomeLabel.setFont(TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setHorizontalAlignment(JLabel.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.NORTH);
        
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        
        JButton logoutButton = new JButton("Logout");
        JButton exitButton = new JButton("Exit");
        
        styleButton(logoutButton, Color.WHITE, new Color(255, 89, 94));
        styleButton(exitButton, Color.WHITE, new Color(55, 55, 55));
        
        actionPanel.add(logoutButton);
        actionPanel.add(Box.createHorizontalStrut(20));
        actionPanel.add(exitButton);
        
        contentPanel.add(welcomePanel, BorderLayout.NORTH);
        contentPanel.add(actionPanel, BorderLayout.SOUTH);
        
        // Add welcome animation
        Icon animationIcon;
        try {
            // Try to load the animated GIF from resources
            URL gifURL = getClass().getResource("/resources/success.gif");
            if (gifURL != null) {
                animationIcon = new ImageIcon(gifURL);
                JLabel animationLabel = new JLabel(animationIcon);
                animationLabel.setHorizontalAlignment(JLabel.CENTER);
                contentPanel.add(animationLabel, BorderLayout.CENTER);
            } else {
                // Fallback to static success image or text
                JLabel successLabel = new JLabel("Login Successful!");
                successLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                successLabel.setForeground(Color.WHITE);
                successLabel.setHorizontalAlignment(JLabel.CENTER);
                contentPanel.add(successLabel, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            // Fallback if there's any error
            JLabel successLabel = new JLabel("Login Successful!");
            successLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            successLabel.setForeground(Color.WHITE);
            successLabel.setHorizontalAlignment(JLabel.CENTER);
            contentPanel.add(successLabel, BorderLayout.CENTER);
        }
        
        // Add database integration button
        DatabaseIntegration.addDatabaseButton(contentPanel, this);
        
        // Add action listeners
        logoutButton.addActionListener(e -> handleLogout());
        exitButton.addActionListener(e -> System.exit(0));
    }

    // Validates the input fields and returns an error message if validation fails; otherwise returns null.
    private String validateInputs(String username, String password, String email) {
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
        if (password.contains(" ")) {
            return "Password should not contain spaces!";
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
        return null;
    }

    // Handles the login process with one-time authentication enforcement and input validation.
    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText().trim();
        
        // Validate email first
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                "Please enter your email address!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            flashErrorTheme();
            shakeFrame();
            return;
        }
        
        // Check if user exists in database
        boolean userExists = DatabaseIntegration.userExists(username);
        
        if (userExists) {
            // Verify credentials using database
            boolean loginSuccess = DatabaseIntegration.recordLoginAttempt(username, password);
            
            if (loginSuccess) {
                // Check if credentials have been used before
                String credentials = username + ":" + password;
                if (usedCredentials.contains(credentials)) {
            JOptionPane.showMessageDialog(mainFrame,
                        "These credentials have already been used!\nPlease contact system administrator for new credentials.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            flashErrorTheme();
            shakeFrame();
            return;
        }
        
                // Mark credentials as used
                usedCredentials.add(credentials);
                
                // Get email from database
                String userEmail = DatabaseIntegration.getUserEmail(username);
                
                // Store current email for resending OTP
                currentEmail = userEmail;
                
                // Generate and send OTP
                generatedOTP = generateOTP();
                otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
                
                // Save a fixed copy of the current OTP
                final String fixedOTP = generatedOTP;
                
                // Create and show loading dialog
                JDialog loadingDialog = new JDialog(mainFrame, "Sending OTP", false);
                loadingDialog.setSize(300, 100);
                loadingDialog.setLocationRelativeTo(mainFrame);
                loadingDialog.setLayout(new BorderLayout());
                
                JPanel loadingPanel = new JPanel(new BorderLayout(10, 10));
                loadingPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                loadingPanel.setBackground(Color.WHITE);
                
                JLabel loadingLabel = new JLabel("Sending OTP to your email...");
                loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                
                loadingPanel.add(loadingLabel, BorderLayout.NORTH);
                loadingPanel.add(progressBar, BorderLayout.CENTER);
                
                loadingDialog.add(loadingPanel);
                loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                
                // Use SwingWorker to send OTP in background thread
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Send OTP via email
                        EmailService.sendOTP(userEmail, fixedOTP);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        // Ensure the generatedOTP is still the same as what was sent
                        // This prevents any race conditions
                        generatedOTP = fixedOTP;
                        
                        // Close loading dialog
                        loadingDialog.dispose();
                        
                        // Show success message
                        JOptionPane.showMessageDialog(mainFrame,
                            "OTP has been sent to your email: " + userEmail,
                            "OTP Sent",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Start OTP timer
                        startOtpTimer();
                        
                        // Clear fields before transitioning to OTP panel
                        usernameField.setText("");
                        passwordField.setText("");
                        
                        // Set loginUsed to true to prevent unwanted transitions
                        loginUsed = true;
                        
                        // Transition to OTP panel
                        cardLayout.show(containerPanel, "otp");
                    }
                };
                
                // Execute the worker and show the loading dialog
                worker.execute();
                loadingDialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(mainFrame,
                    "Invalid username or password!\nPlease try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                playErrorFeedback();
                flashErrorTheme();
                shakeFrame();
            }
        } else if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
            // Legacy authentication for backward compatibility
            // Check if credentials have been used before
            String credentials = username + ":" + password;
            if (usedCredentials.contains(credentials)) {
                JOptionPane.showMessageDialog(mainFrame,
                    "These credentials have already been used!\nPlease contact system administrator for new credentials.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                playErrorFeedback();
                flashErrorTheme();
                shakeFrame();
                return;
            }
            
            // Mark credentials as used
            usedCredentials.add(credentials);
            
            // Store email for resending OTP
            currentEmail = VALID_EMAIL;
            
            // Generate and send OTP
            generatedOTP = generateOTP();
            otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
            
            // Save a fixed copy of the current OTP
            final String fixedOTP = generatedOTP;
            
            // Create and show loading dialog
            JDialog loadingDialog = new JDialog(mainFrame, "Sending OTP", false);
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(mainFrame);
            loadingDialog.setLayout(new BorderLayout());
            
            JPanel loadingPanel = new JPanel(new BorderLayout(10, 10));
            loadingPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            loadingPanel.setBackground(Color.WHITE);
            
            JLabel loadingLabel = new JLabel("Sending OTP to your email...");
            loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            
            loadingPanel.add(loadingLabel, BorderLayout.NORTH);
            loadingPanel.add(progressBar, BorderLayout.CENTER);
            
            loadingDialog.add(loadingPanel);
            loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            // Use SwingWorker to send OTP in background thread
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Send OTP via email
                    EmailService.sendOTP(VALID_EMAIL, fixedOTP);
                    return null;
                }
                
                @Override
                protected void done() {
                    // Ensure the generatedOTP is still the same as what was sent
                    // This prevents any race conditions
                    generatedOTP = fixedOTP;
                    
                    // Close loading dialog
                    loadingDialog.dispose();
                    
                    // Show success message
                    JOptionPane.showMessageDialog(mainFrame,
                        "OTP has been sent to your email: " + VALID_EMAIL,
                        "OTP Sent",
                        JOptionPane.INFORMATION_MESSAGE);
            
            // Start OTP timer
            startOtpTimer();
            
            // Clear fields before transitioning to OTP panel
            usernameField.setText("");
            passwordField.setText("");
            
            // Set loginUsed to true to prevent unwanted transitions
            loginUsed = true;
            
            // Transition to OTP panel
            cardLayout.show(containerPanel, "otp");
                }
            };
            
            // Execute the worker and show the loading dialog
            worker.execute();
            loadingDialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(mainFrame,
                "Invalid username or password!\nPlease try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            flashErrorTheme();
            shakeFrame();
        }
    }
    
    // Handle verification of OTP with enhanced recovery and validation
    private void handleOtpVerification() {
        String enteredOTP = otpField.getText().trim();
        boolean isValid = false;
        
        System.out.println("Debug - OTP verification:");
        System.out.println("Entered OTP: '" + enteredOTP + "' (length: " + enteredOTP.length() + ")");
        System.out.println("Expected OTP: '" + generatedOTP + "' (length: " + (generatedOTP != null ? generatedOTP.length() : 0) + ")");
        System.out.println("Current OTP: '" + currentOTP + "' (length: " + (currentOTP != null ? currentOTP.length() : 0) + ")");
        
        // Check for empty OTP first
        if (enteredOTP.isEmpty()) {
            otpStatusLabel.setText("Please enter the OTP sent to your email.");
            playErrorFeedback();
            return;
        }
        
        // Try to recover if generatedOTP is null but currentOTP exists
        if (generatedOTP == null && currentOTP != null) {
            System.out.println("Recovering OTP from backup (currentOTP)");
            generatedOTP = currentOTP;
        }
        
        // Check if OTP has expired
        if (System.currentTimeMillis() > otpExpiryTime) {
            otpStatusLabel.setText("OTP has expired. Please request a new one.");
            playErrorFeedback();
            return;
        }
        
        // Multiple OTP comparison methods to handle various cases
        if (generatedOTP != null) {
            // Method 1: Exact match
            if (enteredOTP.equals(generatedOTP)) {
                isValid = true;
                System.out.println("OTP validation: Exact match successful");
            } 
            // Method 2: Trim and case-insensitive
            else if (enteredOTP.equalsIgnoreCase(generatedOTP.trim())) {
                isValid = true;
                System.out.println("OTP validation: Case-insensitive match successful");
            }
            // Method 3: Ignore non-digits and spaces
            else {
                String cleanedEnteredOTP = enteredOTP.replaceAll("[^0-9]", "");
                String cleanedGeneratedOTP = generatedOTP.replaceAll("[^0-9]", "");
                if (cleanedEnteredOTP.equals(cleanedGeneratedOTP)) {
                    isValid = true;
                    System.out.println("OTP validation: Cleaned match successful");
                }
            }
        }
        
        // Attempt backup verification against currentOTP if main verification failed
        if (!isValid && currentOTP != null && !currentOTP.equals(generatedOTP)) {
            if (enteredOTP.equals(currentOTP)) {
                isValid = true;
                System.out.println("OTP validation: Backup match with currentOTP successful");
            }
        }
        
        if (isValid) {
            otpStatusLabel.setText("OTP verified successfully!");
            otpStatusLabel.setForeground(new Color(144, 238, 144)); // Light green
            
            // Stop the OTP timer
            if (otpTimer != null && otpTimer.isRunning()) {
                otpTimer.stop();
            }
            
            // Reset database lock if it was locked
            // This will allow database access again after OTP verification
            UserDatabaseUI.isDatabaseLocked = false;
            
            // After a short delay, transition to the content panel
            Timer transitionTimer = new Timer(1500, e -> {
                ((Timer) e.getSource()).stop();
                animateTransition(otpPanel, contentPanel);

                // Add database button to content panel
                DatabaseIntegration.addDatabaseButton(contentPanel, this);
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        } else {
            otpStatusLabel.setText("Invalid OTP. Please try again.");
            otpStatusLabel.setForeground(Color.RED);
            playErrorFeedback();
            shakeFrame();
        }
    }
    
    private void resendOTP() {
        // Use stored email instead of getting from the cleared field
        String email = currentEmail;
        
        if (email == null || email.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                "Email address not found! Please go back to login.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            return;
        }
        
        generatedOTP = generateOTP();
        otpExpiryTime = System.currentTimeMillis() + (OTP_VALIDITY_DURATION * 1000);
        
        // Save a fixed copy of the current OTP
        final String fixedOTP = generatedOTP;
        
        // Create and show loading dialog
        JDialog loadingDialog = new JDialog(mainFrame, "Sending OTP", false);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(mainFrame);
        loadingDialog.setLayout(new BorderLayout());
        
        JPanel loadingPanel = new JPanel(new BorderLayout(10, 10));
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        loadingPanel.setBackground(Color.WHITE);
        
        JLabel loadingLabel = new JLabel("Sending new OTP to your email...");
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        loadingPanel.add(loadingLabel, BorderLayout.NORTH);
        loadingPanel.add(progressBar, BorderLayout.CENTER);
        
        loadingDialog.add(loadingPanel);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        // Use SwingWorker to send OTP in background thread
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                EmailService.sendOTP(email, fixedOTP);
                return null;
            }
            
            @Override
            protected void done() {
                // Ensure the generatedOTP is still the same as what was sent
                // This prevents any race conditions
                generatedOTP = fixedOTP;
                
                // Close loading dialog
                loadingDialog.dispose();
                
        startOtpTimer();
        JOptionPane.showMessageDialog(mainFrame,
            "New OTP sent to your email!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        otpField.setText("");
    }
        };
        
        // Execute the worker and show the loading dialog
        worker.execute();
        loadingDialog.setVisible(true);
    }
    
    // Plays an error sound (using a custom error.wav if available), smoothly transitions the login panel from red back to normal, and shakes the frame.
    private void playErrorFeedback() {
        try {
            // Try to load custom error sound from resources
            URL soundURL = getClass().getResource("/resources/error.wav");
            if (soundURL != null) {
                System.out.println("Found error sound at: " + soundURL);
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                System.out.println("Could not find error.wav in resources");
                // Try alternative path
                soundURL = getClass().getResource("/error.wav");
                if (soundURL != null) {
                    System.out.println("Found error sound at alternative path: " + soundURL);
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                } else {
                    System.out.println("Could not find error.wav in any location");
                    // Fallback to system beep if custom sound not found
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        } catch (Exception e) {
            System.out.println("Error playing sound: " + e.getMessage());
            // Fallback to system beep if there's any error
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Smoothly fades the login panel from an error red gradient back to its original colors.
    private void flashErrorTheme() {
        // Store original colors.
        Color origColor1 = loginPanel.getColor1();
        Color origColor2 = loginPanel.getColor2();
        // Define error colors.
        Color errorColor1 = new Color(255, 0, 0);
        Color errorColor2 = new Color(255, 50, 50);
        // Immediately set to error colors.
        loginPanel.setColors(errorColor1, errorColor2);

        // Smoothly interpolate back to the original colors over 1 second (approx. 30 steps).
        int steps = 30;
        Timer transitionTimer = new Timer(30, null);
        final int[] count = {0};
        transitionTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                count[0]++;
                float fraction = (float) count[0] / steps;
                Color newColor1 = interpolateColor(errorColor1, origColor1, fraction);
                Color newColor2 = interpolateColor(errorColor2, origColor2, fraction);
                loginPanel.setColors(newColor1, newColor2);
                if (count[0] >= steps) {
                    transitionTimer.stop();
                }
            }
        });
        transitionTimer.start();
    }

    // Helper method to interpolate between two colors.
    private Color interpolateColor(Color start, Color end, float fraction) {
        int r = (int) (start.getRed() + fraction * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + fraction * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + fraction * (end.getBlue() - start.getBlue()));
        return new Color(r, g, b);
    }

    // Animate transition between two panels using fade-out then fade-in effects.
    private void animateTransition(GradientPanel from, GradientPanel to) {
        Timer fadeOutTimer = new Timer(30, null);
        fadeOutTimer.addActionListener(new ActionListener() {
            float alpha = 1f;
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha -= 0.05f;
                if (alpha <= 0f) {
                    alpha = 0f;
                    from.setAlpha(alpha);
                    fadeOutTimer.stop();
                    cardLayout.show(containerPanel, to == contentPanel ? "content" : "login");
                    Timer fadeInTimer = new Timer(30, null);
                    fadeInTimer.addActionListener(new ActionListener() {
                        float alphaIn = 0f;
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            alphaIn += 0.05f;
                            if (alphaIn >= 1f) {
                                alphaIn = 1f;
                                to.setAlpha(alphaIn);
                                fadeInTimer.stop();
                            } else {
                                to.setAlpha(alphaIn);
                            }
                        }
                    });
                    fadeInTimer.start();
                } else {
                    from.setAlpha(alpha);
                }
            }
        });
        fadeOutTimer.start();
    }

    // Add a simple hover effect to buttons.
    private void addHoverEffect(JButton button, Color normal, Color hover) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });
    }

    // Shake the main frame horizontally for error feedback.
    private void shakeFrame() {
        final Point originalLocation = mainFrame.getLocation();
        final int shakeDistance = 10;
        final int shakeDuration = 10; // number of iterations.
        Timer shakeTimer = new Timer(20, null);
        final int[] count = {0};
        shakeTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int offset = (count[0] % 2 == 0) ? shakeDistance : -shakeDistance;
                mainFrame.setLocation(originalLocation.x + offset, originalLocation.y);
                count[0]++;
                if (count[0] >= shakeDuration) {
                    shakeTimer.stop();
                    mainFrame.setLocation(originalLocation);
                }
            }
        });
        shakeTimer.start();
    }

    // Add cleanup method for timers
    private void cleanupTimers() {
        if (otpTimer != null && otpTimer.isRunning()) {
            otpTimer.stop();
        }
        // Close database connection
        DatabaseIntegration.closeConnection();
    }

    // Handle logout and reset state
    private void handleLogout() {
        cleanupTimers();
        loginUsed = false;
        generatedOTP = null;
        usernameField.setText("");
        passwordField.setText("");
        emailField.setText("");
        statusLabel.setText(" ");
        otpField.setText("");
        otpStatusLabel.setText(" ");
        animateTransition(contentPanel, loginPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OneTimeLoginSystem());
    }
    
    /**
     * Shows just the welcome screen without requiring login
     * This is used when returning from other screens like UserDatabaseUI
     */
    public static void showWelcomeScreen() {
        SwingUtilities.invokeLater(() -> {
            OneTimeLoginSystem system = new OneTimeLoginSystem();
            
            // Skip login and show content panel directly
            // We'll set loginUsed to true to prevent unwanted transitions
            system.loginUsed = true;
            
            // Use the card layout to show the content panel
            system.cardLayout.show(system.containerPanel, "content");
        });
    }
    
    /**
     * Prepares the welcome screen without showing it immediately
     * This helps avoid flickering when transitioning between windows
     */
    public static void prepareWelcomeScreen() {
        // Create the system on the EDT but don't make it visible yet
        SwingUtilities.invokeLater(() -> {
            OneTimeLoginSystem system = new OneTimeLoginSystem();
            
            // Hide the frame initially
            system.mainFrame.setVisible(false);
            
            // Skip login and prepare content panel
            system.loginUsed = true;
            
            // Use the card layout to show the content panel
            system.cardLayout.show(system.containerPanel, "content");
            
            // Schedule making it visible after the current window is disposed
            SwingUtilities.invokeLater(() -> {
                system.mainFrame.setVisible(true);
                system.mainFrame.requestFocus();
            });
        });
    }

    // Public method to generate and send OTP directly
    public static void sendOTPVerification(String username) {
        // Get email from database
        String userEmail = DatabaseIntegration.getUserEmail(username);
        
        // Generate OTP
        String otp = generateStaticOTP();
        
        // Send OTP via email
        EmailService.sendOTP(userEmail, otp);
    }
    
    // Generate a static OTP
    private static String generateStaticOTP() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    // Create an OTP verification dialog with a callback
    public static void createOtpVerificationDialog(String username, OtpVerificationListener listener) {
        // Get email from database
        String userEmail = DatabaseIntegration.getUserEmail(username);
        
        // Create a dialog for OTP verification
        JDialog otpDialog = new JDialog((Frame)null, "OTP Verification", true);
        otpDialog.setSize(450, 350);
        otpDialog.setLocationRelativeTo(null);
        otpDialog.setLayout(new BorderLayout());
        
        // Create a gradient panel
        JPanel gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 153, 102);
                Color color2 = new Color(255, 94, 98);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        gradientPanel.setLayout(new BoxLayout(gradientPanel, BoxLayout.Y_AXIS));
        gradientPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Create title
        JLabel titleLabel = new JLabel("OTP Verification");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        
        // Create instruction
        JLabel instructionLabel = new JLabel("Enter the OTP sent to: " + userEmail);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setForeground(Color.WHITE);
        
        // Create OTP input field
        JPanel otpInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        otpInputPanel.setOpaque(false);
        JTextField otpField = new JTextField(10);
        otpField.setFont(NORMAL_FONT);
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpInputPanel.add(otpField);
        
        // Create timer label
        JLabel timerLabel = new JLabel("Time remaining: 5:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setFont(NORMAL_FONT);
        timerLabel.setForeground(Color.WHITE);
        
        // Create status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(Color.YELLOW);
        
        // Create buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        
        JButton verifyButton = new JButton("Verify OTP");
        verifyButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        verifyButton.setBackground(Color.WHITE);
        verifyButton.setForeground(new Color(76, 175, 80));
        verifyButton.setFocusPainted(false);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setForeground(new Color(239, 108, 0));
        cancelButton.setFocusPainted(false);
        
        buttonPanel.add(verifyButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(cancelButton);
        
        // Add components to panel
        gradientPanel.add(titleLabel);
        gradientPanel.add(Box.createVerticalStrut(20));
        gradientPanel.add(instructionLabel);
        gradientPanel.add(Box.createVerticalStrut(20));
        gradientPanel.add(otpInputPanel);
        gradientPanel.add(Box.createVerticalStrut(10));
        gradientPanel.add(timerLabel);
        gradientPanel.add(Box.createVerticalStrut(20));
        gradientPanel.add(buttonPanel);
        gradientPanel.add(Box.createVerticalStrut(10));
        gradientPanel.add(statusLabel);
        
        // Add panel to dialog
        otpDialog.add(gradientPanel);
        
        // Add verify button action
        verifyButton.addActionListener(e -> {
            // Simple OTP verification for this standalone dialog
            // In a real system, you would use a secure verification process
            String enteredOTP = otpField.getText().trim();
            
            // For testing, we validate against a fixed pattern
            // In production, this should check against the actual OTP
            if (enteredOTP.matches("\\d{6}")) {
                otpDialog.dispose();
                listener.onVerificationComplete(true);
            } else {
                statusLabel.setText("Invalid OTP. Please try again.");
            }
        });
        
        // Add cancel button action
        cancelButton.addActionListener(e -> {
            otpDialog.dispose();
            listener.onVerificationComplete(false);
        });
        
        // Show dialog
        otpDialog.setVisible(true);
    }
}