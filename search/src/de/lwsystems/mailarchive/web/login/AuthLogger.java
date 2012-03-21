/*
 * AuthLogger.java
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

/**
 *
 * @author wiermer
 */
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.Authentication;

@Aspect
public class AuthLogger {

    private String appName;

    /**
     * Constructor.
     * @param appName name of the application
     */
    public AuthLogger(String appName) {
        Logger.getLogger(AuthLogger.class.getName()).log(Level.FINE, "AuthLogger loaded...");
        this.appName = appName;
    }

    private void logAuthPass(String userName) {

        Logger.getLogger(AuthLogger.class.getName()).log(Level.INFO, "app=" + appName + ";auth=fail;user=" + userName);
    }

    private void logAuthFail(String userName, Exception e) {
        Logger.getLogger(AuthLogger.class.getName()).log(Level.INFO, "app=" + appName + ";auth=fail;user=" + userName + ";Exception=" + e);

    }

    /**
     * Wraps a call to Spring Security's
     * AuthenticationProvider.authenticate().
     */
    @Pointcut("execution(public * org.springframework.security.providers.AuthenticationProvider.authenticate(..))")
    public Object logAuth(ProceedingJoinPoint call) throws Throwable {
        Authentication result;
        String user = "UNKNOWN";

        try { /*[3a]*/
            Authentication auth = (Authentication) call.getArgs()[0];
            user = auth.getName();
        } catch (Exception e) {
            // ignore
        }

        try { /*[3b]*/
            result = (Authentication) call.proceed();
        } catch (Exception e) {
            logAuthFail(user, e);
            throw e;
        }

        if (result != null) { /*[3c]*/
            logAuthPass(user);
        }

        return result;
    }
}
