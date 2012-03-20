/*
 * BaseArchive.java
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
 */package de.lwsystems.mailarchive.repository;

import de.lwsystems.mailarchive.parser.Field;
import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.mailarchive.repository.container.ContainerArchive;
import de.lwsystems.utils.LimitInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

/**
 *
 * @author wiermer
 */
public abstract class BaseArchive implements Archive {

    private String[] defaultfields = {"from", "to", "text", "title"};
    private long MAX_PARTPARSING_TIMEOUT = 5*60;

    Logger logger = LoggerFactory.getLogger(BaseArchive.class.getName());

    public static Archive autodetectArchive(String repodir, String indexdir,boolean readOnly) throws IOException {
      File frepo=new File(repodir);
      File findex=new File(indexdir);

      //clean archive
      if ((!frepo.exists()&&!frepo.exists())) {
          return new ContainerArchive(indexdir, repodir, readOnly);
      }
      if (!frepo.isDirectory()||!findex.isDirectory()) {
          throw new IOException("Index or repo name is not a directory");
      }
      // test for new version or empty directory
      if (new File(repodir+File.separator + ".archive-type").exists()||frepo.list().length==0) {
         return new ContainerArchive(indexdir, repodir, readOnly);
      }
      return new SingleIndexArchive(indexdir, repodir, readOnly);

    }


    protected abstract Iterable<IndexWriter> getAllIndexWriters() throws CorruptIndexException, LockObtainFailedException, IOException;

    public boolean optimize() {
        Boolean noErrors = true;
        try {
            for (IndexWriter iw : getAllIndexWriters()) {

                iw.optimize();
                if (hasToCloseIndexWriter()) {
                    iw.close();
                }
            }
        } catch (CorruptIndexException ex) {
            logger.error("Corrupt index. ",ex);
            noErrors = false;
        } catch (IOException ex) {
            logger.error("IO error. ",ex);
            noErrors = false;
        }

        return noErrors;
    }

    public TopDocsCollector<ScoreDoc> query(String querystring) {
        QueryParser qp = new MultiFieldQueryParser(Version.LUCENE_24, defaultfields, new StandardAnalyzer(Version.LUCENE_24));
        Query q = null;
        try {
            q = qp.parse(querystring);
        } catch (ParseException ex) {
            System.err.println("Error parsing query:" + ex);
            return null;
        }
        TopScoreDocCollector collect = TopScoreDocCollector.create(1000, true);
        try {
            getIndexSearcher().search(q, collect);
            return collect;
        } catch (IOException ex) {
            System.err.println("I/O error while accessing index:" + ex);

        }
        return null;

    }

    public boolean checkConsistency() {
        long missed = 0;
        Iterator<MessageID> it = getRepository().getIterator();
        while (it.hasNext()) {
            MessageID mid = it.next();
            if (mid != null) {
                System.out.println("Checking message " + mid);
                TermQuery q = new TermQuery(new Term("id", mid.toString()));
                TopDocsCollector<ScoreDoc> collector = TopScoreDocCollector.create(1, true);
                try {
                    getIndexSearcher().search(q, collector);
                } catch (IOException ex) {
                    System.err.println("I/O exception while searching index");
                    System.exit(-1);
                }
                if (collector.getTotalHits() == 0) {
                    System.err.println("Document " + mid + " is not in the index!");
                    missed++;
                }
            }
        }
        return (missed == 0);
    }

    public boolean checkIntegrity() {
        long missed = 0;
        Iterator<MessageID> it = getRepository().getIterator();
        System.out.println("Check integrity...");
        while (it.hasNext()) {
            MessageID current = it.next();
            if (current != null) {
                try {
                    byte[] repoDigest = UniqueID.getDigest(getRepository().getDocument(current));

                    if (!(equalDigest(repoDigest, current.getDigest()))) {
                        logger.debug("Mismatch in message {} (Digest from repo {}).", new Object [] { current, new MessageID(repoDigest,"") });
                        System.err.println("Mismatch in message " + current + " (Digest from repo " + new MessageID(repoDigest, "") + ")");
                        missed++;
                    }
                } catch (IOException ex) {
                    logger.error("I/O Exception ",ex);
                    System.err.println("I/O Exception " + ex);
                } catch (Exception ex) {
                    System.err.println("Ohoh! " + ex);
                }
            }
        }
        return (missed == 0);
    }

