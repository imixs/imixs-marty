package org.imixs.marty.web.util;

import java.util.ArrayList;
import java.util.Iterator;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

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
public class FileUploadBean {
	private ArrayList<String> filesUploaded = new ArrayList<String>();

	private int maxAttachments = 10;
	private boolean autoUpload = true;
	private boolean useFlash = false;
	private String fileName;

	private BLOBWorkitemController workitemBlobMB = null;

	public FileUploadBean() {
	}

	public void reset() {
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
	 * adds a uploaded file into the blobBean
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void listener(UploadEvent event) throws Exception {
		UploadItem item = event.getUploadItem();

		this.getWorkitemBlobBean().addFile(item.getData(), item.getFileName(),
				item.getContentType());

		filesUploaded.add(item.getFileName());
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
			}
			else {
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
		
		System.out.println("getUploadsAvailable="+(maxAttachments - this.getWorkitemBlobBean().getFiles().length));
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

	private BLOBWorkitemController getWorkitemBlobBean() {
		if (workitemBlobMB == null)
			workitemBlobMB = (BLOBWorkitemController) FacesContext
					.getCurrentInstance().getApplication().getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemBlobMB");

		return workitemBlobMB;

	}
}
