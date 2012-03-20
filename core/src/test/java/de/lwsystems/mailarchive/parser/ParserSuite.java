/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.lwsystems.mailarchive.parser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author wiermer
 */
public class ParserSuite extends TestCase {
    
    public ParserSuite(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("ParserSuite");
 
        suite.addTest(FieldTest.suite());   
        suite.addTest(SMTPDocumentHandlerTest.suite());

        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
