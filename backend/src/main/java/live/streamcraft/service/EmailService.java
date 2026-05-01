package live.streamcraft.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
	private final JavaMailSender mailSender;
	
	@Value("${app.email.from}")
    private String fromEmail;
	
	@Async
	public void sendActivationEmail(String to, String uname, String emailVerificationCode) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject("Account Activation - StreamCraft");
			message.setText(String.format(
				"Hello %s \n" +
				"Welcome to StreamCraft!. \n\n" +
				"Your email verification code is: %s\n\n" +
                "This code will expire in 1 hour.\n\n" +
                "Enter this code in the app to verify your account.\n\n" +
                "Best regards,\nStreamCraft Team" ,uname, emailVerificationCode));
			mailSender.send(message);
			log.info("Activation email sent to: {}", to);
		}catch(Exception e) {
			log.error("Failed to send verification email to: {}", to, e);
		}
	}
	
	/**
	 * @param to
	 * @param uname
	 * @param emailVerificationCode
	 */
	public void resendActivationEmail(String to, String uname, String emailVerificationCode) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject("Account Activation - StreamCraft");
			message.setText(String.format(
				"Hello %s \n" +
				"Your email verification code is: %s\n\n" +
                "This code will expire in 1 hour.\n\n" +
                "Enter this code in the app to verify your account.\n\n" +
                "Best regards,\nStreamCraft Team" ,uname, emailVerificationCode));
			mailSender.send(message);
			log.info("Activation email sent to: {}", to);
		}catch(Exception e) {
			log.error("Failed to send verification email to: {}", to, e);
		}
	}
	
	@Async
    public void sendPasswordResetEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Password Reset Request - StreamCraft");
            message.setText(String.format(
                "You requested a password reset.\n\n" +
                "Your password reset code is: %s\n\n" +
                "This code will expire in 1 hour.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\nStreamCraft Team",
                code
            ));
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

	
	
	/**
	 * @param to
	 * @param code
	 */
	@Async
    public void resendPasswordResetEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Password Reset Request - StreamCraft");
            message.setText(String.format(
                "You requested a password reset.\n\n" +
                "Your password reset code is: %s\n\n" +
                "This code will expire in 1 hour.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\nStreamCraft Team",
                code
            ));
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

}
