package org.imixs.marty.util;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.business.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.plugins.jee.MailPlugin;

public class SywappMailPlugin extends MailPlugin {

	private ProfileService profileService = null;
	private boolean hasMailSession = false;

	@Override
	public void init(WorkflowContext actx) throws Exception {

		try {

			super.init(actx);
			hasMailSession = true;

		} catch (Exception e) {
			System.out.println("WARNING: Mail session not found: " + e);
		}

		// lookup profile service EJB
		String jndiName = "ejb/ProfileServiceBean";
		InitialContext ictx = new InitialContext();
		Context ctx = (Context) ictx.lookup("java:comp/env");
		profileService = (ProfileService) ctx.lookup(jndiName);

	}

	@Override
	public void close(int arg0) throws Exception {

		if (hasMailSession)
			super.close(arg0);
	}

	@Override
	public int run(ItemCollection arg0, ItemCollection arg1) throws Exception {

		if (hasMailSession)
			return super.run(arg0, arg1);
		else
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
				System.out.println("[SywAppMailPlugin] smtp mail address for '" + aAddr
						+ "' could not be resolved!");
				return null;
			}
		} catch (NamingException e) {
			// no valid email was found!
			System.out.println("[SywAppMailPlugin] mail for '" + aAddr
					+ "' could not be resolved!");
			// e.printStackTrace();
			// avoid sending mail to this address!
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

		ItemCollection itemColProfile = profileService
				.findProfileByName(aOpenID);

		if (itemColProfile == null)
			throw new NamingException(
					"[SywAppMailPlugin] No Profile found for: " + aOpenID);

		String sEmail = itemColProfile.getItemValueString("txtEmail");

		// System.out.println("***** DEBUG ***** ProfileService - EmailLookup ="
		// + sEmail);

		if (sEmail != null && !"".equals(sEmail)) {
			if (sEmail.indexOf("http") > -1 || sEmail.indexOf("//") > -1)
				throw new NamingException(
						"[SywAppMailPlugin] Invalid Email: ID=" + aOpenID
								+ " Email=" + sEmail);
			return sEmail;
		}

		// test if account contains protokoll information - this
		if (aOpenID.indexOf("http") > -1 || aOpenID.indexOf("//") > -1)
			throw new NamingException("[SywAppMailPlugin] Invalid Email: ID="
					+ aOpenID);

		return aOpenID;
	}

}
