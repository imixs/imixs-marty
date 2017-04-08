package org.imixs.marty.plugins;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.AbstractWorkflowEnvironment;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for ApproverPlugin
 * 
 * @author rsoika
 */
public class TestApproverPlugin extends AbstractWorkflowEnvironment {
	ApproverPlugin approverPlugin = null;
	ItemCollection documentActivity;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	/**
	 * Setup script
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 */
	@Before
	public void setup() throws PluginException, AccessDeniedException, ProcessingErrorException, ModelException {

		super.setup();
		approverPlugin = new ApproverPlugin();
		// init plugin..
		try {
			approverPlugin.init(workflowService);
		} catch (PluginException e) {
			e.printStackTrace();
		}

		documentContext = new ItemCollection();
	}

	/**
	 * This simple test verifies if a approver list is added correctly into the
	 * workitem
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testNewApproverList() throws PluginException, ModelException {

		documentActivity = this.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with ronny
		when(workflowService.getUserName()).thenReturn("ronny");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(0, documentContext.getItemValue("namProcessManagerApprovedByBy").size());

	}

	/**
	 * This simple test verifies if a approver list is added correctly into the
	 * workitem if the current user is equals one of the approvers!
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testNewApproverListImmediateApproval() throws PluginException, ModelException {

		documentActivity = this.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with manfred
		when(workflowService.getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(2, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(1, documentContext.getItemValue("namProcessManagerApprovedBy").size());

	}

	/**
	 * Complex test verifies if a approver list is updated in a second run, a
	 * new approver (which may be added by the deputy plug-in) is added
	 * correctly into the existing list
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testUpdateApproverListNewApprover() throws PluginException, ModelException {

		documentActivity = this.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with manfred
		when(workflowService.getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(2, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(1, documentContext.getItemValue("namProcessManagerApprovedBy").size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		nameList.add("ronny");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with manfred
		when(workflowService.getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(3, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(1, documentContext.getItemValue("namProcessManagerApprovedBy").size());

	}

	/**
	 * Complex test verifies if a approver list is updated in a second run, if a
	 * new approver (which may be added by the deputy plug-in) is added
	 * correctly into the existing list (in this case a user which already
	 * approved)
	 * 
	 * @throws PluginException
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testUpdateApproverListExistingApprover() throws PluginException, ModelException {

		documentActivity = this.getModel().getEvent(100, 10);
		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name='approvedby'>ProcessManager</item>");

		List<String> nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with manfred
		when(workflowService.getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		Assert.assertEquals(2, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(1, documentContext.getItemValue("namProcessManagerApprovedBy").size());

		// second run - change soruce list

		nameList = new ArrayList<String>();
		nameList.add("anna");
		nameList.add("manfred");
		nameList.add("eddy");
		documentContext.replaceItemValue("namProcessManager", nameList);

		// test with manfred
		when(workflowService.getUserName()).thenReturn("manfred");
		documentContext = approverPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		// list should not be changed!
		Assert.assertEquals(2, documentContext.getItemValue("namProcessManagerApprovers").size());
		Assert.assertEquals(1, documentContext.getItemValue("namProcessManagerApprovedBy").size());

	}

}
