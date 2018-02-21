package org.imixs.marty.plugins;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;

/**
 * Test class for TeamPlugin
 * 
 * @author rsoika
 */
public class TestTeamWildcardAdapter {
	TeamPlugin teamPlugin = null;
	ItemCollection documentActivity;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 */
	@Before
	public void setup() throws PluginException {
		ItemCollection entity = null;

		System.out.println("ClassName: " + TestTeamWildcardAdapter.class.getName());

		// simulate process and space entities
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "process");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "P0000-0000" + i);
			entity.replaceItemValue("txtName", "Process " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "space");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "S0000-0000" + i);
			entity.replaceItemValue("txtName", "Space " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "C0000-0000" + i);
			entity.replaceItemValue("txtName", "ChildWorkitem " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		// Mockito setup
		WorkflowService workflowContextMock = Mockito.mock(WorkflowService.class);
		when(workflowContextMock.getSessionContext()).thenReturn(null);

		DocumentService documentService = Mockito.mock(DocumentService.class);
		when(workflowContextMock.getDocumentService()).thenReturn(documentService);

		// Simulate entityService.load()...
		when(documentService.load(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				ItemCollection result = database.get(id);
				return result;
			}
		});

		teamPlugin = new TeamPlugin();
		teamPlugin.init(workflowContextMock);

		documentActivity = new ItemCollection();
		documentContext = new ItemCollection();

	}

	/**
	 * This test validates the wildcard orgunit role as a ACL notation:
	 * 
	 * {process:?:team}
	 * 
	 * The Plugin should compute the orgunit based on the assigement to the current
	 * workitem.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testWildcardOrgunitRole() throws PluginException {

		// test case-1 :
		// assign space as an invalid ref

		// new id....
		documentContext.replaceItemValue("txtSpaceRef", "S0000-00002");
		documentContext.replaceItemValue("txtProcessRef", "P0000-00003");

		// set ACL (namaddreadaccess)....
		documentActivity.replaceItemValue("namaddreadaccess", "{space:?:team}");
		// set ACL (namaddwriteaccess)....
		documentActivity.replaceItemValue("namaddwriteaccess", "{process:?:manager}");
		// set ACL (namOwnershipNames)....
		documentActivity.replaceItemValue("namOwnershipNames", "{process:?:member}");

		documentContext = teamPlugin.run(documentContext, documentActivity);
		Assert.assertNotNull(documentContext);

		// now we expect that the namaddreadaccess is addapted with the real team roles
		List accessList = documentActivity.getItemValue("namaddreadaccess");
		// wildcard role should be replaced
		Assert.assertFalse(accessList.contains("{space:?:team}"));
		Assert.assertTrue(accessList.contains("{space:S0000-00002:team}"));
		Assert.assertTrue(accessList.contains("{space:Space 2:team}"));

		// next we expect that also the namaddwriteaccess is addapted with the real team
		// roles
		accessList = documentActivity.getItemValue("namaddwriteaccess");
		// wildcard role should be replaced
		Assert.assertFalse(accessList.contains("{process:?:team}"));
		Assert.assertTrue(accessList.contains("{process:P0000-00003:manager}"));
		Assert.assertTrue(accessList.contains("{process:Process 3:manager}"));

		// and finally we expect that also the namOwnershipNames is addapted with the
		// real team roles
		accessList = documentActivity.getItemValue("namOwnershipNames");
		// wildcard role should be replaced
		Assert.assertFalse(accessList.contains("{process:?:team}"));
		Assert.assertTrue(accessList.contains("{process:P0000-00003:member}"));
		Assert.assertTrue(accessList.contains("{process:Process 3:member}"));

	}
}
