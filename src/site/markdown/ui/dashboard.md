# The Dashboard

Imixs-office-Workflow provides a dashboard feature to display tasks ins portlet style.

TBD.....


## Customize Portlets

It is possible to customize the query selector of a singel portlet by changing the faces-config-custom.xml.

Following example shows how to filter minute items from the status portlet



	<!-- do not display minute items -->
	<managed-bean>
		<managed-bean-name>portletWorklistCreator</managed-bean-name>
		<managed-bean-class>org.imixs.workflow.faces.workitem.ViewController</managed-bean-class>
		<managed-bean-scope>view</managed-bean-scope>
		<managed-property>
			<property-name>pageSize</property-name>
			<property-class>int</property-class>
			<value>5</value>
		</managed-property>
		<managed-property>
			<property-name>sortBy</property-name>
			<property-class>java.lang.String</property-class>
			<value>$modified</value>
		</managed-property>
		<managed-property>
			<property-name>sortReverse</property-name>
			<property-class>boolean</property-class>
			<value>true</value>
		</managed-property>
	
		<managed-property>
			<property-name>query</property-name>
			<property-class>java.lang.String</property-class>
			<value>(type:"workitem" AND namcreator:"#{loginController.remoteUser}") AND NOT (minutetype:"minuteitem")</value>
		</managed-property>
	</managed-bean>