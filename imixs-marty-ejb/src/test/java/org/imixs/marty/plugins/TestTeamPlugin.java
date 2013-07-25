package org.imixs.marty.plugins;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test class for RulePlugin
 * 
 * @author rsoika
 */
public class TestTeamPlugin {
	TeamPlugin teamPlugin = null;
	ItemCollection documentActivity;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	@Before
	public void setup() throws PluginException {
		ItemCollection entity = null;

		// simulate process and space entities
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "process");
			entity.replaceItemValue(EntityService.UNIQUEID, "P0000-0000" + i);
			entity.replaceItemValue("txtName", "Process " + i);
			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "space");
			entity.replaceItemValue(EntityService.UNIQUEID, "S0000-0000" + i);
			entity.replaceItemValue("txtName", "Space " + i);
			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(EntityService.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue(EntityService.UNIQUEID, "C0000-0000" + i);
			entity.replaceItemValue("txtName", "ChildWorkitem " + i);
			database.put(entity.getItemValueString(EntityService.UNIQUEID),
					entity);
		}

		// Mockito setup
		WorkflowService workflowContextMock = Mockito
				.mock(WorkflowService.class);
		when(workflowContextMock.getSessionContext()).thenReturn(null);

		EntityService entityService = Mockito.mock(EntityService.class);
		when(workflowContextMock.getEntityService()).thenReturn(entityService);

		// Simulate entityService.load()...
		when(entityService.load(Mockito.anyString())).thenAnswer(
				new Answer<ItemCollection>() {
					@Override
					public ItemCollection answer(InvocationOnMock invocation)
							throws Throwable {
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
	 * This test verifies if the txtProcessRef is transfered into $UnqiueIDref
	 * 
	 * @throws PluginException
	 * 
	 * */
	@Test
	public void testProcessRefInit() throws PluginException {

		documentContext.replaceItemValue("txtProcessRef", "P0000-00001");

		int result = teamPlugin.run(documentContext, documentActivity);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(uniqueIDref.contains("P0000-00001"));

	}

	/**
	 * This test verifies if the txtProcessRef is transfered into $UnqiueIDref
	 * and an old process id is removed correctly
	 * 
	 * @throws PluginException
	 * 
	 * */
	@Test
	@Ignore
	public void testProcessRefUpdate() throws PluginException {

		documentContext.replaceItemValue("txtProcessRef", "P0000-00002");
		documentContext.replaceItemValue("$UnqiueIDRef", "P0000-00001");

		int result = teamPlugin.run(documentContext, documentActivity);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertEquals(1, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.contains("P0000-00002"));

		// now test it also with a space ref
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtProcessRef", "P0000-00002");
		documentContext.replaceItemValue("txtSpaceRef", "P0000-00002");
		documentContext.replaceItemValue("$UnqiueIDRef", "P0000-00001");
		documentContext.replaceItemValue("$UnqiueIDRef", "S0000-00001");
		documentActivity = new ItemCollection();

		result = teamPlugin.run(documentContext, documentActivity);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertEquals(2, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.contains("P0000-00002"));

	}

}
