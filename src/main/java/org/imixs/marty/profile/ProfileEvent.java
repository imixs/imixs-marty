/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.marty.profile;

import org.imixs.workflow.ItemCollection;

/**
 * The ProfileEvent provides a CDI observer pattern. The ProfileEvent is fired
 * by the ProfileService EJB. An event Observer can react on a lookup or create
 * event.
 * 
 * 
 * The ProfileEvent defines the following event types:
 * <ul>
 * <li>ON_PROFILE_LOOKUP - send if a local lookup for a profile failed
 * <li>ON_PROFILE_CREATE - send immediately after a profile was created
 * </ul>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.marty.ejb.ProfileService
 */
public class ProfileEvent {

    public static final int ON_PROFILE_LOOKUP = 1;
    public static final int ON_PROFILE_CREATE = 2;
    public static final int ON_PROFILE_LOGIN = 3;

    private int eventType;
    private ItemCollection profile;
    private String userId = null;

    /**
     * Creates a profile event based on a existing Profile ItemCollection
     * 
     * @param userId    - userid
     * @param profile   - optional profile ItemCollection
     * @param eventType
     */
    public ProfileEvent(String userId, ItemCollection profile, int eventType) {
        this.eventType = eventType;
        this.profile = profile;
        this.userId = userId;
    }

    public int getEventType() {
        return eventType;
    }

    public ItemCollection getProfile() {
        return profile;
    }

    public void setProfile(ItemCollection profile) {
        this.profile = profile;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
