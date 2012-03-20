/*  
 * ExtendedSearchFrame.java  
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
package de.lwsystems.mailarchive.web.extendedsearch;

import de.lwsystems.mailarchive.web.*;
import de.lwsystems.mailarchive.parser.MetaDocument;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.wings.SButton;
import org.wings.SCheckBox;
import org.wings.SComboBox;
import org.wings.SConstants;
import org.wings.SDimension;
import org.wings.SGridLayout;
import org.wings.SLabel;
import org.wings.SPanel;
import org.wings.SSeparator;
import org.wings.STextField;
import org.wings.border.SLineBorder;
import org.wings.text.SDateFormatter;
import org.wingx.XCalendar;
import org.wingx.XSuggest;

class MutableBoolean {

    private boolean wert;

    public MutableBoolean(boolean b) {
        wert = b;
    }

    public void toggle() {
        wert = !wert;
    }

    public boolean getValue() {
        return wert;
    }
}

/**
 * Represents the contents of the extended search dialog.
 * @author rene
 */
public class ExtendedSearchFrame extends SPanel {

    public static int CLAUSE_FIRST = 0;
    public static int CLAUSE_OR = 1;
    public static int CLAUSE_AND = 2;
    SearchController sc;

    private Set<String> getFromAddresses() {
        return sc.getFromAddresses();
    }

    private Set<String> getFromDomains() {
        return sc.getFromDomains();
    }

    private Set<String> getToAddresses() {
        return sc.getToAddresses();
    }

    private Set<String> getToDomains() {
        return sc.getToDomains();
    }

    private Date getEarliestDate() {
        return sc.getEarliestDate();
    }

    private Date getLatestDate() {
        return sc.getLatestDate();
    }
    
    private Set<String> getHeader() {
        return sc.getHeaders();
    }

    private String parseStartDate(String q) {
        Matcher startDateMatcher = Pattern.compile("sent:\\[([0-9]*)").matcher(q);
        String result = "";
        if (startDateMatcher.find()) {
            result = startDateMatcher.group(1);
        }
        return result;
    }

    private String parseEndDate(String q) {
        Matcher endDateMatcher = Pattern.compile("sent:\\[([0-9]*) TO ([0-9]*)").matcher(q);
        String result = "";
        if (endDateMatcher.find()) {
            result = endDateMatcher.group(2);
        }
        return result;
    }

    private String parseRecipient(String q) {
        return "";
    }

    private String parseSender(String q) {
        Pattern senderPattern = Pattern.compile("from:([_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+))");
        Matcher senderMatcher = senderPattern.matcher(q);
        String result = "";
        while (senderMatcher.find()) {
            if (!result.equals("")) {
                result = result.concat(",");
            }
            result = result.concat(senderMatcher.group(1));
        }
        return result;
    }

    /**
     * Represents a search clause for a header field
     */
    class HeaderClause {

        int type;
        String field;
        String value;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    Query constructQuery(String froms, String tos, Date begin, Date end, String title, String main, boolean attachments, String headerField,String headerContent) {
        BooleanQuery q = new BooleanQuery();
        if (!froms.trim().equals("")) {
            BooleanQuery fromQuery = new BooleanQuery();
            String[] adds = froms.split(",");
            for (String a : adds) {
                if (a.toLowerCase().startsWith("domain:")) {
                    fromQuery.add(new TermQuery(new Term("fromdomain", a.toLowerCase().replace("domain:", "").trim())), BooleanClause.Occur.SHOULD);
                } else {
                    fromQuery.add(new TermQuery(new Term("from", a.trim())), BooleanClause.Occur.SHOULD);
                }
            }
            q.add(fromQuery, BooleanClause.Occur.MUST);
        }
        if (!tos.trim().equals("")) {
            BooleanQuery toQuery = new BooleanQuery();
            String[] adds = tos.split(",");
            for (String a : adds) {
                if (a.toLowerCase().startsWith("domain:")) {
                    toQuery.add(new TermQuery(new Term("todomain", a.toLowerCase().replace("domain:", "").trim())), BooleanClause.Occur.SHOULD);
                } else {
                    toQuery.add(new TermQuery(new Term("to", a.trim())), BooleanClause.Occur.SHOULD);
                }
            }
            q.add(toQuery, BooleanClause.Occur.MUST);
        }

        if (begin != null) {
            if (end != null) {
                q.add(new TermRangeQuery("sent", MetaDocument.getDateFormat().format(begin),
                        MetaDocument.getDateFormat().format(new Date(end.getTime()+(24L*60L*60L*1000L))), true, true), BooleanClause.Occur.MUST);
            }
        }
        if (!title.trim().equals("")) {
            q.add(new TermQuery(new Term("title", title.trim())), BooleanClause.Occur.MUST);
        }
        if (!main.trim().equals("")) {
            q.add(new TermQuery(new Term("text", main.trim())), BooleanClause.Occur.MUST);
        }
        if (attachments) {
            q.add(new TermQuery(new Term("multipart", "true")), BooleanClause.Occur.MUST);
        }
        if (headerField!=null&&headerContent!=null&&!headerField.trim().equals("")&&!headerContent.trim().equals("")) {
            q.add(new TermQuery(new Term("header-"+headerField, headerContent.trim())),BooleanClause.Occur.MUST);
        }
        return q;
    }

