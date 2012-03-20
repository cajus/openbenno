/*  
 * SingleIndexArchive.java  
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

import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.utils.MiscUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rene
 */
public class SingleIndexArchive extends BaseArchive {

    @Override
    protected void resetIndex() {
        File f = new File(indexdir);
        if (f.exists() && f.isDirectory()) {
            MiscUtils.deleteDirectory(f);
        }
        f.mkdirs();
    }
    private String indexdir;
    private String repositorydir;
    private Repository repository = null;
    private IndexSearcher indexsearcher = null;
    private IndexWriter indexwriter = null;
    private boolean readOnly = false;
    Logger logger = LoggerFactory.getLogger(SingleIndexArchive.class.getName());

    public SingleIndexArchive() {
        super();
    }

    public SingleIndexArchive(String indexdir, String repodir, boolean readOnly) throws IOException {
        super();

        this.readOnly = readOnly;

        if (indexdir != null) {
            File indexdirfile = new File(indexdir);

            if (!indexdirfile.exists()) {
                logger.info("Index directory {} does not exist. Create new index directory.", indexdir);
                if (!indexdirfile.mkdirs()) {
                    throw new IOException("SingleIndexArchive: could not create index directory " + indexdir);
                }
            }

            if (!indexdirfile.isDirectory()) {
                throw new IOException("SingleIndexArchive: Index is not a directory " + indexdir);
            }
        }

        setIndexDir(indexdir);

        if (!readOnly) {
            assert repodir != null;
            File repodirfile = new File(repodir);
            if (!repodirfile.exists()) {
                logger.info("Repository direcotory {} does not exist. Create new repository directory.", repodir);
                if (!repodirfile.mkdirs()) {
                    throw new IOException("SingleIndexArchive: could not create Repository directory " + repodir);
                }
            }
            if (!repodirfile.isDirectory()) {
                throw new IOException("SingleIndexArchive: Repository is not a directory " + repositorydir);
            }
        }
        setRepositoryDir(repodir);

    }

    public void setIndexDir(String indexdir) {
        this.indexdir = indexdir;
    }

    public void setRepositoryDir(String repositorydir) {
        this.repositorydir = repositorydir;
    }

    public Searcher getIndexSearcher() throws CorruptIndexException, IOException {
        if (indexsearcher == null) {
            //NIOFSDirectory is problematic on certain Windows platforms.
            indexsearcher = new IndexSearcher(new NIOFSDirectory(new File(indexdir)), true);
        }
        return indexsearcher;
    }

    public Repository getRepository() {
        if (repository == null) {
            repository = new TrieRepository(repositorydir);
        }
        return repository;
    }

    public TermEnum[] getTerms() {
        try {
            return new TermEnum[]{getIndexReader().terms()};
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
        }
        return null;
    }

    public TermEnum[] getTerms(Term t) {
        try {
            return new TermEnum[]{getIndexReader().terms(t)};
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
        }
        return null;
    }

    public IndexReader getIndexReader() {
        return indexsearcher.getIndexReader();
    }

    public Searcher updateSearcher() throws CorruptIndexException, IOException {


        if (indexsearcher!=null) {
            try {
            //indexsearcher.getIndexReader().close();

            indexsearcher.close();
            }
            catch (IOException ex) {
                //could not close
            }
        }

        indexsearcher = new IndexSearcher(new NIOFSDirectory(new File(indexdir)), true);
        return indexsearcher;
    }

    @Override
    public Iterable<IndexWriter> getIndexWriters(MetaDocument meta) throws CorruptIndexException, LockObtainFailedException, IOException {
        if (indexwriter == null) {
            //indexwriter=new IndexWriter(indexdir,new StandardAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
            //DEBUG
            logger.debug("Generate a new IndexWriter...");
            System.out.println("Generate a new IndexWriter...");
            indexwriter = new IndexWriter(new NIOFSDirectory(new File(indexdir)), new StandardAnalyzer(Version.LUCENE_24), IndexWriter.MaxFieldLength.LIMITED);
        }
        ArrayList<IndexWriter> iws = new ArrayList<IndexWriter>(1);
        iws.add(indexwriter);

        return iws;
    }

    public void close() {
        try {
            if (indexwriter != null) {
                indexwriter.close();
            }
            if (indexsearcher!=null) {
                indexsearcher.close();
            }
        } catch (CorruptIndexException ex) {
            logger.error("Corrupt index. ",ex);
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
        }
    }

    @Override
    protected void postprocessAddDocument(MessageID mid, MetaDocument meta) {
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    protected Iterable<IndexWriter> getAllIndexWriters() throws CorruptIndexException, LockObtainFailedException, IOException {

        //we have only a single index
        return getIndexWriters(null);

    }

    @Override
    protected boolean hasToCloseIndexWriter() {
        return false;
    }

    public String getDescription() {
        return "SingleIndexArchive repodir:"+repositorydir+" indexdir:"+indexdir;
    }
}
