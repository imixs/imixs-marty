package org.imixs.marty.ejb;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imixs.marty.plugins.TeamPlugin;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
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

	@Spy
	TeamRoleWildcardAdapter teamRoleWildcardAdapter;

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 */
	@Before
	public void setup() throws PluginException {
		MockitoAnnotations.initMocks(this);

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
		DocumentService documentService = Mockito.mock(DocumentService.class);
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

		documentActivity = new ItemCollection();
		documentContext = new ItemCollection();

		teamRoleWildcardAdapter.documentService = documentService;

	}

	/**
	 * This test validates the wildcard orgunit role as a ACL notation:
	 * 
	 * {process:?:team}
	 * 
	 * The Plugin should compute the orgunit bas"{space:?:team}"ed on the assigement
	 * to the current workitem.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testWildcardOrgunitRole() throws PluginException {

		// test a space team role...
		documentContext.replaceItemValue("txtSpaceRef", "S0000-00002");
		documentContext.replaceItemValue("txtProcessRef", "P0000-00003");

		String testRole = "{space:?:team}";
		TextEvent event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		String roleResult = event.getText();
		
		Assert.assertEquals(roleResult, "{space:S0000-00002:team}");

		// test member role
		testRole = "{process:?:member}";
		event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		roleResult = event.getText();
		Assert.assertEquals(roleResult, "{process:P0000-00003:member}");

	}




	/**
	 * This test validates the wildcard orgunit role as a ACL notation:
	 * 
	 * {process:?:team}
	 * 
	 * The Plugin should compute the orgunit bas"{space:?:team}"ed on the assigement
	 * to the current workitem.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testWildcardMultiOrgunitRole() throws PluginException {

		// test two space team roles...
		documentContext.replaceItemValue("txtSpaceRef", "S0000-00002");
		documentContext.appendItemValue("txtSpaceRef", "S0000-00003");
		
		
		documentContext.replaceItemValue("txtProcessRef", "P0000-00003");

		String testRole = "{space:?:team}";
		TextEvent event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		String roleResult = event.getText();
		
		List<String> listResult=event.getTextList();
		
		Assert.assertEquals(2,listResult.size());
		
		Assert.assertEquals(listResult.get(0), "{space:S0000-00002:team}");
		Assert.assertEquals(listResult.get(1), "{space:S0000-00003:team}");

		// test member role
		testRole = "{process:?:member}";
		event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		roleResult = event.getText();
		Assert.assertEquals(roleResult, "{process:P0000-00003:member}");

	}


}