    private boolean isInIndex(MessageID current) throws IOException {
        return (getIndexSearcher().search(new TermQuery(new Term("id", current.toString())), 1).totalHits > 0);
    }

    boolean equalDigest(byte[] repoDigest, byte[] digest) {
        if (repoDigest == null || digest == null) {
            System.err.println("ERROR: no digest to compare! repoDigest=" + repoDigest + " digest=" + digest);
            return false;
        }
        if (repoDigest.length != digest.length) {
            System.err.println("ERROR: different digest lengths! Different hash algorithm?");
            return false;
        }
        for (int i = 0; i < repoDigest.length; i++) {
            if (repoDigest[i] != digest[i]) {
                System.err.println("ERROR: difference in digests at position " + i + "!");
                return false;
            }
        }
        return true;
    }

    public boolean rebuildIndex() {
        if (isReadOnly()) {
            throw new AssertionError("WARNING: archive to rebuild is read-only. This should not happen.");
        }

        resetIndex();
        long missed = 0;
        Iterator<MessageID> it = getRepository().getIterator();
        System.out.println("# of Documents in Repository: " + getRepository().size());
        while (it.hasNext()) {
            MessageID current = it.next();
            if (current != null) {
                System.out.println("Adding Message " + current.toString());
                try {
                    MetaDocument meta = generateMetaDocument(current);
                    addDocumentToIndex(meta);
                } catch (LockObtainFailedException ex) {
                    logger.error("Cannot obtain lock for message. ",ex);
                    missed += 1;
                } catch (CorruptIndexException ex) {
                    logger.error("Corrupt index. ",ex);
                    missed += 1;
                } catch (IOException ex) {
                    logger.error("I/O error. ",ex);
                    missed += 1;
                } catch (MessagingException ex) {
                    logger.warn("Error adding message! Reason: {}.",ex);
                    System.err.println("    Error adding message! Reason:" + ex);
                    missed += 1;
                }
            }
        }
        return (missed == 0);


    }

    public boolean updateIndex() {
        if (isReadOnly()) {
            throw new AssertionError("WARNING: archive to update is read-only. This should not happen.");
        }
        long counter = 0;
        long missed = 0;
        Iterator<MessageID> it = getRepository().getIterator();
        long numofdocs = getRepository().size();
        System.out.println("# of Documents in Repository: " + numofdocs);
        while (it.hasNext()) {
            MessageID current = it.next();
            if (current != null) {
                counter++;
                System.out.print("Checking Message " + counter + "/" + numofdocs + "\r");
                try {
                    if (!isInIndex(current)) {
                        System.out.println("Adding Message " + current.toString());
                        try {
                            MetaDocument meta = generateMetaDocument(current);
                            addDocumentToIndex(meta);
                        } catch (MessagingException ex) {
                            System.err.println("    Error adding message! Reason:" + ex);
                            missed += 1;
                        }
                    } else {
                        //System.out.println("Message " + current.toString() + " already in index!");
                    }
                } catch (IOException ex) {
                    System.err.println("I/O exception");
                    logger.error("I/O exception.");
                    return false;
                }
            }
        }
        return (missed == 0);
    }

    public abstract Searcher getIndexSearcher() throws CorruptIndexException, IOException;

    public abstract Iterable<IndexWriter> getIndexWriters(MetaDocument meta) throws IOException, LockObtainFailedException, CorruptIndexException;

    public abstract Searcher updateSearcher() throws CorruptIndexException, IOException;

    public abstract TermEnum[] getTerms();

    public abstract IndexReader getIndexReader();

    MessageID addDocumentToRepository(InputStream in, String[] recipients) throws DuplicateException {
        return getRepository().addDocument(in, recipients);
    }

    public String extractTextFromPart(Part p) throws MessagingException {
        return  extractTextFromPart(p, false);
    }

