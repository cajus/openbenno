/*  
 * JavamailDocumentHandler.java  
 *   
 * Copyright (C) 2009 LWsystems GmbH & Co. KG  
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU General Public License for more details.  
 *   
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */
package de.lwsystems.mailarchive.parser;

import de.lwsystems.mailarchive.repository.Archive;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.utils.LimitInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Date;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Common superclass for import handlers. Responsible for parsing mails.
 * @author wiermer
 */
public class JavamailDocumentHandler extends DocumentHandler {

    Logger logger = LoggerFactory.getLogger(JavamailDocumentHandler.class.getName());

    public JavamailDocumentHandler(Archive archive) {
       super(archive);
    }

//    public JavamailDocumentHandler(Repository repo, IndexWriter iw) {
//        this.repo = repo;
//        this.indexwriter = iw;
//    }
//
//    public JavamailDocumentHandler(Repository repo, IndexWriter iw, boolean debug) {
//        this(repo, iw);
//        debugoutput = debug;
//    }

    public Archive getArchive() {
        return archive;
    }
    /**
     * Moved to repository.BaseArchive
     * @param mid
     * @return
     * @throws MessagingException
     * @deprecated
     */
    @Deprecated
    public MetaDocument generateMetaDocument(MessageID mid) throws MessagingException   {

        //Suns Javamail code is stupid, and reads the whole stream in for parsing, so that the JVM memory is easily filled, when large mails arrive
        //so we need to prevent indexing the whole chunk. As an educated guess, read up to the size of half of the free memory.

        InputStream is = new LimitInputStream(getArchive().getRepository().getDocument(mid), Runtime.getRuntime().freeMemory() / 2);
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()), is);

        MetaDocument meta = new MetaDocument(MetaDocument.MAIL, mid);
        if (message instanceof MimeMessage) {
            MimeMessage m = (MimeMessage) message;
            meta.setTitle("");
            debug("Inspect message \""+mid+"\"");
            try {
                //Subject
                meta.setTitle(m.getSubject());
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch \"Subject\" header from message \"{}\".",mid, ex);
            }
            //From
            Address[] from=null;
            try {
                from = m.getFrom();
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch \"From\" header from message \"{}\".",mid, ex);
            }
            if (from != null) {
                for (Address a : from) {
                    if (a instanceof InternetAddress) {
                        InternetAddress ia = (InternetAddress) a;
                        logger.debug("handleMessage: adding from address {}.",ia.getAddress());
                        meta.addFrom(ia.getAddress());
                    }
                }
            }
            //To
            Address[] to=null;
            try {
                to = m.getAllRecipients();
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch recipient(s) header from message \"{}\".",mid,ex);
            }
            if (to != null) {
                for (Address a : to) {
                    if (a instanceof InternetAddress) {
                        InternetAddress ia = (InternetAddress) a;
                        logger.debug("handleMessage: adding to address {}",ia.getAddress());
                        meta.addTo(ia.getAddress());
                    }
                }
            }

            //Handle "Delivered-to:" to get BCCs after an mail server got it. Ugly. 
            //At least postfix, qmail and Exchange do this.
            // !> Add additional possible headers
            // !> X-Envelope-To:, Envelope-to:, X-Original-To, X-RCPT-To:
            String deliveredto[]=null;
            try {
                deliveredto = m.getHeader("Delivered-To");
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch \"Delivered-To\" header from message \"{}\".",mid,ex);
            }
            if (deliveredto != null) {
                for (String dt : deliveredto) {
                    try {
                        InternetAddress a = new InternetAddress(dt);
                        logger.debug("handleMessage: adding Deliver-To {}",a.getAddress());
                        meta.addTo(a.getAddress());
                    } catch (javax.mail.internet.AddressException ex) {
                        logger.debug("handleMessage: could not parse deliver-to address, ignoring {}", dt.toString());
                    }
                }
            }

            //Received
            Date d=null;
            try {
                d = m.getReceivedDate();
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch date from message \"{}\".", mid, ex);
            }
            if (d != null) {
                logger.debug("Received: {}",d);
                // _mw_: better at warning level(?)
                meta.setReceived(MetaDocument.getDateFormat().format(d));
            }
            d=null;
            try {
                //Sent
                d = m.getSentDate();
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch sent date from message \"{}\".", mid, ex);
            }
            if (d != null) {
                logger.debug("Sent: {}",d);
                meta.setSent(MetaDocument.getDateFormat().format(d));
            }

            meta.setMultipart(false);
            try {
                //is Multipart?
                meta.setMultipart(m.isMimeType("multipart/*"));
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch multipart header from message \"{}\".", mid, ex);
            }

            try {
                //Headers
                meta.setHeaders(m.getAllHeaders());
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.error("Cannot fetch headers from message \"{}\".", mid, ex);
            }

            String res="";
//            try {
//               // res = extractTextFromPart(m);
//            } catch (MessagingException ex) {
//                // _mw_: better at warning level(?)
//                logger.error("Cannot extract text from message part \"{}\".",m, ex);
//            }

            //Summary
            String summary = res.substring(0, Math.max(Math.min(res.length(), 90), 0)).trim().replace('\n', ' ');
            meta.setSummary(summary);

            //Main Text
            meta.setMainText(new StringReader(res));

            return meta;
        }
        //no mime message;
        logger.debug("No MIME Message!");
        return null;
    }

}
