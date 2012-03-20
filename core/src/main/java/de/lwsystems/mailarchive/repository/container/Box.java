/*
 * Box.java
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
package de.lwsystems.mailarchive.repository.container;

import de.lwsystems.mailarchive.repository.MessageID;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author wiermer
 */
public abstract class  Box {
    public static int OPEN=0;
    public static int CLOSED=1;
    private static int CLOSE;
    private int status=Box.OPEN;

    abstract File getDirectory();
    public int getStatus() {
        return status;
    }

    private void close() {
        status=Box.CLOSE;
    }

    public abstract void add(File f) throws IOException;

    //returns the pure name of directory the Box is in
    public abstract String getBoxName();

    public abstract String getContainerName();
    //returns the filename
    abstract String  getFileName(MessageID id);

    abstract Iterable<MessageID> getDocumentIDsByDigest(byte[] digest);


  
}
