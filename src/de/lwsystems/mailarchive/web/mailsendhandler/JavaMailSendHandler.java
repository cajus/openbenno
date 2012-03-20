package de.lwsystems.mailarchive.web.mailsendhandler;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

/**
 *
 * @author wiermer
 */
public class JavaMailSendHandler implements MailSendHandler {

    Properties props = new Properties();
    boolean authenticate = false;
    String password;
    String username;

    @Override
    public String toString() {
       String result="";
       if (getHost()!=null) {
           result=result+"Host "+getHost();
       }
              if (getUser()!=null) {
           result=result+"User "+getHost();
       }
              if (getPassword()!=null) {
           result=result+"Password "+getHost();
       }
       return result;
    }

    public String getHost() {
        return props.getProperty("mail.smtp.host");

    }

    public void setHost(String host) {
        if (host != null) {
            props.setProperty("mail.smtp.host", host);
        }

    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null) {
            authenticate = false;
            props.setProperty("mail.smtp.auth", "false");
            return;
        }
        authenticate = true;
        props.setProperty("mail.smtp.auth", "true");
        this.password = password;

    }

    public String getUser() {
        return username;
    }

    public void setUser(String user) {

        this.username = user;
    }

    public JavaMailSendHandler() {
    }

    public void sendMail(String from, String[] to, String[] cc, String[] bcc, InputStream in) throws MailSendFailureException {
        try {
            boolean debug = false;
            Session session;

            if (authenticate) {


                Authenticator auth = new javax.mail.Authenticator() {

                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(getUser(), getPassword());
                    }
                };
                session = Session.getDefaultInstance(props, auth);
            } else {
                session = Session.getDefaultInstance(props, null);
            }

            session.setDebug(debug);
            // create a message
            Message msg = new MimeMessage(session, in);
            // set the from and to address
            if (from != null) {
                InternetAddress addressFrom = new InternetAddress(from);
                msg.setFrom(addressFrom);
            }
            if (to != null) {
                InternetAddress[] addressTo = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++) {
                    addressTo[i] = new InternetAddress(to[i]);
                }
                msg.setRecipients(Message.RecipientType.TO, addressTo);
            }
            if (cc != null) {
                InternetAddress[] addressCc = new InternetAddress[cc.length];
                for (int i = 0; i < cc.length; i++) {
                    addressCc[i] = new InternetAddress(cc[i]);
                }
                msg.setRecipients(Message.RecipientType.CC, addressCc);
            }
            if (bcc != null) {
                InternetAddress[] addressBcc = new InternetAddress[bcc.length];
                for (int i = 0; i < bcc.length; i++) {
                    addressBcc[i] = new InternetAddress(to[i]);
                }
                msg.setRecipients(Message.RecipientType.BCC, addressBcc);
            }

            Transport.send(msg);
        } catch (MessagingException ex) {
            Logger.getLogger(JavaMailSendHandler.class.getName()).log(Level.SEVERE, null, ex);





            throw new MailSendFailureException(ex.toString());
        }
    }

    public boolean isReady() {
        return (getHost() != null && !getHost().equals(""));
    }
}
