# The Imixs Office Worklfow User Database

Imixs Office Workflow provides a internal user database. This database can be used to manage user IDs  and user groups and authenticate users with a JDBCRealm. To install these database tables make sure  that the persistence.xml file contains a reference to the actual imixs-office-ejb module


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

When the Imxis-Marty components are deployed the first time and the imixs.properties 'setup.mode=auto' is set, a default admin account with the userid='admin' 
and the password 'adminadmin' will be created automatically. The password for this account should be changed after the first login!

### How to Restore the Default User 'admin'

If the admin account need to be restored, first the account need to be deleted in the user database tables:

* USERID_USERGROUP 
* USERID 
* USERGROUP. 

After a restart of the application the default account will be recreated.

## How to enable the User DB Frontend

If the BASIC configuation flag 'keyEnableUserDB' is set to 'true', a database user management in the frontend of Imixs-Office-Worklfow appears. 

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