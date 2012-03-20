/*
 * ContainerFactory.java
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

import de.lwsystems.mailarchive.repository.MessageID;

/**
 *
 * @author wiermer
 */
public interface ContainerFactory {
    public Container createContainer(String name);
    public ContainerSplitStrategy getSplitStrategy();
    //public static String getCanonicalFileName(MessageID messageid);
    public Container makeDefaultContainer();

    public String getContextAwareCanonicalFileName(MessageID id);
}
