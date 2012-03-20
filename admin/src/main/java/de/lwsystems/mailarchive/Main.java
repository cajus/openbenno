/*  
 * Main.java  
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
package de.lwsystems.mailarchive;

import com.sun.mail.pop3.POP3Message;
import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.mailarchive.repository.Archive;
import de.lwsystems.mailarchive.repository.BaseArchive;
import de.lwsystems.mailarchive.repository.DuplicateException;
import de.lwsystems.mailarchive.repository.FilesystemRepository;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.repository.PostAddRepositoryHandler;
import de.lwsystems.mailarchive.repository.SingleIndexArchive;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocsCollector;
import org.bouncycastle.jce.provider.symmetric.Grain128.Base;

/**
 *
 * @author rene
 */
public class Main {

    static Archive a = null;
    private String[] defaultfields = {"from", "to", "text", "title"};

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        int errorCode = 0;
        try {
            //Archive arch=new SingleIndexArchive("/tmp/repo","/tmp/index",false);
            doMain(args);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            errorCode = -1;
        } finally {
            if (a != null) {
                a.close();
            }
            System.exit(errorCode);
        }

    }

    public static void printUsage() {
        System.out.println("BennoAdmin " + de.lwsystems.mailarchive.Main.class.getPackage().getImplementationVersion());
        String msg = "Usage:\n"
                + "add [REPODIR INDEXDIR]: adds a single mail from standard input to the repository and index. Additional recipients are stored in the BENNO_RECIPIENTS variable.\n"
                + "add-dir DIRECTORY [REPODIR INDEXDIR]: adds all mails in a directory and its subdirectories. The $MAILFILENAME.recipients contains additional recipients, one per line.\n"
                + "add-zip ZIPFILE [REPODIR INDEXDIR]: adds a collection of mails in a zip file\n"
                + "rebuild-index [REPODIR INDEXDIR]: rebuilds the index (can take a LONG time)\n"
                + "update-index [REPODIR INDEXDIR]: add missing messages to index\n"
                + "query QUERY [INDEXDIR]: searches the index\n"
                + "check-integrity [REPODIR]: checks, if all messages are unmodified\n"
                + "check-consistency [REPODIR INDEXDIR]: checks, if all messages from the repository are in the index\n"
                + "optimize [INDEXDIR]: optimizes index, if necessary\n"
                + "checkandroll [REPODIR]: prints out a checksum and start a new, chained log\n"
                + "checksum [REPODIR]: prints a checksum of the current log\n"
                + "pop3 HOST USERNAME PASSWORD [REPODIR INDEXDIR]: POP3 client \n"
                + "pop3s HOST USERNAME PASSWORD [REPODIR INDEXDIR]: POP3s client \n"
                + "\nINDEXDIR and REPODIR are optional. They default paths can be configured in /etc/benno/archive.properties (see example in the scripts directory)\n";
        System.out.println(msg);
    }

    public static void doMain(String[] args) throws Exception {
        String repodir = null;
        String indexdir = null;
        if (args.length < 1) {
            printUsage();
            System.exit(0);
        }

        File propsFile = new File("/etc/benno/archive.properties");
        Properties props = new Properties();
        if (propsFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(propsFile);
                props.load(fis);
                fis.close();
                indexdir = props.getProperty("archive.indexDir");
                repodir = props.getProperty("archive.repoDir");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (repodir == null) {
            repodir = "/srv/mailarchive/repo";
        }
        if (indexdir == null) {
            indexdir = "/srv/mailarchive/index";
        }

        String[] writeArchiveCommands = {"rebuild-index", "update-index", "add", "add-zip", "pop3", "pop3s", "add-dir"};
        String[] writeIndexCommands = {"optimize"};
        String[] writeRepositoryCommands = {"checkandroll"};
        String[] readCommands = {"query", "checksum", "check-consistency", "check-integrity"};

        String zipfile = "";
        String host = "";
        String username = "";
        String password = "";
        String adddirectory = "";

        int i;
        if ((i = inIndex(args[0], writeArchiveCommands)) >= 0) {

            if (args.length > 2) {

                int offset = 0;
                if (i == 3) //add-zip
                {
                    zipfile = args[1];
                    offset = 1;
                }
                if (i == 4 || i == 5) {//pop3(s)
                    host = args[1];
                    username = args[2];
                    password = args[3];
                    offset = 3;

                }
                if (i == 6) { //add-dir
                    adddirectory = args[1];
                    offset = 1;
                }
                if (args.length > offset + 2) {
                    repodir = args[offset + 1];
                    indexdir = args[offset + 2];
                }
            }

            try {
                a = createArchive(repodir, indexdir, false);
                String protocol = "pop3s";
                switch (i) {
                    case 0: { //rebuildindex

                        System.out.println("Rebuilding index..");
                        if (a.rebuildIndex()) {
                            System.out.println("Successfully rebuild index");
                        } else {
                            System.err.println("Failed to rebuild index");
                            throw new IOException("Failed to build index");
                        }

                    }
                    ;
                    break;
                    case 1: { //update-index
                        System.out.println("Updating index..");
                        if (a.updateIndex()) {
                            System.out.println("Successfully updated index");
                        } else {
                            System.err.println("Failed to update index");
                            throw new IOException("Failed to update index");
                        }
                    }
                    break;
                    case 2: { //add
                        System.out.println("Adding..");

                        String recipients = System.getenv("BENNO_RECIPIENTS");
                        String[] recipientsArray = null;
                        if (recipients != null) {
                            recipientsArray = recipients.split("\n");
                        }

                        try {
                            a.addDocument(System.in, recipientsArray, new PostAddRepositoryHandler() {

                                public void handle(MessageID id, MetaDocument d) {
                                }
                            });

                        } catch (MessagingException ex) {
                            System.err.println("Messaging error: " + ex);
                        }

                    }
                    break;


                    case 3:  //add-zip
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(zipfile);
                            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                            ZipEntry entry;


                            while ((entry = zis.getNextEntry()) != null) {
                                System.out.println("Extracting: " + entry);


                                if (!entry.isDirectory()) {

                                    try {

                                        a.addDocument(zis, null, new PostAddRepositoryHandler() {

                                            public void handle(MessageID id, MetaDocument d) {
                                            }
                                        });
                                    } catch (MessagingException ex) {
                                        System.err.println("Messaging error: " + ex);
                                    } catch (DuplicateException ex) {
                                        System.err.println("Found a duplicate !");
                                    }
                                }
                            }
                        } catch (FileNotFoundException ex) {
                            System.err.println("Zip file not found");
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.err.println("I/O exception");
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            try {
                                fis.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        break;
                    case 4: //pop3
                        pop3("pop3", host, username, password, a);
                        break;
                    case 5:  //pop3s - keep default
                        pop3("pop3s", host, username, password, a);
                        break;

                    case 6: //add-dir
                        adddir(adddirectory, a);

                }
            } catch (IOException ex) {
                System.err.println("Could not open a writable archive");
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }


        } else if ((i = inIndex(args[0], writeIndexCommands)) >= 0) {
            if (args.length > 1) {
                indexdir = args[1];
            }
            switch (i) {
                case 0: //optimize

                    try {
                        System.out.println("Optimize ");
                        a = createArchive(repodir, indexdir, false);
                        a.optimize();
                    } catch (IOException ex) {
                        System.out.println(ex);
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

            }
        } else if ((i = inIndex(args[0], writeRepositoryCommands)) >= 0) {
            try {
                if (args.length > 2) {
                    repodir = args[1];
                    indexdir = null;
                }
                switch (i) {
                    case 0:
                        // checkandroll
                        System.out.println("Checkroll ");
                        a = createArchive(repodir, indexdir, false);
                        a.getRepository().getLogWriter().checkAndRoll();
                }
            } catch (IOException ex) {
                System.err.println("Could not open a writable repository");
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if ((i = inIndex(args[0], readCommands)) >= 0) {
            //initalize archive
            String querystring = "*";
            if (args.length > 1) {
                if (i == 0) { //query
                    querystring = args[1];
                    if (args.length > 2) {
                        indexdir = args[2];
                    }
                }
                if (i == 1 || i == 3) {//checksum or check-integrity
                    repodir = args[1];
                    indexdir = null;

                }
                if (i == 2) {
                    repodir = args[1];
                    if (args.length > 2) {
                        indexdir = args[2];
                    }
                }
            }
            try {
                a = createArchive(repodir, indexdir, true);

                switch (i) {
                    case 0:  //query
                        query(querystring, a);
                        break;
                    case 1:  //checksum
                        System.out.println(a.getRepository().getLogReader().getChecksum());
                        break;
                    case 2:  //check-consistency
                        if (a.checkConsistency()) {
                            System.out.println("Archive is consistent");
                            return;
                        } else {
                            System.err.println("Archive is NOT consistent");
                            throw new Exception("Archive is NOT consistent");
                        }

                    case 3:  //check-integrity
                        if (a.checkIntegrity()) {
                            System.out.println("Archive integrity check passed.");
                            return;
                        } else {
                            System.err.println("Archive integrity check FAILED.");
                            System.exit(-1);
                        }
                        ;
                        break;

                }
            } catch (IOException ex) {
                System.out.println(ex);
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else { //print syntax help
            printUsage();
        }


    }

    private static void query(String querystring, Archive a) {

        TopDocsCollector<ScoreDoc> h = a.query(querystring);
        ScoreDoc[] docs = h.topDocs().scoreDocs;


        long count = 1;


        try {
            for (ScoreDoc sdoc : docs) {
                //Hit hit = (Hit) it.next();
                //Document doc = hit.getDocument();
                Document doc = a.getIndexSearcher().doc(sdoc.doc);
                String id = doc.getField("id").stringValue();


                if (id == null) {
                    id = "";


                }
                String title = "";
                try {
                    title = doc.getField("title").stringValue();
                } catch (NullPointerException ex) {
                    //non-existent field;
                }
                System.out.println("" + count + " " + sdoc.score + " " + id + " " + title);
                count++;

            }


        } catch (IOException ex) {
            System.err.println("I/O error while accessing index:" + ex);
            System.exit(-1);


        }

    }

    private static void pop3(String provider, String host, String username, String password, Archive a) {
        try {
            Properties props = new Properties();
            // Connect to the POP3 server
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore(provider.toLowerCase());
            System.out.println("Connect to server..");
            store.connect(host, username, password);
            // Open the folder
            System.out.println("Open inbox..");
            Folder inbox = store.getFolder("INBOX");


            if (inbox == null) {
                System.out.println("No INBOX");
                System.exit(1);


            }
            inbox.open(Folder.READ_WRITE);

            // Get the messages from the server
            System.out.println("Get messages (total " + inbox.getMessageCount() + ")..");
            Message[] messages = inbox.getMessages();


            for (int i = 0; i
                    < messages.length; i++) {
                System.out.println(" Message " + (i + 1) + "/" + messages.length + "\r");
                File tmpFile = File.createTempFile("pop3", ".eml");
                tmpFile.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tmpFile);
                messages[i].writeTo(new FileOutputStream(tmpFile));
                fos.close();

                System.gc();
                FileInputStream fis = new FileInputStream(tmpFile);
                try {
                    a.addDocument(fis, null, new PostAddRepositoryHandler() {

                        public void handle(MessageID id, MetaDocument d) {
                        }
                    });
                } catch (DuplicateException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                fis.close();
                if (messages[i] instanceof POP3Message) {
                    ((POP3Message) messages[i]).invalidate(true);
                }
                messages[i].setFlag(Flags.Flag.DELETED, true);
            }
            // Close the connection
            //inbox.delete(true);
            inbox.close(true);
            store.close();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (MessagingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }


    }

    static Archive createArchive(String repodir, String indexdir, boolean readOnly) throws IOException {
        //TODO add autodetection for archive format
        // at the moment assume old format
        System.out.println("Access archive " + repodir + " " + indexdir + " ");
        
        Archive arch = new SingleIndexArchive(indexdir, repodir, readOnly);
        //Archive arch= new ContainerArchive(indexdir, repodir, readOnly);
        //Archive arch=BaseArchive.autodetectArchive(repodir, indexdir, readOnly);
        return arch;
    }

    private static void adddir(String adddirectory, Archive a) {
        try {
            File addDir = new File(adddirectory);
            if (!addDir.exists() && !addDir.isDirectory() && !addDir.canRead()) {
                System.err.println("Error: " + adddirectory + " is not a valid and readable directory");
            }
            List<File> mailFiles = getMailFilesRecursively(addDir);
            for (File f : mailFiles) {
                File fRecipients = new File(f.getAbsolutePath() + ".recipients");
                String[] recipients = null;
                BufferedReader br;
                Logger.getLogger(Main.class.getName()).log(Level.FINER,"Adding "+f.getName());
                Logger.getLogger(Main.class.getName()).log(Level.FINEST,"Memory Total: "+Runtime.getRuntime().maxMemory()+" Memory Free: "+Runtime.getRuntime().freeMemory());
                try {
                    br = new BufferedReader(new FileReader(fRecipients));
                    String s;
                    ArrayList<String> recs = new ArrayList<String>();
                    while ((s = br.readLine()) != null) {
                        recs.add(s);
                        //DEBUG
                        System.out.println("Found recipient " + s);
                    }
                    recipients = recs.toArray(new String[1]);
                } catch (FileNotFoundException ex) {
                    //silently ignore missing .recipients file (is optional)
                } catch (IOException exec) {
                    Logger.getLogger(FilesystemRepository.class.getName()).log(Level.SEVERE, null, exec);
                }


                InputStream fis = new FileInputStream(f);

                try {
                    fis = new GZIPInputStream(fis);
                } catch (IOException ex) {
                    //try to read as GZIP, if not ignore and read plain text
                }

                try {
                    if (recipients != null) {
                        System.out.println("Adding " + f.getName() + " with " + recipients.length + " additional recipients");
                    } else {
                        System.out.println("Adding " + f.getName());
                    }
                    a.addDocument(fis, recipients, new PostAddRepositoryHandler() {

                        public void handle(MessageID id, MetaDocument d) {
                            //intentionally left blank
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MessagingException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch (DuplicateException ex) {
                }


            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }



    }

    private static List<File> getMailFilesRecursively(File startingDir) throws FileNotFoundException {
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Collecting Mails from "+startingDir);
        List<File> result = new LinkedList<File>();
        File[] filesAndDirs = startingDir.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        for (File file : filesDirs) {
            if (file.isFile() && !file.getName().endsWith(".recipients")) {
                result.add(file);
            }
            if (file.isDirectory()) {
                List<File> fromBelow = getMailFilesRecursively(file);
                result.addAll(fromBelow);
            }
        }
        return result;
    }

    static int inIndex(String string, String[] writeArchiveCommands) {
        int index = 0;


        for (String s : writeArchiveCommands) {
            if (string.equalsIgnoreCase(s)) {
                return index;
            }
            index++;

        }


        return -1;

    }
}
