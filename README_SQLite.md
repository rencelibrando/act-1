# SQLite Database Implementation

This project now uses SQLite for persistent data storage. User data and login history are stored in a SQLite database file named `user_database.db`, which is automatically created in the project root directory when the application is first run.

## Setup Instructions

1. Before running the application, execute the `download_sqlite.bat` script to download the SQLite JDBC driver:
   ```
   .\download_sqlite.bat
   ```

2. Build the application using the build script:
   ```
   .\build.bat
   ```

3. Run the application:
   ```
   .\run.bat
   ```

## Database Structure

The SQLite database contains two tables:

### Users Table
Stores user account information:
- `username` (TEXT, PRIMARY KEY): The unique username
- `password` (TEXT): User's password
- `email` (TEXT): User's email address
- `full_name` (TEXT): User's full name
- `registration_date` (TEXT): Date and time when the user registered

### Login History Table
Tracks login attempts:
- `id` (INTEGER, PRIMARY KEY): Auto-incremented ID
- `username` (TEXT): The username used for login
- `login_time` (TEXT): Date and time of the login attempt
- `successful` (INTEGER): 1 if login was successful, 0 if failed

## User Data Persistence

- All user data is now stored permanently in the SQLite database
- When you add, modify, or delete users through the application, changes are immediately saved to the database
- User login history is also recorded in the database
- When the application starts, it checks if the database is empty and populates it with 10 sample users if needed

## Database Location

The database file `user_database.db` is stored in the root directory of the project. If you need to reset the database, simply delete this file. The application will recreate it with sample users the next time it starts.

## Dependencies

The application uses the SQLite JDBC driver, which provides Java database connectivity to SQLite. The driver is automatically downloaded when you run the `download_sqlite.bat` script. 