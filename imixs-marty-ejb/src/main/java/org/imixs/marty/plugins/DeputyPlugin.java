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
 * @author rsoika
 * 
 */
public class DeputyPlugin extends AbstractPlugin {

	public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";

	ItemCollection workitem = null;
	ProfileService profileService = null;
	private String[] ignoreList = { "namcreator", "namcurrenteditor",
			"namlasteditor", "namrequester" };
	private static Logger logger = Logger.getLogger(DeputyPlugin.class
			.getName());

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

			throw new PluginException(MailPlugin.class.getName(),
					PROFILESERVICE_NOT_BOUND, "ProfileService not bound", e);
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
		Map<String, Object> map = workitem.getAllItems();
		for (String key : map.keySet()) {
			key = key.toLowerCase();

			if (!key.startsWith("nam"))
				continue;

			if (ignoreArrayList.contains(key))
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
	 * the method updates a given vector with names and adds the deputies if
	 * defined. The method returns the a new list of names.
	 * 
	 * @param vNameList
	 * @return new list of names
	 */
	@SuppressWarnings("unchecked")
	private List<String> updateDeputies(List<String> vNameList) {
		Vector<String> vNameListNew = new Vector<String>();

		// test for each entry if a deputy is defined
		for (String aName : vNameList) {
			vNameListNew.add(aName);
			// now lookup the deputies

			ItemCollection profile = profileService.findProfileById(aName);
			if (profile != null) {

				List<String> vDeputies = profile.getItemValue("namdeputy");

				// if we found deputies - we need to add them to the list
				for (String deputy : vDeputies) {
					if (deputy != null && !deputy.isEmpty()
							&& vNameListNew.indexOf(deputy) == -1) {
						// add new entry
						vNameListNew.add(deputy);
					}
				}
			}

		}
		return vNameListNew;
	}

	public String[] getIgnoreList() {
		return ignoreList;
	}

	public void setIgnoreList(String[] ignoreList) {
		this.ignoreList = ignoreList;
	}

}
