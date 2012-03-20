/*
 * FilesystemLogWriter.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiermer
 */
public class FilesystemLogWriter implements RepositoryLogWriter {

    File f;

    Logger logger = LoggerFactory.getLogger(FilesystemLogWriter.class.getName());

    public FilesystemLogWriter(String fname) {
        f = new File(fname);
    }

    public synchronized void addActionReport(ActionReport ar) {
        BufferedWriter bw = null;
        FileOutputStream fas;
        FileLock lock;

        try {
            fas = new FileOutputStream(f, true);

            FileChannel channel = fas.getChannel();
            lock = channel.lock();
            try {
                bw = new BufferedWriter(new OutputStreamWriter(fas));
                bw.write(ar.toString() + "\n");
            } finally {
                lock.release();
            }
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                logger.error("I/O exception. ",ex);
            }


        }

    }

    public String checkAndRoll() {

        FileOutputStream fas;
        FileLock lock = null;
        try {
            Date curDate = new Date();
            String origName = f.getAbsolutePath();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hh:mm:ss");
            File newFile = new File(origName + "." + dateFormat.format(curDate));
            f.renameTo(newFile);
            fas = new FileOutputStream(f);
//            FileChannel channel = fas.getChannel();
//            lock = channel.lock();
//
//            lock.release();
            f = new File(origName);
            String chksum = new FilesytemLogReader(newFile.getAbsolutePath()).getChecksum();
            addActionReport(new LineActionReport(new Date(), "CHAIN " + newFile.getName(), chksum, ActionStatus.SUCCESS));
            return chksum;
        } catch (IOException ex) {
            logger.error("I/O exception. ",ex);

        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    logger.error("I/O exception. ",ex);
                }
            }
        }
        return "";
    }
}
