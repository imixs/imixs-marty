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



# The User Database

Imixs-Marty provides a internal user database. This database can be used to manage user IDs  and user groups and authenticate users with a JDBCRealm. To install these database tables make sure  that the persistence.xml file contains a reference to the actual imixs-office-ejb module


        <persistence-unit name="org.imixs.workflow.jpa" transaction-type="JTA">     
                <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>    
                <jta-data-source>${imixs-office.jta-data-source}</jta-data-source>
                <jar-file>imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
                <jar-file>imixs-marty-ejb-${org.imixs.marty.version}.jar</jar-file>
                .....                       
        </persistence-unit>       


After the first deployment the connected database contains three tables:

 * USERID - listing userids and passwords
 
 * USERGROUP - listing group names
 
 * USERID_USERGROUP - contains the assignment of userids to groups.



## The Default User 'admin'

When the Imxis-Marty components are deployed the first time and the imixs.properties 'security.setup.mode=auto' is set, a default admin account with the UserID='admin' and the password 'adminadmin' will be created automatically. The password for this account should be changed after the first login!

The initialization of the userdb is triggered by the *[SetupService](./install/setup.html)*. This service will call the *UserGroupService* EJB method *initUserIDs()* which is responsible for the creation of the admin account.

**Note:** 

1. A admin account will only be created if no default admin account is yet stored in the table 'UserId'. 

2. The admin account will be created in lower case per default. Only if the property 'security.userid.input.mode' is set to 'UPPER' the account will be created in upper case ('ADMIN').

3. To disable the creation of an admin account the imixs.property 'security.setup.mode' must be set to 'NONE'.

### How to Restore the Default User 'admin'

If the admin account need to be restored, first the account need to be deleted in the user database tables:

* USERID_USERGROUP 
* USERID 
* USERGROUP. 

After a restart of the application the default account will be recreated.

## How to enable the User DB Frontend

If the BASIC configuation flag 'keyEnableUserDB' is set to 'true', a database user management in the frontend of Imixs-Office-Workflow appears. 

To change the UserDB flag, navigate to 'Administration->Configuration' in Imixs-Office-Workflow and enable the option 'UserDB'.

In the field 'Groups' you should at least configure the following default groups:

	IMIXS-WORKFLOW-Manager
	IMIXS-WORKFLOW-Author
	IMIXS-WORKFLOW-Reader
	IMIXS-WORKFLOW-Editor

**Note:** do not leave this field empty if you setup the configuration the first time! Remove spaces at the beginning  and the end of a role name!

Now new user profiles can be added. In the profile form you will now also see  the input fields 'Password' and 'Groups' which are used to set a default password and assign a user to one of  the predefined user groups.

If you are working with a custom build from Imixs Office Workflow, you can also add additional Groups into the  configuration depending on your current implementation.

### Update the Workflow System Model

To synchronize the user profiles with the user database it is necessary to add the following Worklfow Plug-In to  the system models system-de.bpmn and system-en.bpmn

	org.imixs.marty.ejb.security.UserGroupPlugin

**Note:** If you did not add this plugin into the system model the user profiles will not be synchronized with the 
 user database! 
 
## Adapt the Setup Phase

You can adapt the setup by observing the CDI Event _SetupEvent_
