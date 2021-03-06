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
package org.olat.core.gui.components.form.flexible.impl.elements;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * renders TextAreaElement as HTML
 * <P>
 * Initial Date: 31.01.2008 <br>
 * 
 * @author rhaag
 */
class TextAreaElementRenderer extends DefaultComponentRenderer {

	/**
	 * @see org.olat.core.gui.components.ComponentRenderer#render(org.olat.core.gui.render.Renderer,
	 *      org.olat.core.gui.render.StringOutput,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.render.URLBuilder,
	 *      org.olat.core.gui.translator.Translator,
	 *      org.olat.core.gui.render.RenderResult, java.lang.String[])
	 */
	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		//
		TextAreaElementComponent teC = (TextAreaElementComponent) source;
		TextAreaElementImpl te = teC.getTextAreaElementImpl();

		String id = teC.getFormDispatchId();
		
		String value = te.getValue();
		if(value == null){
			value = "";
		}
		
		// calculate rows height
		int rows = teC.getRows();
		if (teC.isAutoHeightEnabled()){
			// try to reduce screen flickering caused by the auto-height code. Search
			// for all line breaks and add for each a row. Maybe it will need even
			// more rows, but we can't do more at this point
			int buestEffortRowCount = value.split("\n").length;
			if (buestEffortRowCount == 0) buestEffortRowCount = 1;
			if (buestEffortRowCount > rows) rows = buestEffortRowCount;
		}
		// Escape HTMl entities
		value = StringEscapeUtils.escapeHtml(value);
		//
		if (!source.isEnabled()) {
			//read only view
			sb.append("<span id=\"").append(id).append("\" ")
			  .append(FormJSHelper.getRawJSFor(te.getRootForm(), id, te.getAction()))
			  .append(" ><textarea id=\"")
			  .append(id)
			  .append("_disabled\" readonly='readonly' class='form-control o_form_element_disabled'");
			if (teC.getCols() != -1) {
				sb.append(" cols='").append(teC.getCols()).append("'");
			}
			if (rows != -1) {
				sb.append(" rows='").append(rows).append("'");
			}
			sb.append(">")
			  .append(value)
			  .append("</textarea></span>");
	
		} else {
			//read write view
			sb.append("<textarea id=\"");
			sb.append(id);
			sb.append("\" name=\"");
			sb.append(id);
			sb.append("\" class='form-control'");
			if (teC.getCols() != -1) {
				sb.append(" cols=\"").append(teC.getCols()).append("\"");
			} 
			if (teC.isAutoHeightEnabled()){
				sb.append(" onkeyup='try{var iter=0; while ( this.scrollHeight>this.offsetHeight && iter < 99){ iter++; this.rows = this.rows + 1}} catch(e){}'");				
			}
			if (rows != -1) {
				sb.append(" rows=\"").append(rows).append("\"");
			}
			sb.append(FormJSHelper.getRawJSFor(te.getRootForm(), id, te.getAction()));
			sb.append(" >");
			sb.append(value);
			sb.append("</textarea>");
			sb.append(FormJSHelper.getJSStartWithVarDeclaration(id));
			//plain textAreas should not propagate the keypress "enter" (keynum = 13) as this would submit the form
			sb.append(id+".on('keypress', function(event, target){if (13 == event.keyCode) {event.stopPropagation()} })");
			sb.append(FormJSHelper.getJSEnd());
		}

		// resize element to fit content
		if (teC.isAutoHeightEnabled()) {
			sb.append("<script type='text/javascript'>jQuery(function(){try{var iter=0; var obj=$('");
			sb.append(id);
			if (!source.isEnabled()) sb.append("_disabled");			
			sb.append("');while (obj.scrollHeight>obj.offsetHeight && iter < 99){ iter++; obj.rows = obj.rows + 1}} catch(e){}});</script>");
		}
		
		if(source.isEnabled()){
			//add set dirty form only if enabled
			FormJSHelper.appendFlexiFormDirty(sb, te.getRootForm(), teC.getFormDispatchId());
		}
	}
}
