package org.imixs.marty.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
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

	@EJB
	DocumentService documentService;

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

		logger.fine("replace role wildcards ...");
		List<String> orgunitIDs;
		// lookup all the spaces.....
		if (role.startsWith("{space:?:")) {
			orgunitIDs = documentContext.getItemValue("txtspaceref");
			for (String id : orgunitIDs) {
				ItemCollection orgunit = documentService.load(id);
				if (orgunit != null) {
					textList.add(role.replace(":?:", ":" + id + ":"));
				}
			}
		}

		// lookup all the processes.....
		if (role.startsWith("{process:?:")) {
			orgunitIDs = documentContext.getItemValue("txtprocessref");
			for (String id : orgunitIDs) {
				ItemCollection orgunit = documentService.load(id);
				if (orgunit != null) {
					textList.add(role.replace(":?:", ":" + id + ":"));
				}
			}
		}

		event.setTextList(textList);
		// set default behavior for text field (see TextEvent.getText())
		event.setText(null);
	}

}
