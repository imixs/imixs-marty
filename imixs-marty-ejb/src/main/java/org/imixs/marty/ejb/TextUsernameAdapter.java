package org.imixs.marty.ejb;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.engine.TextItemValueAdapter;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.util.XMLParser;

/**
 * The TextUsernameAdapter replaces text fragments with the tag
 * <username>..</username>. The values of the item will be replaced with the
 * display name from the corresponding user profile display name.
 * <p>
 * Example:
 * <p>
 * {@code
 *    Workitem updated by: <username>namcurrenteditor</username>.
 * }
 * <p>
 * This will replace the namcurrenteditor with the corrsponding profile full
 * username. If the username item value is a multiValue object the single values
 * can be spearated by a separator
 * <p>
 * Example:
 * <p>
 * {@code 
 * Team List: <username separator="<br />">txtTeam</username>
 * }
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextUsernameAdapter {

	private static Logger logger = Logger.getLogger(AbstractPlugin.class.getName());

	@EJB
	ProfileService profileService;

	/**
	 * This method reacts on CDI events of the type TextEvent and parses a string
	 * for xml tag <username>. Those tags will be replaced with the corresponding
	 * display name of the user profile.
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onEvent(@Observes TextEvent event) {
		String text = event.getText();
		ItemCollection documentContext = event.getDocument();

		String sSeparator = " ";
		if (text == null)
			return;

		// lower case <itemValue> into <itemvalue>
		if (text.contains("<userName") || text.contains("</userName>")) {
			logger.warning("Deprecated <userName> tag should be lowercase <username> !");
			text = text.replace("<userName", "<username");
			text = text.replace("</userName>", "</username>");
		}

		List<String> tagList = XMLParser.findTags(text, "username");
		logger.finest(tagList.size() + " tags found");
		// test if a <value> tag exists...
		for (String tag : tagList) {

			// next we check if the start tag contains a 'separator' attribute
			sSeparator = XMLParser.findAttribute(tag, "separator");

			// extract Item Value
			String sItemValue = XMLParser.findTagValue(tag, "username");

			List<String> tempList = documentContext.getItemValue(sItemValue);
			// clone List
			List<String> vUserIDs = new Vector(tempList);
			// get usernames ....
			for (int i = 0; i < vUserIDs.size(); i++) {
				ItemCollection profile = profileService.findProfileById(vUserIDs.get(i));
				if (profile != null) {
					vUserIDs.set(i, profile.getItemValueString("txtUserName"));
				}
			}

			// format field value
			String sResult = TextItemValueAdapter.formatItemValues(vUserIDs, sSeparator, "");

			// now replace the tag with the result string
			int iStartPos = text.indexOf(tag);
			int iEndPos = text.indexOf(tag) + tag.length();

			text = text.substring(0, iStartPos) + sResult + text.substring(iEndPos);
		}
		event.setText(text);
	}

}
