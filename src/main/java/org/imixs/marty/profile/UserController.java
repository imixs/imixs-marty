/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.profile;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.marty.security.UserGroupService;
import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.faces.data.WorkflowController;
import org.imixs.workflow.faces.data.WorkflowEvent;
import org.imixs.workflow.faces.util.LoginController;

/**
 * This backing beans handles the Profile entity for the current user and
 * provides a application scoped access to all other profiles through the
 * ProfileService EJB.
 * 
 * A new user profile will be created automatically if no profile yet exists!
 * The user is identified by its principal user name. This name is mapped to the
 * attribute txtname.
 * 
 * The UserController provides the user 'locale' and 'language' which is used in
 * JSF Pages to display pages using the current user settings.
 * 
 * With the methods mark() and unmark() workitems can be added into the users
 * profile favorite list.
 * <p>
 * The controller allows to store up to 50 favorite workitem IDs in the profile.
 * 
 *  
 * @author rsoika
 */
@Named("userController")
@SessionScoped
public class UserController implements Serializable {

    public static final String LINK_PROPERTY = "$workitemref";
    public static final String LINK_PROPERTY_DEPRECATED = "txtworkitemref";

    public final static String ITEM_USER_ICON = "user.icon";
    public final static String ITEM_SIGNATURE_IMAGE = "signature.image";
    public final static int MAX_FAVORITE_ENTRIES = 50;
    public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;
    public final static String DEFAULT_LOCALE = "de_DE";
    public final static String COOKIE_LOCALE = "imixs.workflow.locale";

    @EJB
    protected ProfileService profileService;

    @EJB
    protected UserGroupService userGroupService;

    @EJB
    protected WorkflowService workflowService;

    @Inject
    protected LoginController loginController;

    @Inject
    @ConfigProperty(name = "profile.login.event", defaultValue = "0")
    private Provider<Integer> profileLoginEvent;

    @Inject
    protected WorkflowController workflowController;

    private static final long serialVersionUID = 1L;
    private ItemCollection workitem = null;
    private boolean profileLoaded = false;
    private Locale locale;

    private static Logger logger = Logger.getLogger(UserController.class.getName());

    public UserController() {
        super();
    }

    /**
     * The init method is used to load a user profile or automatically create a new
     * one if no profile for the user is available. A new Profile will be filled
     * with default values.
     * 
     * This method did not use the internal cache of the ProfileService to lookup
     * the user profile, to make sure that the entity is uptodate when a user logs
     * in.
     * 
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     */
    @PostConstruct
    public void init() throws AccessDeniedException, ProcessingErrorException {

        // test user is logged-in and automatically create profile if no profile
        // exists yet
        if (this.loginController.isAuthenticated() && !profileLoaded) {

            // try to load the profile for the current user
            ItemCollection profile = profileService.lookupProfileById(loginController.getUserPrincipal());
            if (profile == null || profile.getModelVersion().isEmpty()) {
                try {
                    profile = profileService.createProfile(loginController.getUserPrincipal(), getLocale().toString());
                } catch (RuntimeException | PluginException | ModelException e) {
                    logger.severe("unable to create profile for userid '" + loginController.getUserPrincipal() + "': "
                            + e.getMessage());
                    // logout user!!
                    logger.severe("logout current userid '" + loginController.getUserPrincipal() + "'...");
                    loginController.doLogout(null);
                    throw new ProcessingErrorException(UserController.class.getName(),
                            ProcessingErrorException.INVALID_WORKITEM, " unable to create profile!", e);
                }
            } else {
                // check if profile.login.event is defined
                logger.fine("profile.login.event=" + profileLoginEvent);
                if (profileLoginEvent.get() > 0) {
                    profile.setEventID(profileLoginEvent.get());
                    try {
                        profile = workflowService.processWorkItem(profile);
                    } catch (PluginException | ModelException | ProcessingErrorException | EJBException e) {
                        logger.warning("Unable to process profile.login.event=" + profileLoginEvent
                                + " - please check configuration!");
                    }
                }

            }

            this.setWorkitem(profile);
            profileLoaded = true;

            // Now reset current locale based on the profile information
            updateLocaleFromProfile();
            logger.info("profile '" + loginController.getUserPrincipal() + "' initialized.");
        }

    }

