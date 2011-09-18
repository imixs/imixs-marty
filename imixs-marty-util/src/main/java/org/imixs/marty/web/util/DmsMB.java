package org.imixs.marty.web.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.util.LoginMB;
import org.imixs.marty.web.workitem.WorklistMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.richfaces.event.UploadEvent;

/**
 * This Bean acts a a front controller for the DMS feature. The bean supports
 * methods to control the DMSService and also to manage the DMSWorkitemList.
 * 
 * In addition this Bean extends the fileUploadBean and implements a
 * lazy-loading mechanism for the blobWorkitem. In different to the
 * FileUploadBean a blobWorkitem will only be loaded if a new file needs to be
 * added or removed. The information of existing files is stored in the property
 * dms from the parent workItem.
 * 
 * @see org.imixs.marty.web.util.FileUploadBean
 * @author rsoika
 * 
 */
public class DmsMB extends FileUploadBean {

	private ItemCollection configItemCollection = null;
	private ItemCollection dmsItemCollection = null;
	private ArrayList<ItemCollection> workitems = null;
	private int count = 10;
	private int row = 0;
	private boolean endOfList = false;
	private boolean blobWorkitemLoaded = false; // indicate that the
												// blobWorkitem still was not
												// loaded after a
												// workitemChanged event
	private boolean assignDMSWorkitem = false; // indicate that the current
												// DMSWorkitem should be
												// assigned to the next workitem
												// after a workitemChangeed
												// event
	private boolean removeDMWWorkitem = false; // indicate that the dmsWorkitem
												// was assigned to the current
												// workitem and can be removed
												// on the onProcessCompleted
												// event

	/* Backing Beans */
	private LoginMB loginMB = null;
	private WorklistMB worklistMB = null;

	/* EJBs */
	@EJB
	org.imixs.marty.dms.DmsSchedulerService dmsSchedulerService;
	@EJB
	EntityService entityService;
	@EJB
	WorkflowService workflowService;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public DmsMB() {
		super();
	}

	/**
	 * This method tries to load the config entity. If no Entity exists than the
	 * method creates a new entity
	 * 
	 * */
	@PostConstruct
	public void init() {
		super.init();
		doLoadConfiguration(null);
	}

	/**
	 * returns the configuration workitem.
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		return this.configItemCollection;
	}

	/**
	 * returns the current DMSWorkitem
	 * 
	 * @return
	 */
	public ItemCollection getWorkitem() {
		return this.dmsItemCollection;
	}

	/**
	 * save method tries to load the config entity. if not availabe. the method
	 * will create the entity the first time
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doSaveConfiguration(ActionEvent event) throws Exception {
		// save entity
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);

	}

	/**
	 * This method reloads the configuration entity for the dms scheduler
	 * service
	 */
	public void doLoadConfiguration(ActionEvent event) {
		configItemCollection = dmsSchedulerService.findConfiguration();
	}

	/**
	 * starts the timer service
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doStartScheduler(ActionEvent event) throws Exception {
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = dmsSchedulerService.start();
	}

	public void doStopScheduler(ActionEvent event) throws Exception {
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = dmsSchedulerService.stop();
	}

	public void doRestartScheduler(ActionEvent event) throws Exception {
		doStopScheduler(event);
		doStartScheduler(event);
	}

	/**
	 * This Method Selects the current dms object. This dms workitem can be
	 * displayed in a form or assigned to a new process
	 * 
	 * @return
	 */
	public void doEdit(ActionEvent event) {

		if (event == null) {
			return;
		}

		// find current data row....
		UIComponent component = event.getComponent();

		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {

			if (!(parent instanceof UIData))
				continue;

			try {
				// get current project from row
				dmsItemCollection = (ItemCollection) ((UIData) parent)
						.getRowData();
				break;
			} catch (Exception e) {
				// unable to select data
			}
		}

	}

