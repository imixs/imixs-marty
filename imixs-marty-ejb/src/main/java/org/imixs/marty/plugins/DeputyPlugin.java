package org.imixs.marty.plugins;

import java.util.Arrays;
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
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plugin computes for each name field (prafix = 'nam') if the user has a
 * deputy entry in the corresponding user profile entity. If so the deputy will
 * be added to the name field.
 * 
 * If a namField is listed in the 'ignoreList' the field will be skipped.
 * 
 * The plugin runns on all kinds of workitems and childworkitems.
 * 
 * The Plugin should run after the TeamPlugin but before the ownerPlugin and
 * accessPlugin
 * 
 * 
 * The Plugin only runs in all workflows
 * 
 * 
 * To avoid conflicts with the ApproverPlugin, the DeputyPlugin ignores fields
 * ending with 'approvers' and 'approvedby'
 * 
 * 
 * @author rsoika
 * 
 */
public class DeputyPlugin extends AbstractPlugin {

	public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";

	ItemCollection workitem = null;
	ProfileService profileService = null;
	private String[] ignoreList = { "namcreator", "namcurrenteditor", "namlasteditor", "namrequester" };
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

		List<String> ignoreArrayList = Arrays.asList(ignoreList);

		// skip if the workitem is from a different type (for example Teams
		// may not be processed by this plugin)
		String type = workitem.getItemValueString("type");

		if (!type.startsWith("workitem") && !type.startsWith("childworkitem"))
			return Plugin.PLUGIN_OK;

		// iterate over name fields
		Map<String, List<Object>> map = workitem.getAllItems();
		for (String key : map.keySet()) {
			key = key.toLowerCase();

			if (!key.startsWith("nam"))
				continue;

			if (ignoreArrayList.contains(key))
				continue;

			// skip Approver Fields (issue #130)
			if (key.endsWith("approvers") || key.endsWith("approvedby"))
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

}
