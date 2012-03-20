/*  
 * ServletUtils.java  
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
package de.lwsystems.mailarchive.web.util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.servlet.ServletContext;

/**
 *
 * @author rene
 */
public class ServletUtils {

    static InputStream is;

    /**
     * 
     * @param con
     * @param res
     * @return
     * @throws java.io.IOException
     */
    public static String getResourceAsFilename(ServletContext con, String res) throws IOException {
        is = res.getClass().getClassLoader().getResourceAsStream(res);
        File tempDir = (File) con.getAttribute("javax.servlet.context.tempdir");
        File tempFile = File.createTempFile("loginconf", "tmp", tempDir);
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) >= 0) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();
        is.close();
        return tempFile.getAbsolutePath();
    }
}
