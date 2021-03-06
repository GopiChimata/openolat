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
package org.olat.modules.qpool.ui.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.modules.qpool.ExportFormatOptions;
import org.olat.modules.qpool.QPoolSPI;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.QuestionPoolModule;

/**
 * 
 * Initial date: 20.03.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ExportTypeController extends StepFormBasicController {

	private String[] formatKeys;
	private String[] formatValues;
	private Map<String, ExportFormatOptions> formatMap = new HashMap<String, ExportFormatOptions>();
	
	private SingleSelection formatEl;
	
	public ExportTypeController(UserRequest ureq, WindowControl wControl, Form rootForm,
			StepsRunContext runContext, List<QuestionItemShort> items) {
		super(ureq, wControl, rootForm, runContext, LAYOUT_DEFAULT, null);
		
		QuestionPoolModule qpoolModule = CoreSpringFactory.getImpl(QuestionPoolModule.class);
		
		Set<ExportFormatOptions> formatSet = new HashSet<ExportFormatOptions>();
		for(QuestionItemShort item:items) {
			QPoolSPI sp = qpoolModule.getQuestionPoolProvider(item.getFormat());
			if(sp != null) {
				formatSet.addAll(sp.getTestExportFormats());	
			}	
		}
		
		List<String> formatKeyList = new ArrayList<String>();
		List<String> formatValueList = new ArrayList<String>();
		for(ExportFormatOptions format:formatSet) {
			String outcome = format.getOutcome().name();
			String key = format.getFormat() + "__" + outcome;
			String translation = translate("export.outcome." + outcome, new String[]{ format.getFormat() });
			formatKeyList.add(key);
			formatValueList.add(translation);
			formatMap.put(key, format);
		}

		formatKeys = formatKeyList.toArray(new String[formatKeyList.size()]);
		formatValues = formatValueList.toArray(new String[formatValueList.size()]);
		initForm(ureq);
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormDescription("export.type.desc");
		setFormContextHelp("org.olat.modules.qpool.ui.wizard", "export-type.html", "help.hover.export-type");
		
		formatEl = uifactory.addDropdownSingleselect("export.type", "export.type", formLayout, formatKeys, formatValues, null);
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if(formatEl.isOneSelected()) {
			ExportFormatOptions options = formatMap.get(formatEl.getSelectedKey());
			addToRunContext("format", options);
		}
		fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
	}
}