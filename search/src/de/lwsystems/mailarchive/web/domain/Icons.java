/*  
 * Icons.java  
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
package de.lwsystems.mailarchive.web.domain;

import java.util.Vector;
import org.wings.SIcon;
import org.wings.SResourceIcon;

/**
 *
 * @author rene
 */
public class Icons {

    class Icon {

        public String mimetype;
        public SIcon icon;

        public Icon(String m, SIcon i) {
            mimetype = m;
            icon = i;
        }
    }
    final static SResourceIcon defaulticon = new SResourceIcon("file.png");
    static Vector<Icon> iconmap = new Vector<Icon>();

    /**
     * 
     */
    public Icons() {
        iconmap.add(new Icon("application/pdf", new SResourceIcon("pdf.png")));
    }

    /**
     * 
     * @param mimetype
     * @return
     */
    public static SIcon getIcon(String mimetype) {
        SIcon currentIcon = defaulticon;
        for (Icon i : iconmap) {
            if (mimetype.startsWith(i.mimetype)) {
                currentIcon = i.icon;
                break;
            }
        }
        return currentIcon;
    }
}
