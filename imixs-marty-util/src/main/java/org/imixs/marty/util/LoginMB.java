package org.imixs.marty.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This Backing Bean acts as a Login Helper Class. Can be used to identify the
 * login state
 * 
 * The bean controls also the locale of a user session. The backing
 * 
 * @author rsoika
 * 
 */
public class LoginMB {
	public final String COOKIE_LOCALE = "imixs.sywapp.locale";
	
	private String locale;
	private String anonymouslocale;
	private ArrayList<SelectItem> localeSelection = null;
	

	
	public boolean isAuthenticated() {
		return (getUserPrincipal() != null);
	}

	public String getUserPrincipal() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		return externalContext.getUserPrincipal() != null ? externalContext
				.getUserPrincipal().toString() : null;
	}

	public String getRemoteUser() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		return remoteUser;
	}

	
	/*
	public String getServerURI() {
		HttpServletRequest servletRequest = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();

		
		String port=""+servletRequest.getLocalPort();
		
		String server = servletRequest.getServerName();
		return "http://"+ server+":"+port+"";

	}
	*/

	public void doLogout(ActionEvent event) {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();

		HttpSession session = (HttpSession) externalContext.getSession(false);

		session.invalidate();

	}

	
	/**
	 * This getter method trys to get the locale out from the cookie if
	 * available. Otherwise it will default to "en"
	 * 
	 * @return
	 */
	public String getLocale() {
		// if no locale is set try to get it from cookie or set default
		if (locale == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			String cookieName = null;
			String cookiePath = null;

			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					cookiePath = cookie[i].getPath();

					if (cookieName.equals(COOKIE_LOCALE)

					) {
						locale = cookie[i].getValue();
						break;
					}

				}
			}

			// still no value found? - default to "en"
			if (locale == null || "".equals(locale) || "null".equals(locale)) {
				Locale ldefault = request.getLocale();
				if (ldefault != null)
					locale = ldefault.toString();
				else
					locale = "en";
			}

			locale = verifyLocale(locale);
		}
		return locale;
	}

	public void setLocale(String locale) {
		if (locale == null)
			locale = "en";
		this.locale = locale;

		// update cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		Cookie cookieLocale = new Cookie(COOKIE_LOCALE, locale);
		cookieLocale.setPath(request.getContextPath());

		// 30 days
		cookieLocale.setMaxAge(2592000);
		response.addCookie(cookieLocale);

	}
	
	
	

	/**
	 * Anonymous Locale is only a language setting during user is not
	 * authenticated. Authenticated user locale will be stored in users profile
	 * 
	 * @return
	 */
	public String getAnonymouslocale() {
		return this.getLocale();
	}

	public void setAnonymouslocale(String anonymouslocale) {
		this.anonymouslocale = anonymouslocale;
	}
	

	/**
	 * This method returns a list of SelectItems with the predefined Languages
	 * The locale is configured in the property file
	 * configuration/locale.properties
	 * 
	 * If a locale is supported also the resource bundles in directory /bundle/
	 * need to be supported
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getLocaleSelection() {
		// load My projects only once...
		if (localeSelection == null) {
			// build new localeSelection
			localeSelection = new ArrayList<SelectItem>();
			ResourceBundle r = ResourceBundle.getBundle("configuration.locale");
			Enumeration<String> enkeys = r.getKeys();
			while (enkeys.hasMoreElements()) {
				String sKey = enkeys.nextElement();
				String sValue = r.getString(sKey);
				localeSelection.add(new SelectItem(sKey, sValue));
			}

			Collections.sort(localeSelection,
					new SelectItemComparator(FacesContext.getCurrentInstance()
							.getViewRoot().getLocale(), true));


		}
		return localeSelection;
	}

	
	/**
	 * Methode for language settings in anonymous mode. This method is available
	 * throug the service nav.
	 * 
	 * @return
	 */
	public void doChangeLanguage(ActionEvent event) {
		this.setLocale(anonymouslocale);
	}
	
	
	/**
	 * This method verifies a locale against the current skin configuration
	 * file: /configuration/skins.properties
	 * 
	 * if the locale is not found the method will default to the frist locale
	 * found in property file So a valid locale will be returned!
	 * 
	 * @param aSkin
	 * @return
	 */
	private String verifyLocale(String aLocale) {

		String sBestLocale = null;

		/* Test if current skin is available in the skin configuration */
		ResourceBundle r = ResourceBundle.getBundle("configuration.locale");

		Enumeration<String> enkeys = r.getKeys();
		while (enkeys.hasMoreElements()) {
			String sKey = enkeys.nextElement();

			// save first skin
			if (sBestLocale == null)
				sBestLocale = sKey;

			// test if current skin match...
			if (sKey.equals(aLocale))
				// yes! aSkin is valid!
				return aLocale;
		}

		// aSkin did not match anny of the available skinn in the skin
		// configuration
		// so return the first skin found in the configuration
		return sBestLocale;

	}


}
