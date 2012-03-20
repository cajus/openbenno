/*
 * ContainerRepository.java
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

import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.mailarchive.repository.FilesystemRepository;
import de.lwsystems.mailarchive.repository.MalformedFilenameException;
import de.lwsystems.mailarchive.repository.MessageID;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiermer
 */
public class ContainerRepository extends FilesystemRepository {

    long size = 0;
    Container tmpContainer = null;
    ContainerFactory containerFactory = null;
    Logger logger = LoggerFactory.getLogger(ContainerRepository.class.getName());

    public ContainerRepository(String root) {

        this(root, new SimpleContainerFactory(new File(root)));
    }

    public ContainerRepository(String root, ContainerFactory factory) {
        super(root);
        containerFactory = factory;
        tmpContainer = factory.createContainer("_tmp");
    }

    public String getDescription() {
        return "ContainerRepository: " + getRootPath();
    }

    public long size() {
        return size;
    }

    @Override
    protected String getRecipientsFilenameByID(MessageID id) {
        return getFilenameByID(id)+".recipients";
    }

    class FileIterator implements Iterator<MessageID> {

        private Iterator<File> files;
        private FileFilter filter;
        private final LinkedList<File> flattenedFileList = new LinkedList<File>();
        long size = 0;

        private long getSize() {
            return size;
        }

        private void traverseDir(File f) {
            for (File m : f.listFiles(new RepoFileFilter())) {
                flattenedFileList.add(m);
                size++;
            }
            for (File d : f.listFiles(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory();
                }
            })) {
                traverseDir(d);
            }
        }

        public FileIterator(File file) {
            super();          
            traverseDir(file);
            files = flattenedFileList.iterator();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public MessageID next() {
            File next = files.next();
            try {
                MessageID id=getIDByFilename(next.getName());
                id.setFullPath(next.getAbsolutePath());
                return id;
            } catch (MalformedFilenameException ex) {
                logger.error("Malformed filename.", ex);
                return null;
            }
        }

        public boolean hasNext() {
            return files.hasNext();
        }
    }

    @Override
    public Iterator<MessageID> getIterator() {
        FileIterator fi = new FileIterator(new File(getRootPath()));
        size = fi.getSize();
        return fi;
    }

    public Collection<MessageID> getDocumentIDsByDigest(byte[] digest) {
        LinkedList<MessageID> ll = new LinkedList<MessageID>();
        for (Box b : getContainerBoxes()) {
            for (MessageID id : b.getDocumentIDsByDigest(digest)) {
                ll.add(id);
            }

        }
        return ll;
    }

    public Iterable<Box> depositInBoxes(MessageID mid, MetaDocument metadocument) throws IOException {
        File tmpFile = new File(getTmpFilenameByID(mid));
        if (!tmpFile.exists()) {
            logger.error("depositInBoxes: File for message \"{}\" does not exists in tmp-Folder",mid);
            return null;
        }
        LinkedList<Box> boxesPuttedIn = new LinkedList<Box>();
        int count = 0;
        Container defaultContainer = null;
        for (Container c : getContainers()) {
            if (c.isDefaultContainer()) {
                defaultContainer = c;
            } else {
                //copy tmpFile in every box that it belongs to
                Iterable<Box> boxes = containerFactory.getSplitStrategy().belongsTo(c, metadocument);
                if (boxes != null) {
                    for (Box b : boxes) {
                        if (b.getStatus() == Box.OPEN) {
                            System.out.println("Found suitable box, which is open;"+b.getContainerName()+" "+b.getBoxName());
                            b.add(tmpFile);
                            boxesPuttedIn.add(b);
                            count++;
                        }
                    }
                }

            }
        }
        if (count == 0) {
            System.out.println("None fitting container found, try to use default container");
            //no suitable box found, put in default
            if (defaultContainer == null) {
                //create a default one
                defaultContainer = containerFactory.makeDefaultContainer();
            }
            Iterable<Box> boxes = containerFactory.getSplitStrategy().belongsTo(defaultContainer, metadocument);
            if (boxes != null) {
                for (Box b : boxes) {
                    if (b.getStatus() == Box.OPEN) {
                        System.out.println("Found suitable box, which is open;"+b.getContainerName()+"/"+b.getBoxName());
                        b.add(tmpFile);
                        boxesPuttedIn.add(b);
                    }
                }
            }
        }
        if (boxesPuttedIn.size()>0) {
            tmpFile.delete();
        }
        return boxesPuttedIn;
    }

    @Override
    protected String getFilenameByID(MessageID id) {
        //first, check if it is in the temporary dir
        String tmpFname = getTmpFilenameByID(id);
        if (new File(tmpFname).exists()) {
            return tmpFname;
        }

        //if not, check all other boxes
        for (Box b : getContainerBoxes()) {
            String name = b.getFileName(id);
            if (new File(name).exists()) {
                return name;
            }
        }
        //we have not found them in the repository.
        return "";
        //return getTmpFilenameByID(id);

    }

    @Override
    protected String getAddFilenameByID(MessageID id) {
        return getTmpFilenameByID(id);
    }

    private String getTmpFilenameByID(MessageID id) {

        return tmpContainer.getDirectory().getAbsolutePath() + File.separator + containerFactory.getContextAwareCanonicalFileName(id);
    }

    public Iterable<Container> getContainers() {
        LinkedList<Container> containers = new LinkedList<Container>();
        File root = new File(getBaseDir());
        for (File f : root.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return arg0.isDirectory() && !arg0.getName().equals("_tmp");
            }
        })) {

            containers.add(containerFactory.createContainer(f.getName()));

        }

        return containers;
    }

    //return all possible ContainerDirs
    private Iterable<Box> getContainerBoxes() {
        LinkedList<Box> boxes = new LinkedList<Box>();


        Iterable<Container> containers = getContainers();
        for (Container c : containers) {
            for (Box b : c.getBoxes()) {
                boxes.add(b);
            }
        }


        return boxes;
    }
}
