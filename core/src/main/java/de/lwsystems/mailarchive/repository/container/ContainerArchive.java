package de.lwsystems.mailarchive.repository.container;

import de.lwsystems.mailarchive.repository.*;
import de.lwsystems.mailarchive.parser.MetaDocument;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;


/**
 *
 * @author wiermer
 * Archive implementation which represents a colllection of Containers (e.g. by
 * domain or single email address, which themselves contain Boxes (by size or date)
 *
 *
 */
public class ContainerArchive extends BaseArchive {

    @Override
    protected void resetIndex() {
        //TODO implement resetIndex which keeps the container structure if possible
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String indexdir;
    private String repositorydir;
    private Repository repository = null;
    private Searcher indexsearcher = null;
    private ContainerRegistry registry = new ContainerRegistry();
    private ContainerSplitStrategy css = new MonthlySplitStrategy();
    private boolean readOnly=false;

    Logger logger = LoggerFactory.getLogger(ContainerArchive.class.getName());

    @Override
    protected Iterable<IndexWriter> getAllIndexWriters() throws CorruptIndexException, LockObtainFailedException, IOException {
        //TODO Container getAllIndexWriters()
        throw new UnsupportedOperationException("Not supported yet.");
    }



    public ContainerArchive(String indexdir, String repodir, boolean readOnly) throws IOException {
        super();
        File indexdirfile = new File(indexdir);
        this.readOnly=readOnly;
        assert indexdir != null;
        if (!indexdirfile.exists()) {
            if (!indexdirfile.mkdirs()) {
                throw new IOException("ContainerArchive: could not create Index directory " + indexdir);
            }
        }

        if (!indexdirfile.isDirectory()) {
            throw new IOException("ContainerArchive: Index is not a directory " + indexdir);
        }
        setIndexDir(indexdir);

        if (!readOnly) {
            assert repodir!=null;
            File repodirfile = new File(repodir);
            if (!repodirfile.exists()) {
                if (!repodirfile.mkdirs()) {
                    throw new IOException("ContainerArchive: could not create Repository directory " + repodir);
                }
            }
            if (!repodirfile.isDirectory()) {
                throw new IOException("ContainerArchive: Repository is not a directory " + repositorydir);
            }

            bumpVersion(repodir);
        }
        if (repodir!=null)
          setRepositoryDir(repodir);

    }

    String getFormatDescription() {
        return "ContainerArchive SimpleBox MonthlySplittingStrategy";
    }

    int getMajorVersion() {
        return 1;
    }

    int getMinorVersion() {
        return 0;
    }

    public void setIndexDir(String indexdir) {
        this.indexdir = indexdir;
        registry.setIndexDir(indexdir);
        try {
            indexsearcher = new MultiSearcher(registry.getSearchables());
        } catch (IOException ex) {
            logger.error("IO Error while accessing index dir.", ex);
        }
    }

    public void setRepositoryDir(String repositorydir) {
        this.repositorydir = repositorydir;
    }

    public ContainerSplitStrategy getContainerSplitStrategy() {
        return css;
    }

    public void setContainerSplitStrategy(ContainerSplitStrategy css) {
        this.css = css;
    }

    @Override
    public Searcher getIndexSearcher() throws CorruptIndexException, IOException {
        return indexsearcher;
    }

    @Override
    public Iterable<IndexWriter> getIndexWriters(MetaDocument meta) throws IOException, LockObtainFailedException, CorruptIndexException {
        ArrayList<IndexWriter> indexwriter = new ArrayList<IndexWriter>();
        for (Box b : meta.getBoxes()) {
            String indexdirname = indexdir + File.separator + b.getContainerName() + File.separator + b.getBoxName();
            File indexdirfile = new File(indexdirname);
            if (!indexdirfile.exists()) {
                indexdirfile.mkdirs();
            }
            IndexWriter iw = new IndexWriter(new NIOFSDirectory(indexdirfile), new StandardAnalyzer(Version.LUCENE_24), IndexWriter.MaxFieldLength.LIMITED);
            indexwriter.add(iw);
        }

        if (indexwriter.size() == 0) {
            throw new SecurityException("FATAL: Could not find any valid IndexWriter for " + meta);
        }
        return indexwriter;
    }

    @Override
    public Searcher updateSearcher() throws CorruptIndexException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TermEnum[] getTerms() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IndexReader getIndexReader() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Repository getRepository() {
        if (repository == null) {
            repository = new ContainerRepository(repositorydir);
        }
        return repository;
    }

    public void close() {
    }

    @Override
    protected void postprocessAddDocument(MessageID mid, MetaDocument meta) throws IOException {
        ContainerRepository crepo = (ContainerRepository) getRepository();
        Iterable<Box> boxes = crepo.depositInBoxes(mid, meta);
        meta.setBoxes(boxes);
    }

    private void bumpVersion(String dir) throws IOException {
        File versionFile = new File(dir + File.separator + ".archive-type");
        if (!versionFile.exists()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(versionFile));
            bw.write(getFormatDescription());
            bw.write(new Integer(getMajorVersion()).toString());
            bw.write(new Integer(getMinorVersion()).toString());
            bw.close();
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public TermEnum[] getTerms(Term t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected boolean hasToCloseIndexWriter() {
        return true;
    }

    public String getDescription() {
        return "ContainerArchive repository dir:"+repositorydir+" indexdir:"+indexdir;
    }
}
