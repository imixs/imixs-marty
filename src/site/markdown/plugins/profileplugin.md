# The ProfilePlugin

The marty ProfilePlugin supports additional business logic for profile entities. The Plugin is used by the System Workflow 
when a userProfile is processed (typically when a User logged in).

## Translate UserIDs into Display Names

In additon the Plugin provides a mechanism to translate elements of an activityEntity to replace placeholders for a user id with the corresponding user name. There for the plugin uses the profileService EJB
 
Example:

    Workitem updated by: <username>namcurrenteditor</username>.

This will replace the namcurrenteditor with the corrsponding profile full username
 

    org.imixs.marty.plugins.ProfilePlugin
 
When your application is using random userIDs for security reasons, then the UserIDs should not be displayed to other users. 
The ProfilePlugin provides a way to mask those userIds. 

For example a history entry like this should be avoided:

	u12345345 approved request.


To translate the userID the following format can be used: 


	<username>namcurrenteditor</username> approved request.

The ProfilePlugin will automatically translate the userid into the display name.

See also the [section concepts/profiles](../concepts/profiles.html).
