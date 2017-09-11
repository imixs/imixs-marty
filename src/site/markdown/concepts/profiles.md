# User Profile Management 

Imixs-Marty provides a user profile management. A User Profile is automatically created after a user logged in the first time. 

The creation of a new profile is triggered by the userController CDI Bean in the @PostConstruct method. The creation is managed by the EJB *ProfileService*.
The User Profiles are stored in the document database with the type attribute *'profile'*.



## Autoprocess a New User Profile
When a user profile is created the first time, the profile will be automatically be processed by the Imixs-Workflow engine with the *ProcessID=200* and the *ActivityID=5*. 
The System Model files *system-de.bpmn* and *system-en.bpmn* provide a valid BPMN model. 


## Autoprocess a User Profile During the Login
When a user profile is created the first time, additional the user profile can be processed by the Imixs-Workflow engine. 
To enable this 'auto process mode'  the system property *'profile.autoProcessOnLogin'* can be set with a BPMN Event ID:

	profile.autoProcessOnLogin=10
	
## The User Database
If the [User-Database](userdb.html) is enabled, new user profiles can also be created manually by the administrator. In this case a UserID and a Password has to be provided along the creation process. 



## The Marty ProfilePlugin

The Plugin org.imixs.marty.plugins.ProfilePlugin provides additional business logic for profile entities and workitems. The plugin is called by the System Workflow Model. When a userProfile is  processed (typically when a User logged in) the plugin validates the username and email address.


### UserID validation

The userID is automatically validated for uniqueness. It is not possible to store two profiles with the same userid (txtName). In addtion the userid is validated against a Input Pattern. The default pattern is:

    ^[A-Za-z0-9.@\\-\\w]+
    
The pattern can be changed by the imixs.property 

    security.userid.input.pattern=
    
With the imixs.property '_security.userid.input.mode_' the userID can be forced to lower or upper case

    security.userid.input.mode=
    
Possible values are: '_lowercase_', '_uppercase_' and '_none_'

### Email Validation

In the default configuration a E-Mail attribute is mandatory for a profile. The Profile validates the email also for uniqueness. In this cas it is not possible to store two profiles with the same email (txtEmail).

To deactivate the e-mail validation the system property '_security.email.unique_' can be set to true:

    # force unique email addresses
    security.email.unique=true

To deactivate the validation for unique email the property can set to 'false'. 

	# allow duplicated email addresses
    security.email.unique=false

## How to Translate userIDs into a User Name

The Marty ProfilePlugin provides a mechanism to translate userIDs in a BPMN Event or BPMN Task with the corresponding User name stored in the User Profile
This mechanism is controlled by the ProfileService EJB.
See the following example where a BPMN Event text (e.g. History text or Email Body) 

	Request saved by <username>namcurrenteditor</username>.

The UserID stored in the attribute namcurrentedtior is translated into the User name stored in the User Profile.

It is also possible to translate list of user entries. See the next example to translate a team list in a mail text:


	Current Team: <username sparator=",">namTeam</username>.

In this example the usernames stored in the property 'namTeam' are replaced with the corresponding user names and separated with a ','
 
 