package org.imixs.marty.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;

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
	 * Returns a string array containing all orgunit names a given UID is member of.
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

		// build an araa with all member strings...
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
			logger.fine("resolved membership for '" + userId);
			for (String aGroup : groups)
				groupListe += "'" + aGroup + "' ";
			logger.fine(groupListe);
		}

		return groups;

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

		List<String> memberList = new ArrayList<String>();

		Collection<ItemCollection> col = documentService.getDocumentsByType(type);
		logger.fine(col.size() + " processes found...");
		// create optimized list
		for (ItemCollection orgunit : col) {
			boolean isMember = false;
			List<String> members = null;
			String orgunitName = orgunit.getItemValueString("txtname");

			members = orgunit.getItemValue("nammanager");
			if (members.contains(userId)) {
				memberList.add("{" + type + ":" + orgunitName + ":manager}");
				isMember = true;
			}
			members = orgunit.getItemValue("namteam");
			if (members.contains(userId)) {
				memberList.add("{" + type + ":" + orgunitName + ":team}");
				isMember = true;
			}
			members = orgunit.getItemValue("namassist");
			if (members.contains(userId)) {
				memberList.add("{" + type + ":" + orgunitName + ":assist}");
				isMember = true;
			}

			if (isMember) {
				memberList.add("{" + type + ":" + orgunitName + ":member}");
				logger.fine(userId + " is member of '" + orgunitName + "'");
			}

		}

		return memberList;
	}

}
