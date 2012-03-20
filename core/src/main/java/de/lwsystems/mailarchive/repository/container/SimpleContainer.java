/*
 * SimpleContainer.java
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
 */package de.lwsystems.mailarchive.repository.container;

import de.lwsystems.utils.Base64;
import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;

/**
 *
 * @author wiermer
 */
public class SimpleContainer implements Container {

    File root = null;



    SimpleContainer(File path) {
        if (!path.exists()) {
            if (!path.mkdirs())
                throw new SecurityException("Could not create container with path "+path);
        }
        root = path;
    }

    public Iterable<Box> getBoxes() {
        LinkedList<Box> boxes = new LinkedList<Box>();

        for (File f : root.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return arg0.isDirectory();
            }
        })) {

            boxes.add(new SimpleBox(f));

        }

        return boxes;
    }

    public File getDirectory() {
        return root;
    }

    public boolean isDefaultContainer() {
        
        return root.getName().equals("_def");
    }

    public Box createBox(String name) {
        File boxdir = new File(getDirectory().getAbsolutePath() + File.separator + name);
        return new SimpleBox(boxdir);
    }

    public String getContainerName() {
       if (isDefaultContainer())
       {
           return "Default container";
       }
       String dirname=root.getName();
       String[] splittedName=root.getName().split(":");
       if (splittedName.length==2)
           return "Unknown";
       if (splittedName.length<2)
         return "Error parsing name";
       return splittedName[2];
     
    }


    public String getContainerCriteria() {
       if (isDefaultContainer()) {
           return "";
       }
       String[] splittedName=root.getName().split(":");
       if (splittedName.length<2)
         return "";
       return new String(Base64.decode(splittedName[1]));
    }

    public int getID() {
        if (isDefaultContainer())
            return 0;
        String[] splittedName=root.getName().split(":");

        return Integer.parseInt(splittedName[1]);
    }

    public int getContextID() {
         String[] splittedName=root.getName().split(":");
        return Integer.parseInt(splittedName[0]);
    }

   
}