    /**
     * This method returns the current users userprofile entity. The method verifies
     * if the profile was yet loaded. if not the method tries to initiate the
     * profile - see method init();
     * 
     * @return
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     */
    public ItemCollection getWorkitem() throws AccessDeniedException, ProcessingErrorException {
        // test if current users profile was loaded
        if (!profileLoaded)
            init();
        if (workitem == null)
            workitem = new ItemCollection();
        return workitem;
    }

    public void setWorkitem(ItemCollection aworkitem) {
        this.workitem = aworkitem;
    }

    /**
     * This method returns the current user locale. If the user is not logged in the
     * method try to get the locale out from the cookie. If no cockie is set the
     * method defaults to "de_DE"
     * 
     * @return - ISO Locale format
     */
    public Locale getLocale() {
        // if no locale is set try to get it from cookie or set default
        if (locale == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                    .getRequest();

            String cookieName = null;

            Cookie cookie[] = ((HttpServletRequest) facesContext.getExternalContext().getRequest()).getCookies();
            if (cookie != null && cookie.length > 0) {
                for (int i = 0; i < cookie.length; i++) {
                    cookieName = cookie[i].getName();
                    if (cookieName.equals(COOKIE_LOCALE)) {
                        String sLocale = cookie[i].getValue();
                        if (sLocale != null && !"".equals(sLocale)) {

                            // split locale
                            StringTokenizer stLocale = new StringTokenizer(sLocale, "_");
                            if (stLocale.countTokens() == 1) {
                                // only language variant
                                String sLang = stLocale.nextToken();
                                String sCount = sLang.toUpperCase();
                                locale = new Locale(sLang, sCount);
                            } else {
                                // language and country
                                String sLang = stLocale.nextToken();
                                String sCount = stLocale.nextToken();
                                locale = new Locale(sLang, sCount);
                            }

                        }
                        break;
                    }

                }
            }

            // still no value found? - default to "en"
            if (locale == null || "".equals(locale.getLanguage())) {
                Locale ldefault = request.getLocale();
                if (ldefault != null) {
                    locale = ldefault;
                } else {
                    locale = new Locale(DEFAULT_LOCALE);
                }
            }

        }
        return locale;
    }

    public void setLocale(Locale alocale) {
        if (alocale == null || "".equals(alocale))
            locale = new Locale(DEFAULT_LOCALE);
        else
            this.locale = alocale;

        // update cookie
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext()
                .getResponse();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        Cookie cookieLocale = new Cookie(COOKIE_LOCALE, locale.toString());
        if (request.getContextPath().isEmpty()) {
            cookieLocale.setPath("/");
        } else {
            cookieLocale.setPath(request.getContextPath());
        }
        // 30 days
        cookieLocale.setMaxAge(2592000);
        response.addCookie(cookieLocale);

    }

    /**
     * returns the user language
     * 
     * @return
     */
    public String getLanguage() {
        return getLocale().getLanguage();
    }

    /**
     * This method returns a cached cloned version of a user profile for a given
     * useraccount. The profile is cached in the current user session
     * 
     * @param aName
     * @return
     */
    public ItemCollection getProfile(String aAccount) {
        return profileService.findProfileById(aAccount);
    }

    /**
     * This method returns the username (displayname) for a useraccount. If no
     * Username is set in the profile then we return the useraccount.
     * 
     * @param aName
     * @return
     */
    public String getUserName(String aAccount) {
        // use internal cache
        ItemCollection profile = getProfile(aAccount);
        if (profile == null) {
            return null;
        } else {
            return profile.getItemValueString("txtuserName");
        }
    }

    /**
     * This method returns the email for a useraccount
     * 
     * @param aName
     * @return
     */
    public String getEmail(String aAccount) {
        // use internal cache
        ItemCollection profile = getProfile(aAccount);
        if (profile == null) {
            return null;
        } else {
            return profile.getItemValueString("txtemail");
        }
    }

    /**
     * removes the current user icon
     * 
     */
    public void removeUserIcon() {
        String userIcon = workflowController.getWorkitem().getItemValueString(ITEM_USER_ICON);
        // support deprecated user icon item name
        if (userIcon.isEmpty() && !"".equals(workflowController.getWorkitem().getItemValueString("txtusericon"))) {
            userIcon = workflowController.getWorkitem().getItemValueString("txtusericon");

        }
        workflowController.getWorkitem().removeFile(userIcon);
        workflowController.getWorkitem().replaceItemValue("txtusericon", "");
        workflowController.getWorkitem().replaceItemValue(ITEM_USER_ICON, "");
    }
 
