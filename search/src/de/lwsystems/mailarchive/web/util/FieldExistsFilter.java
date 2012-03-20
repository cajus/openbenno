package de.lwsystems.mailarchive.web.util;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

/**
 * Constructs a filter that is t
 *
 */
public class FieldExistsFilter extends Filter {

    SortedSet fields = new TreeSet();

    /**
     * Adds a field to the list of acceptable fields
     * @param term
     */
    public void addField(String field) {
        fields.add(field);
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader)
     */

    public BitSet bits(IndexReader reader) throws IOException {
        final BitSet result = new BitSet(reader.maxDoc());
        for (Iterator iter = fields.iterator(); iter.hasNext();) {
            String field = (String) iter.next();
            TermEnum terms = reader.terms(new Term(field, ""));
            while (terms.next() && terms.term().field().equals(field)) {
                TermDocs td = reader.termDocs(terms.term());
                while (td.next()) {
                    result.set(td.doc());
                }

            }
        }
        return result;

    }

    /* (non-Javadoc)
     * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
     */
    @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
        final OpenBitSet result = new OpenBitSet(reader.maxDoc());
        for (Iterator iter = fields.iterator(); iter.hasNext();) {
            String field = (String) iter.next();
            TermEnum terms = reader.terms(new Term(field, ""));
            while (terms.next() && terms.term().field().equals(field) &&!terms.term().text().equals("")) {
                TermDocs td = reader.termDocs(terms.term());
                while (td.next()) {
                    result.set(td.doc());
                }

            }
        }
        return result;


    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        FieldExistsFilter test = (FieldExistsFilter) obj;
        return (fields== test.fields || (fields != null && fields.equals(test.fields)));


    }

    @Override
    public int hashCode() {
        int hash = 9;
        for (Iterator iter = fields.iterator(); iter.hasNext();) {
            String field = (String) iter.next();
            hash = 31 * hash + field.hashCode();

        }
        return hash;

    }
}
