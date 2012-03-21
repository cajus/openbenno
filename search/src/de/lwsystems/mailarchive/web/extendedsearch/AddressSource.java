/*
 * AdresssSource.java
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
package de.lwsystems.mailarchive.web.extendedsearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.wingx.XSuggestDataSource;

class AddressSource implements XSuggestDataSource {

    int SUGGESTIONS_CUTOFF = 20;

    class Entry implements Map.Entry<String, String> {

        String s;
        String v;

        public Entry(String s, String v) {
            super();
            this.s = s;
            this.v = v;
        }

        public String getKey() {
            return s;
        }

        public String getValue() {
            return v;
        }

        public String setValue(String arg0) {
            this.v = arg0;
            return v;
        }
    }
    Set addresses;

    public AddressSource(Set s) {
        addresses = s;
    }

    public List<Map.Entry<String, String>> generateSuggestions(String part) {
        List<Map.Entry<String, String>> returning = new ArrayList<Map.Entry<String, String>>();
        for (Iterator iter = addresses.iterator(); iter.hasNext();) {

            //prevents argument overflow in frontend
            if (returning.size() > SUGGESTIONS_CUTOFF) {
                break;
            }
            Object o = iter.next();
            if (o instanceof String) {
                String s = (String) o;
                String[] singleaddresses = part.split(",");
                if (s.toLowerCase().contains(singleaddresses[singleaddresses.length - 1].trim().toLowerCase())) {
                    String oldadresses = "";
                    for (int i = 0; i < singleaddresses.length - 1; i++) {
                        oldadresses = oldadresses.concat(singleaddresses[i]);
                        oldadresses = oldadresses.concat(",");
                    }
                    returning.add(new AddressSource.Entry(oldadresses.concat(s), s));
                }
            }
        }
        return returning;
    }
}
