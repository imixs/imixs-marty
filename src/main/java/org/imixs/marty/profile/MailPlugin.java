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

package org.imixs.marty.profile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.util.XMLParser;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

/**
 * This Plugin extends the Imixs Workflow MailPlugin.
 * <p>
 * The Plugin translates recipient addresses with the mail address stored in the
 * users profile.
 * <p>
 * The Plugin also supports the environment variable 'mail.authenticatedSender'
 * that can be used to send out mails with a from address in the format:
 * "COMPANY - {username} <webmaster@info.com>"
 * <p>
 * In addition this plugin adds the attachments from a snapshot workItem into
 * the mail body if the tag <attachment/> was found. A attachment can be named:
 * <p>
 * 
 * <pre>
 * {@code
 *     <attachments>order.pdf</attachments>
 *  
 *  <attachments><itemvalue>_ordernumber</itemvalue>.pdf</attachments>
 * 
 * }
 * </pre>
 * 
 * <p>
 * An Attachment can also be taken from a textblock. *
 * 
 * <pre>
 * {@code
 *  
 *     <attachments textblock="my_block"></attachments>
 * }
 * </pre>
 * 
 * @author rsoika
 * @version 2.0
 */
public class MailPlugin extends org.imixs.workflow.engine.plugins.MailPlugin {

    public static String SNAPSHOTID = "";
    public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";
    public static String PROPERTYSERVICE_NOT_BOUND = "PROPERTYSERVICE_NOT_BOUND";
    public static String INVALID_EMAIL = "INVALID_EMAIL";

    private static Logger logger = Logger.getLogger(MailPlugin.class.getName());

    @Inject
    @ConfigProperty(name = "mail.authenticatedSender")
    Optional<String> mailAuthenticatedSender;

    @Inject
    ProfileService profileService;

    @Inject
    DocumentService documentService;

