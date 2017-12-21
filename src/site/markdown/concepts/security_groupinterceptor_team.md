# The TeamInterceptor

This _TeamInterceptor_  provides a mechanism to compute the orgunits (process, space) a user belongs to. The Result is put into the EJB session context attribute: 

    USER_GROUP_LIST
    
This attribute is read by the DocumentSerivce EJB to grant access by dynamic user roles.

As a orgunit contains 3 general roles (manger, team, assist) the syntax for a group membership computed by this interceptor is as followed:
 
	{ORGUNIT:NAME:ROLE}

e.g.

	{process:Finance:assist}

## General Role Mappings

In case the user is at least member of one of the roles '_team_', '_manager_' or '_assist_', the general mapping '_member_' is added to the group list: 
 
	{process:Finance:member}
	
Also a anonymous role is added for each of the roles '_team_', '_manager_' or '_assist_'. For example if the user is 'assist' of the Process 'Finance' the anonymous assist role for process will be added:


	{process:assist}
	
## Deployment	

The interceptor can be enabled by the deployment descriptor of the DocumentService. See the following example for a ejb-jar.xml configuration



	<!-- Interceptors -->
	<interceptors>
	    ....
		<interceptor>
			<interceptor-class>org.imixs.marty.ejb.TeamInterceptor</interceptor-class>
		</interceptor>
	</interceptors>
	<assembly-descriptor>
	   ...
		<!-- TeamInterceptor -->
		<interceptor-binding> 
		    <description>Intercepter to add orgunit-role mapping into EJB Context Data</description> 
		    <ejb-name>DocumentService</ejb-name> 
			<interceptor-class>org.imixs.marty.ejb.TeamInterceptor</interceptor-class> 
		</interceptor-binding>
	</assembly-descriptor>

