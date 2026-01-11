package isa.jutjub.service;

import isa.jutjub.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Base URL for your frontend activation link
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(User user) {
        String token = user.getActivationToken();
        String activationLink = frontendUrl + "/activate?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Activate your account");
        String userName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : user.getUsername();
        message.setText("Hi " + userName + ",\n\n"
                + "Please activate your account by clicking the link below:\n"
                + activationLink + "\n\n"
                + "Thank you!");

        mailSender.send(message);
    }
}
