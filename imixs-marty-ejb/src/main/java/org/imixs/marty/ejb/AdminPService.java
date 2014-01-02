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
package org.imixs.marty.ejb;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * The AmdinPService provides methods to replace entries in the fields
 * $WriteAccess, $ReadAccess and namOwner. An update request is stored in a
 * adminp entity containing alll necessary informations. The service starts a
 * timer instances for each update process
 * 
 * The result of a adminp process are documented into an log entity from
 * type='adminp'
 * 
 * 
 * JQPL Statement to quey workitems:
 * 
 * <code>
   SELECT workitem FROM Entity AS workitem
   	 LEFT JOIN workitem.writeAccessList as wa
   	 LEFT JOIN workitem.readAccessList as ra
	      JOIN workitem.textItems AS n1
	  WHERE  workitem.type IN ('workitem', 'workitemarchive', 'childworkitem', 'childworkitemarchive','workitemlob')
	  AND ( ( wa.value = 'rsoika') 
	  	OR	( ra.value = 'rsoika') 
	  	OR	( n1.itemName='namowner' AND n1.itemValue='rsoika')
	  	OR	( n1.itemName='namcreator' AND n1.itemValue='rsoika')
	  	)
	  ORDER BY workitem.created asc
	  
	 
	</code>
 * 
 * 
 * @see AdminPController
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
//@Local
@LocalBean
public class AdminPService {

	@Resource
	SessionContext ctx;

	@Resource
	javax.ejb.TimerService timerService;

	@EJB
	WorkflowService workflowService;

	@EJB
	EntityService entityService;

	private String lastUnqiueID = null;
	private static int MAX_COUNT = 1000;
	private static Logger logger = Logger.getLogger(AdminPService.class
			.getSimpleName());

	/**
	 * This method starts a new replacement.
	 * 
	 * @throws AccessDeniedException
	 */
	public ItemCollection start(String fromName, String toName, boolean replace)
			throws AccessDeniedException {

		logger.info("[AdminP] started:");
		logger.info("[AdminP]   fromName=" + fromName);
		logger.info("[AdminP]   toName=" + toName);

		// create new adminp document
		ItemCollection adminp = new ItemCollection();

		if (fromName == null || "".equals(fromName)) {
			logger.info("[AdminP] wrong configuration!");
			return adminp;
		}

		adminp.replaceItemValue("type", "adminp");
		adminp.replaceItemValue("namFrom", fromName);
		adminp.replaceItemValue("namTo", toName);
		adminp.replaceItemValue("numProcessed", 0);
		adminp.replaceItemValue("txtworkflowStatus", "New");
		adminp.replaceItemValue("keyReplace", replace);

		adminp.replaceItemValue("numMaxCount", MAX_COUNT);
		adminp.replaceItemValue("numLastCount", 0);

		// Select Statement

		String sQuery = "";
		sQuery = "SELECT workitem FROM Entity AS workitem"
				+ "	 LEFT JOIN workitem.writeAccessList as wa"
				+ "  LEFT JOIN workitem.readAccessList as ra"
				+ "  JOIN workitem.textItems AS n1"
				+ "	 WHERE  workitem.type IN ('workitem', 'workitemarchive', 'childworkitem', 'childworkitemarchive','workitemlob')"
				+ " AND ( ( wa.value = '" + fromName + "')"
				+ "	  	 OR	( ra.value = '" + fromName + "')"
				+ "		 OR	( n1.itemName='namowner' AND n1.itemValue='"
				+ fromName + "')"
				+ "		 OR	( n1.itemName='namcreator' AND n1.itemValue='"
				+ fromName + "'))  ORDER BY workitem.created asc";

		adminp.replaceItemValue("txtQuery", sQuery);

		// save it...
	//	entityService.save(adminp);

		// start timer
		startTimer(adminp);
		return adminp;

	}

