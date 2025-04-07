import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages SQLite database connections and operations
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:user_database.db";
    private static Connection connection;
    
    /**
     * Initialize database and create tables if they don't exist
     */
    public static void initialize() {
        try {
            // Try to load the SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("SQLite JDBC driver not found: " + e.getMessage());
                System.err.println("Please make sure the SQLite JDBC driver is in the classpath.");
                return;
            }
            
            // Check if we need to reset the database (for debugging)
            boolean resetDatabase = true;
            
            if (resetDatabase) {
                System.out.println("Resetting the database...");
                // Delete the database file
                java.io.File dbFile = new java.io.File("user_database.db");
                if (dbFile.exists()) {
                    if (dbFile.delete()) {
                        System.out.println("Database file deleted.");
                    } else {
                        System.out.println("Failed to delete database file.");
                    }
                }
            }
            
            // Create a connection to the database
            connection = DriverManager.getConnection(DB_URL);
            
            System.out.println("Connected to SQLite database");
            
            // Create tables if they don't exist
            createTables();
            
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Close the database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create database tables if they don't exist
     */
    private static void createTables() throws SQLException {
        Statement statement = connection.createStatement();
        
        // Enable foreign key constraints
        statement.execute("PRAGMA foreign_keys = ON");
        
        // Create users table
        statement.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "username TEXT PRIMARY KEY, " +
            "password TEXT NOT NULL, " +
            "email TEXT NOT NULL, " +
            "full_name TEXT NOT NULL, " +
            "registration_date TEXT NOT NULL" +
            ")"
        );
        
        // Create login_history table
        statement.execute(
            "CREATE TABLE IF NOT EXISTS login_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL, " +
            "login_time TEXT NOT NULL, " +
            "successful INTEGER NOT NULL, " +
            "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" +
            ")"
        );
        
        statement.close();
    }
    
    /**
     * Get database connection
     */
    public static Connection getConnection() {
        return connection;
    }
    
    /**
     * Insert a new user into the database
     */
    public static boolean insertUser(UserDatabase.User user) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password, email, full_name, registration_date) VALUES (?, ?, ?, ?, ?)"
            );
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getFullName());
            statement.setString(5, new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(user.getRegistrationDate()));
            
            int rowsInserted = statement.executeUpdate();
            statement.close();
            
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a user and their login history from the database
     */
    public static boolean deleteUser(String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM users WHERE username = ?"
            );
            statement.setString(1, username);
            
            int rowsDeleted = statement.executeUpdate();
            statement.close();
            
            // Delete login history (foreign key constraint should handle this automatically)
            PreparedStatement historyStatement = connection.prepareStatement(
                "DELETE FROM login_history WHERE username = ?"
            );
            historyStatement.setString(1, username);
            historyStatement.executeUpdate();
            historyStatement.close();
            
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Find a user by username
     */
    public static UserDatabase.User findUserByUsername(String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?"
            );
            statement.setString(1, username);
            
            ResultSet resultSet = statement.executeQuery();
            UserDatabase.User user = null;
            
            if (resultSet.next()) {
                String password = resultSet.getString("password");
                String email = resultSet.getString("email");
                String fullName = resultSet.getString("full_name");
                String registrationDateStr = resultSet.getString("registration_date");
                Date registrationDate = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").parse(registrationDateStr);
                
                user = new UserDatabase.User(username, password, email, fullName, registrationDate);
            }
            
            resultSet.close();
            statement.close();
            
            return user;
        } catch (Exception e) {
            System.err.println("Error finding user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Find a user by email
     */
    public static UserDatabase.User findUserByEmail(String email) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM users WHERE email = ?"
            );
            statement.setString(1, email);
            
            ResultSet resultSet = statement.executeQuery();
            UserDatabase.User user = null;
            
            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String fullName = resultSet.getString("full_name");
                String registrationDateStr = resultSet.getString("registration_date");
                Date registrationDate = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").parse(registrationDateStr);
                
                user = new UserDatabase.User(username, password, email, fullName, registrationDate);
            }
            
            resultSet.close();
            statement.close();
            
            return user;
        } catch (Exception e) {
            System.err.println("Error finding user by email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get all users from the database
     */
    public static List<UserDatabase.User> getAllUsers() {
        List<UserDatabase.User> users = new ArrayList<>();
        
        try {
            System.out.println("Retrieving all users from database...");
            
            if (connection == null || connection.isClosed()) {
                System.out.println("Database connection is null or closed. Reconnecting...");
                initialize();
                
                if (connection == null) {
                    System.out.println("Failed to reconnect to database.");
                    return users;
                }
            }
            
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users");
            
            int userCount = 0;
            while (resultSet.next()) {
                userCount++;
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String email = resultSet.getString("email");
                String fullName = resultSet.getString("full_name");
                String registrationDateStr = resultSet.getString("registration_date");
                
                System.out.println("Found user: " + username);
                
                try {
                    Date registrationDate = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").parse(registrationDateStr);
                    UserDatabase.User user = new UserDatabase.User(username, password, email, fullName, registrationDate);
                    users.add(user);
                } catch (java.text.ParseException e) {
                    System.err.println("Error parsing date for user " + username + ": " + e.getMessage());
                    // Try alternate format for backward compatibility
                    try {
                        Date registrationDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(registrationDateStr);
                        UserDatabase.User user = new UserDatabase.User(username, password, email, fullName, registrationDate);
                        users.add(user);
                    } catch (java.text.ParseException ex) {
                        System.err.println("Error parsing date with alternate format: " + ex.getMessage());
                    }
                }
            }
            
            System.out.println("Retrieved " + userCount + " users from database.");
            
            resultSet.close();
            statement.close();
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    /**
     * Record a login attempt
     */
    public static void recordLogin(String username, boolean successful) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO login_history (username, login_time, successful) VALUES (?, ?, ?)"
            );
            statement.setString(1, username);
            statement.setString(2, new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()));
            statement.setInt(3, successful ? 1 : 0);
            
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error recording login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get login history for a specific user
     */
    public static List<UserDatabase.LoginRecord> getUserLoginHistory(String username) {
        List<UserDatabase.LoginRecord> history = new ArrayList<>();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM login_history WHERE username = ? ORDER BY login_time DESC"
            );
            statement.setString(1, username);
            
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                String loginTimeStr = resultSet.getString("login_time");
                Date loginTime = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").parse(loginTimeStr);
                boolean successful = resultSet.getInt("successful") == 1;
                
                UserDatabase.LoginRecord record = new UserDatabase.LoginRecord(loginTime, successful);
                history.add(record);
            }
            
            resultSet.close();
            statement.close();
        } catch (Exception e) {
            System.err.println("Error getting login history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return history;
    }
    
    /**
     * Get user statistics
     */
    public static Map<String, Object> getUserStatistics(String username) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Check if the user exists
            UserDatabase.User user = findUserByUsername(username);
            if (user == null) {
                System.out.println("User not found for statistics: " + username);
                return stats;
            }
            
            // Add user details to stats
            stats.put("username", user.getUsername());
            stats.put("fullName", user.getFullName());
            stats.put("email", user.getEmail());
            stats.put("registrationDate", new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(user.getRegistrationDate()));
            
            // Get login history
            List<UserDatabase.LoginRecord> history = getUserLoginHistory(username);
            
            int totalLogins = history.size();
            int successfulLogins = 0;
            for (UserDatabase.LoginRecord record : history) {
                if (record.isSuccessful()) {
                    successfulLogins++;
                }
            }
            
            stats.put("totalLogins", totalLogins);
            stats.put("successfulLogins", successfulLogins);
            stats.put("failedLogins", totalLogins - successfulLogins);
            stats.put("successRate", totalLogins > 0 ? (double) successfulLogins / totalLogins : 0.0);
            
            System.out.println("Retrieved statistics for user: " + username);
        } catch (Exception e) {
            System.err.println("Error getting user statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Check if a user exists (by username)
     */
    public static boolean userExists(String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ?"
            );
            statement.setString(1, username);
            
            ResultSet resultSet = statement.executeQuery();
            int count = resultSet.getInt(1);
            
            resultSet.close();
            statement.close();
            
            return count > 0;
        } catch (SQLException e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verify user credentials
     */
    public static boolean verifyCredentials(String username, String password) {
        try {
            UserDatabase.User user = findUserByUsername(username);
            if (user != null) {
                return user.getPassword().equals(password);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying credentials: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Login record class
     */
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
                   "loginTime=" + new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(loginTime) +
                   ", successful=" + successful +
                   '}';
        }
    }
} 