    /**
     * removes the current user signature
     * 
     */
    public void removeSignature() {
        String userSignature = workflowController.getWorkitem().getItemValueString(ITEM_SIGNATURE_IMAGE);
        workflowController.getWorkitem().removeFile(userSignature);
        workflowController.getWorkitem().replaceItemValue(ITEM_SIGNATURE_IMAGE, "");
    }

    /**
     * WorkflowEvent listener listens to WORKITEM events to reset the current
     * username workitem if processed.
     * <p>
     * The method also updates the user Locale
     * <p>
     * In case a new image is uplaoded the method set the item user.icon. The
     * deprecated item name 'txtusericon' is still supported..
     * <p>
     * Optional Signature images can be uploaded if the file name starts with
     * 'signatrue.'. If a signature image exists the item 'signature.image' is set
     * <p>
     * A profile can hole one user.icon and one signature.image. Deprecated images
     * will be removed by this method automatically.
     * 
     * @param workflowEvent
     * 
     **/
    public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
        String userID = null;
        String userIcon = null;
        String signatureImage = null;

        if (workflowEvent == null || workflowEvent.getWorkitem() == null) {
            return;
        }

        String sType = workflowEvent.getWorkitem().getItemValueString("type");

        // skip if not a profile...
        if (!sType.startsWith("profile"))
            return;

        // Update usericon, signature image imformation
        if ("profile".equals(sType) && WorkflowEvent.WORKITEM_BEFORE_PROCESS == workflowEvent.getEventType()) {

            userID = workflowEvent.getWorkitem().getItemValueString("txtname");

            userIcon = getWorkitem().getItemValueString(ITEM_USER_ICON);
            // support deprecated user icon item name
            if (userIcon.isEmpty() && !"".equals(getWorkitem().getItemValueString("txtusericon"))) {
                userIcon = getWorkitem().getItemValueString("txtusericon");
                getWorkitem().replaceItemValue(ITEM_USER_ICON, userIcon);
            }

            signatureImage = workflowEvent.getWorkitem().getItemValueString(ITEM_SIGNATURE_IMAGE);

            /*
             * Test new uploaded images. we support two images, a user icon and a signature
             * image. The method removes deprecated entries.
             */
            List<FileData> fileDataList = workflowEvent.getWorkitem().getFileData();
            // reverse list - newest first
            Collections.sort(fileDataList, new FileDataComparator());
            boolean signatureFound = false;
            boolean usericonFound = false;
            for (FileData fileData : fileDataList) {
                String filenametest = fileData.getName().toLowerCase();

                // test if the image is a signature.image...
                if (filenametest.startsWith("signature.") && signatureFound == false) {
                    signatureFound = true;
                    if (!signatureImage.equals(fileData.getName())) {
                        signatureImage = fileData.getName();
                        workflowEvent.getWorkitem().replaceItemValue(ITEM_SIGNATURE_IMAGE, signatureImage);
                        logger.info("... '" + userID + "' new signature image upload: " + signatureImage);
                    }
                }

                // test if the image is a user.icon...
                if (!filenametest.startsWith("signature.") && (filenametest.endsWith(".png")
                        || filenametest.endsWith(".gif") || filenametest.endsWith(".jpg")) && usericonFound == false) {
                    usericonFound = true;
                    if (!userIcon.equals(fileData.getName())) {
                        userIcon = fileData.getName();
                        workflowEvent.getWorkitem().replaceItemValue(ITEM_USER_ICON, userIcon);
                        logger.info("... '" + userID + "' new user icon upload: " + userIcon);
                        // support deprecated image
                        workflowEvent.getWorkitem().replaceItemValue("txtusericon", userIcon);
                    }
                }

            }

            // remove deprecated images files which are not user.icon or signature.image
            ListIterator<FileData> iter = fileDataList.listIterator();
            while (iter.hasNext()) {

                FileData fileData = iter.next();
                String fileName = fileData.getName();

                if (fileName.startsWith("signature.") && !fileName.equals(signatureImage)) {
                    // iter.remove();
                    workflowEvent.getWorkitem().removeFile(fileName);
                }
                if (!fileName.startsWith("signature.") && !fileName.equals(userIcon)) {
                    // iter.remove();
                    workflowEvent.getWorkitem().removeFile(fileName);
                }
            }

        }

