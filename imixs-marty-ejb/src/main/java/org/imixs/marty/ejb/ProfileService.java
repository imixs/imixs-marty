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
 *      http://www.imixs.org
 *      http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.ejb;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.PropertyService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The Marty ProfileService is a sigelton EJB providing user attributes like the
 * username and user email. The service is used to cache names application wide.
 * 
 * 
 * @author rsoika
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ProfileService {
    public final static int START_PROFILE_PROCESS_ID = 200;
    public final static int CREATE_PROFILE_ACTIVITY_ID = 5;

    final int DEFAULT_CACHE_SIZE = 100;

    final int MAX_SEARCH_COUNT = 1;
    private Cache cache;

    private static Logger logger = Logger.getLogger(ProfileService.class.getName());

    @EJB
    private DocumentService documentService;

    @EJB
    private PropertyService propertyService;

    @EJB
    protected WorkflowService workflowService;

    @Resource
    private SessionContext ctx;

    /**
     * PostContruct event - loads the imixs.properties.
     */
    @PostConstruct
    void init() {
        // initialize cache...
        reset();
    }

    /**
     * resets the profile cache..
     */
    public void reset() {
        // try to lookup cache size...
        int iCacheSize = DEFAULT_CACHE_SIZE;
        // early initialization did not work in Wildfly because of security
        // problem
        // see issue-#59
        // initialize cache
        cache = new Cache(iCacheSize);
    }

    /**
     * This method returns a profile by its id. The method uses an internal cache.
     * The method returns null if no Profile for this name was found
     * 
     * The returned workitem is a cloned version of the profile entity and can not
     * be processed or updated. Use lookupProfile to get the full entity of a
     * profile.
     * 
     * @param userid
     *            - the profile id
     * @return cloned workitem
     */
    public ItemCollection findProfileById(String userid) {
        return findProfileById(userid, false);

    }

    /**
     * This method returns a profile by its id. The method uses an internal cache.
     * The method returns in any case a user profile, even if no Profile for this
     * name was found. In this case a dummy profile will be created. The userid is
     * case sensitive.
     * <p>
     * The returned workitem is a cloned version of the profile entity and can not
     * be processed or updated. Use lookupProfile to get the full entity of a
     * profile.
     * <p>
     * If the boolean 'refresh' is true the method lookup the user in any case with
     * a search query and updates the cache.
     * 
     * @param userid
     *            - the profile id
     * @param refresh
     *            - boolean indicates if the internal cache should be used
     * @return cloned workitem
     */
    public ItemCollection findProfileById(String userid, boolean refresh) {

        if (userid == null || userid.isEmpty()) {
            return null;
        }

        // try to get name out from cache
        ItemCollection userProfile = null;

        // use cache?
        if (!refresh) {
            logger.finest("......lookup profile '" + userid + "' from cache...");
            Map<String, List<Object>> profileMap = cache.get(userid);
            if (profileMap != null) {
                userProfile = new ItemCollection(profileMap);
                logger.finest("......return profile '" + userid + "' from cache");
                return userProfile;
            }
        }
        // no profile found or refresh==true
        userProfile = lookupProfileById(userid);
        if (userProfile != null) {
            // clone workitem....
            userProfile = cloneWorkitem(userProfile);
        } else {
            logger.finest("......profile '" + userid + "' not found, create a 'default' profile...");
            // put dummy entry into cache to avoid next lookup!
            userProfile = new ItemCollection();
            userProfile.replaceItemValue("txtname", userid);
            userProfile.replaceItemValue("txtusername", userid);
        }

        // cache profile
        logger.finest("......put profile '" + userid + "' into cache");
        cache.put(userid, userProfile.getAllItems());

        return userProfile;

    }

    /**
     * This method returns a profile by its id. In different to the findProfileById
     * method this method lookups the profile and returns the full entity. The
     * returned workItem can be processed. THe userid is case sensitive.
     * 
     * Use findProfileById to work with the internal cache if there is no need to
     * update the profile.
     * 
     * @param userid
     *            - the profile id
     * @return profile workitem
     */
    public ItemCollection lookupProfileById(String userid) {

        if (userid == null || userid.isEmpty()) {
            logger.warning("lookupProfileById - no id provided!");
            return null;
        }

        // try to get name out from cache
        ItemCollection userProfile = null;

        logger.finest("......lookupProfileById '" + userid + "'");
        // lookup user profile....
        String sQuery = "(type:\"profile\" AND txtname:\"" + userid + "\")";
        logger.finest("......search: " + sQuery);

        Collection<ItemCollection> col;
        try {
            col = documentService.find(sQuery, 1, 0);
        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

        if (col.size() > 0) {
            userProfile = col.iterator().next();
        } else {
            logger.finest("......lookup profile '" + userid + "' failed");
        }
        return userProfile;
    }

    /**
     * This method removes a profile from the cache.
     * 
     * @param userid
     */
    public void discardCache(String userid) {
        cache.remove(userid);
    }

    /**
     * This method closes a profile entity and computes the attributes txtUsername
     * and txtInitials
     * 
     * @param aWorkitem
     * @return
     */
    public static ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
        ItemCollection clone = new ItemCollection();

        // clone the standard WorkItem properties
        clone.replaceItemValue("Type", aWorkitem.getItemValue("Type"));
        clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
        clone.replaceItemValue("$ModelVersion", aWorkitem.getItemValue("$ModelVersion"));
        clone.replaceItemValue("$ProcessID", aWorkitem.getItemValue("$ProcessID"));
        clone.replaceItemValue("$Created", aWorkitem.getItemValue("$Created"));
        clone.replaceItemValue("$Modified", aWorkitem.getItemValue("$Modified"));
        clone.replaceItemValue("$isAuthor", aWorkitem.getItemValue("$isAuthor"));

        clone.replaceItemValue("txtWorkflowStatus", aWorkitem.getItemValue("txtWorkflowStatus"));
        clone.replaceItemValue("txtWorkflowSummary", aWorkitem.getItemValue("txtWorkflowSummary"));
        clone.replaceItemValue("txtWorkflowAbstract", aWorkitem.getItemValue("txtWorkflowAbstract"));
        clone.replaceItemValue("txtEmail", aWorkitem.getItemValue("txtEmail"));
        clone.replaceItemValue("namdeputy", aWorkitem.getItemValue("namdeputy"));
        clone.replaceItemValue("txtusericon", aWorkitem.getItemValue("txtusericon"));

        // get accountName
        String sAccountName = aWorkitem.getItemValueString("txtName");
        clone.replaceItemValue("txtName", sAccountName);

        // test txtuserName
        String sUserName = aWorkitem.getItemValueString("txtUserName");
        if (!sUserName.isEmpty()) {
            clone.replaceItemValue("txtUserName", sUserName);
        } else {
            // use account name
            clone.replaceItemValue("txtUserName", sAccountName);
        }

        // construct initials (2 digits)
        String sInitials = aWorkitem.getItemValueString("txtinitials");
        if (sInitials.isEmpty()) {
            // default
            sInitials = "NO";
            if (!sUserName.isEmpty() && sUserName.length() > 2) {
                int iPos = sUserName.indexOf(' ');
                if (iPos > -1) {
                    sInitials = sUserName.substring(0, 1);
                    sInitials = sInitials + sUserName.substring(iPos + 1, iPos + 2);
                } else {
                    sInitials = sUserName.substring(0, 2);
                }
            } else {
                sInitials = sAccountName;
            }
            clone.replaceItemValue("txtinitials", sInitials);
        } else {
            clone.replaceItemValue("txtinitials", sInitials);
        }
        return clone;
    }

    /**
     * Creates a new profile document
     * 
     * @param userid
     * @return
     * @throws ModelException
     * @throws PluginException
     * @throws ProcessingErrorException
     * @throws AccessDeniedException
     */
    public ItemCollection createProfile(String userid, String locale)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        logger.info("create new profile for userid '" + userid + "'.... ");
        // create new Profile for current user
        ItemCollection profile = new ItemCollection();
        profile.replaceItemValue("type", "profile");
        profile.replaceItemValue("$processID", START_PROFILE_PROCESS_ID);
        // get system model version from imixs.properties
        String modelVersion = this.propertyService.getProperties().getProperty("system.model.version", "");
        profile.replaceItemValue("$modelversion", modelVersion);
        // the workflow group can not be guessed here...
        // profile.replaceItemValue("$workflowgroup", "Profil");
        profile.replaceItemValue("txtName", userid);
        profile.replaceItemValue("txtLocale", locale);
        // set default group
        profile.replaceItemValue("txtgroups", "IMIXS-WORKFLOW-Author");
        // process new profile...
        profile.setEventID(CREATE_PROFILE_ACTIVITY_ID);

        profile = workflowService.processWorkItem(profile);

        logger.finest("......new profile created for userid '" + userid + "'");

        return profile;
    }

    /**
     * Cache implementation to hold config entities
     * 
     * @author rsoika
     * 
     */
    class Cache extends ConcurrentHashMap<String, Map<String, List<Object>>> implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int capacity;

        public Cache(int capacity) {
            super(capacity + 1, 1.1f);
            this.capacity = capacity;
        }

        protected boolean removeEldestEntry(Entry<String, ItemCollection> eldest) {
            return size() > capacity;
        }
    }

}
