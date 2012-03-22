/*  
 * ContextResource.java  
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
package de.lwsystems.mailarchive.web.service;

import de.lwsystems.mailarchive.web.SearchResultModel;
import de.lwsystems.mailarchive.web.UsefulTermsExtractor;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.xml.bind.JAXBException;
import org.apache.lucene.index.CorruptIndexException;

/**
 * REST Web Service
 *
 * @author rene
 */
@Path("context")
public class ContextResource {

    @Context
    private UriInfo context;
    @Context
    private ServletContext servletContext;

    void appendLines(StringBuilder sb, Set<String> s) {
        for (String str : s) {
            sb.append(str + "\n");
        }
    }

    /**
     * Retrieves representation of an instance of de.lwsystems.mailarchive.repository.ValidEmailResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    public String getText() {
        SearchResultModel srm = null;
        UsefulTermsExtractor ute = null;

        try {
            srm = SearchResultModel.getDefaultInstance(servletContext);
            ute = new UsefulTermsExtractor(srm.getArchive());


            //return new ContextConverter(ute.getYears(), ute.getToAddresses(),ute.getFromAddresses(),ute.getToDomains(),ute.getFromDomains());
            StringBuilder sb = new StringBuilder();
            sb.append("@@YEARS\n");
            appendLines(sb, ute.getYears());
            sb.append("@@FROM\n");
            appendLines(sb, ute.getFromAddresses());
          
            return sb.toString();

        } catch (CorruptIndexException ex) {
            Logger.getLogger(BennoSearchResource.class.getName()).log(Level.SEVERE, null, ex);

        } catch (IOException ex) {
            Logger.getLogger(BennoSearchResource.class.getName()).log(Level.SEVERE, null, ex);

        } finally {
        }
        return "";
    }
}
