/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.id.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.id.Identity;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.resource.Resourceable;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupImpl;
import org.olat.repository.RepositoryEntry;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * <h3>Description:</h3>
 * <p>
 * <p>
 * Initial Date:  26 jan. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class HistoryManager extends BasicManager {
	
	private static HistoryManager THIS;
	private static XStream historyReadStream = XStreamHelper.createXStreamInstance();
	private static XStream historyWriteStream = XStreamHelper.createXStreamInstance();
	static {
		//xstream config
		historyReadStream.omitField(BusinessGroup.class, "type");
		historyReadStream.omitField(BusinessGroup.class, "ownerGroup");
		historyReadStream.omitField(BusinessGroup.class, "partipiciantGroup");
		historyReadStream.omitField(BusinessGroup.class, "waitingGroup");
		historyReadStream.omitField(BusinessGroup.class, "groupContext");
		historyReadStream.omitField(BusinessGroupImpl.class, "type");
		historyReadStream.omitField(BusinessGroupImpl.class, "ownerGroup");
		historyReadStream.omitField(BusinessGroupImpl.class, "partipiciantGroup");
		historyReadStream.omitField(BusinessGroupImpl.class, "waitingGroup");
		historyReadStream.omitField(BusinessGroupImpl.class, "groupContext");
		historyReadStream.omitField(RepositoryEntry.class, "ownerGroup");
		historyReadStream.omitField(RepositoryEntry.class, "participantGroup");
		historyReadStream.omitField(RepositoryEntry.class, "tutorGroup");
		historyReadStream.omitField(RepositoryEntry.class, "metaDataElements");
		historyReadStream.omitField(RepositoryEntry.class, "version");
		historyReadStream.omitField(org.olat.core.commons.persistence.PersistentObject.class, "version");
		
		historyReadStream.alias("org.olat.core.util.resource.OresHelper$1", Resourceable.class);
		historyReadStream.alias("org.olat.core.util.resource.OresHelper$2", Resourceable.class);
		historyReadStream.alias("org.olat.core.util.resource.OresHelper$3", Resourceable.class);
		historyReadStream.aliasAttribute(Resourceable.class, "resourceableTypeName", "val_-type");
		historyReadStream.aliasAttribute(Resourceable.class, "resourceableTypeName", "val$type");
		historyReadStream.aliasAttribute(Resourceable.class, "resourceableId", "val_-key");
		historyReadStream.aliasAttribute(Resourceable.class, "resourceableId", "val$key");
	}
	
	public HistoryManager() {
		THIS = this;
	}
	
	public static HistoryManager getInstance() {
		return THIS;
	}
	
	public void persistHistoryPoint(Identity identity, HistoryPoint historyPoint) {
		try {
			String pathHomePage = FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomePage(identity.getName());
			File resumeXml = new File(pathHomePage, "resume.xml");
			if(!resumeXml.getParentFile().exists()) {
				resumeXml.getParentFile().mkdirs();
			}
			FileOutputStream out = new FileOutputStream(resumeXml);
			historyWriteStream.toXML(historyPoint, out);
			FileUtils.closeSafely(out);
		} catch (Exception e) {
			logError("UserSession:::logging off write resume: ", e);
		}
	}
	
	public HistoryPoint readHistoryPoint(Identity identity) {
		try {
			String pathHomePage = FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomePage(identity.getName());
			File resumeXml = new File(pathHomePage, "resume.xml");
			return readHistory(resumeXml);
		} catch (Exception e) {
			logError("Cannot read resume file: ", e);
			return null;
		}
	}
	
	public void deleteHistory(Identity identity) {
		try {
			String pathHomePage = FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomePage(identity.getName());
			File resumeXml = new File(pathHomePage, "resume.xml");
			if(resumeXml.exists()) {
				resumeXml.delete();
			}
		} catch (Exception e) {
			logError("Can not delete history file", e);
		}
	}
	
	protected HistoryPoint readHistory(File resumeXml) throws IOException {
		if(resumeXml.exists()) {
			FileInputStream in = new FileInputStream(resumeXml);
			HistoryPoint point = (HistoryPoint)historyReadStream.fromXML(in);
			FileUtils.closeSafely(in);
			return point;
		}
		return null;
	}
}
