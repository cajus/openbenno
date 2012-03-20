/*
 * ExtendedLink.java
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
package de.lwsystems.mailarchive.web;

import java.io.IOException;
import org.wings.URLResource;
import org.wings.header.Link;
import org.wings.io.Device;

/**
 *
 * @author wiermer
 */
public class ExtendedLink extends Link {
    
    protected String title=null;
        public ExtendedLink(String rel, String rev, String type, String target, URLResource urlSource,String title) {
       super(rel,rev,type,target,urlSource);
       this.title=title;
    }
    @Override
    public void write(Device d) throws IOException {
        d.print("<link");
        if (rel != null)
            d.print(" rel=\"" + rel + "\"");
        if (rev != null)
            d.print(" rev=\"" + rev + "\"");
        if (type != null)
            d.print(" type=\"" + type + "\"");
        if (target != null)
            d.print(" target=\"" + target + "\"");
        if (title != null)
            d.print(" title=\"" + title + "\"");
        if (urlSource != null && urlSource.getURL() != null) {
            d.print(" href=\"");
            urlSource.getURL().write(d);
            d.print("\"");
        }
        d.print("/>");
    }


}
