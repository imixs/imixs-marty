/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *******************************************************************************/
package org.imixs.marty.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * user Group service to provide method for managing user and groups settings.
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class UserGroupService {

    public static String ACCESSLEVEL_NOACCESS = "org.imixs.ACCESSLEVEL.NOACCESS";

    public static String DEFAULT_ACCOUNT = "admin";
    public static String DEFAULT_PASSWORD = "adminadmin";

    public static final String[] CORE_GROUPS = { "org.imixs.ACCESSLEVEL.MANAGERACCESS",
            "org.imixs.ACCESSLEVEL.EDITORACCESS", "org.imixs.ACCESSLEVEL.AUTHORACCESS",
            "org.imixs.ACCESSLEVEL.READERACCESS" };
    public static final String[] DEPRECATED_CORE_GROUPS = { "IMIXS-WORKFLOW-Manager", "IMIXS-WORKFLOW-Editor",
            "IMIXS-WORKFLOW-Author", "IMIXS-WORKFLOW-Reader" };

    @PersistenceContext(unitName = "org.imixs.workflow.jpa")
    private EntityManager manager;

    @Resource
    SessionContext ctx;

    @EJB
    DocumentService documentService;

    @Inject
    @ConfigProperty(name = "security.userid.input.mode", defaultValue = "LOWERCASE")
    String userInputMode;

    private static Logger logger = Logger.getLogger(UserGroupService.class.getName());

    /**
     * This method verifies the profile data and creates or update the corresponding
     * user entries in the user tables.
     * <p>
     * NOTE: this method did not change a userid. To do this use the method
     * changeUser!
     * <p>
     * The Method also verifies deprecated role names, fix it and prints out a
     * warning in such a case.
     * <p>
     * If a new userId entity is generated but no password is provided, the method
     * generates an encrypted random password
     * 
     * @param profile
     */
    @SuppressWarnings("unchecked")
    public void updateUser(ItemCollection profile) {
        boolean debug = logger.isLoggable(Level.FINE);

        String sType = profile.getItemValueString("Type");
        if (!("profile".equals(sType)))
            return;

        String sID = profile.getItemValueString("txtName");
        String sPassword = profile.getItemValueString("txtPassword");
        Collection<String> groups = profile.getItemValue("txtGroups");

        UserId user = null;
        user = manager.find(UserId.class, sID);
        if (user == null) {
            user = new UserId(sID, Crypter.crypt(WorkflowKernel.generateUniqueID())); // generate default random
                                                                                      // password
            manager.persist(user);
        }

        // encrypt and update password
        if (sPassword != null && !"".equals(sPassword)) {
            String sEncryptedPasswort = Crypter.crypt(sPassword);
            user.setPassword(sEncryptedPasswort);
            // remove password....
            profile.removeItem("txtPassword");
            logger.info("password change for userid '" + sID + "' by '" + ctx.getCallerPrincipal().getName() + "'");
            profile.replaceItemValue("txtpasswordhash", sEncryptedPasswort);
        }

        // find group relation ships
        Set<UserGroup> groupList = new HashSet<UserGroup>();
        for (String aGroup : groups) {
            // we do not except empty groupnames here!
            if (aGroup != null && !aGroup.isEmpty()) {
                UserGroup group = manager.find(UserGroup.class, aGroup);
                // if group dos not exist - create it...
                if (group == null) {
                    group = new UserGroup(aGroup);
                    manager.persist(group);
                }
                groupList.add(group);
            }
        }

        // if grouplist is empty we set the role 'org.imixs.ACCESSLEVEL.NOACCESS'
        if (groupList.size() == 0) {
            // verify if no access exists...
            UserGroup noAccessGroup = manager.find(UserGroup.class, ACCESSLEVEL_NOACCESS);
            // if group dos not exist - create it...
            if (noAccessGroup == null) {
                noAccessGroup = new UserGroup(ACCESSLEVEL_NOACCESS);
                manager.persist(noAccessGroup);
            }
            groupList.add(noAccessGroup);
        }
        // update groups
        user.setUserGroups(groupList);

        // log debug messages
        if (debug) {
            logger.fine("...update '" + sID + "'  Groups: ");
            groups.forEach(n -> logger.fine("...       '" + n + "'"));
        }
    }

    /**
     * This method changes the userID of an existing user entry and updates the
     * userGroup table entries.
     * 
     * @param oldID - the existing userEntry
     * @param newID - the name of the new id
     */
    public void changeUserId(String oldID, String newID) {
        UserId user = null;

        // test if new userid still exits
        user = manager.find(UserId.class, newID);
        if (user != null) {
            logger.warning("changeUser - new userId '" + newID + "'is still in Use!");
            return;
        }

        // find old user entry....
        user = manager.find(UserId.class, oldID);
        if (user == null) {
            logger.warning("changeUser - UserID '" + oldID + "' not found!");
            return;
        }

        // change id
        UserId newUser = new UserId(newID);
        newUser.setPassword(user.getPassword());
        newUser.setUserGroups(user.getUserGroups());
        manager.persist(newUser);

        // remove old
        manager.remove(user);

        // log
        logger.info("changeUserId '" + oldID + "' to '" + newID + "' by '" + ctx.getCallerPrincipal().getName());

    }

    /**
     * This method deletes the userID of an existing user entry and also the
     * userGroup table entries.
     * 
     * @param userID - the existing userEntry
     */
    public void removeUserId(String userID) {
        UserId user = null;

        // test if userid exits
        user = manager.find(UserId.class, userID);
        if (user == null) {
            logger.warning("removeUserId - userId '" + userID + "' did not exist!");
            return;
        }

        // remove old
        manager.remove(user);

        // log
        logger.info("removeUserId '" + userID + "' by '" + ctx.getCallerPrincipal().getName());
    }

    /**
     * This method verifies if a default user id already exists. If no userID exists
     * the method generates a default account 'admin' with password 'adminadmin'
     * 
     * @throws AccessDeniedException
     */
    @SuppressWarnings("unchecked")
    public void initUserIDs() {
        logger.finest("......init UserIDs...");

        // verfiy existing profiles
        verifyExistingProfileData();

        // migrate deprecated user roles
        // Issue #373
        migrateDeprecatedUserRoles();

        // create default admin account if missing
        String sAdminAccount = DEFAULT_ACCOUNT;

        if ("uppercase".equalsIgnoreCase(userInputMode)) {
            sAdminAccount = sAdminAccount.toUpperCase();
        }

        String sQuery = "SELECT user FROM UserId AS user WHERE user.id='" + sAdminAccount + "'";

        Query q = manager.createQuery(sQuery);
        q.setFirstResult(0);
        q.setMaxResults(1);

        Collection<UserId> entityList = q.getResultList();

        if (entityList == null || entityList.size() == 0) {
            logger.info("Create default admin account...");
            // create a default account
            ItemCollection profile = new ItemCollection();
            profile.replaceItemValue("type", "profile");
            profile.replaceItemValue("txtName", sAdminAccount);
            // set default password
            profile.replaceItemValue("txtPassword", DEFAULT_PASSWORD);
            profile.replaceItemValue("$WorkflowGroup", "Profile");
            profile.replaceItemValue("txtGroups", "IMIXS-WORKFLOW-Manager"); // deprecated
            profile.appendItemValue("txtGroups", "org.imixs.ACCESSLEVEL.MANAGERACCESS");

            // hard coded version nummer!
            profile.replaceItemValue("$modelversion", "system-de-0.0.1");
            profile.replaceItemValue("$workflowgroup", "Profil");
            profile.replaceItemValue("$processid", 210);

            try {
                updateUser(profile);
                documentService.save(profile);
            } catch (AccessDeniedException e) {
                logger.warning("UserGroupService - unable to initialize default admin account");
                logger.severe(e.getMessage());
                // throw new RuntimeException(e);
                return;
            }

        }

    }

    /**
     * This method migrates deprecated user roles
     * <p>
     * <ul>
     * <li>IMIXS-WORKFLOW-Manager => org.imixs.ACCESSLEVEL.MANAGERACCESS
     * <li>IMIXS-WORKFLOW-Editor => org.imixs.ACCESSLEVEL.EDITORACCESS
     * <li>IMIXS-WORKFLOW-Author => org.imixs.ACCESSLEVEL.AUTHORACCESS
     * <li>IMIXS-WORKFLOW-Reader => org.imixs.ACCESSLEVEL.READERACCESS
     * <p>
     * First the method tests if a migration is necessary. For that the method tests
     * if the new roles already are existing in the system. Only if not the
     * migration is started
     * <p>
     * This method can be removed in later versions (but it may need some time)
     */
    public void migrateDeprecatedUserRoles() {

        boolean needMigration = false;

        // test existence of roles
        for (String aGroup : CORE_GROUPS) {
            UserGroup group = manager.find(UserGroup.class, aGroup);
            // if group dos not exist migration is necessary...
            if (group == null) {
                needMigration = true;
                // don't waste time
                break;
            }
        }

        if (!needMigration) {
            // no migration needed
            logger.info("...Imixs core userGroups OK...");
            return;
        }

        // start migration
        logger.info("*************************************************");
        logger.info("...System contains deprecated userGroups!");
        logger.info("...migration to new Imixs core user groups starting....");
        logger.info("*************************************************");
        // first create missing core user groups
        for (String aGroup : CORE_GROUPS) {
            UserGroup group = manager.find(UserGroup.class, aGroup);
            if (group == null) {
                group = new UserGroup(aGroup);
                manager.persist(group);
            }
        }
        // migrate existing user profiles and userGroup objects...
        migrateExistingProfileData();

        logger.info("*************************************************");
        logger.info("...migration to new Imixs core user groups finished successful.");
        logger.info("*************************************************");

    }

    /**
     * This method is called by the initUserIDs method.
     * <p>
     * The method tries to update the userData based on existing profiles data and
     * restore groups and encrypted passwords.
     * 
     */
    @SuppressWarnings("unchecked")
    private void verifyExistingProfileData() {
        logger.info("Verify existing profile data...");
        List<ItemCollection> profiles = documentService.getDocumentsByType("profile");
        for (ItemCollection profile : profiles) {

            String id = profile.getItemValueString("txtname");

            // try to update data.....
            UserId user = null;
            user = manager.find(UserId.class, id);
            if (user != null) {
                // user object exits
                continue;
            }

            // userobject does not exist, so we restore the user object based on the
            // existing profile data if the profile contains a password hash....
            String passwordhash = profile.getItemValueString("txtpasswordhash");
            if (!passwordhash.isEmpty()) {
                logger.info("...restore userid '" + id + "' from existing profile data...");
                user = new UserId(id);
                user.setPassword(passwordhash);
                manager.persist(user);

                // find group relation ships
                Collection<String> groups = profile.getItemValue("txtGroups");
                Set<UserGroup> groupList = new HashSet<UserGroup>();
                for (String aGroup : groups) {
                    // we do not except empty groupnames here!
                    if (aGroup != null && !aGroup.isEmpty()) {
                        UserGroup group = manager.find(UserGroup.class, aGroup);
                        // if group dos not exist - create it...
                        if (group == null) {
                            group = new UserGroup(aGroup);
                            manager.persist(group);
                        }
                        groupList.add(group);
                    }
                }

                // if grouplist is empty we set the role 'org.imixs.ACCESSLEVEL.NOACCESS'
                if (groupList.size() == 0) {
                    // verify if no access exists...
                    UserGroup noAccessGroup = manager.find(UserGroup.class, ACCESSLEVEL_NOACCESS);
                    // if group dos not exist - create it...
                    if (noAccessGroup == null) {
                        noAccessGroup = new UserGroup(ACCESSLEVEL_NOACCESS);
                        manager.persist(noAccessGroup);
                    }
                    groupList.add(noAccessGroup);
                }
                // update groups
                user.setUserGroups(groupList);
            }
        }

    }

    /**
     * This method is called by the initUserIDs method.
     * <p>
     * The method looks for deprecated roles in the existing profiles data and
     * migrates groups if necessary.
     * 
     */
    @SuppressWarnings("unchecked")
    private void migrateExistingProfileData() {
        int count = 0;
        List<String> deprecatedCoreGrouplist = Arrays.asList(DEPRECATED_CORE_GROUPS);
        logger.info("migrate deprecated profile data...");
        List<ItemCollection> profiles = documentService.getDocumentsByType("profile");
        for (ItemCollection profile : profiles) {

            String id = profile.getItemValueString("txtname");
            UserId user = null;
            user = manager.find(UserId.class, id);
            if (user != null) {
                logger.info("...migate deprecated userroles for '" + id + "' ...");

                // find group relation ships
                List<String> groupNames = profile.getItemValue("txtGroups");
                // check if we have deprecated roles
                List<String> newGroupNames = new ArrayList<String>();
                newGroupNames.addAll(groupNames);
                for (String aGroup : groupNames) {
                    if (deprecatedCoreGrouplist.contains(aGroup) && !groupNames.contains(getCoreGroupName(aGroup))) {
                        String newGroup = getCoreGroupName(aGroup);
                        logger.info("..." + id + " contains depreacted userrole " + aGroup);
                        logger.info("... Group will be automatically migrated to " + newGroup);
                        newGroupNames.add(newGroup);
                    }

                }
                // update group object in any case!
                logger.info("...Updating UserGroup objects....");
                Set<UserGroup> groupList = new HashSet<UserGroup>();
                for (String aGroup : newGroupNames) {
                    // we do not except empty groupnames here!
                    if (aGroup != null && !aGroup.isEmpty()) {
                        UserGroup group = manager.find(UserGroup.class, aGroup);
                        // if group dos not exist - create it...
                        if (group == null) {
                            group = new UserGroup(aGroup);
                            manager.persist(group);
                        }
                        groupList.add(group);
                    }
                }
                user.setUserGroups(groupList);

                // update also profile?
                if (newGroupNames.size() != groupNames.size()) {
                    profile.setItemValue("txtGroups", newGroupNames);
                    documentService.save(profile);
                    count++;
                }
            }
        }
        logger.info("... " + count + " user profiles updated....");
    }

    /**
     * Returns the deprecated group name for a given core group name
     * <ul>
     * <li>IMIXS-WORKFLOW-Manager => org.imixs.ACCESSLEVEL.MANAGERACCESS
     * <li>IMIXS-WORKFLOW-Editor => org.imixs.ACCESSLEVEL.EDITORACCESS
     * <li>IMIXS-WORKFLOW-Author => org.imixs.ACCESSLEVEL.AUTHORACCESS
     * <li>IMIXS-WORKFLOW-Reader => org.imixs.ACCESSLEVEL.READERACCESS
     * <p>
     * 
     * @param newGroupName
     * @return
     */
    public String getDeprecatedGroupName(String newGroupName) {
        List<String> grouplist = Arrays.asList(CORE_GROUPS);
        int pos = grouplist.indexOf(newGroupName);
        if (pos >= 0) {
            return DEPRECATED_CORE_GROUPS[pos];
        }
        return null;
    }

    /**
     * Returns the core group name for a given deprecated group name
     * 
     * @param deprecatedGroupName
     * @return
     */
    public String getCoreGroupName(String deprecatedGroupName) {
        List<String> grouplist = Arrays.asList(DEPRECATED_CORE_GROUPS);
        int pos = grouplist.indexOf(deprecatedGroupName);
        if (pos >= 0) {
            return CORE_GROUPS[pos];
        }
        return null;
    }
}
