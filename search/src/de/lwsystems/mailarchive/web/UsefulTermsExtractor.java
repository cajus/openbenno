/*  
 * UsefulTermsExtractor.java  
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

import de.lwsystems.mailarchive.parser.MetaDocument;
import de.lwsystems.mailarchive.repository.Archive;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

/**
 *
 * @author rene
 */
class TermFrequencyComparator implements Comparator<String> {

    Archive archive;
    HashMap<String, Long> freqs;

    public TermFrequencyComparator(Archive a, HashMap<String, Long> m) {
        archive = a;
        freqs = m;
    }

    public int compare(String field0, String field1) {
        long freq0 = 0;
        long freq1 = 0;

        Long f0 = freqs.get(field0);
        Long f1 = freqs.get(field1);

        if (f0 != null) {
            freq0 = f0;
        }
        if (f1 != null) {
            freq1 = f1;
        }

        if (freq0 < freq1) {
            return -1;
        }
        if (freq0 == freq1) {
            return field0.compareTo(field1);
        }
        return 1;
    }
}

public class UsefulTermsExtractor {

    SortedSet<String> froms = new TreeSet<String>();
    SortedSet<String> tos = new TreeSet<String>();
    SortedSet<String> years = new TreeSet<String>();
    SortedSet<String> fromdomains = new TreeSet<String>();
    SortedSet<String> todomains = new TreeSet<String>();
    SortedSet<String> headers = new TreeSet<String>();
    HashMap<String, Long> headerswithfreq;
    String earliestDate = null;
    String latestDate = null;
    Archive archive;

    public UsefulTermsExtractor(Archive ar) {
        archive = ar;
        headerswithfreq = new HashMap<String, Long>();
        collectUsefulTerms();

    }

    interface TermCollector {

        public void process(Term t);
    }

    private void collectTerm(String field, TermCollector c) {
        TermEnum[] enums = archive.getTerms(new Term(field, ""));
        for (TermEnum e : enums) {
            try {
                while (e != null && e.next() && e.term().field().equals(field)) {
                    c.process(e.term());
                }
            } catch (IOException ex) {
                Logger.getLogger(UsefulTermsExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private void collectTerm(String field, final Collection<String> results) {
        collectTerm(field, new TermCollector() {

            public void process(Term t) {
                results.add(t.text());
            }
        });

    }

    private void collectUsefulTerms() {

        collectTerm("from", froms);
        collectTerm("fromdomain", fromdomains);
        collectTerm("to", tos);
        collectTerm("todomain", todomains);


        //sent
        collectTerm("sent", new TermCollector() {

            public void process(Term t) {
                String date = t.text();
                if (earliestDate == null || date.compareTo(earliestDate) < 0) {
                    earliestDate = date;
                }
                if (latestDate == null || date.compareTo(latestDate) > 0) {
                    latestDate = date;
                }
                years.add(date.substring(0, 4));
            }
        });


        //header
        TermEnum[] enums = archive.getTerms(new Term("header-", ""));
        for (TermEnum e : enums) {
            boolean cont = true;
            try {
                while (e.next() & e.term().field().startsWith("header-")) {
                    String header = e.term().field().substring(7);
                    //do some sanity checks
                    if (!header.contains(" ") && !header.contains("\t") && !header.contains("\n") && !(header.length() > 50)) {

                        if (headerswithfreq.containsKey(header)) {
                            headerswithfreq.put(header, headerswithfreq.get(header) + new Long(e.docFreq()));
                        } else {
                            headerswithfreq.put(header, new Long(e.docFreq()));
                        }

                        headers.add(header);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(UsefulTermsExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Vector<String> todelete = new Vector<String>();

        for (String i : headerswithfreq.keySet()) {
            if (headerswithfreq.get(i) <= 1) {
                todelete.add(i);
            }
        }

        for (String i : todelete) {
            headerswithfreq.remove(i);
        }
    }

    public Date getEarliestDate() {
        try {
            return MetaDocument.getDateFormat().parse(earliestDate);
        } catch (java.text.ParseException ex) {

            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);

        } catch (NullPointerException ex) {
            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);

        } finally {

            return new Date(0);
        }
    }

    public Date getLatestDate() {
        try {
            return MetaDocument.getDateFormat().parse(latestDate);
        } catch (java.text.ParseException ex) {
            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (NullPointerException ex) {
            Logger.getLogger(SearchController.class.getName()).log(Level.SEVERE, null, ex);

        } finally {

            return new Date();
        }
    }

    //returns a sorted vector with all unique email addresses.
    public Set<String> getFromAddresses() {
        return froms;
    }

    public Set<String> getToAddresses() {
        return tos;
    }

    public Set<String> getFromDomains() {
        return fromdomains;
    }

    public Set<String> getToDomains() {
        return fromdomains;
    }

    public Set<String> getYears() {
        return years;
    }

    public Set<String> getHeaders() {
        return headers;
    }
}
