/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.lwsystems.mailarchive.context;

import de.lwsystems.mailarchive.repository.Archive;
import de.lwsystems.mailarchive.repository.container.ContainerArchive;
import java.io.IOException;

/**
 *
 * @author wiermer
 */
public class Context {

    Archive archive=null;
    boolean readOnly;
    int id;
    public Context(int id,Archive archive) {
        this.archive=archive;
    }

    public int getID() {
        return id;
    }

    public synchronized Archive getArchive() throws IOException {
        return archive;

    }

    public String getName() {
        return "Archive Context ID: "+getID()+" with archive: "+getName();
    }

    public boolean isReadOnly() {
       return readOnly;
    }

}
