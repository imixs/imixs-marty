package org.imixs.marty.ejb;

/**
 * The SetupEvent provides a CDI observer pattern. The SetupEvent is
 * fired by the SetupService EJB. An event Observer can react on this event
 * to extend the setup routine.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.DocumentService
 */
public class SetupEvent {

	private String result;
	
	public SetupEvent(String result) {
		super();
		this.result = result;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	

}
