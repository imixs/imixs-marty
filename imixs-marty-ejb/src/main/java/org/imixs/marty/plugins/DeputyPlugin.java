package org.imixs.marty.plugins;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin computes for each name field (prefix = 'nam') if the
 * corresponding user profile contains a deputy. If so the deputy will be added
 * into the name field.
 * 
 * If a name Field is listed in the 'ignoreList' the field will be skipped. The
 * ignoreList can include regular expressions and can be modified by a client.
 * 
 * The plugin runs on all kinds of workitems and childworkitems.
 * 
 * The Plugin should run after the TeamPlugin but before the ownerPlugin,
 * approverPlugin and accessPlugin
 * 
 * To avoid conflicts with the ApproverPlugin, the DeputyPlugin ignores fields
 * ending with 'approvers' and 'approvedby'.
 * 
 * @see https://github.com/imixs/imixs-marty/issues/130
 * 
 * 
 * @author rsoika
 * 
 */
public class DeputyPlugin extends AbstractPlugin {

	public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";

	ItemCollection workitem = null;
	ProfileService profileService = null;
	private String[] ignoreList = { "namcreator", "namcurrenteditor", "namlasteditor", "namrequester",
			"nam+(?:[a-z0-9_]+)approvers", "nam+(?:[a-z0-9_]+)approvedby", "[^nam(.*)]" };

	private static Logger logger = Logger.getLogger(DeputyPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {

		super.init(actx);

		// userCache = new HashMap<String,List<String>>();

		// lookup profile service EJB
		String jndiName = "ejb/ProfileService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			profileService = (ProfileService) ctx.lookup(jndiName);
		} catch (NamingException e) {

			throw new PluginException(MailPlugin.class.getName(), PROFILESERVICE_NOT_BOUND, "ProfileService not bound",
					e);
		}

	}

	/**
	 * iterate over all fields with the prefix nam...
	 * 
	 * Skip if not a workitem
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection aworkItem, ItemCollection documentActivity) {

		workitem = aworkItem;

		// skip if the workitem is from a different type (for example Teams
		// may not be processed by this plugin)
		String type = workitem.getItemValueString("type");

		if (!type.startsWith("workitem") && !type.startsWith("childworkitem"))
			return Plugin.PLUGIN_OK;

		// iterate over name fields
		Map<String, List<Object>> map = workitem.getAllItems();
		for (String key : map.keySet()) {
			key = key.toLowerCase();

			if (matchIgnoreList(key))
				continue;

			// lookup deputies
			logger.fine("[DeputyPlugin] lookup=" + key);
			List<String> oldNameList = workitem.getItemValue(key);
			List<String> newNameList = updateDeputies(oldNameList);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("[DeputyPlugin] new list=");
				for (String aentry : newNameList) {
					logger.fine(aentry);
				}
			}
			// update field
			workitem.replaceItemValue(key, newNameList);
		}

		return Plugin.PLUGIN_OK;
	}

	public void close(int arg0) {
		// no op

	}

	/**
	 * This method updates a given list of names. For each name the method
	 * lookups a deputy. If a deputy is defined but not par of the list he will
	 * be added to the new list.
	 * 
	 * @param sourceNameList
	 *            - source list of names
	 * @return new list with all names plus deputies.
	 */
	@SuppressWarnings("unchecked")
	private List<String> updateDeputies(List<String> sourceNameList) {
		Vector<String> resultNameList = new Vector<String>();

		resultNameList.addAll(sourceNameList);
		// test for each entry if a deputy is defined
		for (String aName : sourceNameList) {
			// now lookup the deputies
			ItemCollection profile = profileService.findProfileById(aName);
			if (profile != null) {
				List<String> deputyList = profile.getItemValue("namdeputy");
				// if we found deputies - we need to add them to the list
				for (String deputy : deputyList) {
					if (deputy != null && !deputy.isEmpty() && resultNameList.indexOf(deputy) == -1) {
						// add new entry
						resultNameList.add(deputy);
					}
				}
			}
		}
		return resultNameList;
	}

	public String[] getIgnoreList() {
		return ignoreList;
	}

	public void setIgnoreList(String[] ignoreList) {
		this.ignoreList = ignoreList;
	}

	/**
	 * This method returns true in case the given fieldName matches the
	 * IgnoreList. Regular expressions are supported by the IgnoreList.
	 * 
	 * @param fieldName
	 * @return true if fieldName matches the ignoreList
	 */
	public boolean matchIgnoreList(String fieldName) {
		if (fieldName == null)
			return false;
		for (String pattern : this.ignoreList) {
			if (fieldName.toLowerCase().matches(pattern)) {
				return true;
			}
		}
		return false;
	}

}
