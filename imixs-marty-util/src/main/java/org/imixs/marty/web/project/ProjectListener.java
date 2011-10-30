package org.imixs.marty.web.project;

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
public interface ProjectListener extends EventListener {

	public void onProjectCreated(ItemCollection e);

	public void onProjectChanged(ItemCollection e);

	public void onProjectProcess(ItemCollection e);

	public void onProjectProcessCompleted(ItemCollection e);
	
	public void onProjectDelete(ItemCollection e);

	
	
	
}
