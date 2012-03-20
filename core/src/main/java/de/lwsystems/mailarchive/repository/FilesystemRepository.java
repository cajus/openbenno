/*  
 * FilesystemRepository.java  
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
package de.lwsystems.mailarchive.repository;

import de.lwsystems.mailarchive.repository.log.FilesytemLogReader;
import de.lwsystems.mailarchive.repository.log.LineActionReport;
import de.lwsystems.mailarchive.repository.log.RepositoryLogReader;
import de.lwsystems.mailarchive.repository.log.ActionStatus;
import de.lwsystems.mailarchive.repository.log.FilesystemLogWriter;
import de.lwsystems.mailarchive.repository.log.RepositoryLogWriter;
import de.lwsystems.utils.CustomGZIPOutputStream;
import de.lwsystems.utils.Base64;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author rene
 */
public abstract class FilesystemRepository implements Repository {

    String basedir;

    public String getBaseDir() {
        return basedir;
    }
    RepositoryLogReader logreader;
    RepositoryLogWriter logwriter;
    Logger logger = LoggerFactory.getLogger(FilesystemRepository.class.getName());

    public FilesystemRepository() {
        this(".");
        initLogger();
    }

    public FilesystemRepository(String path) {
        File f = new File(path);
        try {
            basedir = f.getCanonicalPath();
        } catch (Exception e) {
            System.err.println("FilesystemRepository: could not find repository");
            e.printStackTrace();
        }
        initLogger();
    }

    public String getRootPath() {
        return basedir;
    }

    public static MessageID getIDByFilename(String fname) throws MalformedFilenameException {
        if (fname.length() < (UniqueID.DIGEST_LENGTH_BASE64 + 3)) {
            throw new MalformedFilenameException();
        }
        byte[] digest = Base64.decode(fname.substring(0, UniqueID.DIGEST_LENGTH_BASE64), Base64.URL_SAFE);
        String supplemental = "";
        if (fname.length() > (UniqueID.DIGEST_LENGTH_BASE64 + 3)) {
            supplemental = fname.substring(UniqueID.DIGEST_LENGTH_BASE64, fname.length() - 3);
        }
        return new MessageID(digest, supplemental);
    }

    @Override
    public MessageID addDocument(InputStream doc, String[] recipients) throws DuplicateException {
        MessageID id = new MessageID(""); //in case of exceptions, log gracefully
        try {
            //first write everything to a temp file
            File tmpFile = File.createTempFile("bennoAddDocument", "tmp");
            FileOutputStream tmpFileOut = new FileOutputStream(tmpFile);
            byte buf[] = new byte[1024];
            int len;
            while ((len = doc.read(buf)) > 0) {
                tmpFileOut.write(buf, 0, len);
            }
            tmpFileOut.close();

            //then read it in
            try {
                id = UniqueID.getID(this, tmpFile);
                if (id == null) {
                    return null;
                }
            } catch (DuplicateException ex) {
                getLogWriter().addActionReport(new LineActionReport(new Date(), "SKIP", ex.getMid().toString(), ActionStatus.DUPLICATE));
                tmpFile.delete();
                throw ex;
            }

            //Start from the beginning and write to final destination
            FileInputStream document=new FileInputStream(tmpFile);
            String filename = getAddFilenameByID(id);

            byte[] data = new byte[1024 * 1024];
            int read = 0;
            // GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(filename));
            GZIPOutputStream out = new CustomGZIPOutputStream(new FileOutputStream(filename), 1);
            //FileOutputStream out=new FileOutputStream(filename);
            while ((read = document.read(data, 0, 1024 * 1024)) > 0) {
                out.write(data, 0, read);
            }
            if (out instanceof GZIPOutputStream) {
                out.finish();
            }
            out.close();
            tmpFile.delete();

            //write recipients information

            if (recipients != null && recipients.length > 0) {
                String recipientsfilename = getRecipientsFilenameByID(id);
                BufferedWriter recout = new BufferedWriter(new FileWriter(recipientsfilename));

                for (String r : recipients) {
                    recout.write(r);
                    recout.newLine();
                }
                recout.close();
            }

            getLogWriter().addActionReport(new LineActionReport(new Date(), "ADD", id.toString(), ActionStatus.SUCCESS));
            return id;
        } catch (FileNotFoundException ex) {
            logger.error("File not found. ", ex);
            System.out.println(ex);
            getLogWriter().addActionReport(new LineActionReport(new Date(), "ADD", id.toString(), ActionStatus.FAILED));
            return null;
        } catch (IOException ex) {
            logger.error("I/O exception. ", ex);
            System.out.println(ex);
            getLogWriter().addActionReport(new LineActionReport(new Date(), "ADD", id.toString(), ActionStatus.FAILED));
            return null;
        }

    }

    @Override
    public InputStream getDocument(MessageID id) {
        InputStream in = null;
        try {
            String fname = getFilenameByID(id);
            //in = new GZIPInputStream(new FileInputStream(fname));
            in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fname)), 8192);
        } catch (FileNotFoundException ex) {
            logger.error("File not found. ", ex);
            return null;
        } catch (IOException ex) {
            logger.error("I/O exception. ", ex);
            return null;
        }
        return in;
    }

    private void initLogger() {
        logreader = new FilesytemLogReader(getLogFileName());
        logwriter = new FilesystemLogWriter(getLogFileName());
    }

    protected abstract String getRecipientsFilenameByID(MessageID id);

    public static class RepoFileFilter implements FileFilter {

        public boolean accept(File f) {
            return f.getName().toLowerCase().endsWith(".gz") && (!f.isDirectory()) && (f.getName().length() >= UniqueID.DIGEST_LENGTH_BASE64 + 3);
        }
    }

    protected class DigestFilenameFilter implements FilenameFilter {

        private String encdigest;

        public DigestFilenameFilter(byte[] digest) {
            encdigest = Base64.encodeBytes(digest, Base64.URL_SAFE);
        }

        public boolean accept(File arg0, String arg1) {
            return arg1.startsWith(encdigest);
        }
    }

    //Give filename to search for, assuming it exists
    protected abstract String getFilenameByID(MessageID id);
    //Give filename to add a document to

    protected abstract String getAddFilenameByID(MessageID id);

    public String[] getRecipients(MessageID id) {
        String fname = getRecipientsFilenameByID(id);

        if (fname != null) {
            File f = new File(fname);
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(f));

                String s;
                LinkedList<String> recs = new LinkedList<String>();
                while ((s = br.readLine()) != null) {
                    recs.add(s);
                }
                return recs.toArray(new String[1]);
            } catch (FileNotFoundException ex) {
                return null;
            } catch (IOException ex) {
                logger.error("I/O exception. ", ex);
            }

        }
        return null;
    }

    protected String getLogFileName() {
        return System.getProperty("benno.repositoryLogFile", basedir + File.separator + "repository.log");
    }

    public RepositoryLogReader getLogReader() {
        return logreader;
    }

    public RepositoryLogWriter getLogWriter() {
        return logwriter;
    }
}
