/*  
 * SMTPDocumentHandler.java  
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
import de.lwsystems.mailarchive.repository.DuplicateException;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.repository.PostAddRepositoryHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.MessagingException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

/**
 * This class represents a fake SMTP server to receive, store and index mails.
 * @author wiermer
 */
public class SMTPDocumentHandler extends JavamailDocumentHandler implements SimpleMessageListener {

    private int port;
    private SMTPServer server;
    Logger logger = LoggerFactory.getLogger(SMTPDocumentHandler.class.getName());
    
    public SMTPDocumentHandler(Archive archive) {
        super(archive);
        logger.info("SMTP Server initialized at port {}.", port);
        Collection<SimpleMessageListener> listeners = new ArrayList<SimpleMessageListener>(1);
        listeners.add(this);
        this.server = new SMTPServer(new SimpleMessageListenerAdapter(listeners));
        this.server.setPort(25);
        server.setDisableReceivedHeaders(true);
    }

    public void setPort(int port) {
        this.server.setPort(port);
    }

    public void setHostname(String hostname) {
        this.server.setHostName(hostname);
    }

    public void start() {
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }

    public boolean accept(String arg0, String arg1) {
        //accept all incoming mail
        return true;
    }

    public synchronized void deliver(final String from, final String recipient, InputStream data) throws TooMuchDataException, IOException {
        synchronized (this) {
            try {
                //logger.info("Add message ({} -> {}) to archive.",new Object [] {from,recipient});
                getArchive().addDocument(data, null, new PostAddRepositoryHandler() {
                    public void handle(MessageID id, MetaDocument d) {
                        logger.info("Add email from {} to {} as {}.",new Object [] {from,recipient,id});
                        d.addFrom(from);
                        d.addTo(recipient);
                    }
                });
            } catch (DuplicateException ex) {
                logger.warn("Duplicate found: ",ex);
            } catch (MessagingException ex) {
                logger.error("Messaging exceptiton (Parser error ?) while accepting mail: ",ex);
                throw new IOException("Messaging exceptiton (Parser error ?) while accepting mail: " + ex);
            } catch (LockObtainFailedException ex) {
                logger.error("Locking exception while accepting mail: ",ex);
                throw new IOException("Locking exception while accepting mail: " + ex);
            } catch (CorruptIndexException ex) {
                logger.error("Corrupt exceptiton while accepting mail: ",ex);
                throw new IOException("Corrupt exceptiton while accepting mail: " + ex);
            } catch (NullPointerException ex) {
                logger.error("Null pointer exception while accepting mail: ",ex);
                throw new IOException("Null pointer exception while accepting mail: " + ex);
            }
            logger.debug("Message successful added.");
        }
    }
}
    
