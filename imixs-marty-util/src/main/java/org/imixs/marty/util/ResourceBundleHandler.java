package org.imixs.marty.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * The ResourceBundleHandler provides helper method to lookup a label in the app
 * or message bundle. This simplifies the front end implementation as the client
 * does not have to know the bundle a specific resource is located:
 * <p>
 * {@code
 * <h1>#{resourceBundleHandler.findMessage('application_title')}</h1>}
 * <p>
 * The ResourceBundleHandler load the bundles based on the current user locale.
 * <p>
 * Resource bundle instances created by the getBundle factory methods are cached
 * by default, and the factory methods return the same resource bundle instance
 * multiple times if it has been cached. For that reason a RequestScoped bean is
 * used here.
 * 
 * @author rsoika
 * @version 1.0
 */
@Named
@RequestScoped
public class ResourceBundleHandler {

    private Locale browserLocale = null;
    private ResourceBundle messagesBundle = null;
    private ResourceBundle appBundle = null;
    private ResourceBundle customBundle = null;

    /**
     * This method finds the browser locale
     * 
     */
    @PostConstruct
    public void init() {
        browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        messagesBundle = ResourceBundle.getBundle("bundle.messages", browserLocale);
        appBundle = ResourceBundle.getBundle("bundle.app", browserLocale);
        customBundle = ResourceBundle.getBundle("bundle.custom", browserLocale);
    }

    public Locale getBrowserLocale() {
        return browserLocale;
    }

    public ResourceBundle getMessagesBundle() {
        return messagesBundle;
    }

    public ResourceBundle getAppBundle() {
        return appBundle;
    }

    public ResourceBundle getCustomBundle() {
        return customBundle;
    }

    
    /**
     * Default getter method
     * @param key
     * @return
     */
    public String get(String key) {
        return findMessage(key);
    }
    /**
     * This helper method findes a message by key searching all bundles.
     * 
     * @param pe
     */
    public String findMessage(String key) {

        // try to find the message text in custom bundle...
        try {

            String messageFromBundle = customBundle.getString(key);
            if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
                return messageFromBundle;
            }
        } catch (MissingResourceException mre) {
            // no op
        }
        // try to find the message text in appp bundle...
        try {

            String messageFromBundle = appBundle.getString(key);
            if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
                return messageFromBundle;
            }
        } catch (MissingResourceException mre) {
            // no op
        }

        // try to find the message text in messages bundle...
        try {

            String messageFromBundle = messagesBundle.getString(key);
            if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
                return messageFromBundle;
            }
        } catch (MissingResourceException mre) {
            // no op
        }

        return "";

    }

}
