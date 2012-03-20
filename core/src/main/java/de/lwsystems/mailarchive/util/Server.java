/*  
 * Server.java  
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
package de.lwsystems.mailarchive.util;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import de.lwsystems.mailarchive.parser.MailDirWatcherDocumentHandler;
import de.lwsystems.mailarchive.parser.SMTPDocumentHandler;
import de.lwsystems.mailarchive.repository.Archive;
import de.lwsystems.mailarchive.repository.SingleIndexArchive;
import de.lwsystems.mailarchive.repository.container.ContainerArchive;
import java.io.File;
import java.net.URL;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 *
 * @author wiermer
 */
public class Server {

    static SMTPDocumentHandler smtpserver;
    static MailDirWatcherDocumentHandler maildirserver;
    static Archive archive;
    //static Repository repo;
    //static IndexWriter iw = null;
    static Logger logger = Logger.getLogger(Server.class.getName());

    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws JSAPException, Exception {
        init(args);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                    if (smtpserver != null) {
                        smtpserver.stop();
                    }
                    archive.close();
            }
        });
        start();
    }

    public static void init(String[] args) throws Exception {

        JSAP parser = new JSAP();
        FlaggedOption portOpt = new FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setDefault("25").
                setRequired(true).
                setShortFlag('p').
                setLongFlag(JSAP.NO_LONGFLAG);
        FlaggedOption maildirOpt = new FlaggedOption("maildir").setStringParser(JSAP.STRING_PARSER).setDefault("").setRequired(true).setShortFlag('d').setLongFlag(JSAP.NO_LONGFLAG);
        FlaggedOption hostOpt = new FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setDefault("localhost").setRequired(true).setShortFlag('h').setLongFlag(JSAP.NO_LONGFLAG);
        FlaggedOption repoOpt = new FlaggedOption("repo").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('r').setLongFlag(JSAP.NO_LONGFLAG);
        FlaggedOption indexOpt = new FlaggedOption("index").setStringParser(JSAP.STRING_PARSER).setDefault("").setRequired(true).setShortFlag('i').setLongFlag(JSAP.NO_LONGFLAG);
        Switch containerOpt = new Switch("container-repository").setShortFlag('c').setDefault("false");
        parser.registerParameter(portOpt);
        parser.registerParameter(maildirOpt);
        parser.registerParameter(hostOpt);
        parser.registerParameter(repoOpt);
        parser.registerParameter(indexOpt);
        parser.registerParameter(containerOpt);
        JSAPResult config = parser.parse(args);

        String logConfigName="log4j.xml";
        //try reading configfile from system property
        if (System.getProperty("log4j.configuration") != null) {
            logConfigName=System.getProperty("log4j.configuration");
            DOMConfigurator.configureAndWatch( logConfigName, 60*1000 );
        }
        else if (new File("/etc/benno/bennocore-log4j.xml").exists()) {
            logConfigName="/etc/benno/bennocore-log4j.xml";
            DOMConfigurator.configureAndWatch( logConfigName, 60*1000 );

        }
        //if not successful, read from standard file in classpath
        if (System.getProperty("log4j.configuration") == null)
           {
            URL url = ClassLoader.getSystemResource("log4j.xml");
            DOMConfigurator.configure(url);
           }

    
        if (!config.success()) {
            System.out.println("BennoCore "+Server.class.getPackage().getImplementationVersion()+"\nUsage: java " +
                    Server.class.getName() + " " +
                    parser.getUsage());
            System.exit(255);
        }
        String repopath = config.getString("repo");
        File repofile = new File(repopath);
        if (repofile.exists()) {
            if (!repofile.isDirectory()) {
                System.out.println("The repository is not a directory!");
                System.exit(255);
            }
        } else {
            logger.info("Repository "+repopath+" does not exist. Create new.");
            System.out.println("Create repository directory..");
            if (!repofile.mkdirs()) {
                System.out.println("Failed to create directory!");
                System.exit(255);
            }
        }
        //repo = new TrieRepository(repopath);
        String indexdir;
        if (config.getString("index").equals("")) {
            indexdir = config.getString("repo") + File.separator + "index";
        } else {
            indexdir = config.getString("index");
        }
        boolean newIndex;
        File indexfile = new File(indexdir);
        if (indexfile.exists()) {
            if (!indexfile.isDirectory()) {
                System.out.println("The index is not a directory!");
                System.exit(255);
            }
            if (indexfile.list().length==0) {  //check whether there is already an index
                newIndex=true;
            } else {
                        newIndex = false;
            }
        } else {
            logger.info("Index does not exist at "+indexdir+". Open a new index.");
            System.out.println("Create index directory..");
            if (!indexfile.mkdirs()) {
                System.out.println("Failed to create directory!");
                System.exit(255);
            }
            newIndex = true;
        }

         if (config.getBoolean("container-repository")) {
           logger.info("Access archive in container format.");
           archive=new ContainerArchive(indexdir, repopath,false);
         } else {
            logger.info("Access archive in (old) single index format.");
            archive=new SingleIndexArchive(indexdir,repopath,false);
         }
            //iw = new IndexWriter(indexdir, new StandardAnalyzer(), newIndex);
            //iw = new IndexWriter(indexdir, new WhitespaceAnalyzer(), newIndex);



        // main part 
        if (archive!=null) {
            if (config.getString("maildir").equals("")) {
                smtpserver = new SMTPDocumentHandler(archive);
                smtpserver.setPort(config.getInt("port"));
                smtpserver.setHostname(config.getString("hostname"));
            } else {
                maildirserver = new MailDirWatcherDocumentHandler(config.getString("maildir"), archive);
            }
        }

    }

    public static void start() throws Exception {
        if (smtpserver != null) {
            smtpserver.start();
        }
    }

    public static void stop() throws Exception {
    }

    public static void destroy() {
    }
}
