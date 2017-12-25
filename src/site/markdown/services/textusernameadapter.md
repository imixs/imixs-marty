# The TextUsernameAdapter

The adapter class org.imixs.marty.ejb.TextUsernameAdapter extends the Imixs-Worlfow Text-Adapter Feature. The adapter automatically 
replaces text fragments with the tag:

	<username>..</username>. 

The values of the item will be replaced with the display name from the corresponding user profile display name.

Example:

	Workitem updated by: <username>namcurrenteditor</username>.
 
This example of a history entry, will replace the namcurrenteditor with the corrsponding profile full username. 
If the username item value is a multiValue object the single values
 can be spearated by a separator

Example:

	Team List: <username separator="<br />">txtTeam</username>

The TextUsernameAdapter works on all text fragments defined by a BPMN model. See the section [AdaptText](http://www.imixs.org/doc/engine/adapttext.html) for more details.