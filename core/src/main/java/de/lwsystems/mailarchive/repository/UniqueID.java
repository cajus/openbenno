/*
 * UniqueID.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiermer
 */
public class UniqueID {

    static class DigestBufferWrapper {

        public byte[] digest = new byte[DIGEST_LENGTH];
        public byte[] bufDigest = new byte[DIGEST_FROM_SIZE];
        public int bytesRead;
    }
    public static final String DIGEST_ALGORITHM = "MD5"; //use MD5 instead of more secure SHA256 due to size and performance concerns
    public static final int DIGEST_FROM_SIZE = 1024; //use the first 1024 bytes of message
    public static final int DIGEST_LENGTH = 16;
    public static final int DIGEST_LENGTH_BASE64 = ((DIGEST_LENGTH + 3 - (DIGEST_LENGTH % 3)) / 3) * 4;
    Repository repo;
    private static DigestBufferWrapper digbuf = new DigestBufferWrapper();
    private static Collection<MessageID> ids_in_repo;
    static Logger logger = LoggerFactory.getLogger(UniqueID.class.getName());

    public UniqueID() {
    }

    public static MessageDigest getMessageDigester() {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            logger.error("No such algorithm!", ex);
            System.out.println("No such algorithm!");
        }
        return dig;
    }

    public UniqueID(Repository repo) {
        this();
        this.repo = repo;
    }

    public static int getDigestLength() {
        return DIGEST_LENGTH;
    }

    public static byte[] getDigest(InputStream is) throws IOException {
        DigestBufferWrapper digbuffer = getDigestInternal(is);
        if ((digbuffer.bytesRead) < 0) {
            System.out.println("ERROR while reading digest!");
            return null;
        }
        return digbuffer.digest;
    }

    private static synchronized DigestBufferWrapper getDigestInternal(InputStream message) throws IOException {
        MessageDigest dig = getMessageDigester();
        DigestInputStream digin = new DigestInputStream(message, dig);
        digbuf.bytesRead = digin.read(digbuf.bufDigest, 0, DIGEST_FROM_SIZE);
        if (digbuf.bytesRead >= 0) {
            //InputStream is not empty.

            //We need to make sure, we read exactly DIGEST_FROM_SIZE bytes.
            //This is a problem of GZIPInputStreams. which seems to stop at block boundaries.
            int lastRead = 0;
            while ((lastRead >= 0) && digbuf.bytesRead < DIGEST_FROM_SIZE) {
                lastRead = digin.read(digbuf.bufDigest, digbuf.bytesRead, DIGEST_FROM_SIZE - digbuf.bytesRead);
                if (lastRead > 0) {
                    digbuf.bytesRead += lastRead;
                }
            }
        }

        digbuf.digest = digin.getMessageDigest().digest();
        return digbuf;

    }



    /**
     * Get a unique ID for the message.
     * The ID is usually the MD5 sum of the first DIGEST_FROM_SIZE bytes of the
     * message (or less, if the message is shorter).
     * The ID of the existing document is returned, if the same message is
     * already stored.
     * In the (unlikely) case of two different messages with the same digest, an
     * alternative ID (MD5+timestamp) is constructed.
     * new in 1.1.6.2: InputStream -> File;
     */

    public static synchronized MessageID getID(Repository repos, File messageFile) throws DuplicateException, FileNotFoundException {
        DigestInputStream digin = null;
        MessageID messageid;
        System.out.println("Get new ID...");
        DigestBufferWrapper digbuffer;
        InputStream bufMessage=new FileInputStream(messageFile);

        try {
            digbuffer = getDigestInternal(bufMessage);
            messageid = new MessageID(digbuffer.digest, "");

            System.out.println("UniqueID: read for digest");
            logger.debug("Read for digest");
        } catch (IOException ex) {
            logger.error("IO error ", ex);
            return null;
        } catch (Exception ex) {
            logger.error("Unknown error ", ex);
            return null;
        }

        System.out.println("UniqueID: First guess " + messageid.toString());
        logger.debug("First guess {}", messageid.toString());
        ids_in_repo = repos.getDocumentIDsByDigest(messageid.getDigest());

        if (ids_in_repo == null || ids_in_repo.size() == 0) {
            //no file with this digest is in the repo
            System.out.println("UniqueID: none other found in repo with this ID");
            logger.debug("None other found in repo with this ID");
            return messageid;
        } else {
            // at least one file with the same digest is in the repo
            // now check for a duplicate
            System.out.println("UniqueID: another one with same digest in repo");
            logger.debug("Another one with same digest in repo");
            byte[] bufRepo = new byte[DIGEST_FROM_SIZE];
            int bytesReadRepo;
            for (MessageID mid : ids_in_repo) {
                try {
                    InputStream in = repos.getDocument(mid);
                    int lastRead = 0;
                    int toBeRead = Math.min(digbuffer.bytesRead, DIGEST_FROM_SIZE);
                    try {
                        lastRead = in.read(bufRepo, 0, toBeRead);
                        bytesReadRepo = lastRead;
                        while (lastRead > 0 && bytesReadRepo < toBeRead) {
                            lastRead = in.read(bufRepo, bytesReadRepo, toBeRead - bytesReadRepo);
                            bytesReadRepo += lastRead;
                        }
                    } catch (IOException ex) {
                        logger.error("Unknown error ", ex);
                        return null;
                    }
                    if (bytesReadRepo < 0 || bytesReadRepo > digbuffer.bytesRead) {
                        //cannot match
                        System.out.println("UniqueID: Messages don't match due to size " + bytesReadRepo + " != " + digbuffer.bytesRead);
                        System.out.println("Message from repo");
                        System.out.println(new String(bufRepo));
                        System.out.println("Original message");
                        System.out.println(new String(digbuffer.bufDigest));
                        logger.debug("Messages don't match due to size {} != {}", bytesReadRepo, digbuffer.bytesRead);
                        logger.debug("Message from repo:");
                        logger.debug("{}", new String(bufRepo));
                        logger.debug("Original message:");
                        logger.debug("{}", new String(digbuffer.bufDigest));
                        continue;
                    }
                    int j = 0;
                    boolean stop = false;
                    while ((j < bytesReadRepo) && !stop) {
                        if (bufRepo[j] != digbuffer.bufDigest[j]) {
                            System.out.println("UniqueID: Messages don't match");
                            logger.info("Messages don't match");
                            stop = true;
                        }
                        j++;
                    }
                    if (stop) {
                        continue;
                    } //read on to compare everything
                    bufMessage.close();
                    bufMessage = new FileInputStream(messageFile);
                    in.close();
                    in = repos.getDocument(mid);
                    int i1 = 0;
                    int i2 = 0;
                    try {
                        do {
                            i1 = in.read();
                            i2 = bufMessage.read();
                            if (i1 != i2) {
                                break;
                            }
                        } while (i1 != -1);
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(UniqueID.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        bufMessage.close();
                    }

                    if (i1 != i2) {
                        System.out.println(mid.toString() + "This message did not match during extensive check. Check next.");
                        continue;
                    }
                    logger.info("Duplicate messages with Message-ID: {}", mid);
                    throw new DuplicateException(mid);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(UniqueID.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            System.out.println("We have a hash collision, but no matching document");
            //if we are here, we have a hash collision. Construct a new ID.
            messageid.setSupplemental(new Long(System.currentTimeMillis()).toString());
            return messageid;
        }
    }
}
