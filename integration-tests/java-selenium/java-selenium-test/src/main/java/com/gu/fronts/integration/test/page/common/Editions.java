package com.gu.fronts.integration.test.page.common;

import static com.gu.fronts.integration.test.page.util.PageElementHelper.elementIsALink;
import static java.lang.String.format;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.gu.fronts.integration.test.fw.selenium.ByTestAttributeSelector;
import com.gu.fronts.integration.test.fw.selenium.FindByTestAttribute;
import com.gu.fronts.integration.test.page.nwfront.NetworkFrontPage;
import com.gu.fronts.integration.test.page.util.AbstractParentPage;

public class Editions extends AbstractParentPage {
    @FindByTestAttribute(using = "editions")
    private WebElement editions;

    @FindByTestAttribute(using = "edition-US")
    private WebElement editionUS;

    @FindByTestAttribute(using = "edition-UK")
    private WebElement editionUK;

    @FindByTestAttribute(using = "edition-AU")
    private WebElement editionAU;

    public Editions(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public Editions isDisplayed() {
        super.assertExistsAndDisplayed(editions, editionUK, editionUS, editionAU);
        return this;
    }

    public NetworkFrontPage selectUSEdition() {
        editionUS.click();
        return pageFactory.initPage(webDriver, NetworkFrontPage.class);
    }

    public NetworkFrontPage selectUKEdition() {
        editionUK.click();
        return pageFactory.initPage(webDriver, NetworkFrontPage.class);
    }

    public NetworkFrontPage selectAUEdition() {
        editionAU.click();
        return pageFactory.initPage(webDriver, NetworkFrontPage.class);
    }

    public Editions usEditionSelected() {
        return editionSelected(editionUS);
    }

    public Editions isUkEditionSelected() {
        return editionSelected(editionUK);
    }

    public Editions auEditionSelected() {
        return editionSelected(editionAU);
    }

    private Editions editionSelected(WebElement editionElement) throws AssertionError {
        if (elementIsALink(editionElement)) {
            throw new AssertionError(format("%s was not selected",
                    editionElement.getAttribute(ByTestAttributeSelector.TEST_ATTR_NAME)));
        }
        return this;
    }

    public Editions isUsEditionPresent() {
        super.assertExistsAndDisplayed(editionUS);
        return this;
    }

    public Editions isAuEditionPresent() {
        super.assertExistsAndDisplayed(editionAU);
        return this;
    }

}