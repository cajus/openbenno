/*  
 * AddressList.java  
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
 */ package de.lwsystems.mailarchive.web.domain;


import java.util.LinkedList;

/**
 * 
 * @author wiermer
 */
public class AddressList {

    String[] addresses = {""};

    public AddressList(String s) {
        addresses = s.split(",");
    }

    public String[] getAddresses() {
        return addresses;
    }

    public String[] getAddressNames() {
        LinkedList<String> addrnames = new LinkedList<String>();
  //      InternetAddress surl = new InternetAddress();
        for (String s : addresses) {
//            try {
//                surl = new InternetAddress(s);
//            } catch (AddressException ex) {
//                Logger.getLogger(AddressList.class.getName()).log(Level.SEVERE, null, ex);
//                addrnames.add(s);
//            }
//            if (surl.getPersonal() != null) {
//                addrnames.add(surl.getPersonal());
//            } else {
//                addrnames.add(surl.getAddress());
//            }
            addrnames.add(s);
        }
        return (String[]) addrnames.toArray();
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public AddressList(String[] addr) {
        if (addr != null) {
            this.addresses = addr;
        }

    }
}
    


