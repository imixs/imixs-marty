package org.imixs.marty.util;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.workflow.WorkflowController;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * This class is to avoid history back button in browser by discarding the
 * browser cache. Also the controller detects multible Browser Tabs/Windows and
 * avoids posting wrong/deprecated workitems
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
	 * Returns a unique increasing id for each request. The id is stored in the
	 * workitemController
	 * 
	 * @return
	 */
	public long getBrowserWindowID() {
		long cacheID = System.currentTimeMillis();

		if (workflowController.getWorkitem() != null) {
			workflowController.getWorkitem().replaceItemValue(
					"_cachedBrowserWindowID", cacheID + "");
		}
		return cacheID;
	}

	/**
	 * Verifies the _browserWindowID before saving or processing a workitem.
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException, PluginException {
		if (workflowEvent == null || workflowEvent.getWorkitem()==null)
			return;

		
		int eventType = workflowEvent.getEventType();

		if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == eventType
				|| WorkflowEvent.WORKITEM_BEFORE_SAVE == eventType) {

			long browserID = Long.parseLong(workflowEvent.getWorkitem()
					.getItemValueString("_browserWindowID"));
			long cachedBrowserID = Long.parseLong(workflowEvent.getWorkitem()
					.getItemValueString("_cachedBrowserWindowID"));

			logger.fine("[BrowserCacheController] - _browserWindowID="
					+ browserID);
			logger.fine("[BrowserCacheController] - _cachedBrowserWindowID="
					+ cachedBrowserID);

			if (browserID != cachedBrowserID) {
				throw new PluginException(
						BrowserCacheController.class.getSimpleName(),
						BROWSER_DATA_INVALID,
						"[BrowserCacheController] Browser Window contains invalid data! ");

			}
			// remove the cacheIds from the workItem which are no longer needed
			workflowEvent.getWorkitem().removeItem("_browserWindowID");
			workflowEvent.getWorkitem().removeItem("_cachedBrowserWindowID");
		}
	}
}
