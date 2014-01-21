package org.imixs.marty.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.imixs.workflow.plugins.RulePlugin;

public class ErrorHandler {

	private static Logger logger = Logger.getLogger(ErrorHandler.class.getName());

	/**
	 * The Method expects a PluginException and adds the corresponding Faces
	 * Error Message into the FacesContext.
	 * 
	 * If the PluginException was thrown from the RulePLugin then the method
	 * test this exception for ErrorParams and generate separate Faces Error
	 * Messages for each param.
	 * */
	public static void handlePluginException(PluginException pe) {
		// if the PluginException was throws from the RulePlugin then test
		// for VALIDATION_ERROR and ErrorParams
		if (RulePlugin.class.getName().equals(pe.getErrorContext())
				&& (RulePlugin.VALIDATION_ERROR.equals(pe.getErrorCode()))
				&& pe.getErrorParameters() != null
				&& pe.getErrorParameters().length > 0) {

			// create a faces messae for each param
			Object[] messages = pe.getErrorParameters();
			for (Object aMessage : messages) {

				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, aMessage
								.toString(), null));
			}
		} else {
			// default behaivor
			addErrorMessage(pe);
		}

		logger.warning("WorkflowController cauth PluginException - error code="
				+ pe.getErrorCode() + " - " + pe.getMessage());
		if (logger.isLoggable(Level.FINE)) {

			pe.printStackTrace(); // Or use a logger.
		}
	}
	
	
	/**
	 * This helper method adds a error message to the faces context, based on
	 * the data in a PluginException. This kind of error message can be
	 * displayed in a page using:
	 * 
	 * <code>
	 *          	<h:messages globalOnly="true" />
	 * </code>
	 * 
	 * As the ProcessingErrorException contains an optional object array the
	 * message is parsed for params to be replaced
	 * 
	 * Example:
	 * 
	 * <code>
	 * ERROR_MESSAGE=Value should not be greater than {0} or lower as {1}.
	 * </code>
	 * 
	 * @param pe
	 */
	public static void addErrorMessage(WorkflowException pe) {

		String message = pe.getErrorCode();
		// try to find the message text in resource bundle...
		try {
			ResourceBundle rb = ResourceBundle.getBundle("bundle.app");
			message = rb.getString(pe.getErrorCode());
		} catch (MissingResourceException mre) {
			logger.warning("WorkflowController: " + mre.getMessage());
		}

		// parse message for params
		if (pe instanceof PluginException) {
			PluginException p = (PluginException) pe;
			if (p.getErrorParameters() != null
					&& p.getErrorParameters().length > 0) {
				for (int i = 0; i < p.getErrorParameters().length; i++) {
					message = message.replace("{" + i + "}",
							p.getErrorParameters()[i].toString());
				}
			}
		}
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));

	}


}
