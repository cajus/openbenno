
import com.thoughtworks.selenium.SeleneseTestCase;

/**
 *
 * @author wiermer
 */
public class loginTest extends SeleneseTestCase {

    @Override

            public void setUp() throws Exception {
		setUp("http://localhost:8084/bennosearch/login.html", "*chrome");
	}
	public void testSimple() throws Exception {
		selenium.open("/bennosearch/login.html");
		selenium.type("j_username", "admin");
		selenium.type("j_password", "passwd");
		selenium.waitForPageToLoad("30000");
		for (int second = 0;; second++) {
			if (second >= 60) fail("timeout");
			try { if (selenium.isTextPresent("Betreff")) break; } catch (Exception e) {}
			Thread.sleep(1000);
		}

		selenium.click("//table[@id='d']/tbody/tr/td[2]/div/span");
		selenium.waitForPageToLoad("30000");
		// selenium.();
	}
}
	
