/*  
 * MailResource.java  
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


import de.lwsystems.mailarchive.repository.Repository;
import de.lwsystems.mailarchive.repository.MessageID;
import java.io.IOException;
import java.io.InputStream;
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
public class MailResource extends DynamicResource {

    private Repository repo;
    private String msgid;

    /**
     * 
     * @param extension
     * @param mimeType
     */
    public MailResource(String extension, String mimeType) {
        super(extension, mimeType);
    }

    /**
     * 
     * @param frame
     */
    public MailResource(SFrame frame) {
        this(frame, "", "");
    }

    /**
     * 
     * @param frame
     * @param extension
     * @param mimeType
     */
    public MailResource(SFrame frame, String extension, String mimeType) {
        super(frame, extension, mimeType);
    }

    /**
     * 
     * @param repo
     */
    public void setRepository(Repository repo) {
        this.repo = repo;
    }

    /**
     * 
     * @param id
     */
    public void setID(String id) {
        this.msgid = id;
    }

    /**
     * 
     * @param out
     * @throws java.io.IOException
     * @throws org.wings.resource.ResourceNotFoundException
     */
    public void write(Device out) throws IOException, ResourceNotFoundException {
        MessageID messageid = new MessageID(msgid);
        InputStream is = repo.getDocument(messageid);
        DeviceOutputStream os=new DeviceOutputStream(out);
        byte[] buf=new byte[4096];
        int nlength;
        while ((nlength=is.read(buf)) != -1) {
            os.write(buf, 0, nlength);
        }
        os.flush();
        os.close();
    }
}
