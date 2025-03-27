import javax.swing.*;
import javax.swing.border.Border;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
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

    public OneTimeLoginSystem() {
        // Set a modern look and feel if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Nimbus not available; default look and feel will be used.
        }

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
            // Remove any non-digit characters
            text = text.replaceAll("[^0-9]", "");
            if (!text.equals(otpField.getText())) {
                otpField.setText(text);
                otpField.setCaretPosition(text.length());
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
        return otp.toString();
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
                timerLabel.setText("OTP Expired!");
                otpStatusLabel.setText("OTP has expired. Please request a new one.");
                generatedOTP = null;
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
        // Create a gradient panel for the content page
        contentPanel = new GradientPanel(new Color(34, 193, 195), new Color(253, 187, 45));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        contentPanel.setAlpha(1f); // fully visible when shown

        JLabel welcomeLabel = new JLabel("Welcome to the System!");
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeLabel.setFont(TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);

        JLabel infoLabel = new JLabel("You have successfully logged in.");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setFont(NORMAL_FONT);
        infoLabel.setForeground(Color.WHITE);

        // Action panel with Logout and Exit buttons
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        JButton logoutButton = new JButton("Logout");
        JButton exitButton = new JButton("Exit");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoutButton.setFocusPainted(false);
        exitButton.setFocusPainted(false);
        logoutButton.setBackground(Color.WHITE);
        logoutButton.setForeground(new Color(34, 193, 195));
        exitButton.setBackground(Color.WHITE);
        exitButton.setForeground(new Color(253, 187, 45));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(logoutButton, Color.WHITE, new Color(230, 230, 230));
        addHoverEffect(exitButton, Color.WHITE, new Color(230, 230, 230));
        actionPanel.add(logoutButton);
        actionPanel.add(Box.createHorizontalStrut(20));
        actionPanel.add(exitButton);

        contentPanel.add(welcomeLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(infoLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(actionPanel);

        // Logout button resets fields and transitions back to the login page
        logoutButton.addActionListener(e -> handleLogout());

        // Exit button with confirmation dialog
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(mainFrame,
                        "Are you sure you want to exit?", "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
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
        
        if (!email.equals(VALID_EMAIL)) {
            JOptionPane.showMessageDialog(mainFrame,
                "Invalid email address!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            flashErrorTheme();
            shakeFrame();
            return;
        }
        
        // Check if credentials are valid
        if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
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
            
            // Generate and send OTP
            generatedOTP = generateOTP();
            otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
            EmailService.sendOTP(VALID_EMAIL, generatedOTP);
            
            // Start OTP timer
            startOtpTimer();
            
            // Clear fields before transitioning to OTP panel
            usernameField.setText("");
            passwordField.setText("");
            
            // Set loginUsed to true to prevent unwanted transitions
            loginUsed = true;
            
            // Transition to OTP panel
            cardLayout.show(containerPanel, "otp");
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
    private void handleOtpVerification() {
        String enteredOTP = otpField.getText().trim();

        if (enteredOTP.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                "Please enter the OTP!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            return;
        }

        if (System.currentTimeMillis() > otpExpiryTime) {
            JOptionPane.showMessageDialog(mainFrame,
                "OTP has expired!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            return;
        }

        if (enteredOTP.equals(generatedOTP)) {
            otpTimer.stop();
            loginUsed = true;
            animateTransition(otpPanel, contentPanel);
        } else {
            JOptionPane.showMessageDialog(mainFrame,
                "Invalid OTP!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
        }
    }
    private void resendOTP() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                "Email address not found!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            playErrorFeedback();
            return;
        }
        
        generatedOTP = generateOTP();
        otpExpiryTime = System.currentTimeMillis() + (OTP_VALIDITY_DURATION * 1000);
        EmailService.sendOTP(email, generatedOTP);
        startOtpTimer();
        JOptionPane.showMessageDialog(mainFrame,
            "New OTP sent to your email!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        otpField.setText("");
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
        // Add any other timers here
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
}

// Custom panel that paints a vertical gradient background with fade support.
class GradientPanel extends JPanel {
    private Color color1;
    private Color color2;
    private float alpha = 1f; // opacity level

    public GradientPanel(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
        setOpaque(false);
    }

    // Getters for original colors.
    public Color getColor1() {
        return color1;
    }
    public Color getColor2() {
        return color2;
    }
    // Allows changing the gradient colors.
    public void setColors(Color c1, Color c2) {
        this.color1 = c1;
        this.color2 = c2;
        repaint();
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        repaint();
    }
    public float getAlpha() {
        return alpha;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        // Apply current opacity.
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        int w = getWidth();
        int h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
        g2d.dispose();
        super.paintComponent(g);
    }
}

// Custom border for rounded (curved) text fields.
class RoundedCornerBorder implements Border {
    private int radius;

    public RoundedCornerBorder(int radius) {
        this.radius = radius;
    }
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
    }
    @Override
    public boolean isBorderOpaque() {
        return false;
    }
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth the border.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
}
