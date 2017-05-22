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
import java.util.List;
import java.util.Map;
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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin extends the Imixs Workflow Plugin.
 * 
 * The Plugin translates recipient addresses with the mail address stored in the
 * users profile
 * 
 * In addition this plugin adds the attachments from the blob workItem into the
 * mail body if the tag <attachment/> was found.
 * 
 * 
 * @author rsoika
 * @version 2.0
 */
public class MailPlugin extends org.imixs.workflow.engine.plugins.MailPlugin {

	private ProfileService profileService = null;

	public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";
	public static String PROPERTYSERVICE_NOT_BOUND = "PROPERTYSERVICE_NOT_BOUND";
	public static String INVALID_EMAIL = "INVALID_EMAIL";

	private static Logger logger = Logger.getLogger(MailPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {

		super.init(actx);

		// lookup profile service EJB
		String jndiName = "ejb/ProfileService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			profileService = (ProfileService) ctx.lookup(jndiName);
		} catch (NamingException e) {

			throw new PluginException(MailPlugin.class.getSimpleName(), PROFILESERVICE_NOT_BOUND,
					"ProfileService not bound", e);
		}

	}

	/**
	 * This method adds the attachments of the blob workitem to the MimeMessage
	 */
	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {
		// run default functionality
		ItemCollection result = super.run(documentContext, documentActivity);

		// now get the Mail Session object
		MimeMessage mailMessage = (MimeMessage) super.getMailMessage();
		if (mailMessage != null) {
			// test for blob workitem to add attachemtns
			ItemCollection blobWorkitem = BlobWorkitemHandler.load(this.getWorkflowService().getDocumentService(),
					documentContext);
			if (blobWorkitem != null) {
				try {
					attachFiles(blobWorkitem);
				} catch (MessagingException e) {
					logger.warning("unable to attach files!");
					e.printStackTrace();
				}
			}
		}
		return result;
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
	public InternetAddress getInternetAddress(String aAddr) throws AddressException {

		// is smtp address skip profile lookup?
		if (aAddr.indexOf('@') > -1)
			return super.getInternetAddress(aAddr);

		// try to get email from the users profile
		try {
			aAddr = fetchEmail(aAddr);
			if (aAddr.indexOf('@') == -1) {
				logger.warning("smtp mail address for '" + aAddr + "' could not be resolved!");
				return null;
			}
		} catch (NamingException e) {
			// no valid email was found!
			logger.warning("smtp mail address for '" + aAddr + "' could not be resolved - " + e.getMessage());
			// e.printStackTrace();
			// avoid sending mail to this address!
			return null;
		}
		return super.getInternetAddress(aAddr);
	}

	/**
	 * This method lookups the emailadress for a given user account through the
	 * ProfileService. If no profile is found or email is not valid the method
	 * throws a NamingException.
	 * 
	 * @param aUserID
	 * @return
	 * @throws NamingException
	 */
	private String fetchEmail(String aUserID) throws NamingException {

		ItemCollection itemColProfile = profileService.findProfileById(aUserID);

		if (itemColProfile == null)
			throw new NamingException("[MartyMailPlugin] No Profile found for: " + aUserID);

		String sEmail = itemColProfile.getItemValueString("txtEmail");

		logger.fine("ProfileService - EmailLookup =" + sEmail);

		if (sEmail != null && !"".equals(sEmail)) {
			if (sEmail.indexOf("http") > -1 || sEmail.indexOf("//") > -1)
				throw new NamingException("[MartyMailPlugin] Invalid Email: ID=" + aUserID + " Email=" + sEmail);
			return sEmail;
		}

		// test if account contains protokoll information - this
		if (aUserID.indexOf("http") > -1 || aUserID.indexOf("//") > -1)
			throw new NamingException("[MartyMailPlugin] Invalid Email: ID=" + aUserID);

		return aUserID;
	}

	/**
	 * This method adds all files of a given BlobWOrkitem to the current
	 * MailMessage
	 * 
	 * 
	 * @param blobWorkitem
	 * @throws MessagingException
	 */
	private void attachFiles(ItemCollection blobWorkitem) throws MessagingException {

		String sFilePattern = null;

		while ((sFilePattern = getAttachmentName()) != null) {
			logger.fine("MailPlugin attach file pattern: \"" + sFilePattern + "\"");
			// get all fileNames....
			List<String> fileNames = blobWorkitem.getFileNames();
			// iterate over all files ....
			for (String aFileName : fileNames) {
				// test if aFilename matches the pattern
				if (sFilePattern.isEmpty() || Pattern.matches(sFilePattern, aFileName)) {

					// fetch the file content
					FileInfo fileInfo = getFileFromWorkItem(aFileName, blobWorkitem);

					logger.fine("MailPlugin - attach : " + aFileName);

					// get Mulitpart Message
					Multipart multipart = super.getMultipart();
					// now attache the file
					MimeBodyPart attachmentPart = new MimeBodyPart();

					// construct the body part from the byte array
					DataSource dataSource = new ByteArrayDataSource(fileInfo.content, fileInfo.contentType);
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

			iTagEndPos = content.toLowerCase().indexOf("</attachments>", iTagStartPos);

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
			String sFilename = content.substring(iContentStartPos, iContentEndPos);

			String sEMTY = "";

			// now replace the tag with an empty string
			content = content.substring(0, iTagStartPos) + sEMTY + content.substring(iTagEndPos);

			// update mail body
			try {
				messagePart.setContent(content, this.getContentType());
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
	private FileInfo getFileFromWorkItem(String fileName, ItemCollection blobWorkitem) {

		// fetch $file from hashmap....
		Map<String, List<Object>> files = blobWorkitem.getFiles();
		// now fetch the file content....
		if (files != null) {
			List<Object> fileData = files.get(fileName);
			if (fileData != null && fileData.size()>=2) {
				String sContentType = fileData.get(0).toString();
				byte[] fileContent = (byte[]) fileData.get(1);
				FileInfo fileInfo = new FileInfo(fileContent, sContentType);
				return fileInfo;
			}
		}
		// file with this name not found!
		return null;

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
