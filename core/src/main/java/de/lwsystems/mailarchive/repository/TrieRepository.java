/*  
 * TrieRepository.java  
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

import de.lwsystems.utils.Base64;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;

import java.util.Iterator;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the AbstractRepository stores the mails in the filesystem in a directory tree. This ensures 
 * less files per directory and faster access times (especially on ext3).
 * @author rene
 */
public class TrieRepository extends FilesystemRepository {

    long size;

    static Logger logger = LoggerFactory.getLogger(TrieRepository.class.getName());

    public TrieRepository(String d) {
        super(d);
    }
    
    /*ATTENTION:This must be called after getIterator
     * 
     */
    @Override
    public long size() {
       return size;
    }

    @Override
    public String getDescription() {
        return "TrieRepository:"+basedir;
    }

    @Override
    protected String getAddFilenameByID(MessageID id) {
        return getFilenameByID(id);
    }

    protected String getRecipientsFilenameByID(MessageID id) {
       return getFilenameByID(id)+".recipients";
    }
    
    private static class FileIterator implements Iterator<MessageID> {

        private Iterator<File> files;
        private FileFilter filter;
        private final LinkedList<File> flattenedFileList = new LinkedList<File>();
        long size=0;

        private long getSize() {
            return size;
        }
        
        private void traverseDir(File f) {
            for (File m : f.listFiles(filter)) {          
                flattenedFileList.add(m);
                size++;
            }
            for (File d:f.listFiles(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory();
                }
            })) {
                traverseDir(d);
            }
        }

        public FileIterator(File file, FileFilter filter) {
            this.filter = filter;
            traverseDir(file);
            files=flattenedFileList.iterator();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public MessageID next() {
            File next=files.next();
            try {
                return getIDByFilename(next.getName());
            } catch (MalformedFilenameException ex) {
                logger.error("Malformed filename. ",ex);
                return null;
            }

        }

        public boolean hasNext() {
            return files.hasNext();
        }
    }


    @Override
    public Iterator<MessageID> getIterator() {
        FileIterator fi=new FileIterator(new File(basedir), new RepoFileFilter());
        size=fi.getSize();
        return fi;
    }

    @Override
    public Collection<MessageID> getDocumentIDsByDigest(byte[] digest) {

        LinkedList<MessageID> ll = new LinkedList<MessageID>();

        File d = new File(generatePrefixDirs(digest));
        String[] files = d.list(new DigestFilenameFilter(digest));
        if (files == null || files.length == 0) {
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            try {
                ll.add(getIDByFilename(files[i]));
            } catch (MalformedFilenameException ex) {
            }
        }


        return ll;
    }

    private String generatePrefixDirs(byte[] digest) {
        String name = Base64.encodeBytes(digest, Base64.URL_SAFE);
        String prefixPath = basedir + File.separator + name.charAt(0) + File.separator + name.charAt(1) + File.separator + name.charAt(2);
        File prefixPathFile = new File(prefixPath);
        if (!prefixPathFile.exists()) {
            if (!prefixPathFile.mkdirs()) {
                throw new SecurityException("Prefix path "+prefixPath+" could not be created. Started with wrong user ? Check permissions of repository subdirs.");
            }
        }
        return prefixPath;
    }

    @Override
    protected String getFilenameByID(MessageID id) {
        return generatePrefixDirs(id.getDigest()) + File.separator + Base64.encodeBytes(id.getDigest(), Base64.URL_SAFE) + id.getSupplemental() + ".gz";
    }
}
