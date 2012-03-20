/*
 * Container.java
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

import java.io.File;

/**
 *
 * @author wiermer
 */
public interface Container {

    public Box createBox(String name);
    public Iterable<Box> getBoxes();
    public int getContextID();
    public String getContainerName();
    public String getContainerCriteria();
    public int getID();
    public File getDirectory();
    public boolean isDefaultContainer();
}