    /**
     * This method adds the attachments of the blob workitem to the MimeMessage
     */
    @Override
    public ItemCollection run(ItemCollection workitem, ItemCollection event) throws PluginException {
        // run default functionality
        ItemCollection result = super.run(workitem, event);

        // now get the Mail Session object
        MimeMessage mailMessage = (MimeMessage) super.getMailMessage();
        if (mailMessage != null) {
            try {
                if (mailAuthenticatedSender.isPresent()) {
                    sendFromAuthenticatedSender(workitem, event, mailMessage);
                }

                String content = null;
                MimeBodyPart messagePart = null;
                // did we have a message body?
                Multipart multipart = super.getMultipart();
                try {
                    messagePart = (MimeBodyPart) multipart.getBodyPart(0);
                    content = (String) messagePart.getContent();
                } catch (MessagingException | ArrayIndexOutOfBoundsException | IOException e) {
                    logger.warning("Unable to parse tag 'attachments' !");
                    e.printStackTrace();
                    return null;
                }
                // did our message body contain a <attachments .....?
                if (content != null && content.toLowerCase().indexOf("<attachments") != -1) {
                    // we can add the attachment
                    try {
                        content = attachFiles(workitem, content);
                    } catch (MessagingException e) {
                        logger.warning("unable to attach files!");
                        e.printStackTrace();
                    }

                    // update mail body
                    try {
                        messagePart.setContent(content, this.getContentType());
                    } catch (MessagingException e) {
                        logger.warning("Unable to parse tag 'attachments' !");
                        e.printStackTrace();
                    }

                }
            } catch (MessagingException | UnsupportedEncodingException e) {
                throw new PluginException(MailPlugin.class.getSimpleName(),
                        ERROR_MAIL_MESSAGE, e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * This helper method builds a from address in the format
     * "COMPANY - {username} <webmaster@info.com>"
     * and set the sender to the given mail address.
     * This ensures that the mail is not rejected by DKIM, SPF, DMARK rules.
     *
     * @param workitem
     * @param event
     * @param mailMessage
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private void sendFromAuthenticatedSender(ItemCollection workitem, ItemCollection event, MimeMessage mailMessage)
            throws MessagingException, UnsupportedEncodingException {
        logger.info("├── 📭 set authenticated sender...");
        InternetAddress adr = getInternetAddress(getFrom(workitem, event));
        String mailDefaultPattern = mailAuthenticatedSender.get();
        String sender = mailDefaultPattern;
        // Expected format "COMPANY - {username} <webmaster@info.com>"
        int start = mailDefaultPattern.lastIndexOf('<');
        int end = mailDefaultPattern.lastIndexOf('>');
        if (start >= 0 && end >= 0) {
            String senderDisplayName = mailDefaultPattern.substring(0, start);
            sender = mailDefaultPattern.substring(start + 1, end);
            logger.info("│  ├── sender='" + sender + "'");
            mailMessage.setHeader("Sender", sender);

            // create custom from...
            String userName = this.getWorkflowService().getUserName();
            String userDisplayName = userName;
            String userEmail = userName;
            ItemCollection profile = profileService.findProfileById(userName);
            if (profile != null) {
                userDisplayName = profile.getItemValueString("txtuserName");
                userEmail = profile.getItemValueString("txtemail");
                senderDisplayName = senderDisplayName.replace("{username}", userDisplayName);
            } else {
                logger.warning(
                        "├── ⚠️ no matching user profile found for " + userName);
            }
            InternetAddress fromAddress = new InternetAddress(sender, senderDisplayName);
            mailMessage.setFrom(fromAddress);
            logger.info(
                    "│  ├── from:" + fromAddress.getPersonal() + "<" + fromAddress.getAddress() + ">");
            logger.info("│  ├── replyTo:" + userEmail);
            InternetAddress[] replyToAddressList = new InternetAddress[1];
            replyToAddressList[0] = getInternetAddress(userEmail);
            mailMessage.setReplyTo(replyToAddressList);

        } else {
            // default behavior - set sender and reply only
            logger.info("│  ├── sender='" + sender + "'");
            mailMessage.setHeader("Sender", sender);
            // Dow we have a replay to?
            String sReplyTo = getReplyTo(workitem, event);
            if ((sReplyTo == null) || (sReplyTo.isEmpty())) {
                sReplyTo = adr.getAddress();
            }
            logger.info("│  ├── replyTo:" + sReplyTo);
            InternetAddress[] resplysAdrs = new InternetAddress[1];
            resplysAdrs[0] = getInternetAddress(sReplyTo);
            mailMessage.setReplyTo(resplysAdrs);

        }
    }

    /**
     * this helper method creates an internet address from a string if the string
     * has illegal characters like whitespace the string will be surrounded with "".
     * If you subclass this MailPlugin Class you can overwrite this method to return
     * a different mail-address name or lookup a mail attribute in a directory like
     * a ldap directory.
     * 
     * @param aAddr
     * @return
     * @throws AddressException
     */
    @Override
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
            throw new NamingException("No Profile found for: " + aUserID);

        String sEmail = itemColProfile.getItemValueString("txtEmail");

        logger.fine("ProfileService - EmailLookup =" + sEmail);

        if (sEmail != null && !"".equals(sEmail)) {
            if (sEmail.indexOf("http") > -1 || sEmail.indexOf("//") > -1)
                throw new NamingException("Invalid Email: ID=" + aUserID + " Email=" + sEmail);
            return sEmail;
        }

        // test if account contains protokoll information - this
        if (aUserID.indexOf("http") > -1 || aUserID.indexOf("//") > -1)
            throw new NamingException("Invalid Email: ID=" + aUserID);

        return aUserID;
    }

    /**
     * This method adds all files defined in the mail body with the <atachement>
     * tag. The files are lookuped either in the workitem or from a textblock.
     * 
     * 
     * @param workitem
     * @return - the new content
     * @throws MessagingException
     */
    private String attachFiles(ItemCollection workitem, String content) throws MessagingException {
        logger.finest("......attaching files");
        ItemCollection attachmentContext = null;

        if (content == null || content.isEmpty())
            return content;

        // get all attachment tags.
        List<String> tags = XMLParser.findTags(content, "attachments");

        for (String _tag : tags) {
            attachmentContext = null;
            logger.finest("......attachments tag=" + _tag);
            // check if the tag contains a textblock attribute.
            if (_tag.contains("textblock")) {
                String textblockName = XMLParser.findAttribute(_tag, "textblock");
                logger.finest("......attaching textblock " + textblockName);
                // <attachments textblock="my_block" />
                if (textblockName != null && !textblockName.isEmpty()) {
                    // it's a textblock file
                    ItemCollection textBlockDocument = loadTextBlock(textblockName);
                    if (textBlockDocument == null) {
                        logger.warning("textblock '" + textblockName + "' is not defined!");
                        // remove the tag
                        content = content.replace(_tag, "");
                        continue;
                    }
                    if (!"FILE".equals(textBlockDocument.getItemValueString("txtmode"))) {
                        logger.warning("textblock '" + textblockName + "' is not defined as type FILE!");
                        // remove the tag
                        content = content.replace(_tag, "");
                        continue;
                    }
                    attachmentContext = textBlockDocument;
                } else {
                    // no attribute value !
                    logger.warning("wrong or empty attribute  'textblock' in tag " + _tag + " - please verify model!");
                    continue;
                }

            } else {
                // default
                attachmentContext = workitem;
            }

            // fetch the snapshot for the current attachmentContext
            ItemCollection snapshotWorkitem = this.getWorkflowService().getDocumentService()
                    .load(attachmentContext.getItemValueString(SNAPSHOTID));
            if (snapshotWorkitem != null) {
                // attachmentContext = snapshotWorkitem;
            }

            // get the value of attachments
            String sFilePattern = XMLParser.findTagValue(_tag, "attachments");
            sFilePattern = sFilePattern.trim();

            logger.finest("......MailPlugin attach file pattern: \"" + sFilePattern + "\"");
            // get all fileNames....
            List<String> fileNames = attachmentContext.getFileNames();

            // build a regex pattern if a pattern exists....
            Pattern pattern = null;
            if (!sFilePattern.isEmpty()) {
                pattern = Pattern.compile(sFilePattern);
            }
            logger.finest("......total count of file=" + fileNames.size());
            // iterate over all files ....
            for (String aFileName : fileNames) {
                // test if aFilename matches the pattern or the pattern is null
                if (pattern == null || pattern.matcher(aFileName).find()) {

                    // fetch the file
                    FileData fileData = attachmentContext.getFileData(aFileName);
                    if (fileData != null) {
                        // it might be that the content of the file is already part of the snapshot
                        if (fileData.getContent().length < 4) {
                            logger.finest("......file found, but we need a snapshot....");
                            // no content - so we can try the snapshot
                            if (snapshotWorkitem != null) {
                                fileData = snapshotWorkitem.getFileData(aFileName);
                                if (fileData == null) {
                                    continue;
                                }
                            } else {
                                logger.warning("Snapshot is missing - can not attache file!!");
                            }
                        }

                        logger.finest("......MailPlugin - attach : " + aFileName);
                        // get Mulitpart Message
                        Multipart multipart = super.getMultipart();
                        // now attache the file
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        // construct the body part from the byte array
                        DataSource dataSource = new ByteArrayDataSource(fileData.getContent(),
                                fileData.getContentType());
                        attachmentPart.setDataHandler(new DataHandler(dataSource));
                        attachmentPart.setFileName(aFileName);
                        attachmentPart.setDescription("");
                        multipart.addBodyPart(attachmentPart);
                    } else {
                        // no op!
                    }
                }
            }

            // remove the tag
            content = content.replace(_tag, "");
        }

        return content;

    }

    /**
     * Helper method to load a text-block workitem
     * 
     * @see Imixs-Office-Workflow
     * 
     * @param name
     * @return
     */
    public ItemCollection loadTextBlock(String name) {
        ItemCollection textBlockItemCollection = null;
        // check cache...

        // try to load by ID....
        textBlockItemCollection = documentService.load(name);
        if (textBlockItemCollection == null) {
            // not found by ID so lets try to load it by txtname.....
            // load text-block....
            String sQuery = "(type:\"" + "textblock" + "\" AND txtname:\"" + name + "\")";
            Collection<ItemCollection> col;
            try {
                col = documentService.find(sQuery, 1, 0);

                if (col.size() > 0) {
                    textBlockItemCollection = col.iterator().next();
                } else {
                    logger.warning("Missing text-block : '" + name + "'");
                }
            } catch (QueryException e) {
                logger.warning("getTextBlock - invalid query: " + e.getMessage());
            }

        }

        return textBlockItemCollection;
    }

}
