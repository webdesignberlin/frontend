package com.gu.fronts.integration.test.page.util;

import static com.gu.fronts.integration.test.fw.selenium.ByTestAttributeSelector.byTestAttribute;
import static com.gu.fronts.integration.test.page.util.PageElementHelper.findElementBy;
import static org.openqa.selenium.By.cssSelector;

import java.lang.reflect.Constructor;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

import com.gu.fronts.integration.test.fw.selenium.CustomElementLocatorFactory;
import com.gu.fronts.integration.test.fw.selenium.CustomFieldDecorator;

public class CustomPageFactory {

    /**
     * Wraps {@link PageFactory#initElements(ElementLocatorFactory, Object)} and provides a custom
     * {@link ElementLocatorFactory}
     */
    public <Page> Page initPage(WebDriver webDriver, Class<Page> pageClass) {
        Page page = instantiatePage(webDriver, pageClass);
        PageFactory.initElements(new CustomFieldDecorator(new CustomElementLocatorFactory(webDriver)), page);
        return page;
    }

    /**
     * Shameless Copy of {@link PageFactory#instantiatePage} as it is private
     */
    private static <T> T instantiatePage(WebDriver driver, Class<T> pageClassToProxy) {
        try {
            try {
                Constructor<T> constructor = pageClassToProxy.getConstructor(WebDriver.class);
                return constructor.newInstance(driver);
            } catch (NoSuchMethodException e) {
                return pageClassToProxy.newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Special page factory method which finds an element by provided test attribute id, starting from the provided
     * parent element, then instantiate a Page Object with only the found element. No other WebElements will be set or
     * initialised unless explicitly done so in the constructor of that page object.
     */
    public <Page> Page initPage(WebDriver webDriver, Class<Page> pageClass, WebElement parentElement,
            String testAttributeId) {
        Page page = instantiatePage(webDriver, pageClass,
                findElementBy(parentElement, cssSelector(byTestAttribute(testAttributeId))));

        return page;
    }

    /**
     * Special page factory method which initializes a Page Object using the provided web driver and WebElement. No
     * other WebElements will be set or initialized
     */
    public <Page> Page initPage(WebDriver webDriver, Class<Page> pageClass, WebElement rootElement) {
        return instantiatePage(webDriver, pageClass, rootElement);
    }

    private static <T> T instantiatePage(WebDriver driver, Class<T> pageClassToProxy, WebElement rootElement) {
        try {
            Constructor<T> constructor = pageClassToProxy.getConstructor(WebDriver.class, WebElement.class);
            return constructor.newInstance(driver, rootElement);
        } catch (Exception e) {
            throw new RuntimeException("Could not initalize Page Object: " + pageClassToProxy, e);
        }
    }
}