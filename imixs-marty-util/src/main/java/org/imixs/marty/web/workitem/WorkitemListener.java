package org.imixs.sywapps.web.workitem;

import java.util.EventListener;

import org.imixs.workflow.ItemCollection;

/**
 * This interface can be implemented by a managed bean to observe the status of
 * the wokitemMB. The WorkitemMB will fire different events on specific program
 * situations.
 * 
 * @author rsoika
 * 
 */
public interface WorkitemListener extends EventListener {

	public void onWorkitemCreated(ItemCollection e);

	public void onWorkitemChanged(ItemCollection e);

	public void onWorkitemProcess(ItemCollection e);

	public void onWorkitemProcessCompleted(ItemCollection e);
	
	public void onWorkitemDelete(ItemCollection e);

	public void onWorkitemDeleteCompleted();
	
	public void onWorkitemSoftDelete(ItemCollection e);

	public void onWorkitemSoftDeleteCompleted(ItemCollection e);

	public void onChildProcess(ItemCollection e);

	public void onChildProcessCompleted(ItemCollection e);

	public void onChildCreated(ItemCollection e);

	public void onChildDelete(ItemCollection e);

	public void onChildDeleteCompleted();
	
	public void onChildSoftDelete(ItemCollection e);

	public void onChildSoftDeleteCompleted(ItemCollection e);
}
