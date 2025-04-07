# One-Time Login System with User Database

This application provides a secure one-time login system with a user database management feature.

## Features

- One-time login authentication with OTP verification
- User database with 10 sample users
- Login history tracking with date and time information
- User statistics and details viewing
- Search functionality for users

## How to Use

### Login

1. Launch the application
2. Enter your username, password, and email
3. Click "Login" or press Enter
4. If credentials are valid, an OTP will be sent to your email
5. Enter the OTP and click "Verify OTP" or press Enter
6. Upon successful verification, you'll be logged in

### User Database Management

1. After logging in, click the "User Database" button on the main screen
2. The User Database Management System will open with three tabs:
   - **User List**: View all users and search for specific users
   - **User Details**: View detailed information about a selected user
   - **Login History**: View login attempts with date and time information

### Searching for Users

1. In the User List tab, use the search field at the top
2. Select the search type (Username, Email, or Full Name)
3. Enter your search term and press Enter or click "Search"

### Viewing User Details

1. Double-click on a user in the User List tab
2. The User Details tab will show comprehensive information about the user, including:
   - Basic user information
   - Login statistics
   - Recent login history with date and time

## Sample Users

The system comes with 10 sample users:

1. Username: john.doe, Password: password123
2. Username: jane.smith, Password: secure456
3. Username: robert.johnson, Password: pass789
4. Username: emily.brown, Password: emily2023
5. Username: michael.wilson, Password: mike123
6. Username: sarah.davis, Password: sarah456
7. Username: david.miller, Password: david789
8. Username: lisa.garcia, Password: lisa2023
9. Username: james.rodriguez, Password: james123
10. Username: patricia.martinez, Password: patricia456

## Legacy Login

For backward compatibility, the system still supports the legacy login:
- Username: rence
- Password: 12345
- Email: clarencemanlolo@gmail.com

## Technical Details

- The system uses a one-time login mechanism, meaning credentials can only be used once
- Login attempts are recorded with date and time information
- The database is in-memory and will reset when the application is restarted
- Email functionality requires proper configuration in the EmailService class 