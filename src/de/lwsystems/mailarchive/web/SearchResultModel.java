/*  
 * SearchResultModel.java  
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
package de.lwsystems.mailarchive.web;

import de.lwsystems.mailarchive.web.util.StringUtil;
import de.lwsystems.mailarchive.web.domain.PopupLabelData;
import de.lwsystems.mailarchive.web.domain.AddressList;
import de.lwsystems.mailarchive.repository.SingleIndexArchive;
import de.lwsystems.mailarchive.repository.Archive;
import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.mailarchive.repository.Repository;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.servlet.ServletContext;
import javax.swing.table.AbstractTableModel;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.context.support.ServletContextResource;
import javax.mail.internet.InternetAddress;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author wiermer
 */
public class SearchResultModel extends AbstractTableModel {

    Archive archive;
    private Searcher indexSearcher;
    private Repository repo;
    //private Hits hits;
    private TopDocs hits;
    private String indexdir;
    private String repodir;
    private String[] columnnames = {"Von", "An", "Anhang", "Betreff", "Datum", "ID"};
    private DateFormat dateformat = new SimpleDateFormat("dd.MM.yy, HH:mm");
    private static int MAXIMUM_TO_SORT = 50;
    private LinkedList<String> toExcludeMailAddresses = new LinkedList<String>();
    //Default fields to be searched
    private int summaryWidth=80;
    /**
     * 
     * @param index
     * @param repodir
     * @throws org.apache.lucene.index.CorruptIndexException
     * @throws java.io.IOException
     */
    public SearchResultModel(String index, String repodir) throws CorruptIndexException, IOException {
        SingleIndexArchive sarchive = new SingleIndexArchive(indexdir, repodir, true);
        sarchive.setIndexDir(index);
        sarchive.setRepositoryDir(repodir);
        this.archive = sarchive;
        repo = archive.getRepository();
        indexSearcher = archive.getIndexSearcher();
        indexdir = index;
        readExcludeAddresses();

    }
      public SearchResultModel(String index, String repodir,int summarywidth) throws CorruptIndexException, IOException {
         this(index,repodir);
          summaryWidth=summarywidth;

    }

