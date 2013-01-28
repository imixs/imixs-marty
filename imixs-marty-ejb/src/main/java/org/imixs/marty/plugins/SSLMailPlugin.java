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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin extends the Imixs Office Workflow MailPlugin.
 * The Plugin overwrite the close() method to implement a smtps transport
 * protocol.
 * 
 * 
 * Note that if you're using the "smtps" protocol to access SMTP over SSL, all
 * the properties would be named "mail.smtps.*".
 * 
 * Note: The property props.put("mail.smtps.quitwait", "false"); is required to
 * get rid of a strange SSL exception :
 * 
 * 
 * 
 * Properties which should be provided by the GlassFish mail resource configuration:
 * 
 * <code>
    "mail.smtps.auth" = "true"
    "mail.smtps.quitwait" "false"
    "mail.smtps.user" = "xxxxx"
    "mail.smtps.password = "xxxxx"
    "mail.smtps.host" = "...."
 * </code>
 * 
 * Optional Properties
 * <code>
    "mail.debug"= "true"
    "mail-smtp.sendpartial" = "true"
 * </code>
 * 
 * If configuration did not work add the mail.smtps. params with '-' between mail and smtps
 * 
 * <code>
    "mail-smtps.auth" = "true"
    "mail-smtps.quitwait" "false"
    "mail-smtps.user" = "xxxxx"
    "mail-smtps.password = "xxxxx"
    "mail-smtps.host" = "...."
 * </code>
 * 
 * Also mail-smtp.* should be tested if no success.
 * 
 * Loglevel for org.imixs.marty.plugins.SSLMailPlugin can be set to 'FINE'
 * 
 * @author rsoika
 * @version 1.0
 */
public class SSLMailPlugin extends org.imixs.marty.plugins.MailPlugin {
private String aBodyText=null;
	private static Logger logger = Logger.getLogger(SSLMailPlugin.class
			.getName());

	
	
	// simply get the plain text body.....
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException {
		// build mail body...
		 aBodyText = documentActivity
				.getItemValueString("rtfMailBody");
		if (aBodyText != null) {
			aBodyText = replaceDynamicValues(aBodyText, documentContext);
		
		}
		
		return super.run(documentContext,documentActivity);
	}
	
	/**
	 * This method overwrites the close method of the marty mail plugin and the
	 * Imixs Workflow MailPlugin.
	 * 
	 * The method creates a ssl smtp transport protocoll
	 * 
	 * Note: The property props.put("mail.smtps.quitwait", "false"); is required
	 * to get rid of a strange SSL exception :
	 * 
	 * Note that if you're using the "smtps" protocol to access SMTP over SSL,
	 * all the properties would be named "mail.smtps.*".
	 * 
	 * 
	 */
	@Override
	public void close(int status) throws PluginException {
		Session mailSession = getMailSession();
		Message mailMessage = getMailMessage();
		if (hasMailSession()) {
			if (status == Plugin.PLUGIN_OK && mailSession != null
					&& mailMessage != null) {
				// Send the message
				try {
					if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
						logger.info("[SSLMailPlugin] SendMessage now...");

					// if send message fails (e.g. for policy reasons) the
					// process
					// will
					// continue. only a exception is thrown

					// Transport.send(mailMessage);

					// A simple transport.send command did not work if mail host
					// needs
					// a authentification. Therefor we use a manual smtp
					// connection

					Transport trans = mailSession.getTransport("smtps"); // ssl!

					trans.connect(mailSession.getProperty("mail.smtps.user"),
							mailSession.getProperty("mail.smtps.password"));

					mailMessage.setContent(getMultipart());
					
					
					logger.info("SSLMailPlugin - setting plain text content 'text/plain'");
				
					
					mailMessage.setContent(aBodyText,"text/plain");

					mailMessage.saveChanges();
					trans.sendMessage(mailMessage,
							mailMessage.getAllRecipients());
					trans.close();

				} catch (Exception esend) {
					logger.warning("[SSLMailPlugin] close - Warning:"
							+ esend.toString());
					
					if (logger.isLoggable(Level.FINE))
						esend.printStackTrace();
				}
			}

		}
	}

}
