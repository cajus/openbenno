/*  
 * MboxDocumentHandler.java  
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
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * Importhandler for Mbox format files
 * not in use by anyone
 *
 * @author wiermer
 */

@Deprecated
public class MboxDocumentHandler extends JavamailDocumentHandler {

    Logger logger = LoggerFactory.getLogger(MboxDocumentHandler.class.getName());

    public MboxDocumentHandler(Archive a) {
        super(a);
    }

    public void readFromMbox(String fname) {
        Message[] messages = null;
        try {
            Session session = Session.getDefaultInstance(new Properties());
            Store store = session.getStore(new URLName("mstor:" + fname));
            store.connect();
            Folder inbox = store.getDefaultFolder();
            inbox.open(Folder.READ_ONLY);
            messages = inbox.getMessages();
        } catch (NoSuchProviderException ex) {
            logger.error("Cannot read from mbox {}.",fname,ex);
            System.out.println("JavamailDocumenthandler: No such Provider");
            return;
        } catch (MessagingException ex) {
            logger.error("Messaging exception while reading from mbox {}.",fname,ex);
            System.out.println("Messaging exception while reading from mbox " + fname + ex);
            return;
        }
        for (int i = 0; i < messages.length; i++) {
            try {
                handleMessage(messages[i]);
            } catch (LockObtainFailedException ex) {
                logger.error("Cannot obtain lock on {}.",fname,ex);
            } catch (CorruptIndexException ex) {
                logger.error("Corrupt index.",ex);
            } catch (DuplicateException ex) {
                logger.error("Duplicate message #{} found while reading from {}.",new Object [] {i,fname,ex});
            } catch (IOException ex) {
                logger.error("IO Error while reading message #{} from {}.",new Object [] {i,fname,ex});
            } catch (MessagingException ex) {
                logger.error("Messaging error while reading message #{} from {}.",new Object [] {i,fname,ex});
                System.out.println("Error reading message #" + i);
            }
        }

    }

    //handles a single Javamail message, stores and indexes them. Be aware that the stored message
    //is not guarranted to be byte identical with the original message. Write your own handle-Method,
    //if this can be avoided.
    public void handleMessage(Message message) throws MessagingException, IOException, LockObtainFailedException, CorruptIndexException, DuplicateException {

        getArchive().addDocument(message.getDataHandler().getDataSource().getInputStream(), null, new PostAddRepositoryHandler() {

            public void handle(MessageID id, MetaDocument d) {
            }
        });
    }
}