    private void readExcludeAddresses() {

        File f = new File("/etc/benno/exclude-addresses");
        if (f.exists()) {
            BufferedReader input = null;
            try {

                input = new BufferedReader(new FileReader(f));
                try {
                    String line = null; //not declared within while loop
                    while ((line = input.readLine()) != null) {
                        toExcludeMailAddresses.add(line.trim().toLowerCase());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ex) {
                        Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    input.close();
                } catch (IOException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    /**
     * 
     * @param archive
     * @throws org.apache.lucene.index.CorruptIndexException
     * @throws java.io.IOException
     */
    public SearchResultModel(Archive archive,int summaryWidth) throws CorruptIndexException, IOException {
        this.archive = archive;
        repo = archive.getRepository();
        indexSearcher = archive.getIndexSearcher();
        readExcludeAddresses();
        this.summaryWidth=summaryWidth;
    }

    public Archive getArchive() {
        return archive;
    }

    /**
     * 
     * @return 
     */
    public static SearchResultModel getDefaultInstance(ServletContext servletContext) throws CorruptIndexException, IOException {
        XmlBeanFactory factory = new XmlBeanFactory(new ServletContextResource(servletContext, "WEB-INF/applicationContext-index.xml"));
        PropertyOverrideConfigurer configurer = new PropertyOverrideConfigurer();
        if (new File("/etc/benno/archive.properties").exists()) {
            configurer.setLocation(new FileSystemResource("/etc/benno/archive.properties"));
            configurer.postProcessBeanFactory(factory);
        }
        Archive archive = (Archive) factory.getBean("archive");

        Properties tableprops=new Properties();
            try {
                tableprops.load(new FileInputStream("/etc/benno/searchtable.properties"));
            } catch (IOException ex) {
                Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        return new SearchResultModel(archive,new Integer(tableprops.getProperty("summarywidth", "80")).intValue());
    }

    /**
     * 
     * @return
     */
    public Repository getRepository() {
        return repo;
    }

    /**
     * 
     * @return
     */
    public Searcher getIndexSearcher() {
        return indexSearcher;
    }

    /**
     * 
     * @return
     */
    public TermEnum[] getTerms() {
        return archive.getTerms();
    }


    /**
     * 
     * @param q
     * @return
     * @throws org.apache.lucene.queryParser.ParseException
     * @throws java.io.IOException
     */
    public boolean query(Query q) throws ParseException, IOException {


        if (indexSearcher == null) {
            return false;
        }

        if (indexSearcher instanceof IndexSearcher) {
            if (!((IndexSearcher) indexSearcher).getIndexReader().isCurrent()) {
                indexSearcher = archive.updateSearcher();
            }
        } else {
            indexSearcher = archive.updateSearcher();
        }


        //hits = indexSearcher.search(q);

       
         hits=archive.getIndexSearcher().search(q,10000);



         //TODO: sort hits
         sortHits();

        fireTableStructureChanged();

        return true;

    }

    private Document getDocumentFromHit(int row) throws IOException {
            if (row<hits.scoreDocs.length&&row>=0) {
            return getIndexSearcher().doc(hits.scoreDocs[row].doc);
            }
            else {
                //create an empty document to prevent null return
                Document doc=new Document();
                return doc;
            }
    }
    /**
     * 
     * @return
     */
    public int getRowCount() {
        if (hits != null) {
            return hits.totalHits;
        }
        return 0;

    }

    /**
     * 
     * @return
     */
    public int getColumnCount() {
        //from,to,attachment,subject,sent, id,status
        return 7;
    }

    /**
     * 
     * @param i
     * @return
     */
    @Override
    public String getColumnName(int i) {
        return columnnames[i];
    }

    /**
     * 
     * @param i
     * @return
     */
    @Override
    public Class getColumnClass(int i) {
        switch (i) {
            case 0:
            case 1:
                return AddressList.class;
            case 2:
                return boolean.class;
            case 3:
                return String.class;
            case 4:
                return Date.class;
            default:
                return String.class;
        }
    }

    private String getNullSafeValue(int row,String val,int index) throws IOException {

            String[] values = getDocumentFromHit(row).getValues(val);
            if (values!=null&&index<values.length) {
                return values[index];
            }
            return "";

        }

    public String getDataBlock(int row) throws CorruptIndexException, IOException, java.text.ParseException {

        StringBuilder sb = new StringBuilder();
        sb.append(StringUtil.join(getDocumentFromHit(row).getValues("header-From"), ",") + "\n");
        sb.append(StringUtil.removeLinebreaks(StringUtil.join(getDocumentFromHit(row).getValues("header-To"), ",")) + "\n");
        sb.append(getNullSafeValue(row,"multipart",0) + "\n");
        sb.append(getNullSafeValue(row,"title",0) + "\n");
        sb.append(StringUtil.removeLinebreaks(getNullSafeValue(row,"summary",0)) + "\n");
        //sb.append(dateformat.format(MetaDocument.getDateFormat().parse((String) getDocumentFromHit(row).getValues("sent")[0])+"\n"));
        sb.append(getNullSafeValue(row,"sent",0) + "\n");
        sb.append(getNullSafeValue(row,"id",0) + "\n");
        return sb.toString();
    }

    /**
     * 
     * @param arg0
     * @param arg1
     * @return
     */
    public Object getValueAt(int arg0, int arg1) {
        String extext;
        try {
            if (hits != null) {

                switch (arg1) {
                    case 0:
                        return fromAddressLabel(arg0);
                    case 1:
                        return toAddressLabel(arg0);
                    case 2:
                        return getDocumentFromHit(arg0).getValues("multipart")[0];
                    case 3:
                        return titlePopup(nullsafe(getDocumentFromHit(arg0).getValues("title")), nullsafe(getDocumentFromHit(arg0).getValues("summary")),summaryWidth);
                    case 4: {
                        String result = nullsafe(getDocumentFromHit(arg0).getValues("sent"));

                        //attention: isEmpty is new in 1.6 !!!
                        //if (result.isEmpty()) {
                        if (result.length() == 0) {
                            return "";
                        }
                        return dateformat.format(MetaDocument.getDateFormat().parse(result));
                    }
                    case 5:
                        return getDocumentFromHit(arg0).getValues("id")[0];
                    case 6:
                        return getStatus(fromAddressLabel(arg0), toAddresses(arg0));

                }
            }
        } catch (java.text.ParseException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
            return ex.toString();
        } catch (CorruptIndexException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
            return ex.toString();
        } catch (IOException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
            return ex.toString();
        } catch (NullPointerException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        return "";

    }

    private PopupLabelData toAddressLabel(
            int arg0) {

//        String[] headerto;
//        String[] to;
//        Set<String> s = new LinkedHashSet<String>();
//        try {
//            headerto = getDocumentFromHit(arg0).getValues("header-To");
//
//
//            for (String i : headerto) {
//                s.add(i);
//            }
//        } catch (CorruptIndexException ex) {
//            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (NullPointerException ex) {
//        }
//
//        //commented out to prevent not so nice headers from the envelope. Maybe needed again for some milter solution
////        try {
////            to = getDocumentFromHit(arg0).getValues("to");
////            for (String i : to) {
////                s.add(i);
////            }
////
////        } catch (CorruptIndexException ex) {
////            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
////        } catch (IOException ex) {
////            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
////        } catch (NullPointerException ex) {
////        }
//
//        return popup(StringUtil.join(s.toArray(new String[0]), ","), 25);
        try {
            return popup(StringUtil.join(toAddresses(arg0), ", "), 25);
        } catch (CorruptIndexException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return popup("", 25);
    }

    private PopupLabelData fromAddressLabel(int arg0) {
        Set<String> s = new LinkedHashSet<String>();
        try {
            String[] headerfrom = getDocumentFromHit(arg0).getValues("header-From");


            for (String i : headerfrom) {
                 InternetAddress a;
                try {
                    a = new InternetAddress(i);
                    s.add(a.getAddress().toLowerCase());
                } catch (AddressException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                }


            }


        } catch (CorruptIndexException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
        }
        try {
            String[] from = getDocumentFromHit(arg0).getValues("from");
            for (String i : from) {
                   InternetAddress a;
                try {
                    a = new InternetAddress(i);
                    s.add(a.getAddress().toLowerCase());
                } catch (AddressException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (CorruptIndexException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
        }
           for (String e:toExcludeMailAddresses) {
            if (s.contains(e)) {
                s.remove(e);
            }
           }
        return popup(StringUtil.join(s.toArray(new String[0]), ","), 25);

    }

    private MailStatus getStatus(PopupLabelData fromAdress, String[] toAdresses) {
        //TODO really determine in- or outgoing mail
        return MailStatus.UNDEFINED;
    }

    private PopupLabelData toAddress(int arg0) throws CorruptIndexException, IOException {
        String[] to = getDocumentFromHit(arg0).getValues("header-To");
        return popup(StringUtil.join(to, ","), 20);

    }

    private String[] toAddresses(int arg0) throws CorruptIndexException, IOException {
        Set<String> addresses = new TreeSet<String>();
        for (String s : getDocumentFromHit(arg0).getValues("to")) {
            addresses.add(s.toLowerCase());
        }
        for (String s : getDocumentFromHit(arg0).getValues("header-To")) {
            try {
                InternetAddress a = new InternetAddress(s);

                addresses.add(a.getAddress().toLowerCase());
            } catch (AddressException ex) {
            }
        }
        for (String s : getDocumentFromHit(arg0).getValues("header-Cc")) {
            try {
                InternetAddress a = new InternetAddress(s);
                addresses.add(a.getAddress().toLowerCase());
            } catch (AddressException ex) {
            }

        }
        //Also remove excludeAddresses from sender
        for (String s:toExcludeMailAddresses) {
            if (addresses.contains(s)) {
                addresses.remove(s);
            }
        }
        String[] dummy = {};
        return (String[]) addresses.toArray(dummy);
    }

    /**
     * 
     * @param row
     * @param col
     * @return
     */
    @Override
    public boolean isCellEditable(int row, int col) {
//        if (col==2)
//            return true;
        return false;
    }

// Createns an Popup object with a long description an a shortened one for the
//use inside a table
    private PopupLabelData popup(String string, int length) {
        if (string == null || string.equals("")) {
            return new PopupLabelData("<Empty>", "<Empty>");
        }

        if (string.length() > length) {
            return new PopupLabelData(string.substring(0, length - 3) + "...", string);
        }

        return new PopupLabelData(string, string);
    }
    //Createns a title representation with a short summary

    private PopupLabelData titlePopup(String title, String summary, int length) {
        String popupText = summary;
        if (popupText == null) {
            popupText = "";
        }
        if (title == null || title.equals("")) {
            return new PopupLabelData("Kein Betreff", popupText);
        }
        if (title.length() + 3 > length) {
            return new PopupLabelData(title.substring(0, length - 3) + "...", popupText);
        }


        if (title.length() + summary.length() + 3 > length) {
            return new PopupLabelData(title, popupText, summary.substring(0, length - title.length() - 3) + "...");
        }
        return new PopupLabelData(title, popupText, summary + "...");
    }

    private String nullsafe(Object o) {
        if (o instanceof String[]) {
            String result = "";
            for (String s : (String[]) o) {
                result = result.concat(s).concat(",");
            }
            if (result.length() > 0) {
                return result.substring(0, result.length() - 1);
            }
        }
        return "";
    }

    private void sortHits() {
       //sort the first 100 results with date
         Comparator<ScoreDoc> cmp = new Comparator<ScoreDoc>() {
               String field="sent";
                boolean order=false;
                public int compare(ScoreDoc h0, ScoreDoc h1) {
                try {
                    Comparable o0;
                    Comparable o1;
                    o0 = getIndexSearcher().doc(h0.doc).get(field);
                    o1 = getIndexSearcher().doc(h1.doc).get(field);
                    if (o0 == null && o1 == null) {
                        return 0;
                    }
                    if (order) {
                        if (o0 == null) {
                            return 1;
                        }
                        return o0.compareTo(o1);
                    } else {
                        if (o1 == null) {
                            return 1;
                        }
                        return o1.compareTo(o0);
                    }
                } catch (CorruptIndexException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);

                } catch (IOException ex) {
                    Logger.getLogger(SearchResultModel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NullPointerException ex) {
                }
                return 0;
                }
         };
       Arrays.sort(hits.scoreDocs,0,Math.min(hits.scoreDocs.length, MAXIMUM_TO_SORT),cmp);
    }


 }

