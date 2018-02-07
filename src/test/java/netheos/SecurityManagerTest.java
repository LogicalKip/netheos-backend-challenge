package com.netheos.db;

import java.util.*;

import com.netheos.SecurityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
 
public class SecurityManagerTest { 
    @Before
    public void setUp() {
    	System.out.println("\n===================Setting up test environnement.===================");
    }
 
    @After
    public void tearDown() {
    	System.out.println("\n===================Tearing down test environnement.===================");
    }
 
    @Test
    public void hasAdminAccessTest() throws Exception {
        final String adminUsername = "admin";
        final String adminPassword = "jGrC4Kp3Nr30";
        /* Success */
        assertTrue(SecurityManager.hasAdminAccess(adminUsername, adminPassword));
        /* Fails */
        assertFalse(SecurityManager.hasAdminAccess(adminUsername, adminPassword.toLowerCase()));
        assertFalse(SecurityManager.hasAdminAccess(adminUsername, "wrong password"));
        assertFalse(SecurityManager.hasAdminAccess("wrong username", adminPassword));
        assertFalse(SecurityManager.hasAdminAccess("wrong username", "wrong password"));
        assertFalse(SecurityManager.hasAdminAccess(null, adminPassword));
        assertFalse(SecurityManager.hasAdminAccess(adminUsername, null));
        assertFalse(SecurityManager.hasAdminAccess(null, null));
    }
}