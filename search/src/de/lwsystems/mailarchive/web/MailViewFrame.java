/*  
 * MailViewFrame.java  
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
 */ package de.lwsystems.mailarchive.web;

import de.lwsystems.mailarchive.web.util.StringUtil;
import de.lwsystems.mailarchive.web.domain.Icons;
import de.lwsystems.mailarchive.web.resources.MailAttachmentResource;
import de.lwsystems.mailarchive.web.resources.MailPrintResource;
import de.lwsystems.mailarchive.web.resources.MailResource;
import de.lwsystems.mailarchive.repository.Repository;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.web.mailsendhandler.MailSendFailureException;
import de.lwsystems.mailarchive.web.mailsendhandler.MailSendHandler;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.wings.SAnchor;
import org.wings.SButton;
import org.wings.SComponent;
import org.wings.SConstants;
import org.wings.SDialog;
import org.wings.SDimension;
import org.wings.SFlowLayout;
import org.wings.SGridLayout;
import org.wings.SIcon;
import org.wings.SLabel;
import org.wings.SLayoutManager;
import org.wings.SMenu;
import org.wings.SMenuBar;
import org.wings.SMenuItem;
import org.wings.SOptionPane;
import org.wings.SPanel;
import org.wings.STextArea;
import org.wings.STextField;
import org.wings.SURLIcon;
import org.wings.border.SLineBorder;
import org.wings.script.JavaScriptEvent;
import org.wings.script.JavaScriptListener;
import org.wings.style.CSSProperty;

/**
 *
 * @author rene
 */
public class MailViewFrame extends SPanel {

    String presentableText;
    String selectionScript = "    var txt = '';" +
            "if (window.getSelection) {txt = window.getSelection();} " +
            "else if (document.getSelection) {txt = document.getSelection();}" +
            " else if (document.selection) {txt = document.selection.createRange().text;    }" +
            " document.getElementById('{0}').getElementsByTagName('input')[0].value = txt;" +
            "document.getElementById('{0}').value = 'Hello!';";
    String selectionScript2 = "var sl = (document.getElementById('{0}').value).substring(document.getElementById('{0}').selectionStart,document.getElementById('{0}').selectionEnd);document.getElementById('{1}').getElementsByTagName('input')[0].value = sl;";

    SPanel similarPanel;
    SLayoutManager similarPanelLayout;
    /**
     * 
     * @param desc
     * @return
     */
    private SLabel getDescLabel(String desc) {
        
        SLabel label = new SLabel(desc);
        label.setVerticalAlignment(SConstants.TOP_ALIGN);
        return label;
    }

    /**
     * 
     * @param content
     * @return
     */
    private SComponent getContentLabel(String content) {
        SLabel label;
        String con;
        if (content==null)
            con="";
        else
            con=content;
        
        if (con.length() < 100) {
            label = new SLabel(con);
        } else {
            label = new SLabel(con.substring(0, 97) + "...");
            label.setToolTipText(con);
        }
        label.setVerticalAlignment(SConstants.TOP_ALIGN);
        label.setPreferredSize(new SDimension(800, 0));
        label.setWordWrap(true);
        return label;


    }

