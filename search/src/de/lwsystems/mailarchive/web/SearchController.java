/*  
 * SearchController.java  
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

import de.lwsystems.mailarchive.web.mailsendhandler.MailSendHandler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.util.Version;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.wings.SFont;
import org.wings.SLabel;
import org.wings.STable;
import org.wings.event.SRequestEvent;
import org.wings.event.SRequestListener;
import org.wings.session.SessionManager;

/**
 *
 * @author wiermer
 */
public class SearchController implements ActionListener, SRequestListener {

    static int FUTURE_DAYS_CUTOFF = 30;   //how many days into the future are not spam ?
    STable results;
    SLabel searchresults;
    Properties props;
    SearchResultModel tablemodel;
    String query = "";
    Query lastQuery = null;
    Date begin = null;
    Date end = null;
    boolean collected = false;
    SortedSet<String> froms = new TreeSet<String>();
    SortedSet<String> tos = new TreeSet<String>();
    SortedSet<String> years = new TreeSet<String>();
    String earliestDate = null;
    String latestDate = null;
    UsefulTermsExtractor ute;
    MailSendHandler mailSendHandler;
    Collection<Filter> spamFilters = null;
    boolean excludeSpam = true;

    public boolean isExcludeSpam() {
        return excludeSpam;
    }

    public void setExcludeSpam(boolean excludeSpam) {
        this.excludeSpam = excludeSpam;
    }

    public Collection<Filter> getSpamFilters() {
        return spamFilters;
    }

    public void setSpamFilters(Collection<Filter> spamFilters) {
        this.spamFilters = spamFilters;
    }

    public MailSendHandler getMailSendHandler() {
        return mailSendHandler;
    }

    public void setMailSendHandler(MailSendHandler mailSendHandler) {
        this.mailSendHandler = mailSendHandler;
    }

    /**
     * 
     * @param results
     * @param searchresults
     */
    public SearchController(STable results, SLabel searchresults,Collection<Filter> exclf) {

        this.searchresults = searchresults;
        this.results = results;
        final STable finrs = results;
        spamFilters=exclf;

        try {
            tablemodel = SearchResultModel.getDefaultInstance(searchresults.getSession().getServletContext());
            ute = new UsefulTermsExtractor(tablemodel.getArchive());
            collected = true;
        } catch (CorruptIndexException ex) {
            Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 
     * @return
     */
    public SearchResultModel getTableModel() {
        return tablemodel;
    }

    /**
     * 
     * @return
     */
    public Query getLastQuery() {
        return lastQuery;
    }

    /**
     * @return null, if the user is in the ROLE_ADMIN; a list of authorized email addresses otherwise.
     */
    public Collection<String> getAcceptedMailAddresses() {
        Vector<String> ll = new Vector<String>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String str = ga.getAuthority().trim();
            if (str.startsWith("ROLE_ADMIN")) {
                return null;
            }
            if (str.startsWith("ROLE_MAIL_")) {
                String email = str.substring(10);
                ll.add(email);
            }
        }
        return ll;
    }

    public Date getEarliestDate() {
        return ute.getEarliestDate();
    }

    public Date getLatestDate() {
        Date d = ute.getLatestDate();
        Date current = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(current);
        cal.add(Calendar.DATE, FUTURE_DAYS_CUTOFF);
        Date currentandskip = cal.getTime();

        //a cutoffdate in the near future (spam filtering);

        if (d.after(currentandskip)) {
            return currentandskip;
        }

        return d;
    }

    //returns a sorted vector with all unique email addresses.
    public Set<String> getFromAddresses() {
           if (ute==null) {
            return new TreeSet<String>();
        }
        return ute.getFromAddresses();
    }

    public Set<String> getToAddresses() {
           if (ute==null) {
            return new TreeSet<String>();
        }
        return ute.getToAddresses();
    }

    public Set<String> getFromDomains() {
           if (ute==null) {
            return new TreeSet<String>();
        }
        return ute.getFromDomains();
    }

    public Set<String> getToDomains() {
           if (ute==null) {
            return new TreeSet<String>();
        }
        return ute.getToDomains();
    }

    public Set<String> getYears() {
        if (ute==null) {
            return new TreeSet<String>();
        }
        return ute.getYears();
    }

    public Set<String> getHeaders() {
        return ute.getHeaders();
    }

    boolean isAdmin() {
        for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
            String str = ga.getAuthority();
            if (str.startsWith("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @return a Query to be added to an AND-type boolean query for restricted access. null for admin rights.
     */
    private Query getRestrictedQuery() {
        return RestrictedQuery.getRestrictedQuery();
    }

    /**
     * 
     * @return
     */
    public String getQuery() {
        return query;
    }

    /**
     * 
     * @param query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * 
     * @param begin
     * @param end
     */
    public void setDateRange(Date begin, Date end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * 
     * @param toCompare
     * @param numofhits
     * @return
     */
    public TopScoreDocCollector getSimilarMails(String toCompare, int numofhits) {
        BooleanQuery q = new BooleanQuery();
        Query qr = getRestrictedQuery();
        if (qr != null) {
            q.add(getRestrictedQuery(), BooleanClause.Occur.MUST);
        }

        Searcher searcher = tablemodel.getIndexSearcher();
        if (searcher instanceof IndexSearcher) {
            MoreLikeThis mlt = new MoreLikeThis(((IndexSearcher) searcher).getIndexReader());
            mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_24));
            String[] fields = {"text"};
            mlt.setFieldNames(fields);
            try {
                q.add(mlt.like(new StringReader(toCompare)), BooleanClause.Occur.MUST);
            } catch (IOException ex) {
                Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                TopScoreDocCollector tdc = TopScoreDocCollector.create(numofhits,true);
                tablemodel.getIndexSearcher().search(q, tdc);

                return tdc;
            } catch (IOException ex) {
                Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return null;
    }

    /**
     * 
     */
    public void executeQuery() {

        Query querywithrestrictions;

        if (tablemodel==null||searchresults==null) {
            return;
        }
        try {
            if (isExcludeSpam()) {
                querywithrestrictions = RestrictedQuery.getParsedQueryWithRestrictions(query, begin, end, getSpamFilters());
            } else {
                querywithrestrictions = RestrictedQuery.getParsedQueryWithRestrictions(query, begin, end, null);
            }

            searchresults.setText("Keine Ergebnisse gefunden!");
            searchresults.setFont(new SFont("sans-serif", SFont.PLAIN, 10));
            //
            tablemodel.query(querywithrestrictions);
            if (tablemodel.getRowCount() != 0) {
                results.setModel(tablemodel);
                results.reload();
                searchresults.setVisible(false);
            } else {
                searchresults.setVisible(true);
            }
        } catch (org.apache.lucene.queryParser.ParseException ex) {
            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 
     * @return
     */
    public Properties getProperties() {
        return props;
    }

    /**
     * 
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeQuery();
    }

    /**
     * 
     * @param re
     */
    public void processRequest(SRequestEvent re) {
        if (re.getType() == SRequestEvent.DISPATCH_START) {
            if (SessionManager.getSession().getServletRequest().getParameter("query") != null) {
                String q = SessionManager.getSession().getServletRequest().getParameter("query");
                setQuery(q);
                executeQuery();

            }
        }

    }
}