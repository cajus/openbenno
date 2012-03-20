/*  
 * StringUtil.java  
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
package de.lwsystems.mailarchive.web.util;


import java.util.Collection;
import java.util.Iterator;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.LinkedList;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author rene
 */

public class StringUtil {
   

    public static String removeLinebreaks(String s) {
        return s.replaceAll("\\n|\\r", " ");
    }
    /**
     * 
     * @param s
     * @param delimiter
     * @return
     */
    public static String join(Collection s, String delimiter) {
        if (s == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    /**
     * 
     * @param s
     * @param delimiter
     * @return
     */
    public static String join(Object[] s, String delimiter) {
        if (s == null || s.length == 0) {
            return "";
        }
        LinkedList ll = new LinkedList();
        for (Object o : s) {
            ll.add(o);
        }
        return join(ll, delimiter);
    }

  

    public static String joinPrettyMail(Address[] a, String delimiter) {
        if (a == null || a.length == 0) {
            return "";
        }
        LinkedList ll = new LinkedList();
        for (Address o : a) {
            if (o instanceof InternetAddress) {
                InternetAddress ia=(InternetAddress)o;
                String personal=ia.getPersonal();
                System.out.println("Personal: "+personal);
                if (personal==null||personal.equals("")){
                    ll.add(ia.getAddress());
                } else {
                    ll.add(personal);
                }
            } else {
                ll.add(o);
            }
        }
        return join(ll, delimiter);
    }
    
    /**
     * 
     * @param e
     * @return
     */
    public static boolean isEmailAdress(String e) {
        final String regex = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)" ; 
        return e.matches(regex) ;
    }
}