        // discard cached user profile and update locale
        if ("profile".equals(sType) && WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent.getEventType()) {

            // check if current user profile was processed....
            String sName = workflowEvent.getWorkitem().getItemValueString("txtName");
            if (sName.equals(this.getWorkitem().getItemValueString("txtName"))) {
                logger.finest("......reload current user profile");
                setWorkitem(workflowEvent.getWorkitem());
                // update locale
                updateLocaleFromProfile();
            }

        }

    }

    /**
     * Returns true if the uniqueid is stored in the profile favorites
     * 
     * @param id
     * @return
     */
    public boolean isFavorite(String id) {
        return getFavoriteIds().contains(id);
    }

    /**
     * Returns a list with all uniqueids stored in the profile favorites
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> getFavoriteIds() {
        if (getWorkitem() == null)
            return new ArrayList<String>();

        // support deprecated ref field
        if (!workitem.hasItem(LINK_PROPERTY) && workitem.hasItem(LINK_PROPERTY_DEPRECATED)) {
            return workitem.getItemValue(LINK_PROPERTY_DEPRECATED);
        } else {
            return workitem.getItemValue(LINK_PROPERTY);
        }

    }

    public void addFavorite(String id) {
        if (getWorkitem() == null)
            return;

        List<String> list = getFavoriteIds();
        // we expect that the id is in the list-..
        if (!list.contains(id)) {
            logger.finest("......add WorkitemRef:" + id);
            list.add(id);
            workitem.replaceItemValue(LINK_PROPERTY, list);
            
            // allow maximum 100 entries !!!
            while (list.size()>MAX_FAVORITE_ENTRIES) {
                list.remove(0);
            }
            
            workitem = workflowService.getDocumentService().save(workitem);
        }
    }

    public void removeFavorite(String id) {
        if (getWorkitem() == null)
            return;

        List<String> list = getFavoriteIds();
        // we expect that the id is in the list-..
        if (list.contains(id)) {
            logger.finest("......remove WorkitemRef:" + id);
            list.remove(id);
            workitem.replaceItemValue(LINK_PROPERTY, list);
            workitem = workflowService.getDocumentService().save(workitem);
        }
    }

    /**
     * This method returns true if the user may be using a mobile device
     * 
     * @return
     */
    public boolean isMobileUser() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String userAgent = request.getHeader("user-agent").toLowerCase();
        return (userAgent.indexOf("mobile") > -1);

    }

    /*
     * HELPER METHODS
     */

    /**
     * This method updates user locale stored in the user profile entity to the
     * faces context.
     * 
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     * 
     */
    private void updateLocaleFromProfile() throws AccessDeniedException, ProcessingErrorException {

        Locale profileLocale = null;

        // Verify if Locale is available in profile
        String sLocale = getWorkitem().getItemValueString("txtLocale");
        if ("".equals(sLocale)) {
            // get default value
            profileLocale = getLocale();
            getWorkitem().replaceItemValue("txtLocale", profileLocale.toString());
        } else {

            if (sLocale.indexOf('_') > -1) {
                String language = sLocale.substring(0, sLocale.indexOf('_'));
                String country = sLocale.substring(sLocale.indexOf('_') + 1);
                profileLocale = new Locale(language, country);
            } else {
                profileLocale = new Locale(sLocale);
            }
        }

        logger.fine("update user locale: " + profileLocale);
        // reset locale to update cookie
        setLocale(profileLocale);
        // set locale for context
        FacesContext.getCurrentInstance().getViewRoot().setLocale(profileLocale);

    }

    /**
     * Compares two Filedata by its creation date value. This functionality should
     * be covered by the ItemCollection.
     * 
     * @author rsoika
     * 
     */
    class FileDataComparator implements Comparator<FileData> {
        @SuppressWarnings("unused")
        private final Collator collator;

        public FileDataComparator() {
            this.collator = Collator.getInstance(Locale.getDefault());
        }

        @SuppressWarnings("rawtypes")
        public int compare(FileData a, FileData b) {
            Date dateA = null;
            Date dateB = null;
            List la = (List) a.getAttribute("$created");
            if (la != null && la.size() > 0) {
                dateA = (Date) la.get(0);
            }
            List lb = (List) b.getAttribute("$created");
            if (lb != null && lb.size() > 0) {
                dateB = (Date) lb.get(0);
            }
            if (dateB == null && dateA != null) {
                return 1;
            }
            if (dateA == null && dateB != null) {
                return -1;
            }
            if (dateA == null && dateB == null) {
                return 0;
            }

            int result = dateA.compareTo(dateB);

            return result;

        }

    }

}
