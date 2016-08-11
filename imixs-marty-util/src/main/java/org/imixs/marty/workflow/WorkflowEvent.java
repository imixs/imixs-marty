package org.imixs.marty.workflow;

import org.imixs.workflow.ItemCollection;

public class WorkflowEvent {

	public static final int WORKITEM_CREATED = 1;
	public static final int WORKITEM_INITIALIZED = 2;
	public static final int WORKITEM_CHANGED = 3;
	public static final int WORKITEM_BEFORE_PROCESS = 4;
	public static final int WORKITEM_AFTER_PROCESS = 5;
	public static final int WORKITEM_BEFORE_SAVE = 14;
	public static final int WORKITEM_AFTER_SAVE = 15;


	// childevents
	public static final int CHILDWORKITEM_CREATED = 21;
	public static final int CHILDWORKITEM_INITIALIZED = 22;
	public static final int CHILDWORKITEM_BEFORE_PROCESS = 24;
	public static final int CHILDWORKITEM_AFTER_PROCESS = 25;
	public static final int CHILDWORKITEM_BEFORE_SOFTDELETE = 28;
	public static final int CHILDWORKITEM_AFTER_SOFTDELETE = 29;

	
	
	private int eventType;
	private ItemCollection workitem;

	public WorkflowEvent(ItemCollection workitem, int eventType) {
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
