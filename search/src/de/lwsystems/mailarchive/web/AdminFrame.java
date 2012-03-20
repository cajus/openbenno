/*  
 * AdminFrame.java  
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
import de.lwsystems.mailarchive.web.domain.UserRightEntity;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import de.lwsystems.mailarchive.web.login.ListUserDetailsManager;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.web.context.support.ServletContextResource;
import org.wings.SButton;
import org.wings.SButtonGroup;
import org.wings.SComboBox;
import org.wings.SComponent;
import org.wings.SConstants;
import org.wings.SDialog;
import org.wings.SDimension;
import org.wings.SGridLayout;
import org.wings.SLabel;
import org.wings.SList;
import org.wings.SPanel;
import org.wings.SPasswordField;
import org.wings.SRadioButton;
import org.wings.SScrollPane;
import org.wings.STable;
import org.wings.STextField;
import org.wings.table.SDefaultTableCellRenderer;
import org.wings.table.STableCellRenderer;
import org.wingx.XTable;

/**
 * This class represents the content of the Admin/User management dialog.
 * @author rene
 */
class AdminFrame extends SPanel {

    SearchController sc;

    private SPanel createDirectoryOptions() {
        SPanel p = new SPanel(new SGridLayout(3, 1));
        p.add(new SLabel("Archive"));
        return p;
    }

