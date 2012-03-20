/*
 * FilesystemLogReader.java
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
package de.lwsystems.mailarchive.repository.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiermer
 */
public class FilesytemLogReader implements RepositoryLogReader {

    File logfile;

    Logger logger = LoggerFactory.getLogger(FilesytemLogReader.class.getName());

    public FilesytemLogReader(String fname) {
        logfile = new File(fname);

    }

    public String getChecksum() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(logfile));
            String thisLine;
            ChecksumBuilder csb;

            csb = new ChecksumBuilder();

            while ((thisLine = br.readLine()) != null) {
                ActionReport ar = LineActionReport.parse(thisLine);
                csb.append(ar.getDate().toString() + ar.getChecksum());
            }
            br.close();
            return csb.getChecksum();

        } catch (ParseException ex) {
            logger.error("Parse exception. ", ex);
            return "";
        } catch (IOException ex) {
            logger.error("I/O exception. ", ex);
            return "";
        } catch (NoSuchAlgorithmException ex) {
            logger.error("No such algorithm. ",ex);
            return "";
        }
    }
}