    public ExtendedSearchFrame(final SearchController sc, final STextField queryField) {

        this.sc = sc;
        //Layout
        SGridLayout frameLayout = new SGridLayout(7, 1);
        frameLayout.setVgap(3);
        setLayout(frameLayout);

        setPreferredSize(SDimension.FULLWIDTH);

        //deconstruct query
        String q = queryField.getText();
        String senderString = parseSender(q);
        String recipientString = parseRecipient(q);
        String startDateString = parseStartDate(q);
        Date startDate;
        if (startDateString.equals("")) {
            startDate = getEarliestDate();
        } else {
            try {
                startDate = MetaDocument.getDateFormat().parse(startDateString);
            } catch (ParseException ex) {
                startDate = getEarliestDate();
            }
        }
        String endDateString = parseEndDate(q);
        Date endDate;
        if (endDateString.equals("")) {
            endDate = getLatestDate();
        } else {
            try {
                endDate = MetaDocument.getDateFormat().parse(endDateString);
            } catch (ParseException ex) {
                endDate = getLatestDate();
            }
        }

        //Layout

        SGridLayout senderLayout = new SGridLayout(2, 7);
        senderLayout.setVgap(5);
        senderLayout.setHgap(7);

        SPanel sender = new SPanel(senderLayout);
        sender.setBorder(new SLineBorder(new Color(200, 200, 200), 3));

        sender.add(new SLabel("Absender:"));
        final XSuggest senderChooser = new XSuggest();
        senderChooser.setText(senderString);


        //senderChooser.setDataSource(new AddressSource(getFromAddresses()));
        senderChooser.setDataSource(new AddressAndDomainSource(getFromAddresses(), getFromDomains()));
        senderChooser.setSuggestBoxWidth(new SDimension(SDimension.INHERIT, SDimension.INHERIT));
        senderChooser.setPreferredSize(SDimension.FULLWIDTH);
        senderChooser.setVerticalAlignment(SConstants.RIGHT_ALIGN);
        sender.add(senderChooser);

        //SPanel recipient = new SPanel(new SGridLayout(1, 2));
        //recipient.add(new SLabel("Empfänger:"));
        sender.add(new SLabel("Betreff:"));
        final STextField titleValue = new STextField();
        titleValue.setPreferredSize(SDimension.FULLWIDTH);
        titleValue.setVerticalAlignment(SConstants.RIGHT_ALIGN);
        sender.add(titleValue);

//        SGridLayout dateRangeLayout = new SGridLayout(1, 4);
//       
//        dateRangeLayout.setHgap(5);
//        SPanel dateRange = new SPanel(dateRangeLayout);
        SGridLayout fromDateLayout = new SGridLayout(1, 3);
        SGridLayout toDateLayout = new SGridLayout(1, 3);
        fromDateLayout.setHgap(10);
        toDateLayout.setHgap(10);
        SPanel fromDate = new SPanel(fromDateLayout);
        SPanel toDate = new SPanel(toDateLayout);
        fromDate.add(new SLabel("Von:"));
        // final STextField beginValue = new STextField();
        //fromDate.add(beginValue);

        final XCalendar beginChooser = new XCalendar(startDate, new SDateFormatter());
        fromDate.add(beginChooser);
        toDate.add(new SLabel("Bis:"));
        //final STextField endValue = new STextField();
        //toDate.add(endValue);
        final XCalendar endChooser = new XCalendar(endDate, new SDateFormatter());
        toDate.add(endChooser);
        sender.add(fromDate);
        sender.add(toDate);


        sender.add(new SLabel(" "));
        SGridLayout titleGrid = new SGridLayout(2, 2);
        titleGrid.setPreferredSize(SDimension.FULLWIDTH);
        SPanel title = new SPanel(titleGrid);

        sender.add(new SLabel("Empfänger:"));
        final XSuggest recipientChooser = new XSuggest();


        recipientChooser.setText(recipientString);
        //recipientChooser.setDataSource(new AddressSource(getToAddresses()));
        recipientChooser.setDataSource(new AddressAndDomainSource(getToAddresses(), getToDomains()));
        recipientChooser.setPreferredSize(SDimension.FULLWIDTH);
        recipientChooser.setVerticalAlignment(SConstants.RIGHT_ALIGN);

        sender.add(recipientChooser);

        //SPanel main = new SPanel(new SGridLayout(1, 2));
        sender.add(new SLabel("Volltext:"));
        final STextField mainValue = new STextField();
        mainValue.setPreferredSize(SDimension.FULLWIDTH);
        mainValue.setVerticalAlignment(SConstants.RIGHT_ALIGN);
        sender.add(mainValue);

        final MutableBoolean attachments = new MutableBoolean(false);
        final SCheckBox attachmentsValue = new SCheckBox("nur mit Attachments");
        sender.add(attachmentsValue);
        attachmentsValue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                attachments.toggle();
            }
        });
          
        final SCheckBox spamValue = new SCheckBox("auch Spam", false);
        sc.setExcludeSpam(true);
        sender.add(spamValue);
        spamValue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                sc.setExcludeSpam(!sc.isExcludeSpam());
            }
        });
        
        sender.add(new SLabel(" ")); //for layout reasons
        SPanel fields = new SPanel(new SGridLayout(1, 2));
        //fields.add(new SLabel("Mail-Header:"));
        final SComboBox headerSelect=new SComboBox(new Vector(getHeader()));

        //fields.add(headerSelect);
        final STextField headerContent=new STextField();
        headerContent.setColumns(20);
        //fields.add(headerContent);
        
        //sender.add(fields);
        sender.add(new SLabel("Mail-Header:"));
        sender.add(headerSelect);
        sender.add(new SLabel("="));
        sender.add(headerContent);
        
        final LinkedList<HeaderClause> clauses = new LinkedList<HeaderClause>();
