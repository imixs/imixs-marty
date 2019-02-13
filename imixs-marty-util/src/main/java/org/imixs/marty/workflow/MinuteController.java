package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.inject.Named;

import org.imixs.marty.plugins.minutes.MinutePlugin;
import org.imixs.marty.util.ErrorHandler;
import org.imixs.marty.util.ValidationException;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The MinuteController is a session scoped backing bean controlling a list of
 * minute workitems assigned to a parent workitem. This controller can be used
 * for different kind of Minute-Workflows.
 * 
 * The method toggleWorkitem can be used to open/closed embedded editors:
 * 
 * 
 * 
 * @author rsoika
 */
@Named
@ConversationScoped
public class MinuteController extends org.imixs.workflow.faces.workitem.WorkflowController implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> minutes = null;

	private ItemCollection parentWorkitem = null; // parent minute

	private static Logger logger = Logger.getLogger(MinuteController.class.getName());

	protected FormController formController = null;

	private FormDefinition formDefinition = null;

	@EJB
	DocumentService documentService;

	@EJB
	WorkflowService workflowService;

	/**
	 * Here we initialize the formController for the minute workitem
	 */
	public MinuteController() {
		super();
		// initialize formControlller
		formController = new FormController();
	}

	/**
	 * Override process to close the current minute.
	 * 
	 * The method handles PluginExceptions and adds the error messages into the faces context. 
	 * 
	 * @throws ModelException
	 */
	@Override
	public String process() throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		String result = "";
		// process workItem and catch exceptions
		try {

			result = super.process();

		} catch (ObserverException oe) {
			result = "";
			// test if we can handle the exception...
			if (oe.getCause() instanceof PluginException) {
				// add error message into current form
				ErrorHandler.addErrorMessage((PluginException) oe.getCause());
			} else {
				if (oe.getCause() instanceof ValidationException) {
					// add error message into current form
					ErrorHandler.addErrorMessage((ValidationException) oe.getCause());
				} else {
					// throw unknown exception
					throw oe;
				}
			}
		} catch (PluginException pe) {
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handlePluginException(pe);
			return "";
		} catch (ModelException me) {
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handleModelException(me);
			return "";
		}

		// we have no errors, so reset the current item and close it
		this.reset();
		minutes = null;
		return result;
	}

	/**
	 * WorkflowEvent listener to set the current parentWorkitem.
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) throws AccessDeniedException {
		if (workflowEvent == null || workflowEvent.getWorkitem() == null)
			return;

		// skip if not a workItem...
		if (workflowEvent.getWorkitem() != null
				&& !workflowEvent.getWorkitem().getItemValueString("type").startsWith("workitem"))
			return;

		int eventType = workflowEvent.getEventType();

		if ((MinutePlugin.MINUTE_TYPE_PARENT
				.equals(workflowEvent.getWorkitem().getItemValueString(MinutePlugin.MINUTETYPE)))
				&& (WorkflowEvent.WORKITEM_CHANGED == eventType)) {
			reset();
			parentWorkitem = workflowEvent.getWorkitem();
		}

	}

	public FormDefinition getFormDefinition() {
		return formDefinition;
	}

	/**
	 * This toggle method will either load a new minute workitem or reset the
	 * current the current minute workitem. This method can be used to open/close
	 * embedded editors
	 * 
	 * @param id
	 */
	public void toggleWorkitem(String id) {
		if (getWorkitem().getUniqueID().equals(id)) {
			// reset
			this.setWorkitem(null);
		} else {
			// load by id
			setWorkitem(documentService.load(id));
		}
	}

	/**
	 * Set the current minute workitem and loads the formDefintion
	 */
	@Override
	public void setWorkitem(ItemCollection workitem) {
		super.setWorkitem(workitem);
		// update formDefinition
		formDefinition = formController.computeFormDefinition(workitem);
	}

	/**
	 * this method returns a list of all minute workitems for the current workitem.
	 * The workitem list is cached. Subclasses can overwrite the method
	 * loadWorkitems() to return a custom result set.
	 * 
	 * @return - list of file meta data objects
	 */
	public List<ItemCollection> getMinutes() {
		if (minutes == null) {
			minutes = loadWorkitems();
		}
		return minutes;

	}

	/**
	 * This method loads the minute worktiems. If one minute workitem has the result
	 * action = 'OPEN' then this minute workitem will be loaded automatically.
	 * 
	 * @return - sorted list of minutes
	 */
	List<ItemCollection> loadWorkitems() {

		logger.fine("load minute list...");
		List<ItemCollection> minutes = new ArrayList<ItemCollection>();

		if (parentWorkitem != null) {
			String uniqueIdRef = parentWorkitem.getItemValueString(WorkflowKernel.UNIQUEID);
			if (!uniqueIdRef.isEmpty()) {
				String sQuery = null;
				sQuery = "( (type:\"workitem\" OR type:\"childworkitem\" OR type:\"workitemarchive\" OR type:\"childworkitemarchive\") ";
				sQuery += " AND ($uniqueidref:\"" + uniqueIdRef + "\")) ";
				try {
					minutes = documentService.find(sQuery, 999, 0);
					// sort by numsequencenumber
					Collections.sort(minutes, new ItemCollectionComparator("numsequencenumber", true));
				} catch (QueryException e) {
					logger.warning("loadWorkitems - invalid query: " + e.getMessage());
				}
			}
		}

		// test if we should open one minute (from last to first)....
		for (int i = minutes.size(); i > 0; i--) {
			ItemCollection minute = minutes.get(i - 1);
			// String minuteID=null;
			String testSubject = minute.getItemValueString("action");
			if (testSubject.equalsIgnoreCase("open")) {
				setWorkitem(minute);
				break;
			}
		}

		return minutes;
	}

}