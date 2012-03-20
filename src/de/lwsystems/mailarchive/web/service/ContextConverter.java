/*  
 * BennoContext.java  
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
  
package de.lwsystems.mailarchive.web.service;

import java.util.Set;
import javax.xml.bind.annotation.*;
/**
 *
 * @author wiermer
 */
@XmlRootElement(name="context")
public class ContextConverter {
    Set<String> year;
    Set<String> to;
    Set<String> from;
    Set<String> todomain;
    Set<String> fromdomain;
@XmlElement
    public Set<String> getFrom() {
        return from;
    }

    public ContextConverter(Set<String> years, Set<String> to, Set<String> from, Set<String> todomains, Set<String> fromdomains) {
        this.year = years;
        this.to = to;
        this.from = from;
        this.todomain = todomains;
        this.fromdomain = fromdomains;
    }

    public void setFrom(Set<String> from) {
        this.from = from;
    }
@XmlElement
    public Set<String> getFromDomain() {
        return fromdomain;
    }

    public void setFromDomain(Set<String> fromdomains) {
        this.fromdomain = fromdomains;
    }
@XmlElement
    public Set<String> getTo() {
        return to;
    }

    public void setTo(Set<String> to) {
        this.to = to;
    }
@XmlElement
    public Set<String> getToDomain() {
        return todomain;
    }
 
    public void setToDomain(Set<String> todomains) {
        this.todomain = todomains;
    }
    

    public void setYear(Set<String> years) {
        this.year = years;
    }
    
      @XmlElement
    public Set<String> getYear() {
        return year;
    }
    
    
  
    
    public ContextConverter() {
        year=null;
    }
    public ContextConverter(Set<String> years) {
        this.year = years;
    }
    
}
