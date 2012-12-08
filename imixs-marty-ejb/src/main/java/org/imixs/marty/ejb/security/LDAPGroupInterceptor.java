package org.imixs.marty.ejb.security;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.imixs.workflow.jee.ejb.EntityService;

/**
 * This Intercepter class provides a mechanism to compute the LDAP groups the
 * user belongs to. The Result is put into the EJB contextDAta which is read by
 * the EntitySerivce EJB to grant access by dynamic user roles.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */

public class LDAPGroupInterceptor {

	@EJB
	LDAPLookupService lookupService;

	@Resource
	SessionContext ejbCtx;

	private static Logger logger = Logger.getLogger("org.imixs.office");

	/**
	 * The interceptor method injects the LDAP groups into the contextData map.
	 * The method only runs for the method calls 'findAllEntities', 'save' and
	 * 'load'
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {

		// test if ldap lookup service is available
		if (lookupService.isEnabled()) {
			// test method name
			String sMethod = ctx.getMethod().getName();
			if ("findAllEntities".equals(sMethod) || "save".equals(sMethod)
					|| "load".equals(sMethod)
					|| "getUserNameList".equals(sMethod)) {

				logger.finest("LDAP Interceptor Method: " + sMethod);

				String sUserID = ejbCtx.getCallerPrincipal().getName();

				String[] sGroups = lookupService.findGroups(sUserID);

				ctx.getContextData()
						.put(EntityService.USER_GROUP_LIST, sGroups);

				if (logger.getLevel().intValue() <= java.util.logging.Level.FINEST
						.intValue()) {
					String groupListe = "";
					for (String aGroup : sGroups)
						groupListe += aGroup + " ";
					logger.finest("LDAP resolved UserGroups: " + groupListe);
				}
			}
		}

		return ctx.proceed();
	}

}
