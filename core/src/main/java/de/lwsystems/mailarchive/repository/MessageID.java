/*  
 * MessageID.java  
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

package de.lwsystems.mailarchive.repository;

import de.lwsystems.utils.Base64;

/**
 *
 * @author wiermer
 */
public class MessageID {


    byte[] digest;
    String supplemental="";
    String fullpath=null;

    public String getFullPath() {
        return fullpath;
    }

    public void setFullPath(String fullpath) {
        this.fullpath = fullpath;
    }

    public MessageID(String id) {
        if (id==null||id.length()<UniqueID.DIGEST_LENGTH_BASE64)
            return;
        digest=Base64.decode(id.substring(0,UniqueID.DIGEST_LENGTH_BASE64),Base64.URL_SAFE);
        if (id.length()>UniqueID.DIGEST_LENGTH_BASE64)
            supplemental=id.substring(UniqueID.DIGEST_LENGTH_BASE64);
    }

    public MessageID(byte[] digest,String supplemental) {
        this(digest,supplemental,null);
    }
    
   public MessageID(byte[] digest,String supplemental,String fullpath) {
        this.digest=digest;
        this.supplemental=supplemental;
        this.fullpath=fullpath;
    }
    
    public byte[] getDigest() {
        return digest;
    }

    public String getSupplemental() {
        return supplemental;
    }
    
    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public void setSupplemental(String supplemental) {
        this.supplemental = supplemental;
    }
    
    @Override
    public String toString() {
        if (digest==null)
            return null;
        return (Base64.encodeBytes(digest,Base64.URL_SAFE) + supplemental);
    }
}
