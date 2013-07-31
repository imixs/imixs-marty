/* This script initialize the marty input widgets with specific behavior.
 * 
 * The Suggest Input widgets are provided by a special deleay function during the blur event.
 * This is for to deley the blue event for SuggestInput fields a little bit, 
 * so the commandlink event can be fired before. 
 * This method is used by the worktiemLink.xhtml and userinputx.html
 * See: http://stackoverflow.com/questions/12677179/delay-a-jsf-ajax-listener-for-checkbox-group 
 * 
 * The method also clears the input value on blur event.
 */
$(document).ready(function() {
	$(".marty-workitemlink-inputbox input").each(function(index, input) {
	    var onblur = input.onblur;
	    input.onblur = null;	
	    $(input).on("blur", function(event) {
	    	// clear the input on blur
	    	$(this).val(''); 
	        delayEvent(function() { onblur.call(input, event); }, 300);
	    });
	    
	    // turn autocomplete of
	    $(this).attr('autocomplete','off');
	});

	
	$(".marty-userlistinput-inputbox input").each(function(index, input) {
	    var onblur = input.onblur;
	    input.onblur = null;	
	    $(input).on("blur", function(event) {
	    	// clear the input on blur
	    	$(this).val(''); 
	        delayEvent(function() { onblur.call(input, event); }, 300);
	    });
	    
	    // turn autocomplete of
	    $(this).attr('autocomplete','off');
	});


	// this is the support for the marty userinput widget
	initUserInput(document.body);
	
});

var delayEvent = (function() {
	
    var timer = 0;
    return function(callback, timeout) {
        clearTimeout(timer);
        timer = setTimeout(callback, timeout);
    };
})();






function initUserInput(context) {
	// this is the support for the marty userinput widget
	$(".marty-userinput-inputbox [id$=\\:username_input]",context).each(function(index, input) {

		// id 
    	var inputfield_id=$(this).next('input');
    	var inputfield_display=$(this).next('input').next('input');
    	
		var onblur = input.onblur;
		input.onblur = null;	
    	// reset the username and userId input on blur event 
	    $(input).on("blur", function(event) {
	    		
	    	// reset userid to '' ? 
	    	if ($(this).val()=='')
	    		inputfield_id.val('');
	    	else {
	    		// if userid selcted then display the user display name 
	    		if (!inputfield_id.val() == '')
	    			$(this).val(inputfield_display.val()); 
	    		else 
	    			$(this).val('');
	    	}
	    	 delayEvent(function() { onblur.call(input, event); }, 300);
	    	
	    });
	    // turn autocomplete of 
	    $(this).attr('autocomplete','off');
	    
		// initialize input field with current display name 
		if (!inputfield_id.val() == '')
			$(this).val(inputfield_display.val());
		
		
		// hide id input field 
		inputfield_id.hide();
		inputfield_display.hide();
	});
	
}


/*
 * This method updates the hidden input fields for the mary userinput widget.
 * The method expects the jquery object of the user input box
 * and the userid and displayname to upate. 
 * The method finds the input fields and updates the values.
 */
function updateUserID(inputBox,id,displayname) {
	 var inputfield_name=$(inputBox).find('[id$=\\:username_input]');
	 var inputfield_id=$(inputBox).find('[id$=\\:userid_input]');
	 var inputfield_display=$(inputfield_id).next();
	 // alert(inputfield_id.val());
	 inputfield_id.val(id);
	 inputfield_name.val(displayname);
	 inputfield_display.val(displayname);
}


