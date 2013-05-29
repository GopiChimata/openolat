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
package org.olat.instantMessaging;

import org.jivesoftware.smack.packet.Presence;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Description:<br>
 * TODO: guido Class Description for IMPrefsTask
 * 
 * <P>
 * Initial Date:  12.08.2008 <br>
 * @author guido
 */
public class IMPrefsTask implements Runnable {
	
	private OLog log = Tracing.createLoggerFor(IMPrefsTask.class);
	
	private Identity ident;

	public IMPrefsTask(Identity ident) {
		this.ident = ident;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		double j = Math.random()*20;
		int z = Long.valueOf((Math.round(j))).intValue();
		try {
			Thread.sleep(z);
		} catch (InterruptedException e) {
			log.error("", e);
		}
		
		ImPrefsManager mgr = ImPrefsManager.getInstance();
		ImPreferences prefs = mgr.loadOrCreatePropertiesFor(ident);
		prefs.setAwarenessVisible(false);
		prefs.setOnlineTimeVisible(false);
		prefs.setRosterDefaultStatus(Presence.Mode.away.toString());
		prefs.setVisibleToOthers(false);
		mgr.updatePropertiesFor(ident, prefs);
		
		double rand = Math.random() * 3;
		int i = Long.valueOf((Math.round(rand))).intValue();
		if (i == 1) {
			mgr.deleteProperties(ident, prefs);
			System.out.println("prefs deleted for user: "+ident.getName());
		}
		DBFactory.getInstance().commitAndCloseSession();
		System.out.println("prefs loaded and updated for user: "+ident.getName());
	}

}