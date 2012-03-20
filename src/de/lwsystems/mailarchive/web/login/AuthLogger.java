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
        System.out.println("AuthLogger loaded...");
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
