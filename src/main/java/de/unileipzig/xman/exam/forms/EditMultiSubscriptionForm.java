package de.unileipzig.xman.exam.forms;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.Form;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormToggle;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.Submit;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;

import de.unileipzig.xman.exam.Exam;

public class EditMultiSubscriptionForm extends FormBasicController {

	private FormToggle box;
	boolean multiSubscription;
	boolean isWritten;

	public EditMultiSubscriptionForm(UserRequest ureq, WindowControl wControl, boolean multiSubscription, boolean isWritten) {
		super(ureq, wControl);
		
		setTranslator(Util.createPackageTranslator(Exam.class, ureq.getLocale()));

		this.multiSubscription = multiSubscription;
		this.isWritten = isWritten;
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		box = uifactory.addToggleButton("EditMultiSubscriptionForm.box", null, formLayout, null, null);
		
		if (multiSubscription) {
			box.toggleOn();
		} else {
			box.toggleOff();
		}
		
		box.setExampleKey("EditMultiSubscriptionForm.info.written", null);
		
		if(isWritten) {
			box.setEnabled(false);
		}

		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		uifactory.addFormSubmitButton("save", "saveButton", buttonGroupLayout);
	}
	
	/**
	 * @return true, if the multiSubscription feature was enabled
	 */
	public boolean getMultiSubscription() {
		return box.isOn();
	}

	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent(ureq, Form.EVNT_VALIDATION_OK);
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}
}