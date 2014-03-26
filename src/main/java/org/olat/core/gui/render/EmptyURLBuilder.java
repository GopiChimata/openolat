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
package org.olat.core.gui.render;

import org.olat.core.gui.components.Component;

/**
 * 
 * Initial date: 21.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class EmptyURLBuilder extends URLBuilder {

	public EmptyURLBuilder() {
		super(null, null, null, null);
	}

	@Override
	public void appendTarget(StringOutput sb) {
		// nothing to do
	}

	@Override
	public void buildJavaScriptBgCommand(StringOutput buf, String[] keys, String[] values, int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(StringOutput buf, String[] keys, String[] values, int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(StringOutput buf, String[] keys, String[] values, String modURI, int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(StringOutput buf, String[] keys, String[] values, String modURI) {
		// nothing to do
	}

	@Override
	public void buildURI(StringOutput buf, String[] keys, String[] values) {
		// nothing to do
	}

	@Override
	public URLBuilder createCopyFor(Component source) {
		return super.createCopyFor(source);
	}

	@Override
	public void setComponentPath(String componentPath) {
		// nothing to do
	}
}