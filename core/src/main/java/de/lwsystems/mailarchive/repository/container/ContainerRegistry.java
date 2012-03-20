/*
 * ContainerRegistry.java
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.store.NIOFSDirectory;

/**
 *
 * @author wiermer
 */
public class ContainerRegistry {

    String indexdir = null;

    Logger logger = LoggerFactory.getLogger(ContainerRegistry.class.getName());

    void setIndexDir(String indexdir) {
        this.indexdir = indexdir;
    }

    Searchable[] getSearchables() {
        File idir = new File(indexdir);
        LinkedList<Searchable> searchables = new LinkedList<Searchable>();
        for (File f : idir.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }
        })) {
            for (File g : f.listFiles(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory();
                }
            })) {
                try {
                    searchables.add(new IndexSearcher(new NIOFSDirectory(g),true));



                } catch (CorruptIndexException ex) {
                    logger.error("Corrupt index.", ex);
                } catch (IOException ex) {
                    logger.error("Corrupt index.", ex);
                }
            }

        }
        return searchables.toArray(new Searchable[0]);

    }
}
