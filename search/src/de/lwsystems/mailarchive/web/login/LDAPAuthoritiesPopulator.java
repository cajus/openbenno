/*
 * LDAPAuthoritiesPopulator.java
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

import java.util.HashSet;
import java.util.Set;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.util.Assert;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**

 * LDAP Authorities provider for the OpenBenno mail archive. Adds the user's email-address as
 * additional role.

 * @author Niels Jäckel <niels.jaeckel@communardo.de>
 * @author Patrick Breucking <breucking@gonicus.de>
 * @author Cajus Pollmeier <pollmeier@gonicus.de>

 */
public class LDAPAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator {

    /**
     * The attribute with the user's e-mail in the LDAP principal. This value may be directory-specific.
     */
    private String mailRoleAttribute = "mail";

    /**
     * The prefix for mail roles. This value is constant as of OpenBenno.
     */
    private String roleMailPrefix = "ROLE_MAIL_";

    /**
     * Logger instance
     */
    private Log log = LogFactory.getLog(LDAPAuthoritiesPopulator.class);

    /**
     * Copy this property from the DefaultLdapAuthoritiesPopulator
     */
    protected boolean convertMailToUpperCase = true;

    public LDAPAuthoritiesPopulator(final ContextSource contextSource,
            final String groupSearchBase) {

        super(contextSource, groupSearchBase);
    }

    @Override
    protected Set getAdditionalRoles(final DirContextOperations user,
            final String username) {

        log.info("mailRoleAttribute: " + mailRoleAttribute);
        StringTokenizer st = new StringTokenizer(mailRoleAttribute, " ");
        Set<GrantedAuthorityImpl> result = new HashSet<GrantedAuthorityImpl>();

        while (st.hasMoreElements()) {
            String elem = st.nextToken();
            log.info("Processing attribute " + elem);
            String[] mailAddresses = user.getStringAttributes(elem);

            if (mailAddresses == null) {
                break;
            }

            for (String mailAddress : mailAddresses) {
                if (mailAddress == null) {
                    continue;
                }

                if (convertMailToUpperCase) {
                    mailAddress = mailAddress.toUpperCase();
                } else {
                    mailAddress = mailAddress.toLowerCase();
                }

                log.info("Adding mail address " + mailAddress);
                String role = roleMailPrefix + mailAddress;
                result.add(new GrantedAuthorityImpl(role));
            }
        }

        return result;
    }

    /**

     * @param mailRoleAttribute the mailRoleAttribute to set

     */
    public void setMailRoleAttribute(String mailRoleAttribute) {
        Assert.notNull(mailRoleAttribute, "mailRoleAttribute must not be null");
        this.mailRoleAttribute = mailRoleAttribute;
    }

    /**

     * @param roleMailPrefix the roleMailPrefix to set

     */
    public void setRoleMailPrefix(String roleMailPrefix) {
        this.roleMailPrefix = roleMailPrefix;
    }

    /**

     * @param convertToUpperCase the convertToUpperCase to set

     */
    @Override
    public void setConvertToUpperCase(boolean convertToUpperCase) {
        this.convertMailToUpperCase = convertToUpperCase;
        super.setConvertToUpperCase(convertToUpperCase);
    }
}
