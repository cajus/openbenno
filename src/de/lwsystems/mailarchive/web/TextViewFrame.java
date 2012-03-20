/*
 * TextViewFrame.java
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
import de.lwsystems.utils.LimitInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wings.SPanel;
import org.wings.SScrollPane;
import org.wings.STextArea;


/**
 *
 * @author wiermer
 */
public class TextViewFrame extends SPanel{
    static final long BYTE_CUTOFF=1024*1024;
    public TextViewFrame(InputStream is) {
        InputStream mailStream=new LimitInputStream(is, BYTE_CUTOFF);
        BufferedReader reader=new BufferedReader(new InputStreamReader(mailStream));
        StringBuilder sb=new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(TextViewFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                mailStream.close();
                 reader.close();
            } catch (IOException ex) {
                Logger.getLogger(TextViewFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        STextArea textArea=new STextArea(sb.toString());
        SScrollPane scrollPane=new SScrollPane(textArea);
        add(scrollPane);



    }

}
