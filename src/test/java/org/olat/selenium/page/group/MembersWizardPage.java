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
package org.olat.selenium.page.group;

import java.util.List;

import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.user.restapi.UserVO;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Drive the wizard to add members
 * 
 * Initial date: 03.07.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MembersWizardPage {

	public static final By nextBy = By.className("o_wizard_button_next");
	public static final By finishBy = By.className("o_wizard_button_finish");
	
	private WebDriver browser;
	
	public MembersWizardPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public MembersWizardPage next() {
		WebElement next = browser.findElement(nextBy);
		Assert.assertTrue(next.isDisplayed());
		Assert.assertTrue(next.isEnabled());
		next.click();
		OOGraphene.waitBusy();
		OOGraphene.closeBlueMessageWindow(browser);
		return this;
	}
	
	public MembersWizardPage finish() {
		WebElement finish = browser.findElement(finishBy);
		Assert.assertTrue(finish.isDisplayed());
		Assert.assertTrue(finish.isEnabled());
		finish.click();
		OOGraphene.waitBusy();
		OOGraphene.closeBlueMessageWindow(browser);
		return this;
	}
	
	/**
	 * Search member and select them
	 * @param user
	 * @return
	 */
	public MembersWizardPage searchMember(UserVO user) {
		//Search by username
		By usernameBy = By.cssSelector(".o_sel_usersearch_searchform input[type='text']");
		OOGraphene.waitElement(usernameBy);
		
		List<WebElement> searchFields = browser.findElements(usernameBy);
		Assert.assertFalse(searchFields.isEmpty());
		searchFields.get(0).sendKeys(user.getLogin());

		By searchBy = By.cssSelector(".o_sel_usersearch_searchform a.btn-default");
		WebElement searchButton = browser.findElement(searchBy);
		searchButton.click();
		OOGraphene.waitBusy();
		
		//check
		By checkAllBy = By.cssSelector("div.modal div.o_table_wrapper input[type='checkbox']");
		List<WebElement> checkAll = browser.findElements(checkAllBy);
		Assert.assertFalse(checkAll.isEmpty());
		for(WebElement check:checkAll) {
			check.click();
		}
		return this;
	}
}