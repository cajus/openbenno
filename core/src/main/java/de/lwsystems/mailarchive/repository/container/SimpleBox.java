/*
 * SimpleBox.java
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
 */package de.lwsystems.mailarchive.repository.container;

import de.lwsystems.mailarchive.repository.MalformedFilenameException;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.utils.MiscUtils;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiermer
 */
public class SimpleBox extends Box {

    File directory = null;

    Logger logger = LoggerFactory.getLogger(SimpleBox.class.getName());

    SimpleBox(File f) {
        directory = f;
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public File getDirectory() {
        return directory;
    }

    @Override
    public void add(File f) throws IOException {
        File dirToCopyTo = generatePrefixDirs(f);
        MessageID id = null;
        try {
            id = SimpleContainerFactory.getIDByFilename(f.getName());
            String fname=getFileName(id);
            MiscUtils.copyFile(f,new File(fname));

        } catch (MalformedFilenameException ex) {
            logger.error("Malformed filename.", ex);
        }
       
    }


    private File generatePrefixDirs(File f) {
        String name = f.getName();
        String prefixPath = directory.getAbsolutePath() + File.separator + name.charAt(0) + File.separator + name.charAt(1) + File.separator + name.charAt(2);
        File prefixPathFile = new File(prefixPath);
        if (!prefixPathFile.exists()) {
            if (!prefixPathFile.mkdirs()) {
                throw new SecurityException("Prefix path " + prefixPath + " could not be created. Started with wrong user ? Check permissions of repository subdirs.");
            }
        }
        return prefixPathFile;
    }

    @Override
    String getFileName( MessageID id) {

        String name = SimpleContainerFactory.getCanonicalFileName(id);
        String fname = getDirectory().getAbsolutePath() +File.separator + name.charAt(0) + File.separator + name.charAt(1) + File.separator + name.charAt(2) + File.separator + name;
        return fname;
    }

    private String getPrefixDirName(byte[] digest) {
        MessageID id = new MessageID(digest, "");
        String name =SimpleContainerFactory.getCanonicalFileName(id);
        String prefixDirName = directory.getAbsolutePath() + File.separator + name.charAt(0) + File.separator + name.charAt(1) + File.separator + name.charAt(2);
        return prefixDirName;
    }

    @Override
    Iterable<MessageID> getDocumentIDsByDigest(byte[] digest) {
        LinkedList<MessageID> ll = new LinkedList<MessageID>();
        File d = new File(getPrefixDirName(digest));

        //SimpleContainerFactory factory = new SimpleContainerFactory();
        String[] files = d.list(SimpleContainerFactory.getDigestFilenameFilter(digest));


        if (files == null) {
            return ll;
        }

        for (String fname : files) {
            try {
                ll.add(SimpleContainerFactory.getIDByFilename(fname));
            } catch (MalformedFilenameException ex) {
                logger.error("Malformed filename.", ex);
            }

        }
        return ll;
    }

    @Override
    public String getBoxName() {
        return directory.getName();
    }

    @Override
    public String getContainerName() {
        return directory.getParentFile().getName();
    }
}
