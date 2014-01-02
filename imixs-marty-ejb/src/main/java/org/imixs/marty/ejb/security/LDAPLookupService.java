package org.imixs.marty.ejb.security;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.util.PropertyService;

/**
 * This EJB provides a ldap lookup service for user informations
 * 
 * The bean reads its configuration from the configuration property file located
 * in the glassfish domains config folder
 * (GLASSFISH_DOMAIN/config/imixs-office-ldap.properties).
 * 
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
@LocalBean
public class LDAPLookupService {

	private boolean enabled = false;
	private Properties configurationProperties = null;

	private String dnSearchFilter = null;
	private String groupSearchFilter = null;
	private String searchContext = null;
	private String[] userAttributes = null;

	@EJB
	LDAPCache ldapCache;
	
	@EJB
	PropertyService propertyService;


	private static Logger logger = Logger.getLogger("org.imixs.office");

	@PostConstruct
	void init() {
		try {
			logger.info("LDAPLookupService @PostConstruct - init");
			// load confiugration entity
			configurationProperties =propertyService.getProperties();

			// skip if no configuration
			if (configurationProperties == null)
				return;

			// initialize ldap connection
			reset();

		} catch (Exception e) {
			logger.severe("Unable to initalize LDAPGroupLookupService");

			e.printStackTrace();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * resets the config params....
	 * 
	 * 
	 */
	public void reset() {
		// determine the cache size....
		logger.fine("LDAPLookupService reset....");

		searchContext = configurationProperties.getProperty("ldap.search-context",
				"");
		dnSearchFilter = configurationProperties.getProperty(
				"ldap.dn-search-filter", "(uid=%u)");
		groupSearchFilter = configurationProperties.getProperty(
				"ldap.group-search-filter", "(member=%d)");

		// read user attributes
		String sAttributes = configurationProperties.getProperty(
				"ldap.user-attributes", "uid,SN,CN,mail");
		userAttributes = sAttributes.split(",");

		// test if ldap is enabled...
		LdapContext ldapCtx = null;
		try {
			ldapCtx = getDirContext();
			enabled = (ldapCtx != null);
		} finally {
			try {
				if (ldapCtx != null) {
					ldapCtx.close();
					ldapCtx=null;
				}
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Returns the default attributes for a given user. If no user was found in
	 * LDAP the method returns null.
	 * 
	 * @param aUID
	 *            - user id
	 * @return ItemCollection containing the user attributes or null if no
	 *         attributes where found.
	 */
	public ItemCollection findUser(String aUID) {

		// also null objects can be returned here (if no ldap attributes exist)
		if (ldapCache.contains(aUID))
			return (ItemCollection) ldapCache.get(aUID);

		// start lookup
		LdapContext ldapCtx = null;
		try {
			logger.fine("LDAP find user: " + aUID);
			ldapCtx = getDirContext();
			ItemCollection user = fetchUser(aUID, ldapCtx);
			// cache user attributes (also null will be set if no entry was
			// found!)
			ldapCache.put(aUID, user);
			return user;

		} finally {
			if (ldapCtx != null)
				try {
					ldapCtx.close();
					ldapCtx = null;
				} catch (NamingException e) {
					e.printStackTrace();
				}

		}

	}

	/**
	 * Returns a string array containing all group names for a given UID. If no
	 * groups exist or the uid was not found the method returns an empty string
	 * array!.
	 * 
	 * 
	 * @param aUID
	 *            - user unique id
	 * @return string array of group names
	 */
	public String[] findGroups(String aUID) {
		// test cache...
		String[] groups = (String[]) ldapCache.get(aUID + "-GROUPS");
		if (groups != null) {
			return groups;
		}

		LdapContext ldapCtx = null;
		try {
			logger.fine("LDAP find user groups for: " + aUID);
			ldapCtx = getDirContext();
			groups = fetchGroups(aUID, ldapCtx);
			if (groups == null)
				groups = new String[0];
			if (logger.isLoggable(java.util.logging.Level.FINE)) {
				String groupList = "";
				for (String aGroup : groups)
					groupList += "'" + aGroup + "' ";
				logger.fine("LDAP groups found for " + aUID + "=" + groupList);
			}

			// cache Group list
			ldapCache.put(aUID + "-GROUPS", groups);

			return groups;

		} finally {
			if (ldapCtx != null)
				try {
					ldapCtx.close();
					ldapCtx = null;
				} catch (NamingException e) {
					e.printStackTrace();
				}
		}

	}

	/**
	 * returns the default attributes for a given user in an ItemCollection. If
	 * ldap service is disabled or the user was not found then the method
	 * returns null.
	 * 
	 * @param aUID
	 *            - user id
	 * @return ItemCollection - containing the user attributes or null if no
	 *         entry was found
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ItemCollection fetchUser(String aUID, LdapContext ldapCtx) {
		ItemCollection user = null;
		String sDN = null;
		if (!enabled) {
			return null;
		}

		NamingEnumeration<SearchResult> answer = null;
		try {
			user = new ItemCollection();
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(userAttributes);

			String searchFilter = dnSearchFilter.replace("%u", aUID);
			logger.finest("LDAP search:" + searchFilter);
			answer = ldapCtx.search(searchContext, searchFilter, ctls);
			if (answer == null)
				return null;

			if (answer.hasMore()) {
				SearchResult entry = (SearchResult) answer.next();
				sDN = entry.getName();
				logger.finest("LDAP DN= " + sDN);

				Attributes attributes = entry.getAttributes();
				// fetch all attributes
				for (String itemName : userAttributes) {
					Attribute atr = attributes.get(itemName.trim());
					if (atr != null) {
						NamingEnumeration<?> values = atr.getAll();

						Vector valueList = new Vector();
						while (values.hasMore()) {
							valueList.add(values.next());
						}
						if (valueList.size() > 0)
							user.replaceItemValue(itemName, valueList);
					}
				}

				user.replaceItemValue("dn", sDN);
			}

			if (sDN == null) {
				// empty user entry
				sDN = aUID;
				user.replaceItemValue("dn", sDN);
			}

		} catch (NamingException e) {
			// return null
			user = null;
			logger.warning("Unable to fetch DN for: " + aUID);
			logger.warning(e.getMessage());
			if (logger.isLoggable(java.util.logging.Level.FINEST))
				e.printStackTrace();

		} finally {
			if (answer != null)
				try {
					answer.close();
					answer = null;
				} catch (NamingException e) {
					e.printStackTrace();
				}
		}
		return user;
	}

	/**
	 * Returns a string array containing all group names for a given uid. If not
	 * groups are found or the uid did not exist the method returns null.
	 * 
	 * @param aUID
	 *            - user id
	 * @return array list of user groups or null if no entry was found
	 */
	private String[] fetchGroups(String aUID, LdapContext ldapCtx) {
		String sDN = null;
		Vector<String> vGroupList = null;
		String[] groupArrayList = null;

		if (!enabled)
			return null;

		NamingEnumeration<SearchResult> answer = null;
		try {

			vGroupList = new Vector<String>();

			String groupNamePraefix = configurationProperties
					.getProperty("group-name-praefix");

			ItemCollection user = fetchUser(aUID, ldapCtx);
			// return null if user was not found
			if (user == null)
				return null;

			sDN = user.getItemValueString("dn");

			logger.fine("LDAP fetchGroups for: " + sDN);

			String returnedAtts[] = { "cn" };

			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(returnedAtts);

			String searchFilter = groupSearchFilter.replace("%d", sDN);
			logger.finest("LDAP search:" + searchFilter);

			answer = ldapCtx.search(searchContext, searchFilter, ctls);
			if (answer == null)
				return null;

			while (answer.hasMore()) {
				SearchResult entry = (SearchResult) answer.next();
				String sGroupName = entry.getName();

				// it is not possible to ask for the attribute cn - maybe a
				// domino
				// problem so we take the name....
				/*
				 * Attributes attrs = entry.getAttributes(); Attribute attr =
				 * attrs.get("cn"); if (attr != null) sGroupName = (String)
				 * attr.get(0);
				 */
				sGroupName = sGroupName.substring(3);
				if (sGroupName.indexOf(',') > -1)
					sGroupName = sGroupName.substring(0,
							sGroupName.indexOf(','));

				// test groupname praefix..
				if (groupNamePraefix != null && !"".equals(groupNamePraefix)
						&& !sGroupName.startsWith(groupNamePraefix))
					continue;

				logger.finest("LDAP found Group= " + sGroupName);
				vGroupList.add(sGroupName);
			}

			logger.finest("LDAP found " + vGroupList.size() + " groups");

			groupArrayList = new String[vGroupList.size()];
			vGroupList.toArray(groupArrayList);

			logger.finest("LDAP put groups into cache for '" + aUID + "'");

		} catch (NamingException e) {
			groupArrayList=null;
			logger.warning("Unable to fetch groups for: " + aUID);
			if (logger.isLoggable(java.util.logging.Level.FINEST))
				e.printStackTrace();
		} finally {
			if (answer != null)
				try {
					answer.close();
					answer = null;
				} catch (NamingException e) {

					e.printStackTrace();
				}
		}
		return groupArrayList;
	}

	/**
	 * lookups the single attribute for a given uid
	 * 
	 * 
	 * @param aQnummer
	 * @return
	 * @throws NamingException
	 */
	private String fetchAttribute(String aUID, String sAttriubteName,
			LdapContext ldapCtx) {
		String sAttriubteValue = null;

		// test cache...
		sAttriubteValue = (String) ldapCache.get(aUID + "-" + sAttriubteName);
		if (sAttriubteValue != null)
			return sAttriubteValue;

		if (!enabled)
			return sAttriubteValue;

		NamingEnumeration<SearchResult> answer = null;
		try {

			// try to lookup....
			logger.fine("LDAP fetch attribute: " + sAttriubteName + " for "
					+ aUID);

			String returnedAtts[] = { sAttriubteName };

			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(returnedAtts);

			String searchFilter = dnSearchFilter.replace("%u", aUID);
			logger.finest("LDAP search:" + searchFilter);
			answer = ldapCtx.search(searchContext, searchFilter, ctls);

			if (answer.hasMore()) {

				SearchResult entry = (SearchResult) answer.next();
				Attributes attrs = entry.getAttributes();

				// Attribute attr = null;

				Attribute attr = attrs.get(sAttriubteName);

				if (attr != null) {
					sAttriubteValue = (String) attr.get(0);
					logger.finest("LDAP fetch attribute= " + sAttriubteValue);
				}
			}

			// no luck.?...
			if (sAttriubteValue == null)
				sAttriubteValue = aUID;

			// cache entry
			ldapCache.put(aUID + "-" + sAttriubteName, sAttriubteValue);
		} catch (NamingException e) {
			logger.warning("Unable to fetch attribute '" + sAttriubteName
					+ "' for: " + aUID);
			if (logger.isLoggable(java.util.logging.Level.FINEST))
				e.printStackTrace();
		} finally {
			if (answer != null)
				try {
					answer.close();
				} catch (NamingException e) {

					e.printStackTrace();
				}

		}
		return sAttriubteValue;
	}

	

	/**
	 * This method lookups the ldap context either from a Jndi name
	 * 'LdapJndiName' (DisableJndi=false) or manually if DisableJndi=true.
	 * 
	 * @see http://java.net/projects/imixs-workflow-marty/pages/Useldapgroups
	 * 
	 * @return LdapContext object
	 * @throws NamingException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private LdapContext getDirContext() {
		String ldapJndiName = null;

		LdapContext ldapCtx = null;

		// test if configuration is available
		if (configurationProperties == null) {
			return null;
		}

		// try to load dirContext...

		Context initCtx;
		try {
			initCtx = new InitialContext();

			// test if manually ldap context should be build
			String sDisabled = configurationProperties
					.getProperty("ldap.disable-jndi");
			if (sDisabled != null && "true".equals(sDisabled.toLowerCase())) {
				logger.fine("LDAPGroupLookupService lookup LDAP Ctx manually.....");
				Hashtable env = new Hashtable();
				
				
				// scann all properties starting with 'java.naming'
				Enumeration<Object> keys=configurationProperties.keys();
				while (keys.hasMoreElements()) {
					String sKey=keys.nextElement().toString();
					if (sKey.startsWith("java.naming")) {
						env.put(sKey,
								configurationProperties
										.getProperty(sKey));
						logger.fine("Set key: " + sKey +" with value: " + configurationProperties
										.getProperty(sKey));
					}
					
				}
				
				
				// set default params...
				
				env.put("java.naming.factory.initial", configurationProperties
						.getProperty("java.naming.factory.initial",
								"com.sun.jndi.ldap.LdapCtxFactory"));
				env.put("java.naming.security.authentication",
						configurationProperties
								.getProperty(
										"java.naming.security.authentication",
										"simple"));
				
				
								
				ldapCtx = new InitialLdapContext(env, null);
				logger.finest("Get DirContext Manually successful! ");

			} else {
				// read GlassFish ldap_jndiName from configuration
				ldapJndiName = configurationProperties
						.getProperty("ldap.jndi-name");
				if ("".equals(ldapJndiName))
					ldapJndiName = "org.imixs.office.ldap";
				logger.fine("LDAPGroupLookupService lookup LDAP Ctx from pool '"
						+ ldapJndiName + "' .....");
				ldapCtx = (LdapContext) initCtx.lookup(ldapJndiName);

			}

			logger.fine("LDAPGroupLookupService Context initialized");

		} catch (NamingException e) {
			logger.severe("Unable to open ldap context: " + ldapJndiName);
			if (logger.isLoggable(java.util.logging.Level.FINE))
				e.printStackTrace();
		}

		return ldapCtx;
	}
}