//        SPanel clausesComponents = renderClauses(clauses);
//        clausesComponents.setPreferredSize(new SDimension(250,20));
//        clausesComponents.setVerticalAlignment(SConstants.RIGHT_ALIGN);
//        fields.add(clausesComponents);
        final SButton doConstructQuery = new SButton("Suche durchführen");
        doConstructQuery.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Query q = constructQuery(senderChooser.getText(), recipientChooser.getText(),
                        beginChooser.getDate(), endChooser.getDate(),
                        titleValue.getText(), mainValue.getText(), attachments.getValue(),
                        (String) headerSelect.getSelectedItem(),headerContent.getText());
                //exclude if you want to copy the query into the search field
                //queryField.setText(" ".concat(q.toString()));
                //sc.setQuery(queryField.getText());
                sc.setQuery(q.toString());
                sc.executeQuery();

            }
        });

        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                doConstructQuery.doClick();
            }
        };
        final SButton doReset= new SButton("Zurücksetzen");
        doReset.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
            senderChooser.setText("");
            recipientChooser.setText("");
            beginChooser.setDate(getEarliestDate());
            endChooser.setDate(getLatestDate());
            titleValue.setText("");
            mainValue.setText("");
            if (attachments.getValue())
                attachments.toggle();
            headerContent.setText("");

        }}
            );
        senderChooser.addActionListener(al);
        recipientChooser.addActionListener(al);
        titleValue.addActionListener(al);
        mainValue.addActionListener(al);

        //Layout
        sender.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        sender.setPreferredSize(SDimension.FULLWIDTH);
        //recipient.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        //recipient.setPreferredSize(SDimension.FULLWIDTH);
//        dateRange.setHorizontalAlignment(SConstants.LEFT_ALIGN);
//        dateRange.setPreferredSize(SDimension.FULLWIDTH);
//        dateRangeLayout.setVgap(10);
//        title.setHorizontalAlignment(SConstants.LEFT_ALIGN);
//        title.setPreferredSize(SDimension.FULLWIDTH);
//        main.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        //main.setPreferredSize(SDimension.FULLWIDTH);
        attachmentsValue.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        //fields.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        //fields.setPreferredSize(SDimension.FULLWIDTH);
        doConstructQuery.setHorizontalAlignment(SConstants.LEFT_ALIGN);

        add(sender);
       
        //add(recipient);
        //add(dateRange);
        //add(title);
        //add(main);
        //add(attachmentsValue);
        add(new SSeparator(SConstants.HORIZONTAL));
        //add(fields);
        sender.add(doConstructQuery);
        sender.add(doReset);

    }
}