	/**
	 * selects the workitem for a suggestion box and opens the corresponding
	 * process. A parameter 'id' with the $uniqueid is expected
	 * 
	 * Later the dmsItemCollection will be assigend to this workitem - @see
	 * onWorkitemProcess
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doAssignWorkitem(ActionEvent event) throws Exception {
		logger.info("dmsMB: doAssignWorkitem....");
		assignDMSWorkitem = true;
		getworkListMB().doEdit(event);
	}

	public void doAssignNewWorkitem(ActionEvent event) throws Exception {
		logger.info("dmsMB: doAssignNewWorkitem....");
		assignDMSWorkitem = true;
		getworkListMB().getWorkitemMB().doCreateWorkitem(event);
	}

	public List<ItemCollection> getWorkitems() {
		if (workitems == null)
			loadDMSWorkitems();
		return workitems;

	}

	/**
	 * resets the current worklist list
	 * 
	 * @return
	 */
	public void doReset(ActionEvent event) {
		workitems = null;
		row = 0;
	}

	public void doLoadNext(ActionEvent event) {
		row = row + count;
		workitems = null;
	}

	public void doLoadPrev(ActionEvent event) {
		row = row - count;
		if (row < 0)
			row = 0;
		workitems = null;
	}

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	/**
	 * Loads the dms list
	 * 
	 * 
	 * 
	 * @return
	 */
	private List<ItemCollection> loadDMSWorkitems() {
		try {
			workitems = new ArrayList<ItemCollection>();
			String sQuery = "SELECT entity FROM Entity entity "
					+ " JOIN entity.textItems AS r"
					+ "  WHERE entity.type='workitemlob'"
					+ "  AND r.itemName = '$uniqueidref'"
					+ "  AND r.itemValue = '' "
					+ "ORDER BY entity.created DESC";

			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, row, count);

			for (ItemCollection aworkitem : col) {
				workitems.add(aworkitem);
			}
			endOfList = col.size() < count;

		} catch (Exception ee) {

			ee.printStackTrace();
		}
		return workitems;
	}

	/**
	 * Overwrite FileUploadBean and implement the lazyLoading mechanism...
	 * 
	 * @param event
	 * @throws Exception
	 */
	@Override
	public void listener(UploadEvent event) throws Exception {
		doLazyLoading();
		super.listener(event);
	}

	/**
	 * Overwrite FileUploadBean and implement the lazyLoading mechanism...
	 */
	@Override
	public void doDeleteFile(ActionEvent event) throws Exception {
		doLazyLoading();
		super.doDeleteFile(event);

		// remove the file also from the current dms property...
		List children = event.getComponent().getChildren();
		String sFileName = "";
		// find the file name....
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
		// try to remove the file metadata form the dms...
		if (sFileName != null && !"".equals(sFileName))
			removeMetadata(sFileName);

	}

	/**
	 * This method updates the property dms from the current BlobWorkitem. The
	 * dms holds a List of meta data for each attached file. If a file was
	 * removed also the meta data will be removed.
	 * 
	 * The method only runs if a new file was added or an existing file was
	 * removed.
	 * 
	 * The DMS meta data includes also the referece (uniqueid) to the blob
	 * workitem. If the blobWorkitem was genereated new no $uniqueid still
	 * exists. In this case we generate a new $unqiueid for the blobWorkitem in
	 * this method.
	 * 
	 */
	@Override
	public void onWorkitemProcess(ItemCollection aworkitem) {
		// if a blobWorkitem was just created the blobWorkitem has no
		// $uniqueid.
		// But this ID is necessary to build the meta data for the parent
		// workitem.
		// This is the reason why we set a uniqueid explicit here.

		String sUnidRef = getWorkitemBlobBean().getWorkitem()
				.getItemValueString("$UniqueID");
		if ("".equals(sUnidRef)) {
			// generate a $uniqueid....
			try {
				getWorkitemBlobBean().getWorkitem().replaceItemValue(
						"$UniqueID", WorkflowKernel.generateUniqueID());
				blobWorkitemLoaded = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (blobWorkitemLoaded == false)
			// no changes - exit!
			return;
		else {
			// update the dms property....
			updateDmsMetaData(aworkitem);
		}
	}


	/**
	 * remove dmsWorkitem if it was assigned before
	 */
	@Override
	public void onWorkitemProcessCompleted(ItemCollection aworkitem) {
		super.onWorkitemProcessCompleted(aworkitem);
		
		try {
			if (removeDMWWorkitem==true && dmsItemCollection!=null) {
				logger.info("DmsMB - removing dmsItemCollection.....");
				entityService.remove(dmsItemCollection);
				removeDMWWorkitem=false;
				
				doReset(null);
				
			}
			
		} catch (Exception e) {

			e.printStackTrace();
		}

	}
	
	/**
	 * Avoid preloading the blobworkitem on a workitemchanged event
	 * 
	 */
	@Override
	public void onWorkitemChanged(ItemCollection aworkitem) {
		blobWorkitemLoaded = false;
		removeDMWWorkitem = false;

		// check if dms files need to be copied...
		if (assignDMSWorkitem == true) {
			// copy file into wokritem....
			logger.info("onWorkitemChanged Jetzt muß ich das dms zuweisen....");

			doLazyLoading();

			copyDmsFiles();

			assignDMSWorkitem = false;
			// indicate that the dmsWorkitem should be removed after
			// onProcessCompleted
			removeDMWWorkitem = true;

		}

		// test if dms property still exists - if not create a new one
		if (!getWorkitemBean().getWorkitem().hasItem("dms")) {
			doLazyLoading();
			// create the dms property....
			updateDmsMetaData(aworkitem);
		}

		// reset the file upload list
		resetFileUpload();
	}

	/**
	 * Overwrite onWorkitemCreated to avoid clearing the BlobWorkitem which was
	 * just created from the onWorkitemChanged method.
	 */
	@Override
	public void onWorkitemCreated(ItemCollection e) {
		// do not call the super method!
		// super.onWorkitemCreated(e);
		logger.info("dms onWorkitemCreated ...");

	}

	/**
	 * This method implements the lazyLoading mechanism for the BlogWOrkitem.
	 * The BlobWorkitem will only be loaded if the flag 'lazyloading' is true.
	 * After the method call the flag will be cleared again.
	 * 
	 */
	private void doLazyLoading() {
		if (!blobWorkitemLoaded) {
			super.onWorkitemChanged(this.getWorkitemBean().getWorkitem());
			// clear flag
			blobWorkitemLoaded = true;
		}
	}

	/**
	 * this Method copies the files form the current dmsItemCollection into the
	 * current BlobWorkitem
	 */
	private void copyDmsFiles() {
		Map fileMapDMS = null;
		Map fileMapBlob = null;

		doLazyLoading();

		// get the $file map from the Blob Workitem....
		Vector vFiles = getWorkitemBlobBean().getWorkitem().getItemValue(
				"$file");
		if (vFiles != null && vFiles.size() > 0)
			fileMapBlob = (HashMap) vFiles.elementAt(0);
		else
			fileMapBlob = new HashMap();

		// now get the $file map from the DMS Workitem...
		vFiles = dmsItemCollection.getItemValue("$file");
		if (vFiles != null && vFiles.size() > 0)
			fileMapDMS = (HashMap) vFiles.elementAt(0);
		else
			fileMapDMS = new HashMap();

		// next copy all file entry from the dmsMap into the BlobMap...

		// now we copy each element from the Map into the current
		// BlobWorkitem...
		Iterator it = fileMapDMS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			fileMapBlob.put(pairs.getKey(), pairs.getValue());
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}

		// finally replace the $file property
		// and update the meta data...
		try {
			getWorkitemBlobBean().getWorkitem().replaceItemValue("$file",
					fileMapBlob);

			updateDmsMetaData(this.getWorkitemBean().getWorkitem());
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	/**
	 * This method updates the property dms from the current Blobworkitem. The
	 * dms holds a List of Maps with the meta data for each file. The standard
	 * property of the meta data is the field txtName holding the file name. All
	 * other properties are optional. If a file was removed also the meta data
	 * will be removed
	 * 
	 * The Meta data also includes the $uniquidRef property which points to the
	 * workitem holding the attachment. <br>
	 * <br>
	 * Situation assigneNewProcess: <br>
	 * If a new DmsWorktitem was currently assigned and the current workitem
	 * still have no uniqueid (because the main workitem was not still
	 * processed) the $uniuqidRef points to the dmsworkitem. This need to be
	 * updated during the processWorkitem method
	 * 
	 */
	private void updateDmsMetaData(ItemCollection aworkitem) {
		try {
			// get the current UniqueidRef
			String sUnidRef = getWorkitemBlobBean().getWorkitem()
					.getItemValueString("$UniqueID");
			if ("".equals(sUnidRef) && dmsItemCollection != null) {
				// no id still exists - so take the dms id if exists....
				sUnidRef = dmsItemCollection.getItemValueString("$UniqueID");
			}

			// get the current file list
			String[] fileNames = getWorkitemBlobBean().getFiles();

			// get the current dms value....
			Vector<Map> vDMSnew = new Vector<Map>();

			// now we test if for each file entry a dms meta data entry still
			// exists
			for (String aFilename : fileNames) {

				// test filename already exists
				ItemCollection itemCol = findMetadata(aFilename);
				if (itemCol != null) {
					// the meta data exists....
					// update the $ref....
					itemCol.replaceItemValue("$uniqueidRef", sUnidRef);
					vDMSnew.add(itemCol.getAllItems());

				} else {
					// no meta data exists.... create a new meta object
					ItemCollection aMetadata = new ItemCollection();

					aMetadata.replaceItemValue("txtname", aFilename);
					aMetadata.replaceItemValue("$uniqueidRef", sUnidRef);
					aMetadata.replaceItemValue("$created", new Date());
					aMetadata.replaceItemValue("namCreator", this
							.getLoginBean().getUserPrincipal());

					// Important! do only store the Map not the ItemCollection!!
					vDMSnew.add(aMetadata.getAllItems());
				}

			}
			// update dms property....
			aworkitem.replaceItemValue("dms", vDMSnew);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method returns a list of files meta data
	 * 
	 * @return
	 */
	public List<ItemCollection> getFileList() {

		List<Map> vDMS = this.getWorkitemBean().getWorkitem()
				.getItemValue("dms");

		List<ItemCollection> filelist = new ArrayList<ItemCollection>();
		try {

			for (Map aMetadata : vDMS) {

				ItemCollection itemCol;
				// itemCol = new ItemCollection(aMetadata);
				itemCol = new ItemCollection();
				itemCol.setAllItems(aMetadata);
				filelist.add(itemCol);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return filelist;
	}

	/**
	 * This method returns the meta data of a specific file in the exiting list.
	 * 
	 * @return
	 */
	private ItemCollection findMetadata(String aFilename) {

		List<Map> vDMS = this.getWorkitemBean().getWorkitem()
				.getItemValue("dms");

		try {

			for (Map aMetadata : vDMS) {

				ItemCollection itemCol;
				itemCol = new ItemCollection(aMetadata);

				// test if filename matches...
				String sName = itemCol.getItemValueString("txtName");
				if (sName.equals(aFilename))
					return itemCol;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// no matching meta data found!
		return null;
	}

	/**
	 * This method removes the meta data of a specific file in the exiting list.
	 * 
	 * @return
	 */
	private void removeMetadata(String aFilename) {
		try {
			List<Map> vDMS = this.getWorkitemBean().getWorkitem()
					.getItemValue("dms");

			for (int i = 0; i < vDMS.size(); i++) {
				Map aMetadata = vDMS.get(i);
				ItemCollection itemCol;
				itemCol = new ItemCollection(aMetadata);
				if (itemCol.getItemValueString("txtName").equals(aFilename)) {
					vDMS.remove(i);
					break;
				}

			}

			this.getWorkitemBean().getWorkitem().replaceItemValue("dms", vDMS);
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	private LoginMB getLoginBean() {
		if (loginMB == null)
			loginMB = (LoginMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "loginMB");
		return loginMB;
	}

	private WorklistMB getworkListMB() {
		// get WorklistMB instance
		if (worklistMB == null)
			worklistMB = (WorklistMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "worklistMB");

		return worklistMB;
	}

}
