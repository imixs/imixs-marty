#Installation Guide WildFly

In the following section we explain the general stepps to install Imixs-Office-Workflow  on a JBoss / WildFly Server. The installation is in general simmilar to other application  servers but some details of WildFly will be explained here. 
  
  
##JPA – EclipseLink

JBoss and Wildfly use per default the JPA implementation Hibernate.  To deploy your application with the EclipseLink JPA implementaion you can add  the EclipseLink.jar into the folder

    modules/system/layers/base/org/eclipse/persistence/main

Additional you need to update the configuration in the config file: 

    modules/system/layers/base/org/eclipse/persistence/main/module.xml

Add the following tag into the section ‘resources'
 
    <resource-root path="eclipselink.jar">
        <filter>
                <exclude path="javax/**" />
        </filter>
    </resource-root>

If you happen to leave the EclipseLink version number in the jar name,  the module.xml should reflect that.
 

If you use the ‘org.eclipse.persistence.jpa.PersistenceProvider’ in the persistence.xml  it is important to add the org.jipijapa.eclipselink.JBossArchiveFactoryImpl afterwards  with the following command form the WildFly bin/ folder when WildFly is running:

    ./jboss-cli.sh --connect '/system-property=eclipselink.archive.factory:add(value=org.jipijapa.eclipselink.JBossArchiveFactoryImpl)'

Note: If you run WildFly with a different portrange you need to add the ‘controller’ param to the jboss-cli script. See the following example accessing the admin console with port 9991:

    ./jboss-cli.sh --connect controller=127.0.0.1:9991 '/system-property=eclipselink.archive.factory:add(value=org.jipijapa.eclipselink.JBossArchiveFactoryImpl)'

You may need to also change the ip address to the configured management inet-address! The command will finally change the standalone.xml configuration file and adds the following entry:

    <system-properties>
     ...
    <property name="eclipselink.archive.factory" value="org.jipijapa.eclipselink.JBossArchiveFactoryImpl"/>
    </system-properties>


