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

/**

 * LDAP Authorities provider for the OpenBenno mail archive. Adds the user's email-address as
 * additional role.

 * @author Niels Jäckel <niels.jaeckel@communardo.de>

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

     * Copy this property from the DefaultLdapAuthoritiesPopulator

     */
    protected boolean convertMailToUpperCase = true;

    public LDAPAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {

        super(contextSource, groupSearchBase);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Set getAdditionalRoles(DirContextOperations user, String username) {
        String mailAddress = user.getStringAttribute(mailRoleAttribute);

        if (convertMailToUpperCase) {
            mailAddress = mailAddress.toUpperCase();
        }

        String role = roleMailPrefix + mailAddress;
        Set result = new HashSet();
        result.add(new GrantedAuthorityImpl(role));

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
