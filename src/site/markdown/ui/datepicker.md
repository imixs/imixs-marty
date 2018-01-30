#The Calendar Widget

Imixs-office-Workflow includes a calendar widget from the [Imixs-Workflow JSF components](http://www.imixs.org/doc/webtools/datepicker.html).
This widget is based on the [jQuery datepicker plugin](http://api.jqueryui.com/datepicker/).
The widget automatically activates when  the [Imixs-Header](http://www.imixs.org/doc/webtools/header.html) is part of a JSF page.  

	<i:imixsHeader dateformat="dd.MM.yyyy" />

To add the calendar widget to a date input field just add the css class 'imixs-date'


	<h:inputText value="#{workitem.item['datDate']}" styleClass="imixs-date">
		<f:convertDateTime pattern="#{message.datePatternShort}" timeZone="#{message.timeZone}" />
	</h:inputText>


Alternatively the Marty JSF imixsDateInput component can be used to activate the Datepicker :

	<i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"/>
	
## Time Picker

With the i:imixsDateInput it is also possible to activate a Time Selector. If the attribute 'showtime' is set to 'true',
an additional selectbox will be displayed to choose the hour and minute.

	<i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"  showtime="true"/>

The default interval for the minute selection is ‘15’. You can override this with the attribute ‘minuteinterval’.

	<i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"  showtime="true" minuteinterval="30"/>


## Localization

The jQuery Datepicker plugin provides support for localizing its content to cater for different languages and date formats. Each localization is contained within its own file with the language code appended to the name, e.g., jquery.ui.datepicker-fr.js for French. The desired localization file should be included after the main datepicker code. Each localization file adds its options to the set of available localizations and automatically applies them as defaults for all instances. Localization files can be found at https://github.com/jquery/jquery-ui/tree/master/ui/i18n.

For JSF applications we recommend to just load the localization file for the corresponding locale of the current user:

	<script type="text/javascript"
		src="#{facesContext.externalContext.requestContextPath}/js/datepicker-#{userControler.locale.country}.js"></script>
		
Typically the language package can be set easily via the resource bundle. This results in a better abstraction: 

	<script type="text/javascript"
		src="#{facesContext.externalContext.requestContextPath}/js/datepicker-#{app.datepicker_locale}.js"></script>

		
## Display Week of Year

The jQuery Calendar widget does not show a Week of Year per default. But you can easily set this feature in the jQuery onLoad() event by selecting the corresponding components.

The following example will set the calendar week for all date input fields: 


	$(document).ready(function() {
	    ....
	    $('.imixs-date').datepicker('option', 'showWeek', true);
	}
	