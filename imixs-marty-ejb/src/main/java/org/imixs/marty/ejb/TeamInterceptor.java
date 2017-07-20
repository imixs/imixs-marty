package org.imixs.marty.ejb;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.imixs.workflow.engine.DocumentService;

/**
 * This Intercepter class provides a mechanism to compute the orgunits (process,
 * space) a user belongs to. The Result is put into the EJB contextData which is
 * read by the DocumentSerivce EJB to grant access by dynamic user roles.
 * 
 * As a orgunit contains 3 gerneal roles (manger, team, assist) the sytax for a
 * group membership is as followed:
 * 
 * {ORGUNIT:NAME:ROLE}
 * 
 * e.g.
 * 
 * {process:Finance:Assist}
 * 
 * In addition if theuser is member of one of the roles (team,manager,
 * assist) the general mapping is added <br />
 * {process:Finance:member}
 * 
 * The interceptor can be enabled by the deployment descriptor of the
 * DocumentService. See the following example for a ejb-jar.xml configuration
 * 
 * <pre>
 * {@code
 * 
 * 	<assembly-descriptor>
		<!-- TeamInterceptor -->
		<interceptor-binding> 
		    <description>Intercepter to add orgunit-role mapping into EJB Context Data</description> 
		    <ejb-name>DocumentService</ejb-name> 
			<interceptor-class>org.imixs.marty.ejb.TeamInterceptor</interceptor-class> 
		</interceptor-binding>
	</assembly-descriptor>
 * }
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */

public class TeamInterceptor {

	@EJB
	TeamLookupService lookupService;

	@Resource
	SessionContext ejbCtx;

	private static Logger logger = Logger.getLogger(TeamInterceptor.class.getName());

	/**
	 * The interceptor method injects the LDAP groups into the contextData map. The
	 * method only runs for the method calls 'findAllEntities', 'save' and 'load'
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {

		// test method name
		String sMethod = ctx.getMethod().getName();
		if ("getUserNameList".equals(sMethod)) {

			logger.finest("TeamInterceptor Method=" + sMethod);

			String sUserID = ejbCtx.getCallerPrincipal().getName();

			String[] sGroups = lookupService.findOrgunits(sUserID);

			ctx.getContextData().put(DocumentService.USER_GROUP_LIST, sGroups);

			if (logger.isLoggable(java.util.logging.Level.FINEST)) {
				String groupListe = "";
				for (String aGroup : sGroups)
					groupListe += "'" + aGroup + "' ";
				logger.finest("resolved UserGroups for '" + sUserID + "' = " + groupListe);
			}
		}

		return ctx.proceed();
	}

}