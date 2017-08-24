# Security
Imixs-Marty provides a multi level security concept which is based on the [Imixs-Workflow ACL concept](http://www.imixs.org/doc/engine/acl.html).


## The UserID

**Note:** In Imixs-Marty a the UserID is always expected in lower-case. therefore a UserID passed by the JAAS login mechanism is automatically lower cased.
Although the internal comparison of UserIDs in the DocumentService EJB is case-insensitive, it is strongly recommended to work only with lower cased UserIDs! For example the Lucene Search for UserIDs is case sensitive. In difference to the UserID a User-Access-Role is case-sensitive as this is handled through the JAAS feature from Java EE.
  
## User Interceptors

Imixs-Marty provides a Profile Service which provides a profile entity for each registered user.  This concept is explained in the section [User Profiles](profiles.html).

In addition to this concept a UserProfile can also be synchronized with a LDAP directory. Therefore the LDAPUserInterceptor can be configured as part of the EJB components. See the [Imixs-Adapters LDAP project](https://github.com/imixs/imixs-adapters/tree/master/imixs-adapters-ldap-ejb) for detailed information. 

## Group Interceptors

GroupInterceptors are a concept to extend the UserNameList which is provided by the [Imixs-Workflow DocumentService EJB](http://www.imixs.org/doc/engine/documentservice.html). The UserNameList provides a String list with all Usernames, Roles and Groups a User belongs to. The UserNameList is used in serveral Services to compare a given access list. 

The Imixs-Marty project provides currently the following Interceptors:

* LDAPGroupInterceptor
* TeamInterceptor

**Note:** The LDAPGroupInterceptor and the TeamInterceptor can not be combined because both use the same _USER_GROUP_LIST_ of the EJB contextData 

### The LDAPGroupInterceptor 

The _LDAPGroupInterceptor_ provides a mechanism to compute the LDAP groups a user belongs to. The Result is put into the _USER_GROUP_LIST_ of the EJB contextData which is read by the DocumentSerivce EJB to grant access by dynamic user roles.

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
 

### The TeamInterceptor

This _TeamInterceptor_  provides a mechanism to compute the orgunits (process, space) a user belongs to. The Result is put into the  _USER_GROUP_LIST_ of the EJB contextData which is read by the DocumentSerivce EJB to grant access by dynamic user roles.

As a orgunit contains 3 general roles (manger, team, assist) the syntax for a group membership computed by this interceptor is as followed:
 
	{ORGUNIT:NAME:ROLE}

e.g.

	{process:Finance:assist}

In addition if theuser is member of one of the roles (team,manager, assist) the general mapping is added 
 
	{process:Finance:member}

The interceptor can be enabled by the deployment descriptor of the DocumentService. See the following example for a ejb-jar.xml configuration

	<assembly-descriptor>
		<!-- TeamInterceptor -->
		<interceptor-binding> 
		    <description>Intercepter to add orgunit-role mapping into EJB Context Data</description> 
		    <ejb-name>DocumentService</ejb-name> 
			<interceptor-class>org.imixs.marty.ejb.TeamInterceptor</interceptor-class> 
		</interceptor-binding>
	</assembly-descriptor>

