/*  
 * UserRightEntity.java  
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

/**
 *
 * @author rene
 */

package de.lwsystems.mailarchive.web.domain;



public enum UserRightEntity {

    MAIL("Emailadresse","ROLE_MAIL_"),
    QUERY("Suchanfrage","ROLE_QUERY_"),
    ROLE("Rolle","ROLE_");
    
    private String Description;
    private String prefix;

    UserRightEntity(String desc,String prefix) {
        this.Description=desc;
        this.prefix=prefix;
    }
     
    public String getDescription() {
        return Description;
    }
    public String getPrefix() {
        return prefix;
    }
    @Override
    public String toString() {
        return getDescription();
    }
}