    /**
     * 
     * @param repo
     * @param id
     * @param searchc
     */
    public MailViewFrame(final Repository repo, final String id, final SearchController searchc, final MailSendHandler mailHandler) {
        try {
            final SearchController sc = searchc;
            Properties sessionprops=new Properties();
            sessionprops.setProperty("mail.mime.address.strict", "false");
            MimeMessage msg = new MimeMessage(Session.getDefaultInstance(sessionprops), repo.getDocument(new MessageID(id)));
            setLayout(new SGridLayout(3, 1));
            setAttribute(CSSProperty.WIDTH, "80%");
            SPanel headerpanel = new SPanel();
            SPanel menupanel = new SPanel();

            SGridLayout headerlayout = new SGridLayout(4, 2);

            headerlayout.setHgap(10);
            //headerlayout.setPreferredSize(new SDimension(600, 0));
            headerpanel.setVerticalAlignment(SConstants.TOP_ALIGN);
            headerpanel.setLayout(headerlayout);

            headerpanel.setHorizontalAlignment(SConstants.LEFT);
            headerpanel.add(getDescLabel("Von:"));
            headerpanel.add(getContentLabel(StringUtil.joinPrettyMail(msg.getFrom(), ",  ")));
            headerpanel.add(getDescLabel("An:"));
            headerpanel.add(getContentLabel(StringUtil.joinPrettyMail(msg.getAllRecipients(), ", ")));
            headerpanel.add(getDescLabel("Datum:"));
            String s;
            if (msg.getSentDate()==null)
                s="";
            else
                s=msg.getSentDate().toString();
            headerpanel.add(getContentLabel(s));
            headerpanel.add(getDescLabel("Betreff:"));
            headerpanel.add(getContentLabel(msg.getSubject()));


            //Mail Sending

        if (mailHandler != null && mailHandler.isReady()) {


            final SMenuBar menuBar = new SMenuBar();
            menuBar.setAttribute(CSSProperty.WIDTH, "100px");
            menuBar.setAttribute(CSSProperty.FLOAT, "left");

            menuBar.setBorder(new SLineBorder(Color.LIGHT_GRAY, 2));



            final SMenu mailSendMenu = new SMenu("Mail verschicken");
 
            menuBar.setBackground(new Color(210, 210, 210));



            SMenu ownAddressMenu = new SMenu("Sende an eigene Adresse");
            for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
                if (ga.getAuthority().startsWith("ROLE_MAIL_")) {
                    final String mailAddress = ga.getAuthority().substring(10);
                    final SMenuItem menuItem = new SMenuItem(mailAddress);
                    menuItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent arg0) {
                            String[] toAddresses = {mailAddress};
                            try {
   
                                    InputStream is = sc.getTableModel().getRepository().getDocument(new MessageID(id));
                                    mailHandler.sendMail(null, toAddresses, new String[0], new String[0], is);

                            } catch (MailSendFailureException ex) {
                                SOptionPane.showMessageDialog(menuItem, "Fehler: " + ex.getLocalizedMessage(), "Information", SOptionPane.ERROR_MESSAGE);
                                Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    ownAddressMenu.add(menuItem);
                }
            }


            mailSendMenu.add(ownAddressMenu);
            if (sc.isAdmin()) {
                SMenuItem sendToItem = new SMenuItem("Sende an ...");
                sendToItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        final SDialog dialog = new SDialog();
                        dialog.add(new SLabel("Zieladresse:"));
                        final STextField toAddress = new STextField();
                        dialog.add(toAddress);
                        SPanel buttonPanel = new SPanel(new SGridLayout(1, 2, 5, 5));
                        SButton okButton = new SButton("OK");
                        buttonPanel.add(okButton);
                        okButton.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                if (toAddress != null && toAddress.getText() != "") {
                                    String[] toAddresses = {toAddress.getText()};
                                    try {
       
                                            InputStream is = sc.getTableModel().getRepository().getDocument(new MessageID(id));
                                            mailHandler.sendMail(null, toAddresses, new String[0], new String[0], is);

                                    } catch (MailSendFailureException ex) {
                                        SOptionPane.showMessageDialog(dialog, "Fehler: " + ex.getLocalizedMessage(), "Information", SOptionPane.ERROR_MESSAGE);
                                        Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    dialog.setVisible(false);
                                }

                            }
                        });

                        SButton cancelButton = new SButton("Abbrechen");

                        buttonPanel.add(cancelButton);

                        cancelButton.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                dialog.setVisible(false);
                            }
                        });

                        dialog.add(buttonPanel);

                        dialog.setVisible(true);
                    }
                });
                mailSendMenu.add(sendToItem);
            }
            final SMenuItem originalItem = new SMenuItem("Sende an Originalempfänger");
            originalItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {

                    try {

                            InputStream is = sc.getTableModel().getRepository().getDocument(new MessageID(id));
                            mailHandler.sendMail(null, null, null, null, is);

                    } catch (MailSendFailureException ex) {
                        SOptionPane.showMessageDialog(menuBar, "Fehler: " + ex.getLocalizedMessage(), "Information", SOptionPane.ERROR_MESSAGE);
                        Logger.getLogger(MailSearch.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                    SOptionPane.showMessageDialog(menuBar, "Email erfolgreich versandt ", "Information", SOptionPane.PLAIN_MESSAGE);
                }
            });
            mailSendMenu.add(originalItem);


            menuBar.add(mailSendMenu);

            menupanel.add(menuBar);
        }

            //Download
            SIcon downloadIcon = new SURLIcon("../images/tango/document-save.png");
            downloadIcon.setIconHeight(18);
            SButton download = new SButton("Email herunterladen");
            download.setIcon(downloadIcon);
            MailResource mailresource = new MailResource("eml", "text/rfc822");
            mailresource.setID(id);
            mailresource.setRepository(repo);
            download.addScriptListener(new JavaScriptListener(JavaScriptEvent.ON_CLICK, "window.location.href='" + this.getSession().getExternalizeManager().externalize(mailresource) + "'"));
            menupanel.add(download);
            //Source

            SIcon sourceIcon = new SURLIcon("../images/tango/format-justify-fill.png");
            sourceIcon.setIconHeight(18);
            SButton source = new SButton("Original");
            source.setIcon(sourceIcon);
            MailResource sourceresource = new MailResource("txt", "text/plain");
            sourceresource.setID(id);
            sourceresource.setRepository(repo);
            source.addScriptListener(new JavaScriptListener(JavaScriptEvent.ON_CLICK, "window.open('" + this.getSession().getExternalizeManager().externalize(sourceresource) + "','"+"Original','menubar=yes,scrollbars=yes,width=1000,height=600')"));

            menupanel.add(source);

            //Print
            SIcon printIcon = new SURLIcon("../images/tango/document-print.png");
            printIcon.setIconHeight(18);
            SButton print = new SButton("Drucken");
            print.setIcon(printIcon);
            MailPrintResource mailprintresource = new MailPrintResource(this.getParentFrame());
            mailprintresource.setFrom(StringUtil.joinPrettyMail(msg.getFrom(), ",  "));
            mailprintresource.setTo(StringUtil.joinPrettyMail(msg.getAllRecipients(), ", "));
            Date d=msg.getSentDate();
            if (d!=null)
                mailprintresource.setSent(d.toString());
            else
                mailprintresource.setSent("");
            mailprintresource.setSubject(msg.getSubject());
            //don't forget to mailprintresource.setMainText() after stripping HTML etc.
            print.addScriptListener(new JavaScriptListener(JavaScriptEvent.ON_CLICK, "window.location.href='" + this.getSession().getExternalizeManager().externalize(mailprintresource) + "'"));
            menupanel.add(print);


            //Similar documents
            SButton similarlink = new SButton("Ähnliche Mails");
            SIcon similarIcon = new SURLIcon("../images/tango/system-search.png");
            similarIcon.setIconHeight(18);
            similarlink.setIcon(similarIcon);
            similarlink.setBackground(new Color(220, 220, 220));
            //don't forget to addActionListener after extracting text

            similarlink.setHorizontalAlignment(SConstants.LEFT);
            menupanel.add(similarlink);

