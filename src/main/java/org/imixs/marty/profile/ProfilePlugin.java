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

import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.marty.util.ImageCompactor;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This plug-in supports additional business logic for profile entities. This
 * Plugins is used by the System Workflow only.
 * <p>
 * The plugin verifies the userId and email address for input patterns and
 * validates for duplicates. The input pattern for the userID can be defined by
 * the imixs property
 * 
 * <ul>
 * <li>security.userid.input.pattern</li>
 * </ul>
 * 
 * The default value is {@code "^[A-Za-z0-9.@\\-\\w]+" }
 * <p>
 * In addition the input mode can be set to LOWERCASE or UPPERCASE. This is
 * controlled by the imixs property
 * 
 * <ul>
 * <li>security.userid.input.mode</li>
 * </ul>
 * The default value is LOWERCASE.
 * 
 * @author rsoika
 */
public class ProfilePlugin extends AbstractPlugin {

    private String userID = null;
    private static Logger logger = Logger.getLogger(ProfilePlugin.class.getName());

    // error codes
    public static String USERNAME_ALREADY_TAKEN = "USERNAME_ALREADY_TAKEN";
    public static String INVALID_USERNAME = "INVALID_USERNAME";
    public static String EMAIL_ALREADY_TAKEN = "EMAIL_ALREADY_TAKEN";
    public static String INVALID_EMAIL = "INVALID_EMAIL";
    public static String NO_PROFILE_SERVICE_FOUND = "NO_PROFILE_SERVICE_FOUND";

    // input patterns
    // public final static String EMAIL_PATTERN =
    // "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public final static String EMAIL_PATTERN = "^[_A-Za-z0-9-\\\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[-A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public final static String DEFAULT_USERID_PATTERN = "^[A-Za-z0-9.@\\-\\w]+";
    public final static String DEFAULT_USER_INPUT_MODE = "LOWERCASE";

    @EJB
    AdminPService adminPService;

    @EJB
    ImageCompactor imageCompactor;

    @EJB
    ProfileService profileService;

    @Inject
    @ConfigProperty(name = "security.userid.input.mode", defaultValue = DEFAULT_USER_INPUT_MODE)
    String userInputMode;

    @Inject
    @ConfigProperty(name = "security.userid.input.pattern", defaultValue = DEFAULT_USERID_PATTERN)
    String userInputPattern;

    @Inject
    @ConfigProperty(name = "security.email.input.pattern", defaultValue = EMAIL_PATTERN)
    String emailInputPattern;

    @Inject
    @ConfigProperty(name = "security.email.unique", defaultValue = "true")
    String sUniqueEmailMode;

    @Inject
    @ConfigProperty(name = "profile.image.maxwith", defaultValue = "600")
    int imageMaxWidth;

    /**
     * The Plug-in verifies if the workitem is from the type 'profile'. The plug-in
     * tests if the usernam or email is unique
     **/
    @Override
    public ItemCollection run(ItemCollection workItem, ItemCollection documentActivity) throws PluginException {
        // validate profile..
        if ("profile".equals(workItem.getItemValueString("type"))) {
            validateUserProfile(workItem);
            // store userID local to discard the cache when close()...
            userID = workItem.getItemValueString("txtName");

            // resize profile image if necessary...
            try {
                imageCompactor.resize(workItem, imageMaxWidth);
            } catch (Exception e) {
                logger.warning("Unable to resize profile image - " + e.getMessage());
            }

            // load the old user profile
            ItemCollection oldProfile = this.getWorkflowService().getDocumentService().load(workItem.getUniqueID());
            if (oldProfile != null) {
                // verify if the deputy has changed?
                String lastDeputy = oldProfile.getItemValueString("namdeputy");
                String currentDeputy = workItem.getItemValueString("namdeputy");
                if (!currentDeputy.isEmpty() && !currentDeputy.equals(lastDeputy)) {
                    // start an adminP job...
                    createAdminPJob(currentDeputy);
                }
            }
        }
        return workItem;
    }

    /**
     * Create an AdminP JOB_RENAME_USER. The method expects the source userId and
     * the target userId.
     * 
     */
    private void createAdminPJob(String targetUserID) {
        logger.info("namdeputy has changed, staring new ein adminp job: " + userID + "=>" + targetUserID);
        if (adminPService != null) {
            ItemCollection adminPJob = new ItemCollection();
            adminPJob.replaceItemValue("type", "adminp");
            adminPJob.replaceItemValue("typelist", "workitem,childworkitem");
            adminPJob.replaceItemValue("namfrom", userID);
            adminPJob.replaceItemValue("namto	", targetUserID);
            adminPJob.replaceItemValue("job", AdminPService.JOB_RENAME_USER);
            adminPService.createJob(adminPJob);
        } else {
            logger.warning("Service not injected!");
        }

    }

    /**
     * This method discards the cache for the current userID (ProfileService). This
     * is called only in case a profile was processed and no rollback is called.
     */
    @Override
    public void close(boolean rollbackTransaction) throws PluginException {

        // if no rollback we can discard the cache
        if (!rollbackTransaction && userID != null && !userID.isEmpty()) {
            // discared cache for this name
            profileService.discardCache(userID);
        }
        super.close(rollbackTransaction);
    }

