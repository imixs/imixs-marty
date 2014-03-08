package org.imixs.marty.util;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.workflow.WorkflowController;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * This class is used by the cacheControl.xhtml facelet to avoid history back
 * navigation and posting wrong or deprecated data.
 * 
 * 
 * @author rsoika
 * 
 */
@Named("browserCacheController")
@RequestScoped
public class BrowserCacheController implements Serializable {
	public static String BROWSER_DATA_INVALID = "BROWSER_DATA_INVALID";

	@Inject
	protected WorkflowController workflowController;

	private static Logger logger = Logger
			.getLogger(BrowserCacheController.class.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * This method changes the response header and set the browser cash to 0.
	 * This disables a browser history back navigation.
	 * 
	 * Can be included into a jsf page:
	 * 
	 * <code>
	 * 
		<f:view>
			<f:event type="preRenderView" listener="#{browserCacheController.clearCache}" />
		   ....
	 * 
	 * </code>
	 * 
	 */
	public void clearCache() {
		// Do here your job which should run right before the RENDER_RESPONSE.
		if (workflowController.getWorkitem() != null) {
			logger.fine("[BrowserCacheController] clear cache-control for: "
					+ workflowController.getWorkitem().getItemValueString(
							WorkflowService.UNIQUEID));
			// clear cache
			ExternalContext context = FacesContext.getCurrentInstance()
					.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) context
					.getResponse();
			response.setHeader("Cache-Control",
					"no-cache, no-store, must-revalidate"); // HTTP 1.1.
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			response.setDateHeader("Expires", 0); // Proxies.

		}
	}

	/**
	 * Verifies the attribute '_invalidRequestID' created during the
	 * phaseListener phase 'APPLY_REQUEST_VALUES'.
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException, PluginException {
		if (workflowEvent == null || workflowEvent.getWorkitem() == null)
			return;

		int eventType = workflowEvent.getEventType();
		if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == eventType
				|| WorkflowEvent.WORKITEM_BEFORE_SAVE == eventType) {

			String requestdata = workflowEvent.getWorkitem()
					.getItemValueString("_invalidRequestID");
			if (!requestdata.isEmpty()) {
				// remove _invalidRequestID which is no longer needed
				workflowEvent.getWorkitem().removeItem("_invalidRequestID");
				throw new PluginException(
						BrowserCacheController.class.getSimpleName(),
						BROWSER_DATA_INVALID,
						"[BrowserCacheController] Browser Window contains invalid data: "
								+ requestdata);
			}
		}
	}

	/**
	 * Phase Listener for PhaseId=APPLY_REQUEST_VALUES
	 * 
	 * The method verifies if the posted workitem data matches the backend data
	 * of the current workitem. If the uniqueId is not equal the method will
	 * reload the backend data. Finally the processData ($modelVersion,
	 * $ProcessID) will be verified. In case that the data can not be verified,
	 * an attribute '_invalidRequestID' will be added. This attribute is checked
	 * in the onWorkflowEvent method.
	 * 
	 * <code>
	 *  <f:view beforePhase="#{browserCacheController.beforePhase}">
	 * 	...
	 * 	</f:view>
	 * </code>
	 * 
	 * @throws PluginException
	 */
	public void beforePhase(PhaseEvent event) throws PluginException {
		String requestUniqueid = null;
		String requestModelversion = null;
		String requestProcessid = null;
		String backendUniqueid = null;
		String backendProcessid = null;
		String backendModelversion = null;

		if (event.getPhaseId() == PhaseId.APPLY_REQUEST_VALUES) {
			logger.fine("[BrowserCacheController] Before phase: "
					+ event.getPhaseId());

			// analyze request data....
			FacesContext fc = FacesContext.getCurrentInstance();
			Map<String, String> requestValues = fc.getExternalContext()
					.getRequestParameterMap();
			for (String key : requestValues.keySet()) {

				if (key.endsWith(":uniqueID")) {
					requestUniqueid = requestValues.get(key);
					logger.fine("[BrowserCacheController] Apply Value for UnqiueID="
							+ requestUniqueid);
				}
				if (key.endsWith(":modelversionID")) {
					requestModelversion = requestValues.get(key);
					logger.fine("[BrowserCacheController] Apply Value for modelversionID="
							+ requestModelversion);
				}
				if (key.endsWith(":processID")) {
					requestProcessid = requestValues.get(key);
					logger.fine("[BrowserCacheController] Apply Value for processID="
							+ requestProcessid);
				}

			}

			if (requestUniqueid == null) {
				logger.fine("[BrowserCacheController] no request data found");
				return;
			}

			// analyze backend data....
			if (workflowController.getWorkitem() != null) {
				backendUniqueid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.UNIQUEID);
				backendProcessid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.PROCESSID);
				backendModelversion = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.MODELVERSION);
			}

			// verify backend workitem
			if (workflowController.getWorkitem() == null
					|| !requestUniqueid.equals(backendUniqueid)) {
				logger.info("[BrowserCacheController] refresh workitem: "
						+ requestUniqueid);

				workflowController.load(requestUniqueid);

				// if null - create empty workitem and set error (e.g no access)
				if (workflowController.getWorkitem() == null) {
					throw new PluginException(
							BrowserCacheController.class.getSimpleName(),
							BROWSER_DATA_INVALID,
							"[BrowserCacheController] Browser Window contains invalid data: no backend data found.");
				}

				// update new process data...
				backendUniqueid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.UNIQUEID);
				backendProcessid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.PROCESSID);
				backendModelversion = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.MODELVERSION);
			}

			// finally verify the process data....
			String requestID = "" + requestUniqueid + requestModelversion
					+ requestProcessid;
			String backendID = "" + backendUniqueid + backendModelversion
					+ backendProcessid;
			if (!requestID.equals(backendID)) {
				logger.warning("[BrowserCacheController] invalid browser data: "
						+ requestID + " - expected: " + backendID);

				workflowController.getWorkitem().replaceItemValue(
						"_invalidRequestID", requestID);
			}

		}
	}
}
