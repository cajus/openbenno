/*  
 * SearchResultTableCellRenderer.java  
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


import de.lwsystems.mailarchive.web.domain.PopupLabelData;
import java.awt.Color;
import org.wings.SComponent;
import org.wings.SConstants;
import org.wings.SDimension;
import org.wings.SGridLayout;
import org.wings.SIcon;
import org.wings.SLabel;
import org.wings.SPanel;
import org.wings.STable;
import org.wings.SURLIcon;
import org.wings.border.SDefaultBorder;
import org.wings.style.CSSProperty;
import org.wings.table.SDefaultTableCellRenderer;
import org.wings.table.STableCellRenderer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author wiermer
 */
public class SearchResultTableCellRenderer implements STableCellRenderer {

    private SDefaultTableCellRenderer delegate = new SDefaultTableCellRenderer();


    /**
     * 
     * @param table
     * @param value
     * @param isSelected
     * @param row
     * @param column
     * @return
     */
    public SComponent getTableCellRendererComponent(STable table, Object value, boolean isSelected, int row, int column) {


        if (column==2) {//Attachments
            SLabel attach=new SLabel();
            attach.setBorder(SDefaultBorder.INSTANCE);
            attach.setIconTextGap(0);
            
            attach.setPreferredSize(new SDimension(22,22));
            if (((String)value).equals("true")) {
                SIcon attachIcon=new SURLIcon("../images/tango/mail-attachment.png");
                attachIcon.setIconWidth(16);
                attach.setIcon(attachIcon);
                attach.setHorizontalAlignment(SConstants.LEFT_ALIGN);
            }
            return attach;
        }
        if (value instanceof PopupLabelData) {

            SLabel text = new SLabel(((PopupLabelData) value).getShortDesc());
            
            if (column == 3) { //Subject
                text.setForeground(Color.BLACK);
                text.setAttribute(CSSProperty.FONT_WEIGHT, "bold");
                text.setToolTipText(((PopupLabelData) value).getLongDesc());
                
                SGridLayout showLayout=new SGridLayout(1,2);
                showLayout.setHgap(5);
                SPanel show=new SPanel(showLayout);
              
                show.add(text); 
                SLabel summary=new SLabel(((PopupLabelData) value).getSummary());
                summary.setForeground(Color.GRAY);
                show.add(summary);

                return show;


            } else {
                text.setToolTipText(((PopupLabelData) value).getLongDesc());
            }
            return text;
        }

        return delegate.getTableCellRendererComponent(table, value, isSelected, row, column);
    }
}
