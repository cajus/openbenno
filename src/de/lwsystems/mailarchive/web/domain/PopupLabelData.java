/*  
 * PopupLabelData.java
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


/**
 *
 * @author wiermer
 */
public class PopupLabelData {
    String shortdesc;
    String longdesc;
    String summary;

    /**
     * 
     * @return
     */
    public String getLongDesc() {
        return longdesc;
    }

    /**
     * 
     * @param longdesc
     */
    public void setLongDesc(String longdesc) {
        this.longdesc = longdesc;
    }

    /**
     * 
     * @return
     */
    public String getShortDesc() {
        return shortdesc;
    }

    /**
     * 
     * @return
     */
    public String getSummary() {
        return summary;
    }
    /**
     * 
     * @param shortdesc
     */
    public void setShortDesc(String shortdesc) {
        this.shortdesc = shortdesc;
    }
    
    
    /**
     * 
     * @param shortdesc
     * @param longdesc
     */
    public PopupLabelData(String shortdesc,String longdesc) {
        this.shortdesc=shortdesc;
        this.longdesc=longdesc;
        this.summary="";
    }
    
    /**
     * 
     * @param shortdesc
     * @param longdesc
     * @param summary
     */
    public PopupLabelData(String shortdesc,String longdesc,String summary) {
        this(shortdesc,longdesc);
        this.summary=summary;
    }
}