    private SPanel createUserOptions() {
        BeanFactory factory = new XmlBeanFactory(new ServletContextResource(this.getSession().getServletContext(), "WEB-INF/applicationContext-security.xml"));
        final ListUserDetailsManager userdetailsmanager = (ListUserDetailsManager) factory.getBean("userdetailsmanager");
        final UserInfoTableModel userinfomodel = new UserInfoTableModel(sc, userdetailsmanager);
        final XTable userInfo = new XTable(userinfomodel);
        final SLabel warning = new SLabel("");
        final SList userList = new SList();
        final SButton doNewUser = new SButton("Neuer Benutzer");

        SGridLayout layout = new SGridLayout(1, 3);
        layout.setHgap(10);
        layout.setVgap(5);
        SPanel p = new SPanel(layout);
        p.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        final SButton doDeleteUser = new SButton("Benutzer entfernen");
        doDeleteUser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (userList == null || userList.isSelectionEmpty()) {
                    return;
                }
                final SDialog confirmationDialog = new SDialog(doDeleteUser.getParentFrame());
                confirmationDialog.setModal(true);
                SGridLayout confirmationDialogLayout=new SGridLayout(2,1);
                confirmationDialog.setLayout(confirmationDialogLayout);
                confirmationDialog.add(new SLabel("Sind sie sicher, dass die den Benutzer " + (String) userList.getSelectedValue() + " löschen wollen ?"));
                SButton okButton = new SButton("Ja");
                SButton cancelButton = new SButton("Nein");
                SGridLayout buttonPanelLayout = new SGridLayout(1, 2);
                buttonPanelLayout.setVgap(10);
                buttonPanelLayout.setHgap(10);
                SPanel buttonPanel = new SPanel(buttonPanelLayout);
                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);
                confirmationDialog.add(buttonPanel);
                okButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        userdetailsmanager.deleteUser((String) userList.getSelectedValue());
                        userList.setListData(userdetailsmanager.getUserList());
                        userList.reload();
                        confirmationDialog.setVisible(false);
                    }
                });
                cancelButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        confirmationDialog.setVisible(false);
                    }
                });
                confirmationDialog.setVisible(true);

            }
        });
        final SButton doChangePassword = new SButton("Passwort ändern");

        userInfo.setDefaultRenderer(new STableCellRenderer() {

            SDefaultTableCellRenderer renderer = new SDefaultTableCellRenderer();

            public SComponent getTableCellRendererComponent(STable table, Object value, boolean isSelected, int row, int column) {
                if (value instanceof UserRightEntity) {
                    return new SLabel(((UserRightEntity) value).getDescription());
                }
                return renderer.getTableCellRendererComponent(table, value, isSelected, row, column);
            }
        });
        userInfo.setSelectionMode(STable.MULTIPLE_SELECTION);

        p.add(doDeleteUser);
        p.add(doChangePassword);
        SButton doDeleteEntries = new SButton("Rechte entfernen");
        doDeleteEntries.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        doDeleteEntries.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                int[] rows = userInfo.getSelectedRows();
                if (rows != null || rows.length > 0) {
                    userinfomodel.deleteRows(rows);
                }
                userinfomodel.reload();
                userInfo.reload();
            }
        });
        SGridLayout rightFrameLayout = new SGridLayout(4, 1);
        rightFrameLayout.setVgap(10);
        final SPanel rightFrame = new SPanel(rightFrameLayout);
        rightFrame.setVisible(false);
        rightFrame.setPreferredSize(new SDimension(400, 300));
        rightFrame.setHorizontalAlignment(SConstants.LEFT_ALIGN);

        rightFrame.add(p);
        rightFrame.add(warning);
        userInfo.setPreferredSize(new SDimension(350, 290));
        SScrollPane userInfoscrollpane = new SScrollPane(userInfo);
        userInfoscrollpane.setPreferredSize(new SDimension(400, 300));
        rightFrame.add(userInfoscrollpane);
        rightFrame.add(doDeleteEntries);
        SGridLayout addRightLayout = new SGridLayout(1, 3);
        addRightLayout.setHgap(10);
        SPanel addRight = new SPanel(addRightLayout);
        addRight.setHorizontalAlignment(SConstants.LEFT_ALIGN);

        final SComboBox key = new SComboBox(UserRightEntity.values());
        final STextField value = new STextField();
        final SButton doAddRight = new SButton("Hinzufügen");
        doAddRight.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (key.getSelectedItem()==UserRightEntity.MAIL&&!StringUtil.isEmailAdress(value.getText())) {
                    SDialog warning=new SDialog(doAddRight.getParentFrame());
                    warning.add(new SLabel("Keine gültige Emailadresse !"));
                    warning.setModal(true);
                    warning.setVisible(true);
                    return;
                }
                userinfomodel.addRight((UserRightEntity) key.getSelectedItem(), value.getText());
            }
        });
        addRight.add(key);
        addRight.add(value);
        addRight.add(doAddRight);
        rightFrame.add(addRight);

        //Left Frame

        userList.setListData(userdetailsmanager.getUserList());
        userList.setSelectionMode(SList.SINGLE_SELECTION);
        userList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent arg0) {
                userinfomodel.updateModelFromUsername((String) userList.getSelectedValue());
                userinfomodel.fireTableDataChanged();
                rightFrame.setVisible(true);
            }
        });
        SGridLayout leftFrameLayout = new SGridLayout(2, 1);
        leftFrameLayout.setHgap(10);
        leftFrameLayout.setVgap(10);
        SPanel leftFrame = new SPanel(leftFrameLayout);
        doNewUser.setHorizontalAlignment(SConstants.LEFT_ALIGN);
        leftFrame.add(doNewUser);
        userList.setPreferredSize(new SDimension(200, 300));

        leftFrame.add(userList);
        ActionListener userpasswdActionListener = new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == doChangePassword && userList.getSelectedValue() == null) {
                    return;
                }
                final SDialog passwordDialog = new SDialog(doNewUser.getParentFrame(), "Passworteingabe");
                passwordDialog.setModal(true);
                SGridLayout dialogLayout = new SGridLayout(5, 2);
                dialogLayout.setHgap(10);
                dialogLayout.setVgap(10);
                SPanel dialogPanel = new SPanel(dialogLayout);

                dialogPanel.add(new SLabel("Benutzer: "));
                final STextField usern = new STextField();
                if (arg0.getSource() == doChangePassword) {
                    usern.setText((String) userList.getSelectedValue());
                    usern.setEnabled(false);
                }
                usern.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                dialogPanel.add(usern);
                dialogPanel.add(new SLabel("Passwort eingeben"));
                final SPasswordField pass1 = new SPasswordField();
                pass1.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                dialogPanel.add(pass1);
                dialogPanel.add(new SLabel("Passwort wiederholen"));
                final SPasswordField pass2 = new SPasswordField();
                pass2.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                dialogPanel.add(pass2);
                if (arg0.getSource() == doChangePassword) {
                    pass1.setText(userinfomodel.userdetails.getPassword());
                    pass2.setText(userinfomodel.userdetails.getPassword());
                }
                SLabel textRoles = new SLabel("Rollen:");
                textRoles.setVerticalAlignment(SConstants.TOP_ALIGN);
                dialogPanel.add(textRoles);
                final SButtonGroup additionalRole = new SButtonGroup();
                final SRadioButton normalUser = new SRadioButton("normaler Benutzer");
                final SRadioButton auditorUser = new SRadioButton("Revisor");
                final SRadioButton adminUser = new SRadioButton("Administrator");
                additionalRole.add(normalUser);
                additionalRole.add(auditorUser);
                additionalRole.add(adminUser);

                additionalRole.setSelected(normalUser, enabled);
                if (arg0.getSource() == doChangePassword) {
                    String currentRole = userinfomodel.getRole();
                    if (currentRole != null) {
                        if (currentRole.equals("ADMIN")) {
                            additionalRole.setSelected(adminUser, enabled);
                        } else if (currentRole.equals("AUDITOR")) {
                            additionalRole.setSelected(auditorUser, enabled);
                        }
                    }
                }
                SPanel buttonPanel = new SPanel(new SGridLayout(3, 1));
                buttonPanel.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                normalUser.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                auditorUser.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                adminUser.setHorizontalAlignment(SConstants.LEFT_ALIGN);
                buttonPanel.add(normalUser);
                buttonPanel.add(auditorUser);
                buttonPanel.add(adminUser);
                dialogPanel.add(buttonPanel);
                final SButton doSubmit = new SButton("Speichern");

                doSubmit.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        String warnings = "";
                        if (!pass1.getText().equals(pass2.getText())) {
                            warnings=warnings.concat("Passwörter stimmen nicht überein\n\n");
                        }
                        if (usern.getText().contains(" ")) {
                            warnings=warnings.concat("Benutzername enthält Leerzeichen\n\n");
                        }                   
                        if (!warnings.equals("")) {
                            SDialog warning = new SDialog(doSubmit.getParentFrame());
                            warning.add(new SLabel(warnings));
                            warning.setModal(true);
                            warning.setVisible(true);
                            return;
                        }
                        String passwd=pass1.getText();
                        userdetailsmanager.changePassword(usern.getText(), passwd);


