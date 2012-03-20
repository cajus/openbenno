/*  
 * MailAttachmentResource.java  
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
 */ package de.lwsystems.mailarchive.web.resources;


import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.internet.MimePart;
import org.wings.SFrame;
import org.wings.io.Device;
import org.wings.io.DeviceOutputStream;
import org.wings.resource.DynamicResource;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import org.wings.resource.ResourceNotFoundException;

/**
 *
 * @author wiermer
 */
public class MailAttachmentResource extends DynamicResource {

    MimePart part;

    /**
     * 
     * @param arg0
     * @param arg1
     * @param arg2
     */
    public MailAttachmentResource(SFrame arg0, String arg1, String arg2) {
        super(arg0, arg1, arg2);
    }

    /**
     * 
     * @param arg0
     */
    public MailAttachmentResource(SFrame arg0) {
        super(arg0);
    }

    /**
     * 
     * @param arg0
     * @param arg1
     */
    public MailAttachmentResource(String arg0, String arg1) {
        super(arg0, arg1);
    }

    /**
     * 
     * @param part
     */
    public void setAttachment(MimePart part) {
        this.part = part;
    }

    /**
     * 
     * @param out
     * @throws java.io.IOException
     * @throws org.wings.resource.ResourceNotFoundException
     */
    public void write(Device out) throws IOException, ResourceNotFoundException {
        InputStream is;
        DeviceOutputStream os = new DeviceOutputStream(out);
        byte[] buf = new byte[4096];
        int nlength;
        try {
            is = part.getInputStream();
        } catch (MessagingException ex) {
            Logger.getLogger(MailAttachmentResource.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        while ((nlength = is.read(buf)) != -1) {
            os.write(buf, 0, nlength);
        }
        os.flush();
        os.close();
    }
}
