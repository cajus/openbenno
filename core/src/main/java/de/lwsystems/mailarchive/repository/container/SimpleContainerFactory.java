/*
 * SimpleContainerFactory.java
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

import de.lwsystems.mailarchive.repository.FilesystemRepository;
import de.lwsystems.mailarchive.repository.MalformedFilenameException;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.repository.UniqueID;
import de.lwsystems.utils.Base64;
import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author wiermer
 */
public class SimpleContainerFactory implements ContainerFactory {

    ContainerSplitStrategy strategy = new MonthlySplitStrategy();

    File root;
    public SimpleContainerFactory(File root) {
        this.root=root;
    }

    public Container createContainer(String name) {
        return new SimpleContainer(new File(root,name));
    }

    public ContainerSplitStrategy getSplitStrategy() {
        return strategy;
    }

    public static String getCanonicalFileName(MessageID id) {
        return Base64.encodeBytes(id.getDigest(), Base64.URL_SAFE) + id.getSupplemental() + ".gz";
    }

    public static FilenameFilter getDigestFilenameFilter(byte[] digest) {
        final String beginName = Base64.encodeBytes(digest, Base64.URL_SAFE);
        return new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith(beginName);
            }
        };
    }

    public static MessageID getIDByFilename(String fname) throws MalformedFilenameException {
        if (fname.length() < (UniqueID.DIGEST_LENGTH_BASE64 + 3)) {
            throw new MalformedFilenameException();
        }
        byte[] digest = Base64.decode(fname.substring(0, UniqueID.DIGEST_LENGTH_BASE64), Base64.URL_SAFE);
        String supplemental = "";
        if (fname.length() > (UniqueID.DIGEST_LENGTH_BASE64 + 3)) {
            supplemental = fname.substring(UniqueID.DIGEST_LENGTH_BASE64, fname.length() - 3);
        }
        return new MessageID(digest, supplemental);
    }

    public Container makeDefaultContainer() {
        return createContainer("_def");
    }

    public String getContextAwareCanonicalFileName(MessageID id) {
       return SimpleContainerFactory.getCanonicalFileName(id);
    }
}
