package org.imixs.marty.workflow;

import org.imixs.workflow.ItemCollection;

public class WorkflowEvent {

	public static final int WORKITEM_CREATED = 1;
	public static final int WORKITEM_INITIALIZED = 2;
	public static final int WORKITEM_CHANGED = 3;
	public static final int WORKITEM_BEFORE_PROCESS = 4;
	public static final int WORKITEM_AFTER_PROCESS = 5;
	public static final int WORKITEM_BEFORE_ARCHIVE = 6;
	public static final int WORKITEM_AFTER_ARCHIVE = 7;
	public static final int WORKITEM_BEFORE_SOFTDELETE = 8;
	public static final int WORKITEM_AFTER_SOFTDELETE = 9;
	public static final int WORKITEM_BEFORE_RESTOREFROMARCHIVE = 10;
	public static final int WORKITEM_AFTER_RESTOREFROMARCHIVE = 11;
	public static final int WORKITEM_BEFORE_RESTOREFROMSOFTDELETE = 12;
	public static final int WORKITEM_AFTER_RESTOREFROMSOFTDELETE = 13;

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
