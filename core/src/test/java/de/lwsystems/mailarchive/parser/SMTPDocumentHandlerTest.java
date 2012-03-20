/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lwsystems.mailarchive.parser;

import de.lwsystems.mailarchive.repository.DuplicateException;
import de.lwsystems.mailarchive.repository.MessageID;
import de.lwsystems.mailarchive.repository.PostAddRepositoryHandler;
import de.lwsystems.mailarchive.repository.SingleIndexArchive;
import de.lwsystems.mailarchive.repository.container.ContainerArchive;
import de.lwsystems.utils.MiscUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import junit.extensions.TestSetup;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

/**
 *
 * @author wiermer
 */
public class SMTPDocumentHandlerTest extends TestCase {

    SMTPDocumentHandler instance;
    SMTPDocumentHandler instanceold;
    static String simpleMail = "From: test@nowhere.de\nTo: to@nowhere.de\n\nThis is a test";
    static String simpleMailLongerThanDigest;
    static String simpleMailEvenLongerThanDigest;
    static String simpleMailEvenLongerThanDigest2;

    public SMTPDocumentHandlerTest(String testName) {
        super(testName);
    }

    private static String messageOrNull(String message) {
        return message == null ? "" : message + ": ";
    }

    public static void assertArrayEquals(String message, byte[] expected, byte[] actual) {
        if (expected == actual) {
            return;
        }
        if (expected == null) {
            fail(messageOrNull(message) + "expected array was null");
        }
        if (actual == null) {
            fail(messageOrNull(message) + "actual array was null");
        }
        assertEquals(messageOrNull(message) + "array length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(messageOrNull(message) + "array element " + i, expected[i], actual[i]);
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SMTPDocumentHandlerTest.class);
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        instance = new SMTPDocumentHandler(new ContainerArchive("/tmp/contindex", "/tmp/contrepo", false));
        instanceold = new SMTPDocumentHandler(new SingleIndexArchive("/tmp/contindexold/", "/tmp/contrepoold", false));
        simpleMailLongerThanDigest = simpleMail;
        for (int i = 0; i < 2048; i++) {
            simpleMailLongerThanDigest = simpleMailLongerThanDigest + "0";
        }
        simpleMailEvenLongerThanDigest = simpleMailLongerThanDigest + "0";
        simpleMailEvenLongerThanDigest2 = simpleMailLongerThanDigest + "1";
    }

    @Override
    protected void tearDown() throws Exception {
        instanceold.getArchive().close();
        File indexdir = new File("/tmp/contindex");
        File repodir = new File("/tmp/contrepo");
        MiscUtils.deleteDirectory(repodir);
        MiscUtils.deleteDirectory(indexdir);
        indexdir = new File("/tmp/contindexold");
        repodir = new File("/tmp/contrepoold");
        MiscUtils.deleteDirectory(repodir);
        MiscUtils.deleteDirectory(indexdir);
        super.tearDown();
    }

    /**
     * Test of accept method, of class SMTPDocumentHandler.
     */
    public void testAccept() {
        System.out.println("accept");
        String arg0 = "";
        String arg1 = "";
        boolean expResult = true;
        boolean result = instance.accept(arg0, arg1);
        assertEquals(expResult, result);
    }

    public void testAcceptOldFormat() {
        System.out.println("accept");
        String arg0 = "";
        String arg1 = "";
        boolean expResult = true;
        boolean result = instanceold.accept(arg0, arg1);
        assertEquals(expResult, result);
    }

    /**
     * Test of deliver method, of class SMTPDocumentHandler.
     */
    public void testDeliverSimpleMail() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String simpleMimeMail = "From: test@nowhere.de\nTo: to@nowhere.de\n\nThis is a test";

        InputStream data = new ByteArrayInputStream(simpleMimeMail.getBytes());

        instance.deliver(from, recipient, data);
    }

    public void testDeliverSimpleMailOldFormat() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String simpleMimeMail = "From: test@nowhere.de\nTo: to@nowhere.de\n\nThis is a test";

        InputStream data = new ByteArrayInputStream(simpleMimeMail.getBytes());

        instanceold.deliver(from, recipient, data);
    }

    /**
     * Test of deliver method, of class SMTPDocumentHandler.
     */
    public void testDeliverNonMimeMail() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String nonMimeMail = "This is a test";

        InputStream data = new ByteArrayInputStream(nonMimeMail.getBytes());

        instance.deliver(from, recipient, data);

    }

    public void testDeliverNonMimeMailOldFormat() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String nonMimeMail = "This is a test";

        InputStream data = new ByteArrayInputStream(nonMimeMail.getBytes());

        instanceold.deliver(from, recipient, data);
    }

    public void testDeliverEmptyMail() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String emptyMail = "";

        InputStream data = new ByteArrayInputStream(emptyMail.getBytes());

        instance.deliver(from, recipient, data);

    }

    public void testDeliverEmptyMailOldFormat() throws Exception {
        System.out.println("deliver");
        String from = "from@nowhere.de";
        String recipient = "to@nowhere.de";

        String emptyMail = "";

        InputStream data = new ByteArrayInputStream(emptyMail.getBytes());

        instanceold.deliver(from, recipient, data);
    }

    public void testDuplicate() throws IOException, MessagingException {
        InputStream data = new ByteArrayInputStream(simpleMail.getBytes());
        InputStream data2 = new ByteArrayInputStream(simpleMail.getBytes());
        try {
            instanceold.getArchive().addDocument(data, null, new PostAddRepositoryHandler() {

                public void handle(MessageID id, MetaDocument d) {
                }
            });
            instanceold.getArchive().addDocument(data2, null, new PostAddRepositoryHandler() {

                public void handle(MessageID id, MetaDocument d) {
                }
            });
            fail("Should have thrown a DuplicateException");
        } catch (DuplicateException excep) {
        }

    }

    public void testDuplicateLongerThanDigest() throws IOException, MessagingException {
        InputStream data = new ByteArrayInputStream(simpleMailLongerThanDigest.getBytes());
        InputStream data2 = new ByteArrayInputStream(simpleMailEvenLongerThanDigest.getBytes());
        InputStream data2secondtime= new ByteArrayInputStream(simpleMailEvenLongerThanDigest.getBytes());
        InputStream data3 = new ByteArrayInputStream(simpleMailEvenLongerThanDigest2.getBytes());



        try {
            MessageID mid1 = instanceold.getArchive().getRepository().addDocument(data, null);
            MessageID mid2 = instanceold.getArchive().getRepository().addDocument(data2, null);

            assertArrayEquals("The digests of these should be equal", mid1.getDigest(), mid2.getDigest());
            assertFalse("The supplemental should be different", mid1.getSupplemental().equals(mid2.getSupplemental()));
            try {
                MessageID mid3 = instanceold.getArchive().getRepository().addDocument(data3, null);
                assertArrayEquals("The digests of these should be equal", mid2.getDigest(), mid3.getDigest());
                assertFalse("The supplemental should be different", mid2.getSupplemental().equals(mid3.getSupplemental()));
            } catch (DuplicateException ex) {
                fail("These mails of the same length are not identical");
            }

        } catch (DuplicateException ex) {
            fail("These mails are not identical");
        }
        
        try {
            MessageID mid4 = instanceold.getArchive().getRepository().addDocument(data2secondtime, null);
            fail("This should have thrown a duplicate exception");
        } catch (DuplicateException ex) {

        }

    }
}
