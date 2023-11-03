package eu.einfracentral.service;

import eu.einfracentral.registry.service.MailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.stream.Collectors;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SimpleMailService implements MailService {

    private static final Logger logger = LogManager.getLogger(SimpleMailService.class);
    private Session session;

    @Value("${mail.smtp.auth}")
    String auth;

    @Value("${mail.smtp.host}")
    String host;

    @Value("${mail.smtp.from}")
    String from;

    @Value("${mail.smtp.user}")
    String user;

    @Value("${mail.smtp.password}")
    String password;

    @Value("${mail.smtp.protocol}")
    String protocol;

    @Value("${mail.smtp.port}")
    String port;

    @Value("${mail.smtp.ssl.enable}")
    String ssl;

    @Value("${emails.send:true}")
    boolean enableEmails;

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.transport.protocol", protocol);
        sessionProps.setProperty("mail.smtp.auth", auth);
        sessionProps.setProperty("mail.smtp.host", host);
        sessionProps.setProperty("mail.smtp.password", password);
        sessionProps.setProperty("mail.smtp.port", port);
        sessionProps.setProperty("mail.smtp.ssl.enable", ssl);
        sessionProps.setProperty("mail.smtp.user", user);
        sessionProps.setProperty("mail.smtp.from", from);
        session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Async
    @Override
    public void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException {
        sendMail(to, cc, Collections.singletonList(from), subject, text);
    }

    @Override
    public void sendMail(List<String> to, List<String> cc, List<String> bcc, String subject, String text) throws MessagingException {
        if (enableEmails) {
            Transport transport = null;
            MimeMessage message;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(from);
                message = new MimeMessage(session);
                message.setFrom(sender);
                if (to != null) {
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                }
                if (cc != null) {
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                }
                if (bcc != null) {
                    message.setRecipients(Message.RecipientType.BCC, createAddresses(bcc));
                }
                message.setSubject(subject);

                message.setText(text, "utf-8", "html");
                message.saveChanges();

                transport.connect();
                sendMessage(message, to, cc, bcc);
            } catch (MessagingException e) {
                logger.error("ERROR", e);
            } finally {
                if (transport != null) {
                    transport.close();
                }
            }
        }
    }

    void sendMessage(Message message, List<String> to, List<String> cc, List<String> bcc) throws MessagingException {
        boolean sent = false;
        int attempts = 0;
        while (!sent && attempts < 20) {
            try {
                attempts++;
                Transport.send(message);
                sent = true;
            } catch (SendFailedException e) {
                if (e.getInvalidAddresses().length > 0) {
                    logger.warn("Send mail failed. Attempting to remove invalid address");
                    for (int i = 0; i < e.getInvalidAddresses().length; i++) {
                        Address invalidAddress = e.getInvalidAddresses()[i];
                        logger.debug("Invalid e-mail address: {}", invalidAddress);
                        to.remove(invalidAddress.toString());
                        cc.remove(invalidAddress.toString());
                        bcc.remove(invalidAddress.toString());
                    }
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                    message.setRecipients(Message.RecipientType.BCC, createAddresses(bcc));
                } else {
                    logger.error(e);
                }
            }
        }
        if (!sent) {
            logger.error("Send Message Aborted...\nTo: {}\nCC: {}\nBCC: {}",
                    String.join(", ", to), String.join(", ", cc), String.join(", ", bcc));
        }
    }

    @Override
    public void sendMail(List<String> to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    @Override
    public void sendMail(String to, String cc, String subject, String text) throws MessagingException {
        List<String> addrTo = new ArrayList<>();
        List<String> addrCc = new ArrayList<>();
        if (to != null) {
            addrTo.addAll(Arrays.stream(to.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (cc != null) {
            addrTo.addAll(Arrays.stream(cc.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        sendMail(addrTo, addrCc, subject, text);
    }

    @Override
    public void sendMail(String to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    private InternetAddress[] createAddresses(List<String> emailAddresses) {
        List<InternetAddress> addresses = new ArrayList<>();
        for (int i = 0; i < emailAddresses.size(); i++) {
            try {
                addresses.add(new InternetAddress(emailAddresses.get(i)));
            } catch (AddressException e) {
                logger.warn(e.getMessage());
            }
        }
        return addresses.toArray(new InternetAddress[0]);
    }
}
