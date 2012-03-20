/*  
 * MailArchiveLdapAuthoritiesPopulator.java  
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

package de.lwsystems.mailarchive.web.login;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.springframework.ldap.core.ContextSource;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;

/**
 *
 * @author rene
 */
public class MailArchiveLdapAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator{

    /**
     * 
     * @param arg0
     * @param arg1
     */
    public MailArchiveLdapAuthoritiesPopulator(ContextSource arg0, String arg1) {
        super(arg0, arg1);
    }

 
    /**
     * 
     * @param user
     * @param username
     * @return
     */
    @Override
    protected Set getAdditionalRoles(org.springframework.ldap.core.DirContextOperations user,
                                 String username) {
        LinkedList<String> roles=new LinkedList<String>();
        String[] mailaddresses =user.getStringAttributes("mail");
        String[] aliasaddresses=user.getStringAttributes("alias");
        return new HashSet(roles);
    }    
    

}
