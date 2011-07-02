package org.imixs.marty.util;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.plugins.ApplicationPlugin;

/**
 * This plugin overwrites the Application Plugin and updates sywapp informations
 * like the subject and the workflowgroup name.
 * 
 * @author rsoika
 * 
 */
public class SywappApplicationPlugin extends ApplicationPlugin {
	ItemCollection documentContext;

	@Override
	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);
	}

	@Override
	public int run(ItemCollection adocumentContext,
			ItemCollection documentActivity) throws Exception {

		documentContext = adocumentContext;

		// Update Subject
		if (!documentContext.hasItem("txtSubject"))
			documentContext.replaceItemValue("txtSubject", " - no subject - ");

		int iResult = super.run(documentContext, documentActivity);

		return iResult;
	}

	@Override
	public void close(int arg0) throws Exception {
		super.close(arg0);

		try { // now cut txtworkflowgroup if ~ is available
			String sGroupName = documentContext
					.getItemValueString("txtWorkflowGroup");
			if (sGroupName.indexOf('~') > -1) {
				sGroupName = sGroupName.substring(sGroupName.indexOf('~') + 1);

				documentContext
						.replaceItemValue("txtWorkflowGroup", sGroupName);
			}
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
}
