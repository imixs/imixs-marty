package org.imixs.marty.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.web.workitem.WorkitemListener;
import org.imixs.marty.web.workitem.WorkitemMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.BLOBWorkitemController;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

/**
 * This bean is a RichFaces helper bean to handle fileUpdloads managed by the
 * richFaces FileUpload component. A File upload commponent can be placed into a
 * jsf page with the following code:
 * 
 * <code>
 * <rich:fileUpload fileUploadListener="#{fileUploadBean.listener}" listHeight="100px"					
		                maxFilesQuantity="#{fileUploadBean.uploadsAvailable}"
		                id="upload"
		                immediateUpload="#{fileUploadBean.autoUpload}"
		                acceptedTypes="jpg, gif, png, bmp" allowFlash="#{fileUploadBean.useFlash}">
		                <a4j:support event="onuploadcomplete" reRender="info" />
		            </rich:fileUpload>
 *  
 * </code>
 * 
 * @author rsoika
 * 
 */
public class FileUploadBean implements WorkitemListener {
	private ArrayList<String> filesUploaded = new ArrayList<String>();

	private int maxAttachments = 10;
	private boolean autoUpload = true;
	private boolean useFlash = false;
	private String fileName;

	/* Backing Beans */
	private WorkitemMB workitemMB = null;
	private BLOBWorkitemController workitemLobMB = null;

	public FileUploadBean() {
	}

	/**
	 * This method registers the workitemListener
	 * 
	 * */
	@PostConstruct
	public void init() {
		// register listener
		getWorkitemBean().addWorkitemListener(this);

		// try to update blobMB with the current workitem
		onWorkitemChanged(getWorkitemBean().getWorkitem());
	}

	public void resetFileUpload() {
		filesUploaded = new ArrayList<String>();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		System.out.println("Filname is updates:" + fileName);
		this.fileName = fileName;
	}

	/**
	 * just a wrapper method to get direct access to the current filelist
	 * 
	 * @return
	 */
	public String[] getFiles() {
		return this.getWorkitemBlobBean().getFiles();
	}

