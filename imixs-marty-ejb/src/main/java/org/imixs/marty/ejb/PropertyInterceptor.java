package org.imixs.marty.ejb;

import java.util.Collection;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.jpa.Document;

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

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

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
					
					ItemCollection basicConfig = getBasicConfigurationDocument();
					if (basicConfig != null) {
						// read properties
						Vector<?> v = (Vector<?>) basicConfig
								.getItemValue("properties");
						if (v.size() > 0) {
							logger.info("Update imixs.properties");
							for (Object o : v) {
								
								
								String sProperty=(String)o;
								int ipos=sProperty.indexOf('=');
								if (ipos>0) {
									String sKey=sProperty.substring(0,sProperty.indexOf('='));
									
									String sValue=sProperty.substring(sProperty.indexOf('=')+1);
									
									logger.fine("Overwrite property/value: "
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

	
	
	/**
	 * Returns the 'BASIC' configuration Document entity by using the EntityManager native.
	 * 
	 * @param query
	 *            - JPQL statement
	 * @return
	 * 
	 */
	public ItemCollection getBasicConfigurationDocument() {
		// select all documenty by type
		String query = "SELECT document FROM Document AS document ";
		query += " WHERE document.type = 'configuration'";
		query += " ORDER BY document.created DESC";
		Query q = manager.createQuery(query);

		@SuppressWarnings("unchecked")
		Collection<Document> documentList = q.getResultList();
		if (documentList != null) {

			// filter resultset by read access
			for (Document doc : documentList) {
				// check name = "BASIC"
				ItemCollection configDocument=new ItemCollection(doc.getData());
				if (configDocument.getItemValueString("txtname").equals("BASIC")) {
					return configDocument;
				}
			}
		}
		logger.fine("BASIC configuration not found.");
		return null;
	}
}
