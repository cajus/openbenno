/*  
 * DocumentHandler.java  
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
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;

/**
 * ABstract superclass for classes delivering mails into the archive
 *
 * @author wiermer
 */
public abstract class DocumentHandler {

    Archive archive=null;
    IndexWriter indexwriter;
    boolean debugoutput=false;

    Logger logger = LoggerFactory.getLogger(DocumentHandler.class.getName());

    public DocumentHandler(Archive a) {
        archive=a;

    }
    
    protected void debug(String s) {
        if (debugoutput)
            System.out.println(s);
    }

    public IndexWriter getIndexwriter() {
        return indexwriter;
    }

    public void setIndexwriter(IndexWriter indexwriter) {
        this.indexwriter = indexwriter;

    }

    /*
     * This method should not be called directly by outside classes. Use the convenience
     * methods of subclasses instead.
     */
    public void addDocumentToIndex(MetaDocument m) {

        try {
            Document d = new Document();
            Collection<Field> stored=m.getStoredFields();
            //System.out.println("Size of stored fields:"+stored.size());
            for (Field s : stored) {
                if (s.isTokenized()) {
                    d.add(new org.apache.lucene.document.Field(s.getKey(),
                            (String) s.getPayload(),
                            org.apache.lucene.document.Field.Store.YES,
                            org.apache.lucene.document.Field.Index.ANALYZED));

                } else {
                    d.add(new org.apache.lucene.document.Field(s.getKey(),
                            (String) s.getPayload(),
                            org.apache.lucene.document.Field.Store.YES,
                            org.apache.lucene.document.Field.Index.NOT_ANALYZED));

                }

            }
            for (Field i : m.getIndexedFields()) {
                d.add(new org.apache.lucene.document.Field(i.getKey(),
                        (Reader) i.getPayload()));
            }
            synchronized(this) {
            indexwriter.addDocument(d);
            }
            logger.info("Successfully added document to index");
            debug("Successfully added to index");
            indexwriter.commit();
        } catch (CorruptIndexException ex) {
            logger.error("Corrupt index!",ex);
            debug("Corrupt index! "+ex);
        } catch (IOException ex) {
            logger.error("I/O execption while writing to index!", ex);
            debug("I/O execption while writing to index! "+ ex);
        }
    }
}
