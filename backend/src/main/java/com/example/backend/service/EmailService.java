package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String to, String token) {
        logger.info("Tentative d'envoi d'email de réinitialisation à: {}", to);
        String subject = "Réinitialisation de votre mot de passe";
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h1 style='color: #1e3a8a; text-align: center;'>Réinitialisation de mot de passe</h1>"
                + "<p>Bonjour,</p>"
                + "<p>Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le lien ci-dessous :</p>"
                + "<div style='text-align: center; margin: 20px 0;'>"
                + "<a href='" + resetUrl + "' style='background-color: #1e3a8a; color: white; padding: 12px 24px; "
                + "text-decoration: none; border-radius: 4px;'>Réinitialiser mon mot de passe</a>"
                + "</div>"
                + "<p style='color: #666;'>Ce lien expirera dans 1 heure.</p>"
                + "<p style='color: #666;'>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>"
                + "</div>";

        sendHtmlEmail(to, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            logger.info("Envoi de l'email à : {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Email envoyé avec succès à : {}", to);
        } catch (MessagingException e) {
            logger.error("Erreur lors de l'envoi de l'email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réinitialisation: ", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
}
