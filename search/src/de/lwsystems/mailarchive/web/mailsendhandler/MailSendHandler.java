/*  
 * MailSendHandler.java  
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
package de.lwsystems.mailarchive.web.mailsendhandler;

import java.io.InputStream;

/**
 * (Re)send an email  from a stream. possibly modify from and recipients. Setting to, from or bcc to null leaves the original recipients unchanged.
 * @author wiermer
 */
public interface MailSendHandler {

    public boolean isReady();

    public void sendMail(String from, String to[], String[] cc, String[] bcc, InputStream in) throws MailSendFailureException;
}
