/* This function is to deleay the blur event for the SuggestInput fields a little bit, 
 * so the commandlink event can be fired before. 
 * This method is used by the worktiemLink.xhtml and userinputx.html
 * See: http://stackoverflow.com/questions/12677179/delay-a-jsf-ajax-listener-for-checkbox-group 
 * 
 * The method also clears the input value on blur event.
 */
$(document).ready(function() {
	$(".suggestinput").each(function(index, input) {
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

	
	
});

var delayEvent = (function() {
	
    var timer = 0;
    return function(callback, timeout) {
        clearTimeout(timer);
        timer = setTimeout(callback, timeout);
    };
})();




