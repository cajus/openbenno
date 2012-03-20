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
public class FieldTest extends TestCase {
    
    public FieldTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(FieldTest.class);
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

    /**
     * Test of isTokenized method, of class Field.
     */
    public void testIsTokenized() {
        System.out.println("isTokenized");
        Field instance = new Field("test", "payload");
        boolean expResult = false;
        boolean result = instance.isTokenized();
        assertEquals(expResult, result);
        instance = new Field("test", "payload",true);
        expResult = true;
        result = instance.isTokenized();
        assertEquals(expResult, result);
    }

    /**
     * Test of setTokenized method, of class Field.
     */
    public void testSetTokenized() {
        System.out.println("setTokenized");
        boolean tokenized = false;
        Field instance = new Field("test", "test");
        instance.setTokenized(tokenized);
        assertEquals(tokenized,instance.isTokenized());
    }

    /**
     * Test of getKey method, of class Field.
     */
    public void testGetKey() {
        System.out.println("getKey");
        Field instance = new Field("test", "test");
        String expResult = "test";
        String result = instance.getKey();
        assertEquals(expResult, result);
    }

    /**
     * Test of setKey method, of class Field.
     */
    public void testSetKey() {
        System.out.println("setKey");
        String key = "";
        Field instance = new Field("test", "test");
        instance.setKey(key);
    }

    /**
     * Test of getPayload method, of class Field.
     */
    public void testGetPayload() {
        System.out.println("getPayload");
        Field instance = new Field("test", "test");
        Object expResult = "test";
        Object result = instance.getPayload();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPayload method, of class Field.
     */
    public void testSetPayload() {
        System.out.println("setPayload");
        Object payload = "newpayload";
        Field instance = new Field("test", "test");
        instance.setPayload(payload);
        assertEquals(payload, instance.getPayload());

    }


}