Additional information can be found {{{https://docs.jboss.org/author/display/WFLY8/JPA+Reference+Guide#JPAReferenceGuide-UsingEclipseLink}here}}.

##Datasource Configuration

To use a JDBC driver you can simply deploy the driver jar into the running Wildfly.  A new datasource configuration can be directly added into the standalone.xml file after the server was stopped.
 
For PostgreSQL a database connection looks like this:

    <datasource jta="true" jndi-name="java:/jdbc/imixs_office" pool-name="imixs_office" enabled="true" use-ccm="true">
                    <connection-url>jdbc:postgresql://localhost/office</connection-url>
                    <driver-class>org.postgresql.Driver</driver-class>
                    <driver>postgresql-9.3-1102.jdbc41.jar</driver>
                    <security>
                        <user-name>postgres</user-name>
                        <password>xxxx</password>
                    </security>
                    <validation>  
                        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
                        <validate-on-match>true</validate-on-match>  
                        <background-validation>false</background-validation>  
                    </validation>                          
                    <statement>
                        <prepared-statement-cache-size>32</prepared-statement-cache-size>
                        <share-prepared-statements>true</share-prepared-statements>
                    </statement>
                </datasource>
                
                 

For MySQL the datasource configuration should look like this:

       <datasource jta="true" jndi-name="java:/jdbc/imixs_office" pool-name="imixs_office" enabled="true" use-ccm="true" statistics-enabled="false">
                    <connection-url>jdbc:mysql://localhost:3306/imixs_office?autoReconnect=true</connection-url>
                    <driver-class>com.mysql.jdbc.Driver</driver-class>
                    <driver>mysql-connector-java-5.1.7-bin.jar</driver>
                    <security>
                        <user-name>root</user-name>
                        <password>xxx</password>
                    </security>
                    <validation>  
                        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"></valid-connection-checker>
                        <validate-on-match>true</validate-on-match>  
                        <background-validation>false</background-validation>  
                    </validation>                      
                    <timeout>
                        <set-tx-query-timeout>false</set-tx-query-timeout>
                        <blocking-timeout-millis>0</blocking-timeout-millis>
                        <idle-timeout-minutes>0</idle-timeout-minutes>
                        <query-timeout>0</query-timeout>
                        <use-try-lock>0</use-try-lock>
                        <allocation-retry>0</allocation-retry>
                        <allocation-retry-wait-millis>0</allocation-retry-wait-millis>
                    </timeout>
                    <statement>
                        <share-prepared-statements>false</share-prepared-statements>
                    </statement>
                 </datasource>

Note: The valid-connection-checker class is the recommended way to avoid connection failures when the MySQL Server is restarted. 
 
For MySQL the valid-connection-checker class is 
 
    org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker

for PostgreSQL the class is

    org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker

An alternative is adding  the query param  ‘?autoReconnect=true’ to the JDBC url. 
 
Its also recommended to set jta to true.
 
##Security Realm

To add the imixs Realm as a JDBC Security Realm into Wildfly open 

    <Configuration->Subsystems->Security->Security Domains>

This is an example for a Database Security configuration:

Attributes:

 * Name: imixsrealm
  
 * CacheType: Default

Authentication:

  * Code: Database
  
  * Flag: required

Module Options:

  * dsJndiName=java:/jdbc/imixs_office
    
  * hashAlgorithm=SHA-256
   
  * hashEncoding=hex
    
  * principalsQuery=select PASSWORD from USERID where ID=?
    
  * rolesQuery=select GROUP_ID,'Roles' from USERID_USERGROUP where ID=?
    
  * unauthenticatedIdentity=anonymous

The security domain configuration in the standalone.xml configuraiton file than looks like this:


	    <security-domain name="imixsrealm">
	      <authentication>
	 <login-module code="Database" flag="required">
	 <module-option name="dsJndiName" value="java:/jdbc/imixs_office"/>
	 <module-option name="hashAlgorithm" value="SHA-256"/>
	 <module-option name="hashEncoding" value="hex"/>
	 <module-option name="principalsQuery" value="select PASSWORD from USERID where ID=?"/>
	 <module-option name="rolesQuery" value="select GROUP_ID,'Roles' from USERID_USERGROUP where ID=?"/>
	 <module-option name="unauthenticatedIdentity" value="anonymous"/>
	 </login-module>
	 </authentication>
	 </security-domain>

To finish the configuration, add the file jboss-web.xml in the folder WEB-INF of your web module with the following content.  This file is used to define the security domain used by the application:

    <?xml version="1.0" encoding="UTF-8"?>
    <jboss-web>
      <security-domain>imixsrealm</security-domain>
    </jboss-web>


##Role Mapping

In different to GlassFish for WildFly there is no explicit role-group mapping necessary. So you need no special deployment descriptor  like the /WEB-INF/glassfish-application.xml. The Roles defined by a application can be directly used in the security configuration for a user.

See also: http://wildfly.org/news/2014/02/06/GlassFish-to-WildFly-migration/

In case you have existing group mappings (e.g. in a database group table or in a LDAP directory) you can add the mapping by defining a  file app.properties, where app is the name of the security domain, as defined above.  Save this file in the folder WILDFLY_HOME/standalone/configuration or WILDFLY_HOME/domain/configuration to be taken into account.

This is an example of my file imixsrealm.properties which mapps the group names to roles defined in my application:

	IMIXS-WORKFLOW-Reader=org.imixs.ACCESSLEVEL.READERACCESS
	IMIXS-WORKFLOW-Author=org.imixs.ACCESSLEVEL.AUTHORACCESS
	IMIXS-WORKFLOW-Editor=org.imixs.ACCESSLEVEL.EDITORACCESS
	IMIXS-WORKFLOW-Manager=org.imixs.ACCESSLEVEL.MANAGERACCESS

Groups are listed on the left of the equal operator and roles are listed on the right. In the example above,  users in the group ‘IMIXS-WORKFLOW-Reader’ fulfill the role ‘org.imixs.ACCESSLEVEL.READACCESS’.

Note: To activate this role mapping the security domain need a login-module section for the RoleMapping:

	 <security-domain name="imixsrealm">
	 <authentication>
	 <login-module code="Database" flag="required">
	 <module-option name="dsJndiName" value="java:/jdbc/imixs_office"/>
	 <module-option name="hashAlgorithm" value="SHA-256"/>
	 <module-option name="hashEncoding" value="hex"/>
	 <module-option name="principalsQuery" value="select PASSWORD from USERID where ID=?"/>
	 <module-option name="rolesQuery" value="select GROUP_ID,'Roles' from USERID_USERGROUP where ID=?"/>
	 <module-option name="unauthenticatedIdentity" value="anonymous"/>
	 </login-module>
	 <login-module code="RoleMapping" flag="required">
	 <module-option name="rolesProperties" value="file:${jboss.server.config.dir}/imixsrealm.properties"/>
	 <module-option name="replaceRole" value="false"/>
	 </login-module>
	 </authentication>
	 </security-domain>

 
 
##WebServices – RestEasy Configuration
Using RestServices makes it necessary to change things in the web.xml file because Jersey (used by GlassFish)  and RestEasy (used by Wildfly) have different configurations. In GlassFish V3 a RestService configuration for Jersey looks typically like this:

	<servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
	 <servlet>
	 <servlet-name>ImixsRestService</servlet-name>
	 <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
	 <init-param>
	 <param-name>com.sun.jersey.config.property.packages</param-name>
	 <param-value>org.imixs.workflow.jaxrs</param-value>
	 </init-param>
	 <load-on-startup>1</load-on-startup>
	 </servlet>

In WildFly you need to chage the configuration like this:

	<context-param>
	 <param-name>resteasy.scan</param-name>
	 <param-value>true</param-value>
	 </context-param>
	 <context-param>
	 <param-name>resteasy.servlet.mapping.prefix</param-name>
	 <param-value>/rest</param-value>
	 </context-param>
	 <listener>
	 <listener-class>
	 org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap
	 </listener-class>
	 </listener>
	 <servlet>
	 <servlet-name>ImixsRestService</servlet-name>
	 <servlet-class>
	 org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher
	 </servlet-class>
	 </servlet>

 
##Mail Configuration

In different to GlassFish, WildFly needs a valid mail configuration if a mail resource is bound to the deployed configuration.  So before your can test a application with a mail resource you need to create it!.

Using mail sessions makes it necessary to know some details about JNDI Resource names.  In GlassFish you can configure a jndi resource with any name you choose.  For example: mail/org.imixs.workflow.mail

The configuration in your ejb-jar.xml or web.xml file looks than like this:

	<!-- Mail Configuration -->
	<env-entry>
	<description> Mail Plugin Session name</description>
	<env-entry-name>IMIXS_MAIL_SESSION</env-entry-name>
	<env-entry-type>java.lang.String</env-entry-type>
	<env-entry-value>mail/org.imixs.workflow.mail</env-entry-value>
	</env-entry>
	<resource-ref>
	<res-ref-name>mail/org.imixs.workflow.mail</res-ref-name>
	<res-type>javax.mail.Session</res-type>
	<res-auth>Container</res-auth>
	<res-sharing-scope>Shareable</res-sharing-scope>
	</resource-ref>


In WildFly the name ‘mail/org.imixs.workflow.mail’ is not allowed to be used as a JNDI resource name. You allways have to start with the prafix ‘java:/’ or ‘java:jboss:/’. This means your jndi mail resource name would be ‘java:/mail/org.imixs.workflow.mail’ And so you also need to change the res-ref-name tag in your ejb-jar.xml or web.xml like this:

	<!-- Mail Configuration -->
	<env-entry>
	<description> Mail Plugin Session name</description>
	<env-entry-name>IMIXS_MAIL_SESSION</env-entry-name>
	<env-entry-type>java.lang.String</env-entry-type>
	<env-entry-value>mail/org.imixs.workflow.mail</env-entry-value>
	</env-entry>
	<resource-ref>
	<res-ref-name>java:/mail/org.imixs.workflow.mail</res-ref-name>
	<res-type>javax.mail.Session</res-type>
	<res-auth>Container</res-auth>
	<res-sharing-scope>Shareable</res-sharing-scope>
	</resource-ref>

In general Wildfly use always the java:/ prafix in jndi names. Se be careful about this  small change in the naming.

###beans.xml

If you extend the EJB Module with custom EJBs you need to take care adding the beans.xml file into the /src/main/resources/META-INF/ folder. The file can be an empty xml file:

	<?xml version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="
	      http://java.sun.com/xml/ns/javaee 
	      http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
	</beans>


###Set Mail-Session in standalone.xml

To add a mail resource to WildFly you can modify the standalone.xml file. Add the new mail resource into the following tag entry

	<subsystem xmlns="urn:jboss:domain:mail:2.0">
	 <mail-session name="default" jndi-name="java:jboss/mail/Default">
	 <smtp-server outbound-socket-binding-ref="mail-smtp"/>
	 </mail-session>
	 <mail-session name="java:/mail/org.imixs.workflow.mail" jndi-name="java:/mail/org.imixs.workflow.mail"/>
	</subsystem>

The outgoing mail server is configured in the socket-binding-group section:

    <outbound-socket-binding name="mail-smtp">
     <remote-destination host="localhost" port="25"/>
    </outbound-socket-binding>
 