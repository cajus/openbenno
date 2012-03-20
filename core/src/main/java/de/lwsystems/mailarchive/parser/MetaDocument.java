/*  
 * MetaDocument.java  
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

import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.repository.container.Box;
import de.lwsystems.utils.MiscUtils;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import javax.mail.Header;

/**
 * Stores all metadata of a mail/document.
 *
 * @author wiermer
 */
public class MetaDocument {

    final static DateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmm");
    private int type = MAIL;
    private String id;
    private String title;
    private Collection<String> from = new TreeSet<String>();
    private Collection<String> to = new TreeSet<String>();
    private Set<String> fromdomains = new TreeSet<String>();
    private Set<String> todomains = new TreeSet<String>();
    private String received; //Lucene recommended form: yyyymmddHHMM
    private String sent;
    private String summary; //max. 50 characters long summary of content (e,g, beginning of text)
    private boolean multipart = false;
    private String parent; //Container for this document
    private Reader mainTextToIndex;
    private Enumeration headers;
    //currently only MAIL-type documents are stored.
    public static final int MAIL = 0;
    public static final int HTML = 1;
    public static final int PDF = 2;
    public static final String[] TYPEDESC = {"mail", "html", "pdf"};
    private Iterable<Box> boxes = null;

    public Iterable<Box> getBoxes() {
        return boxes;
    }

    public void setBoxes(Iterable<Box> boxes) {
        this.boxes = boxes;
    }

    public MetaDocument(int type, MessageID id) {
        setType(type);
        setId(id);
    }
    //gets the DateFormat for formatting/parsing dates
    public static DateFormat getDateFormat() {
        return dateformat;
    }

    public MessageID getId() {
        return new MessageID(id);
    }

    public void setId(MessageID id) {
        this.id = id.toString();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void addFrom(String addr) {
        from.add(addr.toLowerCase());
        String dom = MiscUtils.extractDomain(addr.toLowerCase());
        if (dom != null && dom.length() > 0) {
            fromdomains.add(dom);
        }
    }


    public void addTo(String addr) {
        to.add(addr.toLowerCase());
        String dom = MiscUtils.extractDomain(addr.toLowerCase());
        if (dom != null && dom.length() > 0) {
            todomains.add(dom);
        }
    }


    public String getReceived() {
        return received;
    }

    public void setReceived(String rec) {
        this.received = rec;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMainText(Reader r) {
        mainTextToIndex = r;
    }

    public Reader getMainText() {
        return mainTextToIndex;
    }

    public void setSummary(String s) {
        this.summary = s;
    }

    public String getSummary() {
        return summary;
    }

    public void setMultipart(boolean b) {
        multipart = b;
    }

    public boolean getMultipart() {
        return multipart;
    }

    public void setHeaders(Enumeration e) {
        headers = e;
    }

    public Enumeration getHeaders() {
        return headers;
    }

    public Collection<Field> getStoredFields() {
        //these are going to be stored in the index
        LinkedList<Field> l = new LinkedList<Field>();
        if (TYPEDESC[type] != null) {
            Field type_ = new Field("type", TYPEDESC[type]);
            l.add(type_);
        }
        if (id != null) {
            Field id_ = new Field("id", id);
            l.add(id_);
        }
        if (received != null) {
            Field received_ = new Field("received", received);
            l.add(received_);
        }
        if (sent != null) {
            Field sent_ = new Field("sent", sent);
            l.add(sent_);
        }
        if (from != null) {
            for (int i = 0; i < from.toArray().length; i++) {
                Field from_ = new Field("from", (String) from.toArray()[i]);
                l.add(from_);
            }
        }
        if (to != null) {
            for (int i = 0; i < to.toArray().length; i++) {
                Field to_ = new Field("to", (String) to.toArray()[i]);
                l.add(to_);
            }
        }

        if (fromdomains != null && fromdomains.size() > 0) {
            for (String s : fromdomains) {
                Field fromdomain_ = new Field("fromdomain", s);
                l.add(fromdomain_);
            }
        }
        if (todomains != null && todomains.size() > 0) {
            for (String s : todomains) {
                Field todomain_ = new Field("todomain", s);
                l.add(todomain_);
            }
        }

        if (title != null) {
            //This should be tokenized for better searching
            Field title_ = new Field("title", title, true);
            l.add(title_);
        }

        if (summary != null) {
            Field summary_ = new Field("summary", summary, true);
            l.add(summary_);
        }


        Field multipart_ = new Field("multipart", new Boolean(multipart).toString(), true);
        l.add(multipart_);

        if (headers != null) {
            while (headers.hasMoreElements()) {
                Header header = (Header) headers.nextElement();
                String name = new String("header-" + header.getName());
                String payload = new String(header.getValue());
                l.add(new Field(name, payload, true));
            }
        }
        return l;

    }

    public Collection<Field> getIndexedFields() {
        // these are just to be indexed and not stored
        ArrayList<Field> l = new ArrayList<Field>();
        if (mainTextToIndex != null) {
            l.add(new Field("text", mainTextToIndex));
        }
        return l;
    }

    @Override
    public String toString() {
        String s = "";
        for (Field f : getStoredFields()) {
            s = s + f + "\n";
        }
        for (Field f : getIndexedFields()) {
            s = s + f + "\n";
        }
        return s;
    }
}
