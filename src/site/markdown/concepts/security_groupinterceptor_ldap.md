# The LDAPGroupInterceptor 

The _LDAPGroupInterceptor_ provides a mechanism to compute the LDAP groups a user belongs to. The Result is put into the EJB session context attribute: 

    USER_GROUP_LIST

This attribute is read by the DocumentSerivce EJB to grant access by dynamic user roles.

The interceptor can be enabled by the deployment descriptor of the  DocumentService. See the following example for a ejb-jar.xml configuration


	<assembly-descriptor>
		<!-- LDAPGroupInterceptor -->
		<interceptor-binding> 
		    <description>Intercepter to add project-role mapping into EJB Context Data</description> 
		    <ejb-name>DocumentService</ejb-name> 
			<interceptor-class>org.imixs.workflow.ldap.LDAPGroupInterceptor</interceptor-class> 
		</interceptor-binding>
	</assembly-descriptor>
 
Find more information at the [Imixs-Adapters LDAP project](https://github.com/imixs/imixs-adapters/tree/master/imixs-adapters-ldap-ejb) 
 
