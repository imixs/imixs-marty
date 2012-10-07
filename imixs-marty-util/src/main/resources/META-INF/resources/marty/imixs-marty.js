/* This function is to deleay the blur event for the SuggestInput fields a little bit, 
 * so the commandlink event can be fired before. 
 * This method is used by teh worktiemLink.xhtml
 * See: http://stackoverflow.com/questions/12677179/delay-a-jsf-ajax-listener-for-checkbox-group 
 */
$(document).ready(function() {
	$(".suggestinput").each(function(index, input) {
	    var onblur = input.onblur;
	    input.onblur = null;
	
	    $(input).on("blur", function(event) {
	        blurDelay(function() { onblur.call(input, event); }, 300);
	    });
	    // turn autocomplete of
	    $(this).attr('autocomplete','off');
	});

});

var blurDelay = (function() {
    var timer = 0;
    return function(callback, timeout) {
        clearTimeout(timer);
        timer = setTimeout(callback, timeout);
    };
})();




