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

package org.imixs.marty.plugins;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.NamingException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;

public class MailPlugin extends org.imixs.workflow.plugins.jee.MailPlugin {

	private WorkflowService workflowService = null;
	private boolean hasMailSession = false;
	private static Logger logger = Logger.getLogger(MailPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		hasMailSession = true;

		// get workflow service instance
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;
		}
	}

	@Override
	public void close(int arg0) throws PluginException {

		if (hasMailSession)
			super.close(arg0);
	}

	@Override
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException {

		if (hasMailSession) {

			// run default functionallity
			int result = super.run(documentContext, documentActivity);

			// terminate if the result was an error
			if (result == Plugin.PLUGIN_ERROR)
				return Plugin.PLUGIN_ERROR;

			// now get the Mail Session object
			MimeMessage mailMessage = (MimeMessage) super.getMailMessage();
			if (mailMessage == null) {
				// no Mail message - so we can return
				return Plugin.PLUGIN_OK;
			}

			// test for blob workitem
			ItemCollection blobWorkitem = loadBlob(documentContext);
			if (blobWorkitem != null)
				try {
					attachFiles(blobWorkitem);
				} catch (MessagingException e) {
					logger.warning("unable to attach files!");
					e.printStackTrace();
				}

		}
		return Plugin.PLUGIN_OK;
	}

	/**
	 * this helper method creates an internet address from a string if the
	 * string has illegal characters like whitespace the string will be
	 * surrounded with "". If you subclass this MailPlugin Class you can
	 * overwrite this method to return a different mail-address name or lookup a
	 * mail attribute in a directory like a ldap directory.
	 * 
	 * @param aAddr
	 * @return
	 * @throws AddressException
	 */
	public InternetAddress getInternetAddress(String aAddr)
			throws AddressException {

		// is smtp address skip profile lookup?
		if (aAddr.indexOf('@') > -1)
			return super.getInternetAddress(aAddr);

		// try to get email from syw profile
		try {
			aAddr = fetchEmail(aAddr);
			if (aAddr.indexOf('@') == -1) {
				logger.warning("[MartyMailPlugin] smtp mail address for '"
						+ aAddr + "' could not be resolved!");
				return null;
			}
		} catch (NamingException e) {
			// no valid email was found!
			logger.warning("[MartyMailPlugin] mail for '" + aAddr
					+ "' could not be resolved!");
			return null;
		}
		return super.getInternetAddress(aAddr);
	}

	/**
	 * This method lookups the emailadress for a given openid account through
	 * the ProfileService. If no profile is found or email is not valid the
	 * method throws a NamingException. This will lead into a situation where
	 * the super class tries to surround account with "" (hopefully)
	 * 
	 * 
	 * @param aOpenID
	 * @return
	 * @throws NamingException
	 */
	private String fetchEmail(String aOpenID) throws NamingException {

		ItemCollection itemColProfile = findProfileByName(aOpenID);

		if (itemColProfile == null)
			throw new NamingException(
					"[MartyMailPlugin] No Profile found for: " + aOpenID);

		String sEmail = itemColProfile.getItemValueString("txtEmail");

		// System.out.println("***** DEBUG ***** ProfileService - EmailLookup ="
		// + sEmail);

		if (sEmail != null && !"".equals(sEmail)) {
			if (sEmail.indexOf("http") > -1 || sEmail.indexOf("//") > -1)
				throw new NamingException(
						"[MartyMailPlugin] Invalid Email: ID=" + aOpenID
								+ " Email=" + sEmail);
			return sEmail;
		}

		// test if account contains protokoll information - this
		if (aOpenID.indexOf("http") > -1 || aOpenID.indexOf("//") > -1)
			throw new NamingException("[MartyMailPlugin] Invalid Email: ID="
					+ aOpenID);

		return aOpenID;
	}

	/**
	 * This method returns a profile ItemCollection for a specified account
	 * name. if no name is supported the remote user name will by used to find
	 * the profile The method returns null if no Profile for this name was found
	 * 
	 * @param aname
	 * @return
	 */
	private ItemCollection findProfileByName(String aname) {

		if (aname == null)
			return null;

		String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
				+ " JOIN profile.textItems AS t2"
				+ " WHERE  profile.type= 'profile' "
				+ " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
				+ aname + "' ";

		Collection<ItemCollection> col = workflowService.getEntityService()
				.findAllEntities(sQuery, 0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}

	/**
	 * This method adds all files of a given BlobWOrkitem to the current
	 * MailMessage
	 * 
	 * 
	 * @param blobWorkitem
	 * @throws MessagingException
	 */
	private void attachFiles(ItemCollection blobWorkitem)
			throws MessagingException {

		String sFilePattern = null;

		while ((sFilePattern = getAttachmentName()) != null) {
			logger.fine("MailPlugin attach file pattern: \"" + sFilePattern
					+ "\"");
			// get all fileNames....
			List<String> fileNames = blobWorkitem.getFileNames();
			// iterate over all files ....
			for (String aFileName : fileNames) {
				// test if aFilename matches the pattern
				if (sFilePattern.isEmpty()
						|| Pattern.matches(sFilePattern, aFileName)) {

					// fetch the file content
					FileInfo fileInfo = getFileFromWorkItem(aFileName,
							blobWorkitem);

					logger.fine("MailPlugin - attach : " + aFileName);

					// get Mulitpart Message
					Multipart multipart = super.getMultipart();
					// now attache the file
					MimeBodyPart attachmentPart = new MimeBodyPart();

					// construct the body part from the byte array
					DataSource dataSource = new ByteArrayDataSource(
							fileInfo.content, fileInfo.contentType);
					attachmentPart.setDataHandler(new DataHandler(dataSource));
					attachmentPart.setFileName(aFileName);
					attachmentPart.setDescription("");
					multipart.addBodyPart(attachmentPart);
				}

			}

		}

	}

	/**
	 * this method parses a mail body for the xml tag
	 * <attachments>name</attachments>. If an tag exists the method removes the
	 * tag and returns the value. The value is used by the method attachFile()
	 * to add files into the mail body.
	 * 
	 * 
	 */
	private String getAttachmentName() {
		int iTagStartPos;
		int iTagEndPos;

		int iContentStartPos;
		int iContentEndPos;

		String content = null;
		MimeBodyPart messagePart = null;

		Multipart multipart = super.getMultipart();

		try {
			
			
			
			messagePart = (MimeBodyPart) multipart.getBodyPart(0);
			if (messagePart!=null)
				content = (String) messagePart.getContent();

		} catch (MessagingException e) {
			logger.warning("Unable to parse tag 'attachments' !");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			logger.warning("Unable to parse tag 'attachments' !");
			e.printStackTrace();
			return null;
		}

		if (content == null || content.isEmpty())
			return null;

		// test if a <value> tag exists...
		if ((iTagStartPos = content.toLowerCase().indexOf("<attachments")) != -1) {

			iTagEndPos = content.toLowerCase().indexOf("</attachments>",
					iTagStartPos);

			// if no end tag found return string unchanged...
			if (iTagEndPos == -1)
				return null;

			// reset pos vars
			iContentStartPos = 0;
			iContentEndPos = 0;

			// so we now search the beginning of the tag content
			iContentEndPos = iTagEndPos;
			// start pos is the last > before the iContentEndPos
			String sTestString = content.substring(0, iContentEndPos);
			iContentStartPos = sTestString.lastIndexOf('>') + 1;

			// if no end tag found return string unchanged...
			if (iContentStartPos > iContentEndPos)
				return null;

			iTagEndPos = iTagEndPos + "</attachments>".length();

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// extract Item Value
			String sFilename = content.substring(iContentStartPos,
					iContentEndPos);

			String sEMTY = "";

			// now replace the tag with an empty string
			content = content.substring(0, iTagStartPos) + sEMTY
					+ content.substring(iTagEndPos);

			// update mail body
			try {
				messagePart.setText(content);
			} catch (MessagingException e) {
				logger.warning("Unable to parse tag 'attachments' !");
				e.printStackTrace();
			}
			// return the file pattern
			return sFilename;
		}

		return null;

	}

	/**
	 * This method returns a FileInfo object for a specific FilenName. The
	 * FileInfo contains the Content (byte[]) and the ContentType
	 * 
	 * @param uniqueid
	 * @param fileName
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private FileInfo getFileFromWorkItem(String fileName,
			ItemCollection blobWorkitem) {

		// fetch $file from hashmap....

		HashMap mapFiles = null;
		Vector vFiles = (Vector) blobWorkitem.getItemValue("$file");
		if (vFiles != null && vFiles.size() > 0) {
			mapFiles = (HashMap) vFiles.elementAt(0);

			Vector<Object> vectorFileInfo = new Vector<Object>();
			vectorFileInfo = (Vector) mapFiles.get(fileName);
			if (vectorFileInfo != null) {
				String sContentType = vectorFileInfo.elementAt(0).toString();
				byte[] fileContent = (byte[]) vectorFileInfo.elementAt(1);

				FileInfo fileInfo = new FileInfo(fileContent, sContentType);
				return fileInfo;
			}
		}
		return null;

	}

	/**
	 * Loads the BlobWorkitem of a given parent Workitem. The BlobWorkitem is
	 * identified by the $unqiueidRef. If no BlobWorkitem exists the method
	 * returns null;
	 * 
	 * @param itemCol
	 *            - parent workitem where the BlobWorkitem will be attached to
	 * @throws Exception
	 */
	private ItemCollection loadBlob(ItemCollection itemCol) {
		ItemCollection blobWorkitem = null;
		String sUniqueID = itemCol.getItemValueString("$uniqueid");

		// search entity...
		String sQuery = " SELECT lobitem FROM Entity as lobitem"
				+ " join lobitem.textItems as t1"
				+ " join lobitem.textItems as t2"
				+ " WHERE t1.itemName = 'type'"
				+ " AND t1.itemValue = 'workitemlob'"
				+ " AND t2.itemName = '$uniqueidref'" + " AND t2.itemValue = '"
				+ sUniqueID + "'";

		Collection<ItemCollection> itemcol = workflowService.getEntityService()
				.findAllEntities(sQuery, 0, 1);
		if (itemcol != null && itemcol.size() > 0) {
			blobWorkitem = itemcol.iterator().next();
		}

		return blobWorkitem;

	}

	/**
	 * Cache implementation to hold userData objects
	 * 
	 * @author rsoika
	 * 
	 */
	class FileInfo {
		String contentType;
		byte[] content = null;

		public FileInfo(byte[] acontent, String aContentType) {

			this.content = acontent;
			this.contentType = aContentType;
		}

	}
}
