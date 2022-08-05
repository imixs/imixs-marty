# The TeamRoleWildcardAdapter

The Marty text adapter '_TeamRoleWildcardAdapter_' can be used to compute team roles using the '?' wildcard.
 
In combination with the [TeamInterceptor](../services/teaminterceptor.html) the '_TeamRoleWildcardAdapter_' computes a Orgunit Roles associated with the current workitem. To add the team role for the orgunit currently associated with the workitem into the ACL the following role definition can be added:


	{process:?:team}
	
<img src="../images/acl002.png" style="width: 75%;"/>

The _TeamRoleWildcardAdapter_ will lookup the associated process orgunit and compute the role name (e.g. from the orgunit 'Finance'):

    {process:8838786e-6fda-4e0d-a76c-5ac3e0b04071:team}

**Note:** The _TeamRoleWildcardAdapter_ computes the role based on the uniqueID only.  