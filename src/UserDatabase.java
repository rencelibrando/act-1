import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class UserDatabase {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
    
    // In-memory storage as fallback when database is not available
    private static final Map<String, User> inMemoryUsers = new HashMap<>();
    private static final Map<String, List<LoginRecord>> inMemoryLoginHistory = new HashMap<>();
    private static boolean useInMemoryStorage = false;
    
    // Track the current logged in user
    private static String currentUsername = null;
    
    // Initialize the database with sample users if empty
    static {
        try {
            // Initialize the SQLite database
            DatabaseManager.initialize();
            
            // Check if database connection is available
            if (DatabaseManager.getConnection() == null) {
                System.out.println("Database connection failed. Using in-memory storage as fallback.");
                useInMemoryStorage = true;
                initializeInMemoryUsers();
            } else {
                // Add sample users if the database is empty
                if (DatabaseManager.getAllUsers().isEmpty()) {
                    // Add 10 sample users
                    addUser("gigi", "password123", "gigi@example.com", "Gigi bulaclac");
                    addUser("der", "secure456", "der@example.com", "xander ");
                    addUser("rendell", "pass789", "rendell@example.com", "rendell domala");
                    addUser("ced", "ced2023", "ced@example.com", "ceddiee");
                    addUser("charles", "cha123", "cha@example.com", "charles");
                    addUser("emmanNI", "nigs456", "Nigg@example.com", "emman");
                    addUser("flok", "flo789", "flok@example.com", "florence");
                    addUser("portu", "portu2023", "portu@example.com", "portu michael");
                    addUser("rendell badang", "badang123", "badang@example.com", "badang domalaon");
                    addUser("jaja", "bai456", "jaja@example.com", "jaja bsiaya");
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            System.out.println("Using in-memory storage as fallback.");
            useInMemoryStorage = true;
            initializeInMemoryUsers();
        }
    }
    
    // Initialize in-memory users
    private static void initializeInMemoryUsers() {
        // Add 10 sample users to in-memory storage
        addUser("john.doe", "password123", "john.doe@example.com", "John Doe");
        addUser("jane.smith", "secure456", "jane.smith@example.com", "Jane Smith");
        addUser("robert.johnson", "pass789", "robert.johnson@example.com", "Robert Johnson");
        addUser("emily.brown", "emily2023", "emily.brown@example.com", "Emily Brown");
        addUser("michael.wilson", "mike123", "michael.wilson@example.com", "Michael Wilson");
        addUser("sarah.davis", "sarah456", "sarah.davis@example.com", "Sarah Davis");
        addUser("david.miller", "david789", "david.miller@example.com", "David Miller");
        addUser("lisa.garcia", "lisa2023", "lisa.garcia@example.com", "Lisa Garcia");
        addUser("james.rodriguez", "james123", "james.rodriguez@example.com", "James Rodriguez");
        addUser("patricia.martinez", "patricia456", "patricia.martinez@example.com", "Patricia Martinez");
    }
    
    // User class to store user information
    public static class User {
        private String username;
        private String password;
        private String email;
        private String fullName;
        private Date registrationDate;
        
        public User(String username, String password, String email, String fullName) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.fullName = fullName;
            this.registrationDate = new Date();
        }
        
        public User(String username, String password, String email, String fullName, Date registrationDate) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.fullName = fullName;
            this.registrationDate = registrationDate;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public Date getRegistrationDate() { return registrationDate; }
        
        @Override
        public String toString() {
            return "User{" +
                   "username='" + username + '\'' +
                   ", email='" + email + '\'' +
                   ", fullName='" + fullName + '\'' +
                   ", registrationDate=" + DATE_FORMAT.format(registrationDate) +
                   '}';
        }
    }
    
    // LoginRecord class to store login attempts
    public static class LoginRecord {
        private Date loginTime;
        private boolean successful;
        
        public LoginRecord(Date loginTime, boolean successful) {
            this.loginTime = loginTime;
            this.successful = successful;
        }
        
        public Date getLoginTime() { return loginTime; }
        public boolean isSuccessful() { return successful; }
        
        @Override
        public String toString() {
            return "LoginRecord{" +
                   "loginTime=" + DATE_FORMAT.format(loginTime) +
                   ", successful=" + successful +
                   '}';
        }
    }
    
    // Add a new user to the database
    public static void addUser(String username, String password, String email, String fullName) {
        if (useInMemoryStorage) {
            // Add to in-memory storage
            User user = new User(username, password, email, fullName);
            inMemoryUsers.put(username, user);
        } else {
            // Add to database
            User user = new User(username, password, email, fullName);
            DatabaseManager.insertUser(user);
        }
    }
    
    // Record a login attempt
    public static void recordLogin(String username, boolean successful) {
        if (successful) {
            // Store the current username when successful login
            currentUsername = username;
        }
        
        if (useInMemoryStorage) {
            // Record in in-memory storage
            LoginRecord record = new LoginRecord(new Date(), successful);
            inMemoryLoginHistory.computeIfAbsent(username, k -> new ArrayList<>()).add(record);
        } else {
            // Record in database
            DatabaseManager.recordLogin(username, successful);
        }
    }
    
    // Get all users
    public static List<User> getAllUsers() {
        if (useInMemoryStorage) {
            // Return users from in-memory storage
            return new ArrayList<>(inMemoryUsers.values());
        } else {
            // Return users from database
            return DatabaseManager.getAllUsers();
        }
    }
    
    // Get login history for a specific user
    public static List<LoginRecord> getUserLoginHistory(String username) {
        if (useInMemoryStorage) {
            // Return login history from in-memory storage
            return inMemoryLoginHistory.getOrDefault(username, new ArrayList<>());
        } else {
            // Return login history from database
            return DatabaseManager.getUserLoginHistory(username);
        }
    }
    
    // Find a user by username
    public static User findUserByUsername(String username) {
        if (useInMemoryStorage) {
            // Find user in in-memory storage
            return inMemoryUsers.get(username);
        } else {
            // Find user in database
            return DatabaseManager.findUserByUsername(username);
        }
    }
    
    // Find a user by email
    public static User findUserByEmail(String email) {
        if (useInMemoryStorage) {
            // Find user in in-memory storage
            for (User user : inMemoryUsers.values()) {
                if (user.getEmail().equals(email)) {
                    return user;
                }
            }
            return null;
        } else {
            // Find user in database
            return DatabaseManager.findUserByEmail(email);
        }
    }
    
    // Verify user credentials
    public static boolean verifyCredentials(String username, String password) {
        if (useInMemoryStorage) {
            // Verify credentials in in-memory storage
            User user = inMemoryUsers.get(username);
            return user != null && user.getPassword().equals(password);
        } else {
            // Verify credentials in database
            return DatabaseManager.verifyCredentials(username, password);
        }
    }
    
    // Get user statistics
    public static Map<String, Object> getUserStatistics(String username) {
        if (useInMemoryStorage) {
            // Calculate statistics from in-memory storage
            Map<String, Object> stats = new HashMap<>();
            List<LoginRecord> history = inMemoryLoginHistory.getOrDefault(username, new ArrayList<>());
            
            int totalLogins = history.size();
            int successfulLogins = 0;
            for (LoginRecord record : history) {
                if (record.isSuccessful()) {
                    successfulLogins++;
                }
            }
            
            stats.put("totalLogins", totalLogins);
            stats.put("successfulLogins", successfulLogins);
            stats.put("failedLogins", totalLogins - successfulLogins);
            stats.put("successRate", totalLogins > 0 ? (double) successfulLogins / totalLogins : 0.0);
            
            return stats;
        } else {
            // Get statistics from database
            return DatabaseManager.getUserStatistics(username);
        }
    }
    
    // Delete a user from the database
    public static boolean deleteUser(String username) {
        if (useInMemoryStorage) {
            // Delete user from in-memory storage
            User removed = inMemoryUsers.remove(username);
            inMemoryLoginHistory.remove(username);
            return removed != null;
        } else {
            // Delete user from database
            return DatabaseManager.deleteUser(username);
        }
    }
    
    // Close database connection
    public static void closeConnection() {
        if (!useInMemoryStorage) {
            // Close database connection
            DatabaseManager.closeConnection();
        }
    }
    
    // Get the current username
    public static String getCurrentUsername() {
        return currentUsername;
    }
    
    // Set the current username (used for direct setting without login)
    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }
    
    // Clear the current username (e.g., on logout)
    public static void clearCurrentUsername() {
        currentUsername = null;
    }
} 