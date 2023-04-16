package org.imixs.marty.team;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.engine.plugins.AbstractPlugin;

/**
 * The TeamRoleWildcardAdapter replaces wildcard teamrols like:
 * 
 * {process:?:team}
 * 
 * With the corresponding orgunit name and orgunite uniqueID
 * 
 * {process:Finance:team}
 * 
 * {process:8838786e-6fda-4e0d-a76c-5ac3e0b04071:team}
 * 
 * This notation is supported for the ACL settings in the Modeler.
 * 
 * The Adapter is called by the method 'mergeValueList' from the AbstractPlugin.
 * 
 * @see org.imixs.workflow.engine.plugins.AbstractPlugin
 * 
 * 
 * @author rsoika
 *
 */
@Stateless
public class TeamRoleWildcardAdapter {

	private static Logger logger = Logger.getLogger(AbstractPlugin.class.getName());

	/**
	 * This method reacts on CDI events of the type TextEvent and parses a role
	 * string with wildcards like '{process:?:team}' with the corresponding uniqueid
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	public void onEvent(@Observes TextEvent event) {

		List<String> textList = new ArrayList<String>();
		String role = event.getText();
		ItemCollection documentContext = event.getDocument();

		// verfiy if a wildcard is used?
		// ^(\{space\:\?\:|\{process\:\?\:)
		// we use quote method to escape the search pattern
		String regex = "^(" + Pattern.quote("{space:?:") + ".*|" + Pattern.quote("{process:?:") + ".*)";
		if (role.matches(regex)) {
			logger.fine("replace role wildcards ...");
			List<String> orgunitIDs;
			// lookup all the spaces.....
			if (role.startsWith("{space:?:")) {
				orgunitIDs = documentContext.getItemValue("space.ref");
				for (String id : orgunitIDs) {
				    // we no longer verify the id - Issue #327
					textList.add(role.replace(":?:", ":" + id + ":"));
				}
			}

			// lookup all the processes.....
			if (role.startsWith("{process:?:")) {
				orgunitIDs = documentContext.getItemValue("process.ref");
				for (String id : orgunitIDs) {
                    // we no longer verify the id - Issue #327
					textList.add(role.replace(":?:", ":" + id + ":"));
				}
			}

			// set the result list
			event.setTextList(textList);
		} else {
			// no wildcard found - no replacement needed.
			// See also issue #254
			event.setText(role);
		}
	}

}
