/*  
 * ExternalCommandUserDetailsManager.java  
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
package de.lwsystems.mailarchive.web.login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * This class is an extension point for authentication and authorization scripts.
 * See wiki for details of expected commands and outputs.
 * @author rene
 */
public class ExternalCommandUserDetailsManager implements ListUserDetailsManager, InitializingBean {

    String command;

    /**
     * 
     * @return
     */
    public String getCommand() {
        return command;
    }

    /**
     * 
     * @param command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * 
     * @param username
     * @return
     * @throws org.springframework.security.userdetails.UsernameNotFoundException
     * @throws org.springframework.dao.DataAccessException
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {

        String thisLine;
        Process process;
        try {
            process = Runtime.getRuntime().exec(getCommand() + " GET " + username);
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Could not call external command " + getCommand());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            if ((thisLine = reader.readLine()) == null) {
                throw new DataRetrievalFailureException("External command "+getCommand() + " GET " + username+"returned nothing");
            }

            if (thisLine.trim().startsWith("NOT_FOUND")) {
                throw new UsernameNotFoundException("Could not find " + username);
            }
            if (thisLine.trim().startsWith("ERROR")) {
                String[] elem = thisLine.trim().split(" ");
                String error = "Access problems within external program " + getCommand();
                if (elem.length > 1) {
                    error += ": ";
                    for (int i = 1; i < elem.length; i++) {
                        error += elem[i] + " ";
                    }
                }

            }
            String password = null;
            LinkedList<GrantedAuthority> authorities = new LinkedList<GrantedAuthority>();

            do {
                if (thisLine.trim().startsWith("PASSWORD")) {
                    password = thisLine.trim().substring(9);
                    continue;
                }
                if (thisLine.trim().startsWith("MAIL")) {
                    authorities.add(new GrantedAuthorityImpl("ROLE_MAIL_" + thisLine.trim().substring(5)));
                    continue;
                }

                if (thisLine.trim().startsWith("ROLE")) {
                    authorities.add(new GrantedAuthorityImpl("ROLE_" + thisLine.trim().substring(5)));
                    continue;
                }

                if (thisLine.trim().startsWith("QUERY")) {
                    if (thisLine.trim().length() < 7) {
                        throw new DataRetrievalFailureException("No query returned: " + thisLine);
                    }
                    authorities.add(new GrantedAuthorityImpl("ROLE_QUERY_" + thisLine.trim().substring(6)));
                    continue;
                }
            } while ((thisLine = reader.readLine()) != null);
            if (password == null) {
                password = "";
            }
            GrantedAuthority[] auth = new GrantedAuthority[authorities.size()];
            for (int i = 0; i < authorities.size(); i++) {
                auth[i] = authorities.get(i);
            }
            User user = new User(username, password, true, true, true, true, auth);
            return user;
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Error while reading from command " + getCommand());
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void afterPropertiesSet() throws Exception {
        if (command == null) {
            throw new ProgramNotFoundException("Command not defined");
        }
    }

    /**
     * 
     * @param user
     */
    public void createUser(UserDetails user) {
        changePassword(user.getUsername(), user.getPassword());
        GrantedAuthority[] ga = user.getAuthorities();
        for (GrantedAuthority a : ga) {
            if (a.getAuthority().startsWith("ROLE_MAIL_")) {
                grant(user.getUsername(), "MAIL", a.getAuthority().substring(10));
            } else if (a.getAuthority().startsWith("ROLE_QUERY_")) {
                grant(user.getUsername(), "QUERY", a.getAuthority().substring(11));
            } else if (a.getAuthority().startsWith("ROLE_")) {
                grant(user.getUsername(), "ROLE", a.getAuthority().substring(5));
            }
        }
    }
    
    /**
     * Updates the details of a user.
     * This implementation simply deletes the user and recreates him new.
     * @param user
     */
    public void updateUser(UserDetails user) {
        deleteUser(user.getUsername());
        createUser(user);
    }

    /**
     * 
     * @param username
     */
    public void deleteUser(String username) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(getCommand() + " REMOVE USER " + username);
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Could not call external command " + getCommand());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response;
        try {
            response = reader.readLine();
            if (response != null && response.equals("SUCCESS")) {
                return;
            }
            throw new DataRetrievalFailureException("Error deleting user:  " + response);
        } catch (IOException ex) {
            throw new DataRetrievalFailureException("Error deleting user:  I/O Exception " + ex);
        }
    }

    /* Changes the password.
     * Note that this is done with the ADD USER command. The script is responsible for deleting the old entry.
     * No check is done, whether the username exists.
     */
    
    /**
     * 
     * @param username
     * @param passwd
     */
    public void changePassword(String username, String passwd) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(getCommand() + " ADD USER " + username + " " + passwd);
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Could not call external command " + getCommand());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response;
        try {
            response = reader.readLine();
            if (response.equals("SUCCESS")) {
                return;
            }
            throw new DataRetrievalFailureException("Error changing password:  " + response);
        } catch (IOException ex) {
            throw new DataRetrievalFailureException("Error changing password:  I/O Exception " + ex);
        }
    }

    /**
     * 
     * @param username
     * @return
     */
    public boolean userExists(String username) {
        try {
            UserDetails ud = loadUserByUsername(username);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void grant(String username, String command, String value) {
        Process process;
        if (value.length() <= 0) {
            throw new DataRetrievalFailureException("Error empty value while adding " + username + " " + command);

        }
        try {
            process = Runtime.getRuntime().exec(getCommand() + " ADD " + command + " " + username + " " + value);
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Could not call external command " + getCommand());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response;
        try {
            response = reader.readLine();
            if (response.equals("SUCCESS")) {
                return;
            }
            throw new DataRetrievalFailureException("Error adding permissions:  " + response);
        } catch (IOException ex) {
            throw new DataRetrievalFailureException("Error adding permissions:  I/O Exception " + ex);
        }
    }

    /**
     * 
     * @return
     */
    public List<String> getUserList() {
        Process process;
        try {
            process = Runtime.getRuntime().exec(getCommand() + " USERLIST");
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new DataRetrievalFailureException("Could not call external command " + getCommand());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        LinkedList<String> names = new LinkedList<String>();
        String name;
        try {
            while ((name = reader.readLine()) != null) {
                names.add(name);
            }
        } catch (IOException ex) {
            Logger.getLogger(ExternalCommandUserDetailsManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        Collections.sort(names);
        return names;
    }
}
