/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.commons.coordinate.cluster.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.commons.coordinate.cluster.ClusterCoordinator;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.TestTable;
import org.olat.core.gui.control.Event;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.SignOnOffEvent;
import org.olat.core.util.coordinate.LockEntry;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.coordinate.Locker;
import org.olat.core.util.resource.OresHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 
 */
public class LockTest extends OlatTestCase {

	private static final int MAX_COUNT = 30; //at least 2
	private static final int MAX_USERS_MORE = 100; //20; //100;
	
	private static OLog log = Tracing.createLoggerFor(LockTest.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private ClusterLockManager clusterLockManager;
	@Autowired
	private ClusterCoordinator clusterCoordinator;

	/**
	 * The test is not trivial, it checks if several beans are
	 * loaded. As they are set with "lazy-init", other tests
	 * would not failed if they are not loaded.
	 */
	@Test
	public void testServices() {
		Assert.assertNotNull(dbInstance);
		Assert.assertNotNull(securityManager);
		Assert.assertNotNull(clusterCoordinator);
		Assert.assertNotNull(clusterLockManager);
	}

	@Test
	public void testCreateDeleteAcquire() {
		// some setup
		List<Identity> identities = new ArrayList<Identity>();
		for (int i = 0; i < MAX_COUNT + MAX_USERS_MORE; i++) {
			Identity i1 = securityManager.createAndPersistIdentity("lock-" + UUID.randomUUID().toString(), null, null, null, null);
			identities.add(i1);
		}
		dbInstance.closeSession();

		Identity ident = identities.get(0);
		Identity ident2 = identities.get(1);
		OLATResourceable ores = OresHelper.createOLATResourceableInstanceWithoutCheck(LockTest.class.getName(), new Long(123456789)); 
		
		// ------------------ test the clusterlockmanager ----------------------
		// create a lock
		String asset = OresHelper.createStringRepresenting(ores, "locktest");
		LockImpl li = clusterLockManager.createLockImpl(asset, ident);
		clusterLockManager.saveLock(li);
		dbInstance.closeSession();
		
		// find it
		LockImpl l2 = clusterLockManager.findLock(asset);
		assertNotNull(l2);
		assertEquals(li.getKey(), l2.getKey());
		
		// delete it 
		clusterLockManager.deleteLock(l2);
		dbInstance.closeSession();
		
		// may not find it again
		LockImpl l3 = clusterLockManager.findLock(asset);
		assertNull(l3);
		
		
		// ------------------ test the clusterlocker ----------------------	
		//access the cluster locker explicitely
		Locker cl = clusterCoordinator.getLocker();
		
		// acquire
		LockResult res1 = cl.acquireLock(ores, ident, "abc");
		assertTrue(res1.isSuccess());
		dbInstance.closeSession();
		
		// reacquire same identity (get from db)
		LockResult res11 = cl.acquireLock(ores, ident, "abc");
		long lock1Ac = res11.getLockAquiredTime();
		assertTrue(res11.isSuccess());
		assertTrue(lock1Ac > 0);
		dbInstance.closeSession();

		// acquire by another identity must fail
		LockResult res2 = cl.acquireLock(ores, ident2, "abc");
		assertFalse(res2.isSuccess());
		dbInstance.closeSession();

		// reacquire same identity
		LockResult res3 = cl.acquireLock(ores, ident, "abc");
		assertTrue(res3.isSuccess());
		dbInstance.closeSession();

		// test the admin
		List<LockEntry> entries = cl.adminOnlyGetLockEntries();
		assertEquals(1, entries.size());
		LockEntry le = entries.get(0);
		// must be original owner
		assertEquals(le.getOwner().getName(), ident.getName());

		// release lock
		cl.releaseLock(res3);
		dbInstance.closeSession();
		// test the admin
		entries = cl.adminOnlyGetLockEntries();
		assertEquals(0,entries.size());

		// make sure it is not locked anymore
		boolean lo = cl.isLocked(ores, "abc");
		assertFalse(lo);
	}

	@Test
	public void testSaveEvent() {
		BaseSecurity baseSecurityManager = applicationContext.getBean(BaseSecurity.class);
    Identity identity = baseSecurityManager.createAndPersistIdentity("lock-save-event-" + UUID.randomUUID().toString(), null, null, null, null);
    dbInstance.closeSession();
    log.info("Created identity=" + identity);
		//
		TestTable entry = new TestTable();
		entry.setField1("bar");
		entry.setField2(2221234354566776L);
		try {
			dbInstance.saveObject(entry);
			dbInstance.commit();
	    fail("Should generate an error");
		} catch (DBRuntimeException dre) {
			log.info("DB connection is in error-state");
		}
		// DB transaction must be in error state for this test
		try {
			
			Locker locker = clusterCoordinator.getLocker();
			assertTrue(locker instanceof ClusterLocker);
			log.info("ClusterLocker created");
	    Event event = new SignOnOffEvent(identity, false);
	    log.info("START locker.event(event)");
	    ((ClusterLocker)locker).event(event);
			log.info("DONE locker.event(event)");
		} catch(Exception ex) {
			log.error("", ex);
			fail("BLOCKER : ClusterLocker.event is not error-safe, db exception could happen and de-register event-listener");
		}
	}
}