//                        if (additionalRole.getSelection() == normalUser) {
//                            userinfomodel.setRole(null);
//                        } else if (additionalRole.getSelection() == auditorUser) {
//                            userinfomodel.setRole("AUDITOR");
//                        } else if (additionalRole.getSelection() == adminUser) {
//                            userinfomodel.setRole("ADMIN");
//                        }
//                        userinfomodel.updateModelFromUsername(usern.getText());
                        userList.setListData(userdetailsmanager.getUserList());
                        passwordDialog.setVisible(false);
                    }
                });

                dialogPanel.add(doSubmit);

                passwordDialog.add(dialogPanel);
                passwordDialog.setX(20);
                passwordDialog.setY(20);
                passwordDialog.setVisible(true);
            }
        };
        doNewUser.addActionListener(userpasswdActionListener);
        doChangePassword.addActionListener(userpasswdActionListener);
        //whole Frame
        SPanel wholeFrame = new SPanel(new SGridLayout(1, 2));
        leftFrame.setVerticalAlignment(SConstants.TOP_ALIGN);
        rightFrame.setVerticalAlignment(SConstants.TOP_ALIGN);
        wholeFrame.add(leftFrame);
        wholeFrame.add(rightFrame);
        return wholeFrame;
    }

    public AdminFrame(SearchController s) {
        sc = s;
        //STabbedPane tabbedPane = new STabbedPane();
        //tabbedPane.add("Benutzer", createUserOptions());
        //tabbedPane.add("Verzeichnisse", createDirectoryOptions());

        add(createUserOptions());
    }
}
