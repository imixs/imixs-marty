package org.imixs.marty.workflow;

import org.imixs.workflow.ItemCollection;

public class WorkflowEvent {

	public static final int WORKITEM_CREATED = 1;
	public static final int WORKITEM_INITIALIZED = 2;
	public static final int WORKITEM_BEFORE_PROCESS = 3;
	public static final int WORKITEM_AFTER_PROCESS = 4;

	private int eventType;
	private ItemCollection workitem;

	public WorkflowEvent(ItemCollection workitem, int eventType) {
		if (workitem == null)
			workitem = new ItemCollection();
		this.eventType = eventType;
		this.workitem = workitem;
	}

	public int getEventType() {
		return eventType;
	}

	public ItemCollection getWorkitem() {
		return workitem;
	}

}
