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
* Copyright (c) 2008 frentix GmbH, Switzerland<br>
* <p>
*/

package org.olat.resource.accesscontrol.model;

import org.olat.resource.accesscontrol.method.TokenAccessHandler;


/**
 * 
 * Description:<br>
 * This a "static" payment method. There is only one instance.
 * 
 * <P>
 * Initial Date:  18 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class TokenAccessMethod extends AbstractAccessMethod {
	
	@Override
	public String getType() {
		return TokenAccessHandler.METHOD_TYPE;
	}
	
	@Override
	public boolean isNeedUserInteraction() {
		return true;
	}

	@Override
	public int hashCode() {
		return getKey() == null ? 34688 : getKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof TokenAccessMethod) {
			TokenAccessMethod method = (TokenAccessMethod)obj;
			return getKey() != null && getKey().equals(method.getKey());
		}
		return false;
	}
}
