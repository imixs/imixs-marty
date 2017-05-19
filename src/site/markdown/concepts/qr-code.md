# QR-Code

The QR-Code feature is part of the [Imixs-Adapters project](https://github.com/imixs/imixs-adapters/tree/master/imixs-adapters-qrcode). 


In Imixs-Office-Workflow the search feature can be activated by setting the imixs.property:

	qr-code.url.prafix=http://root.url.comoffice?workitem=
	qr-code.url.sufix=

This property is not a default property.
 

## Deployment

To activate the feature the following deployment information must be set:

Into the parent pom.xml add:

	<!-- QR-Code service -->
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-adapters-qrcode</artifactId>
			<version>${imixs.adapters.version}</version>
			<scope>provided</scope>
		</dependency>
			
Into the web module pom.xml add:

	<!-- QR-Code service -->
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-adapters-qrcode</artifactId>
			<scope>runtime</scope>
		</dependency> 