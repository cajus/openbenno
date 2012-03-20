/*  
 * AbstractRepository.java  
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

package de.lwsystems.mailarchive.repository;

import de.lwsystems.mailarchive.repository.log.RepositoryLogReader;
import de.lwsystems.mailarchive.repository.log.RepositoryLogWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 *  The interface of a repository.
 * @author wiermer
 */
public interface Repository {
    public String getDescription();
    public  long size();
    /**
     * Returns an iterator over all messages in the repository. Fill MessageID with full path.
     * @return Iterator<MessageID> msgs
     */
    public  Iterator<MessageID> getIterator();
    public  InputStream getDocument(MessageID id);
    public String[] getRecipients(MessageID id);
    public  Collection<MessageID> getDocumentIDsByDigest(byte[] digest);
    public  MessageID addDocument(InputStream document, String[] recipients) throws DuplicateException;
    //public  boolean deleteDocument (MessageID id);
    public RepositoryLogReader getLogReader();
    public RepositoryLogWriter getLogWriter();
}
