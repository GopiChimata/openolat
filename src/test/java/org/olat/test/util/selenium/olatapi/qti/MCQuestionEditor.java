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
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.test.util.selenium.olatapi.qti;

import com.thoughtworks.selenium.Selenium;

/**
 * This is a Multiple choice QuestionEditor with 2 modes: Test and Questionnaire.  
 * (The class might be splitted later in specific types.)
 * <p>
 * Used for tests/questionnaires editing. 
 * The Questionnaire functionality is a subset of the Test mode.
 * 
 * @author Lavinia Dumitrescu
 *
 */
public class MCQuestionEditor extends QuestionEditor {

 
	/**
	 * @param selenium
	 */
	public MCQuestionEditor(Selenium selenium) {
		super(selenium);
	}
	
	/**
	 * Adds new answer, while in Question/answers tab of the current selected question.
	 *
	 */
	public void addNewAnswer() {
		selectQuestionAndAnswersTab();
		selenium.click("ui=testEditor::content_questionAnswers_addNewAnswer()");
		selenium.waitForPageToLoad("30000");
	}
		

	/**
	 * Edits the answer with the answerIndex for the MULTIPLE_CHOICE, 
	 * while in Question/answers tab of the current selected question.
	 * The answerIndex must be greater that 0.
	 * @param newText
	 * @param answerIndex
	 */
	public void editAnswer(String newText, int answerIndex) {
		selectQuestionAndAnswersTab();
		selenium.click("ui=testEditor::content_questionAnswers_editAnswerMc(indexOfAnswer=" + String.valueOf(answerIndex) + ")");
		selenium.waitForPageToLoad("30000");
		editRichText(newText);
	}
		
	/**
	 * Only for tests!
	 * Selects the correct answer for the current selected MULTIPLE_CHOICE question.
	 * @param answerIndex
	 */
	public void setMultipleChoiceSolution(int answerIndex) {
		selectQuestionAndAnswersTab();
		selenium.click("ui=testEditor::content_questionAnswers_setCorrectMc(indexOfAnswer=" + answerIndex + ")");
		selenium.click("ui=testEditor::content_questionAnswers_save()");
		selenium.waitForPageToLoad("30000");
	}
	
}
