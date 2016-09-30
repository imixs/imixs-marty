package org.imixs.marty.plugins;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;

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

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 */
	@Before
	public void setup() throws PluginException {
		ItemCollection entity = null;

		
		System.out.println("ClassName: "+TestTeamPlugin.class.getName());
		
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

		DocumentService documentService = Mockito.mock(DocumentService.class);
		when(workflowContextMock.getDocumentService()).thenReturn(documentService);

		// Simulate entityService.load()...
		when(documentService.load(Mockito.anyString())).thenAnswer(
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
	 * This simple test verifies if the txtProcessRef is transfered into
	 * $UnqiueIDref
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessRefInit() throws PluginException {

		documentContext.replaceItemValue("txtProcessRef", "P0000-00001");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(uniqueIDref.contains("P0000-00001"));

	}

	/**
	 * If the property txtProcessRef not exists, but $UnqiueIDref contains a
	 * valid Process then the value in $UnqiueIDref must be transfered into
	 * txtProcessRef
	 * 
	 * This test verifies if the txtProcessRef is created and if the value in
	 * $UnqiueIDref is transfered into txtProcessRef
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessRefInitNoProcessRef() throws PluginException {

		// Case-1 - one unqiueid
		documentContext.replaceItemValue("$UniqueIDRef", "P0000-00001");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> processRef = documentContext.getItemValue("txtProcessRef");
		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(processRef.contains("P0000-00001"));

		Assert.assertEquals(processRef, uniqueIDref);

		// Case-2 - two uniqueids
		documentContext = new ItemCollection();
		Vector<String> refs = new Vector<String>();
		refs.add("P0000-00001");
		refs.add("P0000-00002");

		documentContext.replaceItemValue("$UniqueIDRef", refs);

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		processRef = documentContext.getItemValue("txtProcessRef");
		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(processRef.contains("P0000-00001"));
		Assert.assertTrue(processRef.contains("P0000-00002"));

		Assert.assertEquals(processRef, uniqueIDref);

	}

	/**
	 * Case-1: If the property txtProcessRef exists but is empty and
	 * $UnqiueIDref contains a Process then the value in $UnqiueIDref must be
	 * removed and txtProcessRef should still be empty.
	 * 
	 * Case-2: If a workitem ref is stored in $uniqueid than this id should be
	 * still available.
	 * 
	 * This test verifies if the ref in $Uniqueid is removed
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessRefInitEmptyProcessRef() throws PluginException {
		// case-1
		documentContext.replaceItemValue("$UniqueIDRef", "P0000-00001");
		// empty txtProcessRef
		documentContext.replaceItemValue("txtProcessRef", "");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> processRef = documentContext.getItemValue("txtProcessRef");
		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(processRef.isEmpty());
		Assert.assertTrue(uniqueIDref.isEmpty());

		// case-2
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("$UniqueIDRef", "W0000-00001");
		// empty txtProcessRef
		documentContext.replaceItemValue("txtProcessRef", "");

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		processRef = documentContext.getItemValue("txtProcessRef");
		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertTrue(processRef.isEmpty());
		Assert.assertEquals(1, uniqueIDref.size());

	}

	/**
	 * If a new Process is assigned into txtProcessRef and $UniqueIDRef holds an
	 * different value then the new process ref will be transfered into
	 * $UniueIdRef and the old id will be removed.
	 * 
	 * This test verifies if the txtProcessRef is transfered into $UnqiueIDref
	 * and an old process id is removed correctly
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	// @Ignore
	@Test
	public void testProcessRefUpdate() throws PluginException {

		// test case-1 :
		// new processRef provided -> old processRef should be
		// removed....

		// new id....
		documentContext.replaceItemValue("txtProcessRef", "P0000-00002");
		// old id....
		documentContext.replaceItemValue("$UnqiueIDRef", "P0000-00001");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		// only one id expect
		Assert.assertEquals(1, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.contains("P0000-00002"));

		// test case-2 :
		// new processRef provided -> old processRef should be
		// removed....
		// now test it also with a space ref
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtProcessRef", "P0000-00002");
		documentContext.replaceItemValue("txtSpaceRef", "S0000-00002");

		// assign two old refs....
		Vector<String> refs = new Vector<String>();
		refs.add("P0000-00001");
		refs.add("S0000-00001");
		documentContext.replaceItemValue("$UnqiueIDRef", refs);
		documentActivity = new ItemCollection();

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");

		Assert.assertEquals(2, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.contains("P0000-00002"));

	}

	/**
	 * If a wrong entity is assigned into txtProcessRef (e.g a space) then the
	 * reference should be removed.
	 * 
	 * This test verifies if a wrong element in txtProcessRef is removed
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	// @Ignore
	@Test
	public void testInvalidProcessRef() throws PluginException {

		// test case-1 :
		// assign space as an invalid ref

		// new id....
		documentContext.replaceItemValue("txtProcessRef", "S0000-00002");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		List<String> processIDref = documentContext
				.getItemValue("txtProcessRef");
		// empty expect
		Assert.assertEquals(0, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.isEmpty());

		Assert.assertEquals(0, processIDref.size());
		Assert.assertTrue(processIDref.isEmpty());
		
		
		// case-2 invalid id
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtProcessRef", "xxxxP0000-00002");

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		processIDref = documentContext.getItemValue("txtProcessRef");
		// empty expect
		Assert.assertEquals(0, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.isEmpty());

		Assert.assertEquals(0, processIDref.size());
		Assert.assertTrue(processIDref.isEmpty());

		// case-3 one valid , one invlid id
		documentContext = new ItemCollection();
		Vector<String> refs = new Vector<String>();
		refs.add("P0000-00001");
		refs.add("S0000-00001");
		documentContext.replaceItemValue("txtProcessRef", refs);

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		processIDref = documentContext.getItemValue("txtProcessRef");
		// empty expect
		Assert.assertEquals(1, uniqueIDref.size());

		Assert.assertEquals(1, processIDref.size());
		Assert.assertEquals(uniqueIDref, processIDref);
	}

	/**
	 * If a wrong entity is assigned into txtSpaceRef (e.g a process) then the
	 * reference should be removed.
	 * 
	 * This test verifies if a wrong element in txtSpaceRef is removed
	 * 
	 * @throws PluginException
	 * 
	 * */
	@SuppressWarnings("unchecked")
	@Test
	public void testInvalidSpaceRef() throws PluginException {

		// test case-1 :
		// assign space as an invalid ref

		// new id....
		documentContext.replaceItemValue("txtSpaceRef", "P0000-00002");

		int result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		List<String> uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		List<String> spaceIDref = documentContext.getItemValue("txtSpaceRef");
		// empty expect
		Assert.assertEquals(0, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.isEmpty());

		Assert.assertEquals(0, spaceIDref.size());
		Assert.assertTrue(spaceIDref.isEmpty());

		// case-2 invalid id
		documentContext = new ItemCollection();
		documentContext.replaceItemValue("txtSpaceRef", "xxxxP0000-00002");

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		spaceIDref = documentContext.getItemValue("txtSpaceRef");
		// empty expect
		Assert.assertEquals(0, uniqueIDref.size());
		Assert.assertTrue(uniqueIDref.isEmpty());

		Assert.assertEquals(0, spaceIDref.size());
		Assert.assertTrue(spaceIDref.isEmpty());

		// case-3 one valid , one invlid id
		documentContext = new ItemCollection();
		Vector<String> refs = new Vector<String>();
		refs.add("P0000-00001");
		refs.add("S0000-00001");
		documentContext.replaceItemValue("txtSpaceRef", refs);

		result = teamPlugin.run(documentContext, documentActivity);
		Assert.assertEquals(Plugin.PLUGIN_OK, result);

		uniqueIDref = documentContext.getItemValue("$UniqueIDRef");
		spaceIDref = documentContext.getItemValue("txtSpaceRef");
		// empty expect
		Assert.assertEquals(1, uniqueIDref.size());

		Assert.assertEquals(1, spaceIDref.size());
		Assert.assertEquals(uniqueIDref, spaceIDref);
	}
}