//            //Marked text search
//            SButton markedTextSearch = new SButton("Markierten Text suchen");
//            menupanel.add(markedTextSearch);
//            //DEBUG: show selection
//            final  STextField selection = new STextField();
//            menupanel.add(selection);
//            selection.setVisible(false);
            
            menupanel.setHorizontalAlignment(SConstants.LEFT);

            SPanel contentpanel = new SPanel();
            //contentpanel.setPreferredSize(new SDimension(600, 600));
            Object content = msg.getContent();
            if (content instanceof String) {
                String contentString = (String) content;

                if (msg.isMimeType("text/html") || contentString.trim().toLowerCase().startsWith("<html>")) {
                    presentableText = stripHTML(contentString);
                } else {
                    presentableText = contentString;
                }
                final STextArea textarea = new STextArea(presentableText);
//                SComponent[] components = new SComponent[]{selection,textarea};
//                markedTextSearch.addScriptListener(new JavaScriptListener(JavaScriptEvent.ON_CLICK, "document.getElementById('{0}').value=(document.getElementById('{1}').value).substring(document.getElementById('{1}').selectionStart,document.getElementById('{1}').selectionEnd); ", components));
//                markedTextSearch.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent arg0) {
//                        selection.getText();
//                    }
//                });
                textarea.setEditable(false);
                textarea.setColumns(110);
                textarea.setRows(30);

                contentpanel.add(textarea);
            } else if (content instanceof MimeMultipart) {
                contentpanel.add(renderMultipart((MimeMultipart) content));
            } else {
                contentpanel.add(new SLabel(content.toString()));
            }
           similarPanelLayout= new SFlowLayout();
           similarPanel = new SPanel(similarPanelLayout);
           similarPanel.setPreferredSize(new SDimension(600,0));
           similarPanel.setHorizontalAlignment(SConstants.LEFT_ALIGN);
           
            //Give stripped text to desired other functions
            mailprintresource.setMainText(presentableText);
            final String finalText = presentableText;
            similarlink.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    //SDialog similars = new SDialog(getParentFrame());
                    //similars.setTitle("Ähnliche Mails");
                    //similars.setX(700);
                    
                  
                    //similars.add(similarPanel);
                    //similars.setVisible(true);
                    
                    similarPanel.add(new SLabel("Suche..."));
                    TopScoreDocCollector tdc = sc.getSimilarMails(finalText, 11);
                    similarPanel.removeAll();
                    if (tdc == null || tdc.getTotalHits() <= 1) {
                        similarPanel.add(new SLabel("Keine Ergebnisse! "));
                        return;
                    }
                    ScoreDoc[] hits = tdc.topDocs().scoreDocs;
                    similarPanel.add(new SLabel("Ähnliche Ergebnisse:"));
                    
                    for (int i = 1; i < hits.length; i++) {
                        float similarity = hits[i].score;
                        Document doc;
                        try {
                            doc = sc.getTableModel().getIndexSearcher().doc(hits[i].doc);
                        } catch (CorruptIndexException ex) {
                            Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        } catch (IOException ex) {
                            Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        }
                        
                        SButton hitShow = new SButton(doc.getField("title").stringValue());
                       hitShow.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                        hitShow.setBackground(Color.WHITE);
                     
                               
                        final String idString = doc.getField("id").stringValue();
                        hitShow.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent arg0) {
                                SDialog mailview = new SDialog(getParentFrame(), "Mail anzeigen", true);
                                mailview.setX(20);
                                mailview.setY(20);
                                mailview.setModal(false);
                                mailview.add(new MailViewFrame(repo, idString, sc,mailHandler));
                                mailview.setVisible(true);
                            }
                        });

                        similarPanel.add(hitShow);
                    }



                }
            });
            add(headerpanel);
            add(menupanel);
            add(similarPanel); 
            add(contentpanel);
            
        } catch (IOException ex) {
            Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
            add(new SLabel("Eingabe/Ausgabefehler beim Zugriff auf die Mail "+id+" im Repository "+repo.getDescription()+"!\n " + ex.toString()));
        } catch (MessagingException ex) {
            Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
            add(
                    new SLabel("Fehler beim Verarbeiten der Email "+id+"! " + ex.toString()));
        } catch (NullPointerException ex) {
            Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
            add(new SLabel("Fehler! " + ex));
        } catch (Exception ex) {
             Logger.getLogger(MailViewFrame.class.getName()).log(Level.SEVERE, null, ex);
            add(new SLabel("Fehler! " + ex.toString()));
            ex.printStackTrace();
        }
    }

    private void flattenMultiParts(LinkedList<MimePart> parts, MimeMultipart multipart) throws MessagingException, IOException {
        for (int i = 0; i <
                multipart.getCount(); i++) {
            if (multipart.getBodyPart(i).isMimeType("multipart/*")) {
                flattenMultiParts(parts, (MimeMultipart) (multipart.getBodyPart(i).getContent()));
            } else {
                parts.add((MimePart) multipart.getBodyPart(i));
            }

        }

    }

    private SComponent renderAttachment(MimePart p) throws MessagingException {
        String desc = p.getFileName();
        if (desc == null) {
            desc = "";
        }

        desc = desc + " (" + p.getContentType() + ")";

        SLabel attachment = new SLabel(desc);
        attachment.setIcon(Icons.getIcon(p.getContentType()));
        MailAttachmentResource attres = new MailAttachmentResource("", p.getContentType());
        attres.setAttachment(p);
        SAnchor link = new SAnchor(this.getSession().getExternalizeManager().externalize(attres));
        link.add(attachment);
        link.setHorizontalAlignment(SConstants.LEFT);
        return link;
    }

    private SPanel renderTextContent(String s) {
        SPanel panel = new SPanel();
        STextArea textarea = new STextArea((String) s);
        textarea.setEditable(false);
        textarea.setColumns(110);
        textarea.setRows(30);
        panel.add(textarea);
        return panel;
    }

    private String stripHTML(String s) {
        return s.replaceAll("<.*>", "");
    }

    private SComponent renderMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        LinkedList<MimePart> parts = new LinkedList<MimePart>();
        flattenMultiParts(parts, multipart);
        MimePart MainPart = parts.pop();
        SPanel composedPanel = new SPanel(new SGridLayout(2, 1));

        if (MainPart.isMimeType("text/html")) {
            presentableText = stripHTML((String) MainPart.getContent().toString());
        } else {
            presentableText = (String) MainPart.getContent().toString();
        }

        SPanel contentPanel = renderTextContent(presentableText);
        composedPanel.add(contentPanel);
        SGridLayout attachmentslayout = new SGridLayout(parts.size(), 1);
        attachmentslayout.setVgap(5);
        SPanel attachments = new SPanel(attachmentslayout);
        for (MimePart p : parts) {
            attachments.add(renderAttachment(p));
        }

        attachments.setHorizontalAlignment(SConstants.LEFT);
        composedPanel.add(attachments);
        return composedPanel;
    }
}
