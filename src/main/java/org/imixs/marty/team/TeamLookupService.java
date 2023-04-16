package org.imixs.marty.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.UserGroupEvent;

/**
 * This EJB provides a lookup service for orgunit member information.
 * 
 * The orgunits are stored in a array of strings with the following format
 * 
 * <p>
 * {process:PROCESS_NAME:ROLE} <br />
 * {space:PROCESS_NAME:ROLE}<br />
 * e.g. <br />
 * {process:Finance:assist} <br />
 * <br />
 * In addition the method adds a role member if one of the roles (team,manager,
 * assist) was found <br />
 * {process:Finance:member}
 * 
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
@LocalBean
public class TeamLookupService {

	@Resource
	SessionContext ctx;

	@EJB
	TeamCache teamCache;

	@EJB
	private DocumentService documentService;

	private static Logger logger = Logger.getLogger(TeamLookupService.class.getName());

	/**
	 * Returns a string array containing all orgunit names a given userID is member
	 * of.
	 * 
	 * 
	 * @param aUID
	 *            - user unique id
	 * @return string array of group names
	 */
	public String[] findOrgunits(String userId) {
		// test cache...
		String[] groups = (String[]) teamCache.get(userId);
		if (groups != null) {
			return groups;
		}

		// build an array with all member strings...
		List<String> memberList = getMemberList("process", userId);
		memberList.addAll(getMemberList("space", userId));
		// convert list to array...
		groups = new String[memberList.size()];
		groups = memberList.toArray(groups);
		// add to cache
		teamCache.put(userId, groups);
		// logging
		if (logger.isLoggable(java.util.logging.Level.FINE)) {
			String groupListe = "";
			for (String aGroup : groups)
				groupListe += "'" + aGroup + "' ";
			logger.finest("......resolved membership for '" + userId + " = " + groupListe);
		}

		return groups;

	}

	/**
	 * This method updates the UserGroup List be reaction on the CDI event
	 * UserGroupEvent. The method uses an internal caching mechanism to avoid
	 * multiple database lookups.
	 */
	public void onUserGroupEvent(@Observes UserGroupEvent userGroupEvent) {

		long l = System.currentTimeMillis();
		// avoid recursive call form getDocumetnsByType...
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

		for (StackTraceElement caller : stackTraceElements) {
			String classname = caller.getClassName();
			String methodName = caller.getMethodName();
			if (classname.equals(TeamLookupService.class.getName()) && "findOrgunits".equals(methodName)) {
				logger.finest("......found recursion from findOrgunits...");
				return;
			}
		}

		String[] groups = findOrgunits(userGroupEvent.getUserId());
		userGroupEvent.setGroups(Arrays.asList(groups));
		logger.finest("......finished in " + (System.currentTimeMillis() - l) + "ms");

	}

	/**
	 * This method reset the intern group cache in case a space or process entity is
	 * updated or deleted.
	 * 
	 * @see DocumentEvent
	 * @param documentEvent
	 */
	public void onDocumentEvent(@Observes DocumentEvent documentEvent) {
		if (documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_SAVE
				|| documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_DELETE) {
			String type = documentEvent.getDocument().getType();
			// test document type
			if (type.startsWith("space") || type.startsWith("process")) {
				logger.finest("......reset teamCache");
				teamCache.resetCache();
			}
		}
	}

	/**
	 * This method returns a list with all process roles a given user is member of
	 * <p>
	 * {process:PROCESS_NAME:ROLE}
	 * 
	 * e.g.
	 * 
	 * {process:Finance:assist}
	 * 
	 * In addition the method adds a role member if one of the roles (team,manager,
	 * assist) was found
	 * 
	 * {process:Finance:member}
	 * 
	 * 
	 * @param type
	 *            - indicates the orgunit type (process or space)
	 * @param userId
	 *            - current userid
	 * 
	 * @return list of roles the user is member of
	 */
	@SuppressWarnings("unchecked")
	List<String> getMemberList(String type, String userId) {
		boolean isGeneralManager = false;
		boolean isGeneralTeam = false;
		boolean isGeneralAssist = false;
		boolean isGeneralMember = false;

		List<String> memberList = new ArrayList<String>();

		Collection<ItemCollection> col = documentService.getDocumentsByType(type);
		logger.fine(col.size() + " orgunits '" + type + "' found...");
		// create optimized list
		for (ItemCollection orgunit : col) {
			boolean isMember = false;
			List<String> members = null;
			String orgunitName = orgunit.getItemValueString(type+".name");

			members = orgunit.getItemValue(type+".manager");
			// if (members.contains(userId)) {
			if (members.stream().anyMatch(userId::equalsIgnoreCase)) {
				memberList.add("{" + type + ":" + orgunitName + ":manager}");
				memberList.add("{" + type + ":" + orgunit.getUniqueID() + ":manager}");
				isMember = true;
				isGeneralManager = true;
			}
			members = orgunit.getItemValue(type+".team");
			// if (members.contains(userId)) {
			if (members.stream().anyMatch(userId::equalsIgnoreCase)) {
				memberList.add("{" + type + ":" + orgunitName + ":team}");
				memberList.add("{" + type + ":" + orgunit.getUniqueID() + ":team}");
				isMember = true;
				isGeneralTeam = true;
			}
			members = orgunit.getItemValue(type+".assist");
			// if (members.contains(userId)) {
			if (members.stream().anyMatch(userId::equalsIgnoreCase)) {
				memberList.add("{" + type + ":" + orgunitName + ":assist}");
				memberList.add("{" + type + ":" + orgunit.getUniqueID() + ":assist}");
				isMember = true;
				isGeneralAssist = true;
			}

			if (isMember) {
				memberList.add("{" + type + ":" + orgunitName + ":member}");
				memberList.add("{" + type + ":" + orgunit.getUniqueID() + ":member}");
				logger.finest(userId + " is member of '" + orgunitName + "'");
				isGeneralMember = true;
			}

		}

		// add general roles....
		if (isGeneralManager)
			memberList.add("{" + type + ":manager}");
		if (isGeneralTeam)
			memberList.add("{" + type + ":team}");
		if (isGeneralAssist)
			memberList.add("{" + type + ":assist}");
		if (isGeneralMember)
			memberList.add("{" + type + ":member}");

		return memberList;
	}

}
