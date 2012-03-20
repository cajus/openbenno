/*  
 * Field.java  
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
package de.lwsystems.mailarchive.parser;

/**
 * Represents a pair of key/value for indexing.
 * @author wiermer
 */
public class Field {

    String key;
    Object payload;
    boolean tokenized;

    public boolean isTokenized() {
        return tokenized;
    }

    public void setTokenized(boolean tokenized) {
        this.tokenized = tokenized;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Field(String key, Object payload, boolean tokenized) {
        this.key = key;
        this.payload = payload;
        this.tokenized = tokenized;
    }

    public Field(String key, Object payload) {
        this(key, payload, false);
    }
    
    @Override
    public String toString() {
        return(key+" -> "+payload);
    }
}
