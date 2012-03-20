/*  
 * MailDirWatcherDocumentHandler.java  
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.MessagingException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

/**
 *
 * @author rene
 */
public class MailDirWatcherDocumentHandler extends JavamailDocumentHandler {

    public static long DEFAULT_WAIT_PERIOD = 3000; //how long to wait before the maildir is checked again

    Logger logger = LoggerFactory.getLogger(MailDirWatcherDocumentHandler.class.getName());

    class WatcherThread implements Runnable {

        String path;
        File pathfile;
        long wait;

        public WatcherThread(String path, long wait) throws FileNotFoundException {
            this.path = path;
            this.wait = wait;
            pathfile = new File(path);

            if ((!pathfile.exists()) || (!pathfile.isDirectory())) {
                throw new FileNotFoundException();
            }


        }

        public boolean isLocked(File f) {
            return f.getName().startsWith("#LOCKED#");

        }

        public boolean lock(File f) {
            try {
                return f.renameTo(new File(f.getParentFile(), "#LOCKED#" + f.getName()));
            } catch (Exception ex) {
                System.out.println("Exception while locking " + f);
            }
            return false;
        }

        private void scan(File[] files) {
            for (File f : files) {
                if (f.isDirectory()) {
                    scan(f.listFiles());
                } else {
                    if (!isLocked(f)) {
                        if (!lock(f)) {
                            System.out.println("could not lock " + f + " to " + new File(f.getParentFile(), "#LOCKED#" + f.getName()).getAbsolutePath());
                            continue;
                        }
                        //Stupid Java API. Have to recreate file with new name;
                        f = new File(f.getParentFile(), "#LOCKED#" + f.getName());
                        MessageID mid = null;
                        MetaDocument meta = null;
                        InputStream data = null;
                        try {
                            data = new FileInputStream(f);
                            getArchive().addDocument(data,null,new PostAddRepositoryHandler() {

                                public void handle(MessageID id, MetaDocument d) {

                                }
                            });
                            data.close();
                            
                        } catch (LockObtainFailedException ex) {
                            logger.error("Cannot lock message: {}.",f.getName(),ex);
                        } catch (CorruptIndexException ex) {
                            logger.error("Corrupt index.",ex);
                        } catch (DuplicateException ex) {
                            logger.error("Duplicate message found: {}.",f.getName(),ex);
                        } catch (FileNotFoundException ex) {
                            logger.error("File not found: {}",f.getName(),ex);
                        } catch (IOException ex) {
                            logger.error("IO error: {}",f.getName(),ex);
                        } catch (MessagingException ex) {
                            logger.error("Messaging error on file: {}",f.getName(),ex);
                        }  finally {
                            try {
                                data.close();
                            } catch (IOException ex) {
                                logger.error("IO error when close file: {}",f.getName(),ex);
                            }
                        }
                    }
                }
            }
        }

        public void run() {

            
                while (true) {
                    try {
                    Thread.sleep(wait);
                    scan(pathfile.listFiles());
                       } catch (InterruptedException ex) {
                debug("Stopped execution of Maildir watching");
            }
                }
         

        }
    }

    public Runnable getThread(String path) throws FileNotFoundException {
        return new WatcherThread(path, DEFAULT_WAIT_PERIOD);
    }

    public MailDirWatcherDocumentHandler(String path, Archive archive) {
        super(archive);
                try {
            Thread t = new Thread(getThread(path));
            t.start();
        } catch (FileNotFoundException ex) {
            debug("Directory not found " + path);
            logger.error("Directory not found: \"{}\"",path, ex);
        }
    }
}
