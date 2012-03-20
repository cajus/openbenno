//package de.lwsystems.mailarchive.web.domain;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.CorruptIndexException;
//import org.apache.lucene.search.Hit;
//import org.apache.lucene.search.HitIterator;
//import org.apache.lucene.search.Hits;
//
///**
// *
// * @author wiermer
// * drop-in replacement for Hits (final) from Lucene.
// * Strategy: be effective: if no sorting is required, access Hits directly; otherwise iterate over all results and sort.
// */
//public class SortedHits {
//
//    boolean sorted = false;
//    Hits realHits;
//    int maximumToSort = 0;
//    ArrayList<Hit> sortedHits;
//
//    public SortedHits(Hits h, int max_hits_sorted) {
//        realHits = h;
//        maximumToSort = max_hits_sorted;
//    }
//
//    public void setSorted(boolean s) {
//        sorted = s;
//    }
//
//    public void sortByField(final String field, final boolean order) {
//        if (field.equals("")) {
//            sorted = false;
//        } else {
//            sorted = true;
//            sortedHits = new ArrayList<Hit>();
//            HitIterator it = (HitIterator) realHits.iterator();
//            int i = 0;
//            while (it.hasNext() && i++ < maximumToSort) {
//                sortedHits.add((Hit) it.next());
//            }
//            Comparator<Hit> cmp = new Comparator<Hit>() {
//
//                public int compare(Hit h0, Hit h1) {
//                    Comparable o0;
//                    Comparable o1;
//                    try {
//                        o0 = h0.get(field);
//                        o1 = h1.get(field);
//                        if (o0 == null && o1 == null) {
//                            return 0;
//                        }
//                        if (order) {
//                            if (o0 == null) {
//                                return 1;
//                            }
//                            return o0.compareTo(o1);
//                        } else {
//                            if (o1 == null) {
//                                return 1;
//
//                            }
//                            return o1.compareTo(o0);
//                        }
//
//
//                    } catch (CorruptIndexException ex) {
//                        Logger.getLogger(SortedHits.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (IOException ex) {
//                        Logger.getLogger(SortedHits.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (NullPointerException ex) {
//
//                    }
//                    return 0;
//
//                }
//            };
//
//            Collections.sort(sortedHits, cmp);
//        }
//
//    }
//
//    public Document doc(
//            int row) throws CorruptIndexException, IOException {
//
//
//        if (sorted && row < maximumToSort) {
//            return sortedHits.get(row).getDocument();
//        }
//        return realHits.doc(row);
//
//    }
//
//    public int length() {
//        return realHits.length();
//    }
//}