	/**
	 * This is the method which processes the timeout event depending on the
	 * running timer settings.
	 * 
	 * The method updates all affected workitems.
	 * 
	 * 
	 * 
	 * 
	 * @param timer
	 */
	@Timeout
	public void scheduleTimer(javax.ejb.Timer timer) {
		String sTimerID = null;

		// Startzeit ermitteln
		long lProfiler = System.currentTimeMillis();
		sTimerID = timer.getInfo().toString();
		// load adminp configuration from database
		ItemCollection adminp = entityService.load(sTimerID);
		try {

			// verify if admin entity still exists
			if (adminp == null) {
				// configuration was removed - so stop the timer!
				logger.info("[AdminPService]  Process " + sTimerID
						+ " was removed - timer will be canceled");
				// stop timer
				timer.cancel();
				// go out!
				return;
			}

			logger.info("[AdminPService]  Processing : " + sTimerID);
			adminp = updateWorkitems(adminp);

			// if numLastCount<numMacCount then we can stop the timer
			int iMax = adminp.getItemValueInteger("numMaxCount");
			int iLast = adminp.getItemValueInteger("numLastCount");
			if (iLast < iMax) {
				timer.cancel();
				adminp.replaceItemValue("txtworkflowStatus", "Completed");
				logger.info("[AdminPService]  all updates completed - timer stopped");

			}
			adminp.replaceItemValue("errormessage", "");
			adminp = entityService.save(adminp);

		} catch (Exception e) {
			e.printStackTrace();
			// stop timer!
			timer.cancel();
			logger.severe("[AdminPService] Timeout sevice stopped: " + sTimerID);
			logger.severe("[AdminPService] Last Workitem: " + lastUnqiueID);
			try {
				adminp.replaceItemValue("txtworkflowStatus", "Error");
				adminp.replaceItemValue("errormessage", e.toString());
				adminp = entityService.save(adminp);
			} catch (Exception e2) {
				e2.printStackTrace();

			}

		}

		logger.fine("[AdminPService] timer call finished successfull after "
				+ ((System.currentTimeMillis()) - lProfiler) + " ms");

	}

	/**
	 * Thisi Method runns the query and updates affected workitems The current
	 * startpos and maxcount are stored in the configuration entity in the
	 * properties 'numStart' 'numMaxCount'
	 * 
	 * @param adminp
	 * @throws AccessDeniedException
	 */
	private ItemCollection updateWorkitems(ItemCollection adminp)
			throws AccessDeniedException {

		// get Query
		String sQuery = adminp.getItemValueString("txtQuery");
		int iStart = adminp.getItemValueInteger("numStart");
		int iCount = adminp.getItemValueInteger("numMaxCount");
		int iUpdates = adminp.getItemValueInteger("numUpdates");
		int iProcessed = adminp.getItemValueInteger("numProcessed");
		String from = adminp.getItemValueString("namFrom");
		String to = adminp.getItemValueString("namTo");
		boolean replace = adminp.getItemValueBoolean("keyReplace");

		adminp.replaceItemValue("txtworkflowStatus", "Processing");
		// save it...
	//	entityService.save(adminp);

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				iStart, iCount);

		// check all selected documents
		for (ItemCollection entity : col) {
			iProcessed++;
			if (updateEntity(entity, from, to, replace,
					adminp.getItemValueString(EntityService.UNIQUEID))) {
				// inc counter
				iUpdates++;
			}
		}

		// adjust start pos and update count
		adminp.replaceItemValue("numUpdates", iUpdates);
		adminp.replaceItemValue("numProcessed", iProcessed);

		adminp.replaceItemValue("numLastCount", col.size());
		iStart = iStart + col.size();
		adminp.replaceItemValue("numStart", iStart);

		logger.info("[AdminP] " + col.size() + " workitems processed");

