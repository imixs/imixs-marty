/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.dms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * This EJB implements a TimerService which scans a configured file path for new
 * files to be imported into BlobWorkitems. Each file which is located in one of
 * the configured file paths will be added into the blobWorkitem . After the
 * successful import the file will be automatically removed from the file
 * system. So this ejb implements a kind of bach-import process.
 * 
 * 
 * The configuration of the timer is stored by this ejb through the method
 * saveConfiguration(); The configuration is stored as an entity from the type =
 * 'configuration' and the txtName = '"org.imixs.marty.dms.scheduler'.
 * 
 * 
 * 
 * 
 * @author rsoika
 * 
 */
@Stateless
@LocalBean
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class DmsSchedulerService {

	final static public String TYPE = "configuration";
	final static public String DMS_TYPE = "workitemlob";
	final static public String NAME = "org.imixs.marty.dms.scheduler";
	final static public String DROP_FOLDER = "imixs_dms";
	final static public long MAX_FILE_SIZE = 10485760; // 10 MB

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	@EJB
	private EntityService entityService;

	@Resource
	javax.ejb.TimerService timerService;

	@Resource
	SessionContext ctx;

	/**
	 * This method loads the current scheduler configuration. If still no
	 * configuration entity exists the method returns an empty ItemCollection
	 * 
	 * @return
	 */
	public ItemCollection findConfiguration() {
		ItemCollection configItemCollection = null;
		String sQuery = "SELECT config FROM Entity AS config "
				+ " JOIN config.textItems AS t2" + " WHERE config.type = '"
				+ TYPE + "'" + " AND t2.itemName = 'txtname'"
				+ " AND t2.itemValue = '" + NAME + "'"
				+ " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			configItemCollection = col.iterator().next();

		} else {
			// create default values
			configItemCollection = new ItemCollection();
			try {
				configItemCollection.replaceItemValue("type", TYPE);
				configItemCollection.replaceItemValue("txtname", NAME);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		updateTimerDetails(configItemCollection);

		return configItemCollection;
	}

	/**
	 * This method saves the timer configuration. The method ensures that the
	 * following properties are set to default.
	 * <ul>
	 * <li>type</li>
	 * <li>txtName</li>
	 * <li>$writeAccess</li>
	 * <li>$readAccess</li>
	 * </ul>
	 * 
	 * @return
	 * @throws Exception
	 */
	public ItemCollection saveConfiguration(ItemCollection configItemCollection)
			throws Exception {
		// update write and read access
		configItemCollection.replaceItemValue("type", TYPE);
		configItemCollection.replaceItemValue("txtName", NAME);
		configItemCollection.replaceItemValue("$writeAccess",
				"org.imixs.ACCESSLEVEL.MANAGERACCESS");
		configItemCollection.replaceItemValue("$readAccess",
				"org.imixs.ACCESSLEVEL.MANAGERACCESS");
		// save entity
		configItemCollection = entityService.save(configItemCollection);

		updateTimerDetails(configItemCollection);

		return configItemCollection;
	}

	/**
	 * This Method starts the TimerService.
	 * 
	 * The method loads the configuration and evaluates the the following
	 * informations:
	 * 
	 * datstart - Date Object
	 * 
	 * datstop - Date Object
	 * 
	 * numInterval - Integer Object (interval in seconds)
	 * 
	 * id - String - unique identifier for the Timer Service.
	 * 
	 * The method throws an exception if the configuration entity contains
	 * invalid attributes or values.
	 * 
	 * After the timer was started the configuration is updated with the latest
	 * statusmessage
	 * 
	 * The method returns the current configuration
	 */
	public ItemCollection start() throws Exception {
		ItemCollection configItemCollection = findConfiguration();

		if (configItemCollection == null)
			return null;

		// configuration = loadConfiguration();
		String id = configItemCollection.getItemValueString("$uniqueid");
		Date startDate = configItemCollection.getItemValueDate("datstart");
		Date endDate = configItemCollection.getItemValueDate("datstop");

		// compute interval
		int hours = configItemCollection.getItemValueInteger("hours");
		int minutes = configItemCollection.getItemValueInteger("minutes");

		long interval = (hours * 60 + minutes) * 60 * 1000;

		configItemCollection
				.replaceItemValue("numInterval", new Long(interval));
		// try to cancel an existing timer for this workflowinstance
		Timer timer = this.findTimer(id);
		if (timer != null)
			timer.cancel();

		// if endDate is in the past we do not start the timer!
		Calendar calNow = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();

		if (endDate != null)
			calEnd.setTime(endDate);
		if (calNow.after(calEnd)) {
			logger.info("[DmsSchedulerService] "
					+ configItemCollection.getItemValueString("txtName")
					+ " not started because stop-date is in the past");

			updateTimerDetails(configItemCollection);
			return configItemCollection;
		}

		// start and set statusmessage

		SimpleDateFormat dateFormatDE = new SimpleDateFormat(
				"dd.MM.yy hh:mm:ss");

		String msg = "started at " + dateFormatDE.format(calNow.getTime())
				+ " by " + ctx.getCallerPrincipal().getName();
		configItemCollection.replaceItemValue("statusmessage", msg);
		timer = timerService.createTimer(startDate, interval,
				configItemCollection);

		logger.info("[DmsSchedulerService] "
				+ configItemCollection.getItemValueString("txtName")
				+ " started: " + id);

		saveConfiguration(configItemCollection);

		return configItemCollection;
	}

	/**
	 * Cancels a running timer instance. After cancel a timer the configuration
	 * will be updated.
	 * 
	 */
	public ItemCollection stop() throws Exception {
		ItemCollection configItemCollection = findConfiguration();

		String id = configItemCollection.getItemValueString("$uniqueid");
		Timer timer = this.findTimer(id);
		if (timer != null) {
			timer.cancel();

			Calendar calNow = Calendar.getInstance();
			SimpleDateFormat dateFormatDE = new SimpleDateFormat(
					"dd.MM.yy hh:mm:ss");

			String msg = "stopped at " + dateFormatDE.format(calNow.getTime())
					+ " by " + ctx.getCallerPrincipal().getName();
			configItemCollection.replaceItemValue("statusmessage", msg);

			logger.info("[DmsSchedulerService] "
					+ configItemCollection.getItemValueString("txtName")
					+ " stopped: " + id);
		} else {
			configItemCollection.replaceItemValue("statusmessage", "");

		}

		configItemCollection = saveConfiguration(configItemCollection);
		return configItemCollection;
	}

	public boolean isRunning() {
		try {
			ItemCollection configItemCollection = findConfiguration();
			if (configItemCollection == null)
				return false;
	
			return (findTimer(configItemCollection
					.getItemValueString("$uniqueid")) != null);
		} catch (Exception e) {
	
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Diese Methode liest Datenaustausch-Daten ein, vearbeitet sie und
	 * verschiebt diese anschließend.
	 * 
	 * @param timer
	 */
	@Timeout
	public void runTimer(javax.ejb.Timer timer) {

		scan();

		ItemCollection configItemCollection = findConfiguration();
		Date endDate = configItemCollection.getItemValueDate("datstop");
		String sTimerID = configItemCollection.getItemValueString("$uniqueid");

		/*
		 * Check if Timer should be canceld now?
		 */

		Calendar calNow = Calendar.getInstance();

		if (endDate != null && calNow.getTime().after(endDate)) {
			timer.cancel();
			System.out.println("[DmsSchedulerService] Timeout sevice stopped: "
					+ sTimerID);

		}

	}

	/**
	 * This method scans a file path and imports the files into blob workitems
	 * After a file was imported it will be removed from the directory.
	 * 
	 * The method did only scan a directory containing the folder name
	 * 'imixs_dms'. This is for security reason to avoid the uncontrolled
	 * parsing of os root folders.
	 * 
	 * 
	 * @param aLog
	 * @throws Exception
	 */
	public void scan() {
	
		logger.info("[DmsSchedulerService] scanning directories...");
	
		ItemCollection configItemCollection = findConfiguration();
	
		// get all import paths....
		List vList = configItemCollection.getItemValue("_filepath");
		for (Object aPath : vList) {
			// test if
			if (!aPath.toString().contains(DROP_FOLDER)) {
				logger.warning("[DmsSchedulerService] invalid file path : "
						+ aPath.toString());
				logger.warning("[DmsSchedulerService] make sure path contains "
						+ DROP_FOLDER);
	
			} else
				scanPath(aPath.toString());
	
		}
	
	}

	/**
	 * This method returns a timer for a corresponding id if such a timer object
	 * exists.
	 * 
	 * @param id
	 * @return Timer
	 * @throws Exception
	 */
	private Timer findTimer(String id) throws Exception {
		for (Object obj : timerService.getTimers()) {
			Timer timer = (javax.ejb.Timer) obj;
			if (timer.getInfo() instanceof ItemCollection) {
				ItemCollection adescription = (ItemCollection) timer.getInfo();
				if (id.equals(adescription.getItemValueString("$uniqueid"))) {
					return timer;
				}
			}
		}
		return null;
	}

	private void updateTimerDetails(ItemCollection configuration) {
		if (configuration == null)
			return;
		String id = configuration.getItemValueString("$uniqueid");
		Timer timer;
		try {
			timer = this.findTimer(id);

			if (timer != null) {
				// load current timer details
				configuration.replaceItemValue("nextTimeout",
						timer.getNextTimeout());
				configuration.replaceItemValue("timeRemaining",
						timer.getTimeRemaining());
			} else {
				configuration.removeItem("nextTimeout");
				configuration.removeItem("timeRemaining");

			}
		} catch (Exception e) {
			logger.warning("[DmsSchedulerService] unable to updateTimerDetails: "
					+ e.getMessage());
			configuration.removeItem("nextTimeout");
			configuration.removeItem("timeRemaining");

		}
	}

	/**
	 * The method did recursively scan a given file path all files found will be
	 * imported into a blob workitem and after that the file will be removed.
	 * 
	 * @param path
	 *            - the file path
	 */
	private void scanPath(String path) {
		logger.info("[DmsSchedulerService] scanPath: " + path);
		File root = new File(path);
		File[] list = root.listFiles();
		for (File f : list) {
			if (f.isDirectory()) {
				// Recursive method call
				scanPath(f.getAbsolutePath());
			} else {
				// start import....
				importFile(f);
			}
		}
	}

	/**
	 * This method imports a file into a blobWorkitem. After a successful import
	 * the fill will be removed.
	 * 
	 * 
	 * @throws IOException
	 */
	private void importFile(File importFile) {

		logger.info("[DmsSchedulerService] importing file:"
				+ importFile.getAbsoluteFile());
		try {
			InputStream is = new FileInputStream(importFile);

			// Get the size of the file
			long length = importFile.length();
			if (length > MAX_FILE_SIZE) {
				logger.warning("[DmsSchedulerService] file is to large - maximum size alowed = "
						+ MAX_FILE_SIZE);
				return;
			}

			// Create the byte array to hold the data
			byte[] bytes = new byte[(int) length];

			// Read in the bytes
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}

			// Ensure all the bytes have been read in
			if (offset < bytes.length) {
				throw new IOException("Could not completely read file "
						+ importFile.getName());
			}

			// Close the input stream and return bytes
			is.close();

			ItemCollection dmsItemCollection = new ItemCollection();

			dmsItemCollection.replaceItemValue("type", DMS_TYPE);
			dmsItemCollection.replaceItemValue("txtname", importFile.getName());
			dmsItemCollection.addFile(bytes, importFile.getName(),
					new MimetypesFileTypeMap().getContentType(importFile));
			dmsItemCollection.replaceItemValue("$writeAccess",
					"org.imixs.ACCESSLEVEL.MANAGERACCESS");
			dmsItemCollection.replaceItemValue("$readAccess",
					"org.imixs.ACCESSLEVEL.MANAGERACCESS");
			
			// add an empty reference
			dmsItemCollection.replaceItemValue("$uniqueidRef",
					"");

			// save item collection
			entityService.save(dmsItemCollection);

			// delete the file
			 importFile.delete();
		} catch (Exception e) {
			logger.warning("[DmsSchedulerService] error importing file "
					+ importFile.getAbsoluteFile());
			logger.warning("[DmsSchedulerService] " + e.getMessage());
		}

	}

	/**
	 * compares file names
	 * 
	 * @author rsoika
	 * 
	 */
	class FileComparator implements Comparator<File> {

		public int compare(File a, File b) {
			long lFileA = a.lastModified();
			long lFileB = b.lastModified();
			if (lFileA == lFileB)
				return 0;
			if (lFileA > lFileB)
				return 1;
			else
				return -1;
		}

	}

}
