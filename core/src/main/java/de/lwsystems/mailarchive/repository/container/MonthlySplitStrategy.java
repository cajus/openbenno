/*
 * MonthlySplitStrategy.java
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
 */package de.lwsystems.mailarchive.repository.container;

import de.lwsystems.mailarchive.parser.Field;
import de.lwsystems.mailarchive.parser.MetaDocument;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author wiermer
 */
public class MonthlySplitStrategy implements ContainerSplitStrategy {

    String[] emailFields = {"to", "from"};
    String[] domainFields = {"fromdomain", "todomain"};

    Logger logger = LoggerFactory.getLogger(MonthlySplitStrategy.class.getName());

    boolean docMatchesContainer(Container c, MetaDocument doc) {


        if (c.isDefaultContainer()) {
            return true;
        }
        String decisionString = c.getContainerCriteria();
        String[] parse = decisionString.split(":", 2);
        Collection<Field> fields = doc.getStoredFields();
        String[] fieldsToSearch = {};

        if (parse.length == 1) {

            //we have a simple case -- assume email address or domain here
            fieldsToSearch = emailFields;
            if (decisionString.startsWith("@")) {
                //domain name
                fieldsToSearch = domainFields;
                decisionString = decisionString.substring(1);
            }
        } else {
            //something like "field:content-of-field"
            fieldsToSearch = new String[1];
            fieldsToSearch[0] = parse[0].toLowerCase();
            decisionString = parse[1];
        }
        Pattern p = null;
        if (decisionString.startsWith(":")) {
            //we want to define a regex
            decisionString = decisionString.substring(1);
            p = Pattern.compile(decisionString);
        }
        for (Field f : fields) {
            for (String ff : fieldsToSearch) {

                if (f.getKey().equalsIgnoreCase(ff)) {

                    if (p == null) {
                        if (decisionString.equalsIgnoreCase(f.getPayload().toString())) {

                            return true;
                        }
                    } else {
                        Matcher m = p.matcher(f.getPayload().toString());
                        if (m.matches()) {

                            return true;
                        }
                    }
                }
            }
        }
        return false;

    }

    public Iterable<Box> belongsTo(Container c, MetaDocument doc) {

        if (docMatchesContainer(c, doc)) {

            Date decisionDate = new Date(); //default to current date
            //try to set it to
            try {
                if (doc.getReceived() != null) {
                    Date d = MetaDocument.getDateFormat().parse(doc.getReceived());
                    decisionDate = d;
                }
            } catch (ParseException ex) {
                logger.error("Cannot parse container date.", ex);
            }
            ArrayList<Box> boxes = new ArrayList(1);
            Calendar cal = new GregorianCalendar();
            cal.setTime(decisionDate);

            boxes.add(c.createBox("" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH)+1)));
            return boxes;
        }
        return null;

    }
}