		return adminp;

	}

	/**
	 * Updates read,write and owner of a entity and returns true if an update
	 * was necessary
	 * 
	 * @param entity
	 * @param from
	 * @param to
	 * @param replace
	 * @return true if the entiy was modified.
	 * @throws AccessDeniedException
	 */
	private boolean updateEntity(ItemCollection entity, String from, String to,
			boolean replace, String adminpUniqueid)
			throws AccessDeniedException {

		boolean bUpdate = false;
		if (entity == null)
			return false;

		lastUnqiueID = entity.getItemValueString(EntityService.UNIQUEID);

		// log current values
		entity.replaceItemValue("txtAdminP", "AdminP:" + adminpUniqueid + " ");

		// Verify Fields
		logOldValues(entity, "$ReadAccess");
		if (updateList(entity.getItemValue("$ReadAccess"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "$WriteAccess");
		if (updateList(entity.getItemValue("$WriteAccess"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "namOwner");
		if (updateList(entity.getItemValue("namOwner"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "namCreator");
		if (updateList(entity.getItemValue("namCreator"), from, to, replace))
			bUpdate = true;

		if (bUpdate) {
			entityService.save(entity);
			logger.fine("[AmdinP] updated: "
					+ entity.getItemValueString(EntityService.UNIQUEID));
		}
		return bUpdate;
	}

	/**
	 * Update the values of a single list.
	 * 
	 * @param list
	 * @param from
	 * @param to
	 * @param replace
	 * @return true if the list was modified.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean updateList(List list, String from, String to,
			boolean replace) {

		boolean update = false;

		if (list == null || list.isEmpty())
			return false;

		if (list.contains(from)) {

			if (to != null && !"".equals(to) && !list.contains(to)) {
				list.add(to);
				update = true;
			}

			if (replace) {
				while (list.contains(from)) {
					list.remove(from);
					update = true;
				}
			}

		}

		return update;
	}

	private void logOldValues(ItemCollection entity, String field) {

		// update log
		String log = entity.getItemValueString("txtAdminP");
		log = log + " " + field + "=";
		List list = entity.getItemValue(field);
		for (Object o : list) {
			log = log + o.toString() + ",";
		}
		entity.replaceItemValue("txtAdminP", log);

	}

	/**
	 * This Method starts a new TimerService.
	 * 
	 * The method loads configuration from a ItemCollection (timerdescription)
	 * with the following informations:
	 * 
	 * datstart - Date Object
	 * 
	 * datstop - Date Object
	 * 
	 * numInterval - Integer Object (interval in seconds)
	 * 
	 * id - String - unique identifier for the schedule Service.
	 * 
	 * The param 'id' should contain a unique identifier (e.g. the EJB Name) as
	 * only one scheduled Workflow should run inside a WorkflowInstance. If a
	 * timer with the id is already running the method stops this timer object
	 * first and reschedules the timer.
	 * 
	 * The method throws an exception if the timerdescription contains invalid
	 * attributes or values.
	 * 
	 * @throws AccessDeniedException
	 */
	private ItemCollection startTimer(ItemCollection adminp)
			throws AccessDeniedException {

		String id = adminp.getItemValueString("$uniqueid");

		// startdatum und enddatum manuell festlegen
		Calendar cal = Calendar.getInstance();
		Date startDate = cal.getTime();
		cal.add(Calendar.YEAR, 10);
		adminp.replaceItemValue("datstart", startDate);

		long interval = 60 * 1000;

		adminp.replaceItemValue("numInterval", new Long(interval));
		// try to cancel an existing timer for this workflowinstance
		Timer timer = this.findTimer(id);
		if (timer != null)
			timer.cancel();

		adminp.replaceItemValue("txtTimerStatus", "Running");

		adminp = entityService.save(adminp);
		// start timer...
		timer = timerService.createTimer(startDate, interval,
				adminp.getItemValueString(EntityService.UNIQUEID));

		logger.info("[AdminPService] Timer Service started - ID=" + id);
		return adminp;
	}

	/**
	 * This method returns a timer for a corresponding id if such a timer object
	 * exists.
	 * 
	 * @param id
	 * @return Timer
	 * @throws Exception
	 */
	private Timer findTimer(String id) {
		if (id==null || id.isEmpty())
			return null;
		
		for (Object obj : timerService.getTimers()) {
			Timer timer = (javax.ejb.Timer) obj;
			if (timer.getInfo() instanceof String) {
				String timerid = timer.getInfo().toString();
				if (id.equals(timerid)) {
					return timer;
				}
			}
		}
		return null;
	}
}