    /**
     * The method validates the userProfile entity. The txtName property will be
     * initialized if a new profile is created The txtName property will always be
     * lower case!
     * 
     * @param profile
     * @throws PluginException
     */
    void validateUserProfile(ItemCollection profile) throws PluginException {
        String sName = profile.getItemValueString("txtName");
        String sEmail = profile.getItemValueString("txtEmail");

        // is txtname set?
        if (sName == null || "".equals(sName)) {
            throw new PluginException(ProfilePlugin.class.getSimpleName(), INVALID_USERNAME, "Missing UserID ");
        }

        // Trim names....
        if (!sName.equals(sName.trim())) {
            sName = sName.trim();
            profile.replaceItemValue("txtName", sName);
        }

        // lower/upper case userid?
        if ("lowercase".equalsIgnoreCase(userInputMode)) {
            sName = sName.toLowerCase();
            profile.replaceItemValue("txtName", sName);
        }
        if ("uppercase".equalsIgnoreCase(userInputMode)) {
            sName = sName.toUpperCase();
            profile.replaceItemValue("txtName", sName);
        }

        // validate userid if pattern defined.
        if (!isValidUserId(sName)) {
            throw new PluginException(ProfilePlugin.class.getSimpleName(), INVALID_USERNAME,
                    "UserID did not match 'security.userid.input.pattern'=" + userInputPattern,
                    new Object[] { profile.getItemValueString("txtName") });
        }

        // verify email pattern
        if (!isValidEmailAddress(sEmail)) {
            throw new PluginException(ProfilePlugin.class.getSimpleName(), INVALID_EMAIL, "Invalid Email Address",
                    new Object[] { profile.getItemValueString("txtEmail") });
        }

        // verify if userid is already taken
        if (isUserNameTaken(profile))
            throw new PluginException(ProfilePlugin.class.getSimpleName(), USERNAME_ALREADY_TAKEN,
                    "Username is already taken - verifiy txtname and txtusername",
                    new Object[] { profile.getItemValueString("txtName") });

        // verify if email is already taken
        if (isEmailTaken(profile))
            throw new PluginException(ProfilePlugin.class.getSimpleName(), EMAIL_ALREADY_TAKEN,
                    "Email is already taken - verifiy txtemail",
                    new Object[] { profile.getItemValueString("txtEmail") });

    }

    /**
     * Validate userID with regular expression provided by the property
     * 'security.userid.input.pattern'
     *
     * @param userid - userID for validation
     * @return true valid userID, false invalid userID
     */
    public boolean isValidUserId(final String userid) {
        Pattern pattern;
        Matcher matcher;

        if (userInputPattern != null && !userInputPattern.isEmpty()) {
            pattern = Pattern.compile(userInputPattern);
            matcher = pattern.matcher(userid);
            return matcher.matches();
        }
        return true;
    }

    /**
     * Validates a Emailaddress with regular expression
     *
     * @param userid - email for validation
     * @return true valid email, false invalid email
     */
    public boolean isValidEmailAddress(final String email) {
        Pattern pattern;
        Matcher matcher;

        // verify email pattern

        if (email != null && !email.isEmpty()) {
            pattern = Pattern.compile(emailInputPattern);
            matcher = pattern.matcher(email);
            return matcher.matches();
        }
        return true;

    }

    /**
     * Verifies if the attributes 'txtName' and 'txtUsername' of a user profile are
     * valid and not yet taken by another profile. The attribute 'txtUsername' is
     * optional and will be only verified if provided.
     * <p>
     * The txtName (userid) is validated against the property
     * security.userid.input.mode if defined.
     * 
     * @param profile - user profile to be validated
     * @return - true if name isn't still taken by another object and is in a valid
     *         format.
     */
    boolean isUserNameTaken(ItemCollection profile) {

        String sName = profile.getItemValueString("txtName");
        String sUserName = profile.getItemValueString("txtUserName");
        String sID = profile.getItemValueString("$uniqueid");

        if (!sUserName.equals(sUserName.trim())) {
            sUserName = sUserName.trim();
            profile.replaceItemValue("txtUserName", sUserName);
        }

        logger.fine("isUserNameTaken :" + sName);
        String sQuery;
        // username provided?
        if (sUserName != null && !"".equals(sUserName)) {
            sQuery = "(type:\"profile\" AND (txtname:\"" + sName + "\" OR txtusername:\"" + sUserName
                    + "\")) NOT $uniqueid:\"" + sID + "\"";
        } else {
            // query only txtName
            sQuery = "(type:\"profile\" AND txtname:\"" + sName + "\") NOT $uniqueid:\"" + sID + "\"";
        }

        Collection<ItemCollection> col;
        try {
            col = this.getWorkflowService().getDocumentService().find(sQuery, 1, 0);
        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

        return (col.size() > 0);

    }

    /**
     * Verifies if the txtEmail is still available.
     * 
     * The validation can be deactivated with the imixs.property
     * 'security.email.unique=false'
     * 
     * @param aprofile
     * @return - true if address isn't still taken by another profile or no email
     *         address is provided.
     */
    boolean isEmailTaken(ItemCollection profile) {

        // is the unique email mode activated?
        if (!"true".equalsIgnoreCase(sUniqueEmailMode.trim())) {
            // validation is deactivated
            return false;
        }

        String sEmail = profile.getItemValueString("txtEmail");
        String sID = profile.getItemValueString("$uniqueid");

        if (sEmail.isEmpty()) {
            return false;
        }

        // Trim email....
        if (!sEmail.equals(sEmail.trim())) {
            sEmail = sEmail.trim();
            profile.replaceItemValue("txtEmail", sEmail);
        }

        String sQuery;
        logger.fine("isEmailTaken :" + sEmail);
        // username provided?
        sQuery = "(type:\"profile\" AND txtemail:\"" + sEmail + "\") NOT $uniqueid:\"" + sID + "\"";
        Collection<ItemCollection> col;
        try {
            col = this.getWorkflowService().getDocumentService().find(sQuery, 1, 0);
        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

        return (col.size() > 0);

    }

}