	/**
	 * adds a uploaded file into the blobBean
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void listener(UploadEvent event) throws Exception {
		UploadItem item = event.getUploadItem();

		getWorkitemBlobBean().addFile(item.getData(), item.getFileName(),
				item.getContentType());

		filesUploaded.add(item.getFileName());
	}

	/**
	 * delete a attachment
	 */
	public void doDeleteFile(ActionEvent event) throws Exception {
		// Find selected filename...
		List children = event.getComponent().getChildren();
		String sFileName = "";

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("filename")
						&& currentParam.getValue() != null) {
					// Value can be provided as String or Integer Object
					sFileName = currentParam.getValue().toString();
					break;
				}
			}
		}

		if (sFileName != null && !"".equals(sFileName)) {
			// getWorkitemBlobBean().load(getWorkitem());
			getWorkitemBlobBean().removeFile(sFileName);
		}
	}

	/**
	 * clears on file from the actual uploaded (but not yet saved) files. The
	 * current selected filename is assigned by an a4j:actionparam to the
	 * property 'fileName'; If filename==null all uploaded files will be removed
	 * 
	 * @see https://community.jboss.org/message/537903#537903
	 * @return
	 */
	public void clearUploadData(ActionEvent event) {
		try {
			// test if a single file was cleared....
			if (fileName != null && !"".equals(fileName)) {
				System.out.println("Removing single fileName=" + fileName);
				this.getWorkitemBlobBean().removeFile(fileName);
				filesUploaded.remove(fileName);
			} else {
				// remove all files form fileUploadBean...
				Iterator<String> iter = filesUploaded.iterator();
				System.out.println("Removing all files....");
				while (iter.hasNext()) {
					String s = iter.next();
					System.out.println("Removing fileName=" + s);
					this.getWorkitemBlobBean().removeFile(s);
				}
				filesUploaded.clear();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long getTimeStamp() {
		return System.currentTimeMillis();
	}

	/**
	 * Defines the maximum count of attachments for one workitem.
	 * 
	 * @param iMaxCount
	 */
	public void setMaxAttachmetns(int iMaxCount) {
		maxAttachments = iMaxCount;
	}

	/**
	 * compute the possible uploads available. = (maxAttachments - current
	 * attachmetns)
	 * 
	 * @return
	 */
	public int getUploadsAvailable() {

		System.out
				.println("getUploadsAvailable="
						+ (maxAttachments - this.getWorkitemBlobBean()
								.getFiles().length));
		return maxAttachments - this.getWorkitemBlobBean().getFiles().length;
	}

	public boolean isAutoUpload() {
		return autoUpload;
	}

	public void setAutoUpload(boolean autoUpload) {
		this.autoUpload = autoUpload;
	}

	public boolean isUseFlash() {
		return useFlash;
	}

	public void setUseFlash(boolean useFlash) {
		this.useFlash = useFlash;
	}

	@Override
	public void onWorkitemCreated(ItemCollection e) {
		this.getWorkitemBlobBean().clear();
	}

	@Override
	public void onWorkitemChanged(ItemCollection aworkitem) {
		// try to load the blobWorkitem...
		try {
			if (aworkitem == null
					|| "".equals(aworkitem.getItemValueString("$UniqueID"))) {
				// new wokitem - clear!
				this.getWorkitemBlobBean().clear();
			} else {
				// caching mechanism:
				// load lobWorkItem only if uniqueid changed since last
				// change....
				if (!aworkitem.getItemValueString("$UniqueID").equals(
						getWorkitemBlobBean().getWorkitem().getItemValueString(
								"$UniqueIDRef"))) {
					this.getWorkitemBlobBean().load(aworkitem);
				}
			}

		} catch (Exception e) {
			this.getWorkitemBlobBean().clear();
			e.printStackTrace();
		}

		resetFileUpload();
	}

	/**
	 * Save the BlobWorkitem content before processing. This is necessary to
	 * provide blobWorkitem informations to other ejb based plugins
	 */
	@Override
	public void onWorkitemProcess(ItemCollection aworkitem) {
		try {
			getWorkitemBlobBean().save(aworkitem);
			this.resetFileUpload();
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	/**
	 * Finally save the blobWorkite with the current access settings
	 */
	@Override
	public void onWorkitemProcessCompleted(ItemCollection aworkitem) {
		try {
			getWorkitemBlobBean().save(aworkitem);
			this.resetFileUpload();
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	@Override
	public void onWorkitemDelete(ItemCollection e) {
	}

	@Override
	public void onWorkitemDeleteCompleted() {
		this.resetFileUpload();
	}

	@Override
	public void onWorkitemSoftDelete(ItemCollection e) {
	}

	@Override
	public void onWorkitemSoftDeleteCompleted(ItemCollection e) {
	}

	@Override
	public void onChildProcess(ItemCollection e) {
	}

	@Override
	public void onChildProcessCompleted(ItemCollection e) {
	}

	@Override
	public void onChildCreated(ItemCollection e) {
	}

	@Override
	public void onChildDelete(ItemCollection e) {
	}

	@Override
	public void onChildDeleteCompleted() {
	}

	@Override
	public void onChildSoftDelete(ItemCollection e) {

	}

	@Override
	public void onChildSoftDeleteCompleted(ItemCollection e) {

	}

	public BLOBWorkitemController getWorkitemBlobBean() {
		if (workitemLobMB == null) {
			workitemLobMB = (BLOBWorkitemController) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemBlobMB");

		}
		return workitemLobMB;

	}

	public WorkitemMB getWorkitemBean() {
		if (workitemMB == null)
			workitemMB = (WorkitemMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemMB");
		return workitemMB;
	}
}
