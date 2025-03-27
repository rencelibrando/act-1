import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.AuthenticationFailedException;
import javax.mail.SendFailedException;
import java.util.Properties;

public class EmailService {
    private static final String FROM_EMAIL = "librando.c.bscs@gmail.com"; // Replace with your email
    private static final String EMAIL_PASSWORD = "dfmq xerh mjpv zfmr"; // Your Gmail App Password
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    public static void sendOTP(String toEmail, String otp) {
        Transport transport = null;
        try {
            System.out.println("Attempting to send OTP to: " + toEmail);
            
            // Set up mail server properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.debug", "true"); // Enable debug mode
            props.put("mail.debug.auth", "true"); // Enable authentication debug

            System.out.println("Creating mail session...");
            // Create a session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    System.out.println("Authenticating with email: " + FROM_EMAIL);
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            });

            System.out.println("Creating message...");
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your OTP for Login");
            
            // Create email content
            String emailContent = String.format(
                "Dear User,\n\n" +
                "Your OTP for login is: %s\n\n" +
                "This OTP will expire in 5 minutes.\n\n" +
                "If you didn't request this OTP, please ignore this email.\n\n" +
                "Best regards,\nYour Application Team",
                otp
            );
            
            message.setText(emailContent);

            System.out.println("Sending message...");
            // Send message
            transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, SMTP_PORT, FROM_EMAIL, EMAIL_PASSWORD);
            Transport.send(message);
            System.out.println("OTP sent successfully to " + toEmail);


        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getName());
            if (e instanceof AuthenticationFailedException) {
                System.err.println("Authentication failed. Please check your email and app password.");
            } else if (e instanceof SendFailedException) {
                System.err.println("Failed to send email. Please check the recipient email address.");
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    System.err.println("Error closing transport: " + e.getMessage());
                }
            }
        }
    }
} 