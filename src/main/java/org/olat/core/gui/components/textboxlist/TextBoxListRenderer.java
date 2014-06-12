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
package org.olat.core.gui.components.textboxlist;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.components.form.flexible.impl.elements.TextBoxListElementComponent;
import org.olat.core.gui.components.form.flexible.impl.elements.TextBoxListElementImpl;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * renderer for the textboxlist-component can be used in a flexiform mode or
 * without and will then provide its own form
 * 
 * <P>
 * Initial Date: 23.07.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class TextBoxListRenderer extends DefaultComponentRenderer {

	/**
	 * @see org.olat.core.gui.components.ComponentRenderer#render(org.olat.core.gui.render.Renderer,
	 *      org.olat.core.gui.render.StringOutput,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.render.URLBuilder,
	 *      org.olat.core.gui.translator.Translator,
	 *      org.olat.core.gui.render.RenderResult, java.lang.String[])
	 */
	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator, RenderResult renderResult,
			String[] args) {

		TextBoxListComponent tblComponent = (TextBoxListElementComponent) source;
		if (tblComponent.isEnabled()) {
			renderEnabledMode(tblComponent, sb);
		} else {
			renderDisabledMode(tblComponent, sb);
		}
	}

	/**
	 * renders the component in Enabled / editable mode
	 * 
	 * @param tblComponent
	 *            the component to render
	 * @param sb
	 *            the StringOutput
	 * @param translator
	 */
	private void renderEnabledMode(TextBoxListComponent tblComponent, StringOutput sb) {
		TextBoxListElementImpl te = ((TextBoxListElementComponent)tblComponent).getTextElementImpl();
		Form rootForm = te.getRootForm();
		String dispatchId = tblComponent.getFormDispatchId();
		String initialValue = tblComponent.getInitialItemsAsString();

		sb.append("<input type='text' id='textboxlistinput").append(dispatchId).append("'")
		  .append(" name='textboxlistinput").append(dispatchId).append("'")
		  .append(" value='").append(initialValue).append("' />\n");

		String o_ffEvent = FormJSHelper.getJSFnCallFor(rootForm, dispatchId, 2);
		// generate the JS-code for the bootstrap tagsinput
		sb.append(FormJSHelper.getJSStart())
		  .append("jQuery(function(){\n")
		  .append("  jQuery('#textboxlistinput").append(dispatchId).append("').tagsinput({\n");
		
		if (tblComponent.getProvider() != null) {
			sb.append("    typeahead: {\n")
			  .append("      source: function() {")
			  .append("      	return jQuery.getJSON('").append(tblComponent.getMapperUri()).append("');")
			  .append("      }")
			  .append("    }\n");
		}
		
		sb.append("  });\n")
		  .append("  jQuery('#textboxlistinput").append(dispatchId).append("').on('itemAdded itemRemoved',function(event) {\n")
		  .append(o_ffEvent).append(";\n")
		  .append("  });\n")
		  .append("});\n")
		  .append(FormJSHelper.getJSEnd());
	}

	/**
	 * Renders the textBoxListComponent in disabled/read-only mode
	 * 
	 * @param tblComponent
	 * @param output
	 */
	private void renderDisabledMode(TextBoxListComponent tblComponent, StringOutput output) {
		// read only view, we just display the initialItems as
		// comma-separated string
		String readOnlyContent = tblComponent.getInitialItemsAsString();
		if (readOnlyContent.length() > 0) {
			output.append("<div class=\"b_with_small_icon_left b_tag_icon\">");
			FormJSHelper.appendReadOnly(readOnlyContent, output);
			output.append("</div>");
		} else {
			FormJSHelper.appendReadOnly("-", output);
		}
	}
}
