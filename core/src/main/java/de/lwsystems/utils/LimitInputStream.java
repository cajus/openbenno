/*  
 * LimitedInputStream.java  
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

package de.lwsystems.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author rene
 */
public class LimitInputStream extends InputStream {

    InputStream is;
    long limit  = 0;
    long alreadyRead=0;


    public LimitInputStream(InputStream in, long limit) {
        
        is = in;
        this.limit = limit;
    }
    @Override 
    public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
    }


    @Override 
    public int read(byte[] b, int offset, int len) throws IOException {
        if (alreadyRead>=limit)
            return -1;
        long availableBytes=limit-alreadyRead;
        if (len>availableBytes) {
           len=(int)availableBytes; 
        }
        int bytesActuallyRead = is.read(b, offset, len);
        alreadyRead+=bytesActuallyRead;
        return bytesActuallyRead;
    }
    
    @Override
    public int read() throws IOException {
        if (alreadyRead<limit) {
            alreadyRead++;
            return is.read();
        }
        return -1;
    }
}
