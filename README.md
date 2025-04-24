# Imixs-Marty

[![Java CI with Maven](https://github.com/imixs/imixs-marty/actions/workflows/maven.yml/badge.svg)](https://github.com/imixs/imixs-marty/actions/workflows/maven.yml)
[![Join a discussion](https://img.shields.io/badge/discuss-on%20github-4CB697)](https://github.com/imixs/imixs-workflow/discussions)
[![License](https://img.shields.io/badge/license-GPL-blue.svg)](https://github.com/imixs/imixs-marty/blob/master/LICENSE)

'imixs-marty' is a sub project of '[Imixs Workflow](https://github.com/imixs/imixs-workflow)'. The project provides several artifacts to build business process management solutions on the Jakarta EE stack. The goal of this project is to simplify the development of workflow management applications by providing a robust and flexible application framework with a set of Java Enterprise components.

The project supports the following components

- User Management
- Team Management
- UI Integration

## BUild

To add the library just extend you maven dependencies:

```xml
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-marty</artifactId>
			<version>${org.imixs.marty.version}</version>
		</dependency>
```

Make sure that your persistence.xml file includes both entity libraries 'imixs-workflow' and 'imixs-marty'

```xml

                <jar-file>lib/imixs-workflow-engine-${org.imixs.workflow.version}.jar</jar-file>
                <jar-file>lib/imixs-marty-${org.imixs.marty.version}.jar</jar-file>
```

The marty library will add the database entities for the user and usergroup objects into your database.

## User Management

The Marty Module provides Jakrta EE components for a user management which can be extended by project needs
