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
package org.olat.admin.landingpages.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;

/**
 * 
 * Initial date: 15.05.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class Rules {
	
	private List<Rule> rules;

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public Rule matchRule(UserRequest ureq) {
		Roles roles = ureq.getUserSession().getRoles();
		if(roles.isGuestOnly() || roles.isInvitee() || rules == null || rules.isEmpty()) {
			return null;
		}
		
		for(Rule rule:rules) {
			if(rule.match(ureq)) {
				return rule;
			}
		}
		return null;
	}
	
	public BusinessControl match(UserRequest ureq) {
		BusinessControl bc = null;
		Rule rule = matchRule(ureq);
		if(rule != null && StringHelper.containsNonWhitespace(rule.getLandingPath())) {
			String path = cleanUpLandingPath(rule.getLandingPath());
			if(StringHelper.containsNonWhitespace(path)) {
				String restPath = BusinessControlFactory.getInstance().formatFromURI(path);
				List<ContextEntry> entries = BusinessControlFactory.getInstance().createCEListFromString(restPath);
				if(entries.size() > 0) {
					bc = BusinessControlFactory.getInstance().createFromContextEntries(entries);
				}
			}
		}
		return bc;
	}
	
	public static String cleanUpLandingPath(String path) {
		if(path == null) return null;//nothing to do
		
		if(path.startsWith("http")) {
			//remove protocol, host, port...
			try {
				URL url = new URL(path);
				path = url.getPath();
			} catch (MalformedURLException e) {
				//silently ignore it
			}
		}
		
		//cut context path if any
		if(path.startsWith(WebappHelper.getServletContextPath())) {
			path = path.substring(WebappHelper.getServletContextPath().length());
		}
		//cut dispatcher name
		if(path.startsWith("/url/")) {
			path = path.substring("/url/".length());
		}
		//factory doesn't like path which starts with /
		if(path.startsWith("/")) {
			path = path.substring("/".length());
		}
		return path;
	}
}