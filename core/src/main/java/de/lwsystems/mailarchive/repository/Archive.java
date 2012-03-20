/*  
 * Archive.java  
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
 */ package de.lwsystems.mailarchive.repository;


import java.io.IOException;
import java.io.InputStream;
import javax.mail.MessagingException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.store.LockObtainFailedException;


/**
 * Interface which represents the access to the index or indexes and repository.
 * Useful to be instantinated by an IoC container like Spring.
 * @author rene
 */
public interface Archive {

    public String getDescription() ;
    /**
     * Returns the correspond Lucene Searcher.
     */
    public Searcher getIndexSearcher() throws CorruptIndexException,IOException;
    /**
     * Returns an updated IndexSearcher, which accounts for changed entries
     * Attention: this is likely to be another object than the original one from getIndexSearcher
     */
    
     public Searcher updateSearcher() throws CorruptIndexException,IOException;
    /**
     *  Returns the repository, which contains the complete documents/mails
     * @return
     */
    public Repository getRepository();
    /**
     * Returns an array of TermEnum. Each Element corresponds to the terms of an individual index
     * Useful to have direct (fast) access to the terms themselves without parsing search results
     * @return
     */
    public TermEnum[] getTerms();
        /**
     * Returns an array of TermEnum, starting at the term equal or greater to Term t. Each Element corresponds to the terms of an individual index
     * Useful to have direct (fast) access to the terms themselves without parsing search results
     * @return
     */
    public TermEnum[] getTerms(Term t);

    public boolean isReadOnly();

    public void addDocument(InputStream in, String[] recipients,PostAddRepositoryHandler posthandler)  throws IOException,MessagingException, LockObtainFailedException, CorruptIndexException, DuplicateException;

    public boolean updateIndex();
    public boolean rebuildIndex();
    public boolean checkIntegrity();
    public boolean checkConsistency();
    public boolean optimize();
    public TopDocsCollector<ScoreDoc> query(String querystring);
    IndexReader getIndexReader();
    public void close();


}