    String extractTextFromPart(final Part p, boolean savepartstofile) throws MessagingException {
        if (p == null) {
            return "";
        }

        String prefix = p.getFileName();
        if (prefix == null) {
            prefix = "";
        }

        if (p.isMimeType("multipart/*")) {
            String text = prefix + " ";
            try {
                Multipart mp = (Multipart) p.getContent();
                int count = mp.getCount();
                for (int i = 0; i
                        < count; i++) {
                    text = text + extractTextFromPart(mp.getBodyPart(i));
                }

                return text;
            } catch (IOException ex) {

                return text;
            }
        }

        try {
            if (p.getContent() instanceof String) {
                if ((p.isMimeType("text/html") || p.isMimeType("text/xml"))) {
                    //brutally remove the poor tags
                    try {
                        return prefix + " " + ((String) p.getContent()).replaceAll("<.*>", "");
                    } catch (IOException ex) {
                        return "";
                    }

                }
                return prefix + " " + (String) p.getContent();
            }
        } catch (IOException ex) {
            return "";
        }

        //for everything else, delegate to the Tika library.
        final Tika tika = new Tika();


        //set maximum string length to 10MB. New in Tika 0.7
        tika.setMaxStringLength(10 * 1024 * 1024);

        final Metadata md = new Metadata();


        //  debug("Free heap space before parsing:"+Runtime.getRuntime().freeMemory());
        // parser.parse(p.getInputStream(), handler, md);

        // debug("Free heap space after parsing:"+Runtime.getRuntime().freeMemory());
        // debug("Metadata:" + md.toString());
        //return prefix + " " + handler.toString();
        final StringBuffer sb = new StringBuffer(prefix + " ");

        Callable<Reader> c = new Callable<Reader>() {

            public Reader call() throws Exception {
                try {
                    return tika.parse(p.getInputStream());

                } catch (MessagingException ex) {
                    logger.error("Messaging exception. ",ex);
                } catch (IOException ex) {
                    logger.error("I/O exception. ",ex);
                }


                return null;
            }
        };


        FutureTask<Reader> task = new FutureTask<Reader>(c);
        Thread t=new Thread(task);
        t.start();
        try {
            Reader s = task.get(MAX_PARTPARSING_TIMEOUT, TimeUnit.SECONDS);
            if (s!=null) {
                BufferedReader bs=new BufferedReader(s);
                String thisLine;
                 while ((thisLine = bs.readLine()) != null) {
                     sb.append(thisLine).append("\n");
                 }
            }

        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
        } catch (InterruptedException ex) {
            System.out.println(ex);
        } catch (ExecutionException ex) {
            System.out.println(ex);
            logger.error("Execution exception. ",ex);
       } catch (TimeoutException ex) {
            System.out.println(ex);

        } finally {
            task.cancel(true);
        }

//        try {
//            sb.append(tika.parseToString(p.getInputStream(), md));
//        } catch (MessagingException ex) {
//            Logger.getLogger(BaseArchive.class.getName()).log(Level.SEVERE, null, ex);
//
//        } catch (TikaException ex) {
//            Logger.getLogger(BaseArchive.class.getName()).log(Level.SEVERE, null, ex);
//
//        } catch (IOException ex) {
//            Logger.getLogger(JavamailDocumentHandler.class.getName()).log(Level.SEVERE, null, ex);
//        }


        // return prefix + " " + tika.parseToString(p.getInputStream(), md);
        return sb.toString();
    }

