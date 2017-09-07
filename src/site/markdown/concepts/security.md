# Security
Imixs-Marty provides a security concept which is based on the [Imixs-Workflow ACL](http://www.imixs.org/doc/engine/acl.html). 


## The UserID

A UserID in Imixs-Workflow is case sensitive as defined by the JAAS login mechanism. Also the internal comparison of UserIDs in the DocumentService EJB is case-sensitive. It is strongly recommended to take care of the correct transport of the UserID through the different application layers. For example a UserID format can be predefined by an external login system like a LDAP directory. 
In that case the Imixs-Marty application should take respect to thus an external format of the UserID. 

As the UserID is not only part of the login process but also for the group and access management, Imixs-Marty provides additional features to deal with UserIDs in a specific format. 

### UserID Input Mode
The *input.mode' defines if a UserID contains only upper or lower case characters, or can contain mixed characters. The input mode can be set by the imixs.property file: 

	# UPPERCASE, LOWERCASE, NONE
	security.userid.input.mode=


 * security.userid.input.mode=LOWERCASE (default) - UserIDs are automatically lower cased by the Imixs-Marty front-end controllers
 * security.userid.input.mode=UPPERCASE - UserIDs are automatically upper cased by the Imixs-Marty front-end controllers
 * security.userid.input.mode=NONE - UserIDs are not converted in upper or lower case (default)

The default mode for the *security.userid.input.mode* is 'LOWERCASE'.


### UserID Input Pattern
With the *input.pattern*  a UserID can be validated by the Imixs front-end controllers. The pattern is a regular expression and is used for internal validation. 

	# e.g. [A-Za-z]
	security.userid.input.pattern=

The default pattern for the user input is:

	^[A-Za-z0-9.@\\-\\w]+
  
  
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
 

### The TeamInterceptor

This _TeamInterceptor_  provides a mechanism to compute the orgunits (process, space) a user belongs to. The Result is put into the EJB session context attribute: 

    USER_GROUP_LIST
    
This attribute is read by the DocumentSerivce EJB to grant access by dynamic user roles.

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

