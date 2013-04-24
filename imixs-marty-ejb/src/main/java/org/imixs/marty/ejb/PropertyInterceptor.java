package org.imixs.marty.ejb;

import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.imixs.workflow.ItemCollection;

/**
 * This Intercepter class provides a mechanism to provide database based
 * properties for the imixs workflow PropertyService. The PropertyInterceptor
 * intercepts the method getProperties() from the
 * org.imixs.workflow.jee.util.PropertyService and checks if the configuration
 * entity 'BASIC' exists. If the configuration entity provides the property
 * field 'properties' the values of this item will overwrite the current
 * settings of the imixs.property configuration file. To avoid multiple checks of
 * the cached object 'properties' the interceptor adds an internal flag into the
 * properties to indicated the custom properties are already updated.
 * 
 * Name of this field 'org.imixs.marty.ejb.PropertyInterceptor.intercepted=true'
 * 
 * The configuration entity 'BASIC' can be controlled by an application (see
 * Imixs-Office-Workflow /pages/admin/config.xhtml)
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */

public class PropertyInterceptor {

	private static String INTERCEPTOR_FLAG = "org.imixs.marty.ejb.PropertyInterceptor.intercepted";

	private static Logger logger = Logger.getLogger(PropertyInterceptor.class
			.getName());

	@EJB
	ConfigService configService;

	/**
	 * The interceptor method tests if a configuration entity with the name
	 * 'BASIC' exists. If so the values of the property 'properties' will be
	 * used to overwrite settings of the imixs.property file which is loaded by
	 * the getProperties() method of the Imixs PropertyService ejb.
	 * 
	 * The interceptor runs only in a 'reset' method
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	@AroundInvoke
	public Object intercept(InvocationContext ic) throws Exception {
		Object result = null;
		try {
			result = ic.proceed();
			return result;
		} finally {
			// test method name
			String sMethod = ic.getMethod().getName();
			if ("getProperties".equals(sMethod)) {

				// check if the properties are already updated
				if (!"true".equals(((Properties) result)
						.getProperty(INTERCEPTOR_FLAG))) {
					// get the BASIC configuration entity
					ItemCollection basicConfig = configService
							.loadConfiguration("BASIC");
					if (basicConfig != null) {
						// read properties
						Vector<?> v = (Vector<?>) basicConfig
								.getItemValue("properties");
						if (v.size() > 0) {
							logger.info("[PropertyInterceptor] Update imixs.properties");
							for (Object o : v) {
								
								
								String sProperty=(String)o;
								int ipos=sProperty.indexOf('=');
								if (ipos>0) {
									String sKey=sProperty.substring(0,sProperty.indexOf('='));
									
									String sValue=sProperty.substring(sProperty.indexOf('=')+1);
									
									logger.fine("[PropertyInterceptor] Overwrite property/value: "
										+ sKey + "=" + sValue);
									((Properties) result).setProperty(sKey, sValue);
								}
							}
						}

					}

					// update intercepted flag...
					((Properties) result).setProperty(INTERCEPTOR_FLAG, "true");
				}

			}
		}

	}

}