    MetaDocument generateMetaDocument(MessageID mid) throws MessagingException {

        //Suns Javamail code is stupid, and reads the whole stream in for parsing, so that the JVM memory is easily filled, when large mails arrive
        //so we need to prevent indexing the whole chunk. As an educated guess, read up to the size of half of the free memory.

        InputStream is = new LimitInputStream(getRepository().getDocument(mid), Runtime.getRuntime().freeMemory() / 2);
        if (is == null) {
            throw new MessagingException("Could not generate Meta document due to underlying I/O problems. See other exceptions for details.");
        }
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()), is);

        MetaDocument meta = new MetaDocument(MetaDocument.MAIL, mid);
        logger.debug("Import data from {}.", mid);
        if (message instanceof MimeMessage) {
            MimeMessage m = (MimeMessage) message;
            meta.setTitle("");
            try {
                //Subject
                meta.setTitle(m.getSubject());
            } catch (MessagingException ex) {
                // _mw_: better at warning level(?)
                logger.warn("Cannot fetch \"Subject\" header from message \"{}\".", mid, ex);
            }
            logger.debug("Message-Id: {}.", m.getHeader("Message-ID"));
            //From
            Address[] from = null;

            try {
                String[] mailFrom = m.getHeader("X-REAL-MAILFROM");
                if ((mailFrom != null)
                        && (mailFrom.length > 0)) {
                    from = new Address[mailFrom.length];
                    for (int i = 0; i < mailFrom.length; i++) {
                        from[i] = new InternetAddress(mailFrom[i]);
                    }
                    logger.debug("Fetch {} from \"X-REAL-MAILFROM\" header.", mailFrom);
                }
            } catch (Exception ex) {
            }
            if (from == null) {
                try {
                    from = m.getFrom();
                } catch (MessagingException ex) {
                    logger.warn("Cannot fetch \"From\" header from message \"{}\".", mid, ex);
                }
            }

            if (from != null) {
                for (Address a : from) {
                    if (a instanceof InternetAddress) {
                        InternetAddress ia = (InternetAddress) a;
                        logger.debug("From: {}.", ia.getAddress());
                        meta.addFrom(ia.getAddress());
                    }
                }
            }

            //To
            Address[] to = null;
            try {
                to = m.getAllRecipients();
            } catch (MessagingException ex) {
                logger.error("Messaging exception. ", ex);
            }

            if (to != null) {
                for (Address a : to) {
                    if (a instanceof InternetAddress) {
                        InternetAddress ia = (InternetAddress) a;
                        logger.debug("To: {}", ia.getAddress());
                        meta.addTo(ia.getAddress());

                    }
                }
            }
            //Add additional recipients
            String[] recipients;

            recipients = getRepository().getRecipients(mid);
            if (recipients != null) {
                for (String addr : recipients) {
                    try {
                        if (addr != null) {
                            InternetAddress a = new InternetAddress(addr, true);
                            meta.addTo(a.getAddress());
                        }

                    } catch (javax.mail.internet.AddressException ex) {
                    }
                }
            }

            //Handle "Delivered-to:" to get BCCs after an mail server got it. Ugly.
            //At least postfix, qmail and Exchange do this.
            // !> Add additional possible headers
            // !> X-Envelope-To:, Envelope-to:, X-Original-To, X-RCPT-To:
            String[] deliveredto = null;
            try {
                deliveredto = m.getHeader("Delivered-To");
            } catch (MessagingException ex) {
                logger.info("Cannot fetch \"Delivered-To\" header from message \"{}\".", mid, ex);
            }
            if (deliveredto != null) {
                for (String dt : deliveredto) {
                    try {
                        InternetAddress a = new InternetAddress(dt);
                        logger.debug("Delivered-To {}", a.getAddress());
                        meta.addTo(a.getAddress());
                    } catch (javax.mail.internet.AddressException ex) {
                        logger.info("Could not parse Delivered-To address, ignoring {}", dt.toString());
                    }
                }
            }

            deliveredto = null;
            try {
                deliveredto = m.getHeader("X-REAL-RCPTTO");
            } catch (MessagingException ex) {
                logger.info("Cannot fetch \"X-REAL-RCPTTO\" header from message \"{}\".", mid, ex);
            }
            if (deliveredto != null) {
                for (String dt : deliveredto) {
                    try {
                        InternetAddress a = new InternetAddress(dt);
                        logger.debug("X-REAL-RCPTTO: {}", a.getAddress());
                        meta.addTo(a.getAddress());

                    } catch (javax.mail.internet.AddressException ex) {
                        logger.debug("Could not parse \"X-REAL-RCPTTO\" address, ignoring {}", dt.toString());
                    }
                }
            }

            //Received
            Date d = null;
            try {
                d = m.getReceivedDate();
            } catch (MessagingException ex) {
                logger.warn("Cannot fetch date from message \"{}\".", mid, ex);
            }

            if (d != null) {
                logger.debug("Received: {}", d);
                meta.setReceived(MetaDocument.getDateFormat().format(d));
            }
            d = null;

            try {
                //Sent
                d = m.getSentDate();
            } catch (MessagingException ex) {
                logger.warn("Cannot fetch sent date from message \"{}\".", mid, ex);
            }
            if (d != null) {
                logger.debug("Sent: {}", d);
                meta.setSent(MetaDocument.getDateFormat().format(d));
            }

            meta.setMultipart(false);

            try {
                //is Multipart?
                meta.setMultipart(m.isMimeType("multipart/*"));

            } catch (MessagingException ex) {
                logger.warn("Cannot fetch multipart header from message \"{}\".", mid, ex);
            }

            try {
                //Headers
                meta.setHeaders(m.getAllHeaders());

            } catch (MessagingException ex) {
                logger.warn("Cannot fetch headers from message \"{}\".", mid, ex);
            }

            //TODO: don't read everything to heap. Use disk storage for larger texts.
            String res = "";


            try {
                res = extractTextFromPart(m);

            } catch (MessagingException ex) {
                logger.error("Messaging exception in message \"{}\". ",mid, ex);
            }

            //Summary
            String summary = res.substring(0, Math.max(Math.min(res.length(), 90), 0)).trim().replace('\n', ' ');
            meta.setSummary(summary);

            //For Debugging: export complete text

            if (!System.getProperty("BENNO_FULLTEXT", "").equals("")) {
                System.out.println("----------------Extraction result--------------------");
                System.out.println(res);
                System.out.println("-----------------------------------------------------");
            }
            //Main Text
            meta.setMainText(new StringReader(res));
            logger.debug("Metadocument for message \"{}\" generated.", mid);
            return meta;

        } //no mime message;
        logger.debug("Email \"{}\" is not a mime message.", mid);
        return null;
    }

    public void addDocument(InputStream in, String[] recipients, PostAddRepositoryHandler posthandler) throws MessagingException, LockObtainFailedException, CorruptIndexException, IOException, DuplicateException {
        if (isReadOnly()) {
            throw new AssertionError("WARNING: archive to addDocument to is read-only. This should not happen.");

        }
        MessageID mid = null;

        //add to repository
        mid = addDocumentToRepository(in, recipients);

        if (mid == null) {
            throw new MessagingException();
        }

        MetaDocument meta = generateMetaDocument(mid);
        posthandler.handle(mid, meta);
        postprocessAddDocument(mid, meta);
        addDocumentToIndex(meta);
        logger.debug("Document \"{}\" added to index.",mid);
    }

    synchronized void addDocumentToIndex(MetaDocument m) throws LockObtainFailedException, CorruptIndexException, IOException {
        logger.debug("Add document \"{}\" to index.",m.getId());
        if (isReadOnly()) {
            throw new AssertionError("WARNING: index of archive to add document to is read-only. This should not happen.");
        }
        Iterable<IndexWriter> indexwriters;

        indexwriters = getIndexWriters(m);

        try {
            Document d = new Document();
            Collection<Field> stored = m.getStoredFields();
            logger.debug("Get stored fields with size: {}.", stored.size());

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
            for (IndexWriter indexwriter : indexwriters) {
                synchronized (this) {
                    indexwriter.addDocument(d);
                    indexwriter.commit();

                    if (hasToCloseIndexWriter()) {
                        indexwriter.close();
                    }

                } //indexwriter.commit();

            } // debug("Successfully added to index");

        } catch (CorruptIndexException ex) {
            logger.error("Corrupt index. ",ex);
            //debug("Corrupt index!");
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
            //debug("I/O execption while writing to index!");
        }





    }

    protected abstract void postprocessAddDocument(MessageID mid, MetaDocument meta) throws IOException;

    protected abstract void resetIndex();

    protected abstract boolean hasToCloseIndexWriter();
}
