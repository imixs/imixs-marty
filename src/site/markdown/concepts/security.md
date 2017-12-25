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

* [LDAPGroupInterceptor](security_groupinterceptor_ldap.html)
* [TeamInterceptor](security_groupinterceptor_team.html)

**Note:** The LDAPGroupInterceptor and the TeamInterceptor can not be combined because both use the same _USER_GROUP_LIST_ of the EJB contextData 
