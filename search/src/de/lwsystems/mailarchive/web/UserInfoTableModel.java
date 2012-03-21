/*  
 * UserInfoTableModel.java  
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

import de.lwsystems.mailarchive.web.domain.UserRightEntity;
import de.lwsystems.utils.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import de.lwsystems.mailarchive.web.login.ListUserDetailsManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 *
 * @author rene
 */
class UserInfoTableModel extends DefaultTableModel {

    String user;
    SearchController sc;
    int rows = 2;
    ListUserDetailsManager udm;
    UserDetails userdetails = null;

    public UserInfoTableModel(SearchController sc, ListUserDetailsManager um) {
        this.sc = sc;
        udm = um;
    }

    public void reload() {
        try {
            userdetails = udm.loadUserByUsername(user);
        } catch (UsernameNotFoundException ex) {
            userdetails = null;
        }
    }

    public void updateModelFromUsername(String username) {
        user = username;
        reload();
    }

    public UserDetails getUserDetails() {
        return userdetails;
    }

    public void deleteRows(int[] rows) {
        if (userdetails == null) {
            return;
        }
        GrantedAuthority[] ga = userdetails.getAuthorities();
        LinkedList<GrantedAuthority> ll = new LinkedList<GrantedAuthority>();
        Collection<Integer> r = new Vector<Integer>();
        for (int i : rows) {
            r.add(new Integer(i));
        }
        for (int i = 0; i < ga.length; i++) {
            if (!r.contains(new Integer(i))) {
                ll.add(ga[i]);
            }
        }
        UserDetails ud = new User(userdetails.getUsername(),
                userdetails.getPassword(),
                true, true, true, true,
                (GrantedAuthority[]) ll.toArray(new GrantedAuthority[0]));
        udm.updateUser(ud);
        userdetails = udm.loadUserByUsername(userdetails.getUsername());
    }

    @Override
    public int getRowCount() {
        if (userdetails == null) {
            return 0;
        }
        return userdetails.getAuthorities().length;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Typ";
            case 1:
                return "Berechtigung";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int arg0) {
        return String.class;

    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;

    }

    @Override
    public Object getValueAt(int row, int col) {
        String role = userdetails.getAuthorities()[row].getAuthority();

        if (col == 0) {
            if (role.startsWith("ROLE_MAIL_")) {
                return UserRightEntity.MAIL;
            }
            if (role.startsWith("ROLE_QUERY_")) {
                return UserRightEntity.QUERY;
            }
            return UserRightEntity.ROLE;

        } else {
            if (role.startsWith("ROLE_MAIL_")) {
                return role.substring(10);
            }
            if (role.startsWith("ROLE_QUERY_")) {
                 return new String(Base64.decode(role.substring(11)));

            }
            return role.substring(5);
        }
    }

    @Override
    public void setValueAt(Object arg0, int arg1, int arg2) {
        //not needed
    }

    public void addRight(UserRightEntity userRightEntity, String text) {
        if (userdetails == null) {
            return;
        }
        GrantedAuthority[] newga = new GrantedAuthority[userdetails.getAuthorities().length + 1];
        int i = 0;
        for (GrantedAuthority ga : userdetails.getAuthorities()) {
            newga[i++] = ga;
        }
        String role = userRightEntity.getPrefix();
        if (userRightEntity == UserRightEntity.QUERY) {
            role += Base64.encodeBytes(text.getBytes());
        } else {
            role += text;
        }
        newga[i] = new GrantedAuthorityImpl(role);

        UserDetails newuser = new User(userdetails.getUsername(), userdetails.getPassword(), true, true, true, true, newga);
        udm.updateUser(newuser);
        updateModelFromUsername(user);
        fireTableDataChanged();
    }

    /*removes old roles and set a new Role.
     *  Input: null for normal user; "AUDITOR" or "ADMIN" 
     */
    public void setRole(String role) {
        removeRight(UserRightEntity.ROLE, "AUDITOR");
        removeRight(UserRightEntity.ROLE, "ADMIN");
        if (role == null) {
            return;
        }
        if (role.equals("AUDITOR")) {
            addRight(UserRightEntity.ROLE, "AUDITOR");
            return;
        }
        if (role.equals("ADMIN")) {
            addRight(UserRightEntity.ROLE, "ADMIN");
            return;
        }
    }

    public String getRole() {
        String found = null;
        for (int i = 0; i < getRowCount(); i++) {
            if ((UserRightEntity) getValueAt(i, 0) == UserRightEntity.ROLE) {
                found = (String) getValueAt(i, 1);
            }
        }
        return found;
    }

    private void removeRight(UserRightEntity userRightEntity, String value) {
        int rolerow = -1; //stays -1, if none is found);
        for (int i = 0; i < getRowCount(); i++) {
            if (userRightEntity == (UserRightEntity) getValueAt(i, 0) && value.equals((String) getValueAt(i, 1))) {
                rolerow = i;
                break;
            }
        }
        if (rolerow >= 0) {
            int[] rowarray = {rolerow};
            deleteRows(rowarray);
        }
    }
}
