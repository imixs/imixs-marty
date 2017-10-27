package org.imixs.marty.plugins;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.marty.ejb.TextBlockService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;

/**
 * Replace Adapter for TextBlocks
 * 
 * @author rsoika
 *
 */
public class ReplaceTextBlockAdapter  {

	private static Logger logger = Logger.getLogger(ReplaceTextBlockAdapter.class.getName());

	@EJB
	TextBlockService textBlockService;

	/**
	 * this method parses a string for xml tag <propertyvalue>. Those tags will be
	 * replaced with the corresponding system property value.
	 * 
	 * 
	 */
	public String replaceText(String aString, ItemCollection documentContext, WorkflowService workflowService)
			throws PluginException {

		// lower case <itemValue> into <itemvalue>
		aString = aString.replace("<textBlock", "<textblock");

		List<String> tagList = XMLParser.findTags(aString, "textblock");
		logger.finest(tagList.size() + " tags found");
		// test if a <value> tag exists...
		for (String tag : tagList) {

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// read the property Value
			String sTextBlockKey = XMLParser.findTagValue(tag, "textblock");
			String vValue = "sdf";
			ItemCollection textblockCol = textBlockService.loadTextBlock(sTextBlockKey);
			if (textblockCol == null) {
				logger.warning(" text-block '" + sTextBlockKey + "' is not defined!");
				vValue = "";
			}

			// now replace the tag with the result string
			int iStartPos = aString.indexOf(tag);
			int iEndPos = aString.indexOf(tag) + tag.length();

			// now replace the tag with the result string
			aString = aString.substring(0, iStartPos) + vValue + aString.substring(iEndPos);

		}

		return aString;

	}

}
