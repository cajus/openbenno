/*
 * ChecksumBuilder.java
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
 */package de.lwsystems.mailarchive.repository.log;

import de.lwsystems.utils.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author wiermer
 */
public class ChecksumBuilder {

    MessageDigest md;
    public ChecksumBuilder() throws NoSuchAlgorithmException {
         md=MessageDigest.getInstance("sha-256");
    }
    public void append(String s) {
        md.update(s.getBytes());
    }
    public String getChecksum() {
        return Base64.encodeBytes(md.digest());
    }
}
