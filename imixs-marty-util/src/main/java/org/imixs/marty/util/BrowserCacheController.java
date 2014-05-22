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
 * Usage:
 * 
 * <code>
 * 		<h:form>
 * 			<marty:cacheControl />
 * 			.....
 * 		</h:form>
 * </code>
 * 
 * @author rsoika
 * 
 */
@Named("browserCacheController")
@RequestScoped
public class BrowserCacheController implements Serializable {
	public static String BROWSER_DATA_INVALID = "BROWSER_DATA_INVALID";

	public static String CACHE_UNIQUEID = "_cached_uniqueid";
	public static String CACHE_MODIFIED = "_cached_modified";
	public static String CACHE_SESSIONID = "_cached_sessionid";

	// request cache values
	private String cachedSessionid = null;
	private String cachedUniqueid = null;
	private String cachedModified = null;

	@Inject
	protected WorkflowController workflowController;

	private static Logger logger = Logger
			.getLogger(BrowserCacheController.class.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * This method changes the response header and disables any browser cache.
	 * This disables a browser history back navigation.
	 * 
	 */
	public void clearCache() {
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
	 * The cacheId is used to identify the workItem independent from its
	 * modified date or unqiueId
	 * 
	 * @return
	 */

	public String getCachedSessionid() {
		cachedSessionid = "";
		if (workflowController.getWorkitem() != null) {
			cachedSessionid = workflowController.getWorkitem()
					.getItemValueString(CACHE_SESSIONID);
			if ("".equals(cachedSessionid)) {
				// compute id
				cachedSessionid = "" + System.currentTimeMillis();
				workflowController.getWorkitem().replaceItemValue(
						CACHE_SESSIONID, cachedSessionid);
			}
		}
		return cachedSessionid;
	}

	public void setCachedSessionid(String cachedSessionid) {
		this.cachedSessionid = cachedSessionid;
	}

	public String getCachedUniqueid() {
		cachedUniqueid = "";
		if (workflowController.getWorkitem() != null) {
			cachedUniqueid = workflowController.getWorkitem()
					.getItemValueString(WorkflowService.UNIQUEID);
		}
		return cachedUniqueid;
	}

	public void setCachedUniqueid(String cachedUniqueid) {
		this.cachedUniqueid = cachedUniqueid;
	}

	public String getCachedModified() {
		cachedModified = "";
		if (workflowController.getWorkitem() != null) {
			cachedModified = workflowController.getWorkitem()
					.getItemValueString("$modified");
		}
		return cachedModified;
	}

	public void setCachedModified(String cachedModified) {
		this.cachedModified = cachedModified;
	}

	/**
	 * Phase Listener for PhaseId=APPLY_REQUEST_VALUES
	 * 
	 * The method verifies if the posted workitem data matches the backend data
	 * of the current workitem. If the uniqueId is not equal the method will
	 * reload the backend data. Finally the modified date will be compared with
	 * the backenddata. In case that the date did not match an attribute
	 * '_invalidRequestID' will be added. This attribute is checked in the
	 * onWorkflowEvents 'WORKITEM_BEFORE_PROCESS' and 'WORKITEM_BEFORE_SAVE'.
	 * 
	 * @see method: onWorkflowEvent
	 * @throws PluginException
	 */
	public void beforePhase(PhaseEvent event) throws PluginException {
		String requestUniqueid = null;
		String requestModified = null;
		String backendUniqueid = null;
		String backendModified = null;
		String backendCacheid = null;
		String requestCacheid = null;

		// only run during APPLY_REQUEST_VALUES phase
		if (event.getPhaseId() == PhaseId.APPLY_REQUEST_VALUES) {
			logger.fine("[BrowserCacheController] Before phase: "
					+ event.getPhaseId());

			// analyze request data....
			FacesContext fc = FacesContext.getCurrentInstance();
			Map<String, String> requestValues = fc.getExternalContext()
					.getRequestParameterMap();
			for (String key : requestValues.keySet()) {

				if (key.endsWith(":" + CACHE_UNIQUEID)) {
					requestUniqueid = requestValues.get(key);

					logger.fine("[BrowserCacheController] Apply Value for UnqiueID="
							+ requestUniqueid);
				}
				if (key.endsWith(":" + CACHE_MODIFIED)) {
					requestModified = requestValues.get(key);
					logger.fine("[BrowserCacheController] Apply Value for modified="
							+ requestModified);
				}

				if (key.endsWith(":" + CACHE_SESSIONID)) {
					requestCacheid = requestValues.get(key);
					logger.fine("[BrowserCacheController] Apply Value for cacheid="
							+ requestCacheid);
				}

				if (requestUniqueid != null && requestModified != null
						&& requestCacheid != null)
					break;
			}

			if (requestUniqueid == null && requestModified == null
					&& requestCacheid == null) {
				logger.fine("[BrowserCacheController] no request data found");
				return;
			}

			// analyze backend data....
			if (workflowController.getWorkitem() != null) {
				backendUniqueid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.UNIQUEID);
				backendModified = workflowController.getWorkitem()
						.getItemValueString("$modified");
				backendCacheid = workflowController.getWorkitem()
						.getItemValueString(CACHE_SESSIONID);
			}

			if (requestUniqueid == null)
				requestUniqueid = "";

			if (backendUniqueid == null)
				backendUniqueid = "";

			if (requestUniqueid.isEmpty() && backendUniqueid.isEmpty()) {
				logger.fine("[BrowserCacheController] no request data found");
				return;
			}

			if (requestUniqueid.isEmpty() && !backendUniqueid.isEmpty()) {
				if (backendCacheid.equals(requestCacheid)) {
					requestUniqueid = backendUniqueid;
					requestModified = backendModified;

					logger.info("[BrowserCacheController] update request data: "
							+ backendUniqueid);
				} else {
					if (workflowController.getWorkitem() != null)
						workflowController.getWorkitem().replaceItemValue(
								"_invalidRequestID", backendModified);

					throw new PluginException(
							BrowserCacheController.class.getSimpleName(),
							BROWSER_DATA_INVALID,
							"[BrowserCacheController] Browser Window contains invalid data: no backend data found.");
				}
			}

			// verify backend workitem
			if (workflowController.getWorkitem() == null
					|| !requestUniqueid.equals(backendUniqueid)) {
				logger.info("[BrowserCacheController] refresh workitem: "
						+ requestUniqueid);

				workflowController.load(requestUniqueid);

				// if null - throw a PluginException (e.g no access)
				if (workflowController.getWorkitem() == null) {
					throw new PluginException(
							BrowserCacheController.class.getSimpleName(),
							BROWSER_DATA_INVALID,
							"[BrowserCacheController] Browser Window contains invalid data: no backend data found.");
				}

				// update new process data...
				backendUniqueid = workflowController.getWorkitem()
						.getItemValueString(WorkflowService.UNIQUEID);
				backendModified = workflowController.getWorkitem()
						.getItemValueString("$modified");
			}

			// finally verify the modified date....

			if (!backendModified.equals(requestModified)) {
				logger.warning("[BrowserCacheController] invalid browser data - modified: "
						+ requestModified + " - expected: " + backendModified);

				workflowController.getWorkitem().replaceItemValue(
						"_invalidRequestID", requestModified);
			}

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

			// remove request fields
			workflowEvent.getWorkitem().removeItem("_invalidRequestID");
			// workflowEvent.getWorkitem().removeItem(CACHE_ID);

			if (!requestdata.isEmpty()) {
				// remove _invalidRequestID which is no longer needed
				throw new PluginException(
						BrowserCacheController.class.getSimpleName(),
						BROWSER_DATA_INVALID,
						"[BrowserCacheController] Browser Window contains invalid data: "
								+ requestdata);
			}
		}
	}

}
