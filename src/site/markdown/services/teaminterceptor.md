# The Team Interceptor

The *org.imixs.marty.ej.TeamIntercepter* class provides a mechanism to compute the orgunits (process,
space) a user belongs to. The Result is put into the EJB contextData which is
 read by the DocumentSerivce EJB to grant access by dynamic user roles.

 
As a orgunit contains 3 gerneal roles (manger, team, assist) the sytax for a
group membership is as followed:

    {ORGUNIT:NAME:ROLE}

e.g.

    {process:Finance:Assist}
 
In addition if the user is member of one of the roles (team,manager, assist) the general mapping is added 

    {process:Finance:member}

## Configuration
    
The interceptor can be enabled by the deployment descriptor of the *DocumentService*. See the following example for a ejb-jar.xml configuration

    <assembly-descriptor>
		<!-- TeamInterceptor -->
		<interceptor-binding> 
		    <description>Intercepter to add orgunit-role mapping into EJB Context Data</description> 
		    <ejb-name>DocumentService</ejb-name> 
			<interceptor-class>org.imixs.marty.ejb.TeamInterceptor</interceptor-class> 
		</interceptor-binding>
	</assembly-descriptor>
  
 
 