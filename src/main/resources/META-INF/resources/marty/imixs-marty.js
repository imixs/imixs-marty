"use strict";

// workitem scripts

IMIXS.namespace("org.imixs.marty");

var autocompleteInputID;
var autcompleteSelectedElement;
var autocompleteSearchReady = false;
var currentSelectCallback;





// define core module
IMIXS.org.imixs.marty = (function () {


	// init user input fields...
	$(document).ready(function () {
		// add autocomplete feature to all user inputs...
		var userGroupInputField = $("textarea[data-item='marty.user.input']");
		$(userGroupInputField).each(function () {
			imixsMarty.userInputInit(this, martyUserSearch, 'marty-userinput-resultlist'); // optional callback method allowed here!
		});
	});


	/*
	 * initializes an input element for autocompletion. 
	 * the param 'resultlistid' is optional and defines the element 
	 * containing the search result.
	 * The selectCallback method is optional and triggered when a new element was selected 
	 * from the suggest list
	 */
	var userInputInit = function (inputElement, searchCallback, resultlistId, selectCallback) {
		// should be hidden
		$(inputElement).hide();
		hideResultList(inputElement);
		autocompleteSearchReady = false;
		// set id for result list element
		if (!resultlistId || resultlistId === '') {
			resultlistId = 'autocomplete-resultlist'; // default name
		}

		// now we can select the new element with the next method
		var searchInputElement = $(inputElement).next();
		$(searchInputElement).attr('data-resultlist', resultlistId);

		// single user 
		if (searchInputElement.is("input")) {
			var username = $(searchInputElement).data('username')
			$(searchInputElement).val(username);
		}

		// add a input event handler with delay to search for suggestions....
		$(searchInputElement).on('input', delay(function () {
			if (!autocompleteSearchReady) {
				return; // start only after first key down! (see below)
			}
			// store the current input name
			autocompleteInputID = $(this).attr('name');
			//alert(autocompleteInputID);
			currentSelectCallback = selectCallback;
			searchCallback({ phrase: this.value });
		}, 500)).trigger('input');


		// hide the suggest list on blur event
		$(searchInputElement).on("blur", delay(function (event) {
			hideResultList(searchInputElement);
			// if single input than reset value...
			var username = $(searchInputElement).data('username');
			$(searchInputElement).val(username);

		}, 200));


		/*execute a function presses a key on the keyboard:*/
		$(searchInputElement).keydown(function (e) {

			autocompleteSearchReady = true; // init serach mode
			if (e.keyCode == 40) {
				/*If the arrow DOWN key is pressed,
				increase the currentFocus variable:*/
				autocompleteSelectNextElement(this);
			} else if (e.keyCode == 38) { //up
				/*If the arrow UP key is pressed,
				decrease the currentFocus variable:*/
				autocompleteSelectPrevElement(this);
			} else if (e.keyCode == 13) {
				/*If the ENTER key is pressed, prevent the form from being submitted,*/
				e.preventDefault();
				selectActiveElement(this);
			}
		});

		/* delete single input entry on '' */
		$(searchInputElement).on("change", function () {
			var value = $(this).val();
			if (value == "") {
				// clear value
				var inputField = $(searchInputElement).prev();
				inputField.val('');
				$(searchInputElement).val('');
				$(searchInputElement).data('username', '');
			}
		});

		// turn autocomplete of
		$(searchInputElement).attr('autocomplete', 'off');
	},


		/**
		 * This mehtod shows the search result panel and places it below the current input element
		 */
		userSearchShowResult = function (data) {
			autcompleteSelectedElement = -1;
			var status = data.status;
			if (status === "success") {
				// select the inital input element by its name...
				var inputElement = $('input[name ="' + autocompleteInputID + '"]');
				// now we pull the result html list to this input field.....
				$("[id$=" + inputElement.data('resultlist') + "]").insertAfter(inputElement).show();
			}
		},

		/* Helper method to select the current element in the result list by enter key */
		selectActiveElement = function (inputElement) {
			var id = $(inputElement).data('resultlist');
			var parent = $("[id$='" + id + "']");
			var resultElementListActive = $(".marty-userinput-resultlist-element.active", parent);
			if (resultElementListActive.length > 0) {
				$("a", resultElementListActive).click();
			} else {
				// special use case overtake current input
				selectUserID($(inputElement).val(), $(inputElement).val());
			}
			hideResultList(inputElement);
		},

		/* Helper method to move and hide the current result list. 
		 * Here we must move the result list back to the default position
		 * because otherwise a ajax event can destroy the list!
		 */
		hideResultList = function (inputElement) {
			var id = $(inputElement).data('resultlist');
			var resultList = $("[id$='" + id + "']");
			var defaultpos = $("[id$='resultlist-defaultpos']");
			if (defaultpos) {
				$(resultList).insertAfter($(defaultpos)).hide();
			}
		},

		/* Helper method to highligt the current element in the result list */
		autocompleteSelectNextElement = function (inputElement) {
			var id = $(inputElement).data('resultlist');
			var parent = $("div[id$='" + id + "']");
			var resultElementList = $(".marty-userinput-resultlist-element", parent);
			// remove acitve if set	
			$(resultElementList).removeClass("active");
			// next element...?
			autcompleteSelectedElement++;
			if (autcompleteSelectedElement >= resultElementList.length) {
				// reset if overflow...
				autcompleteSelectedElement = 0;
			}
			// set new active
			$(resultElementList[autcompleteSelectedElement]).addClass("active");
		},


		autocompleteSelectPrevElement = function (inputElement) {
			var id = $(inputElement).data('resultlist');
			var parent = $("div[id$='" + id + "']");
			var resultElementList = $(".marty-userinput-resultlist-element", parent);
			// remove acitve if set	
			$(resultElementList).removeClass("active");
			autcompleteSelectedElement--;
			// next element...?
			if (autcompleteSelectedElement < 0) {
				autcompleteSelectedElement = resultElementList.length - 1;
			}
			// set new active
			$(resultElementList[autcompleteSelectedElement]).addClass("active");
		},

		selectUserID = function (userid, username) {
			// find the value field and the search field
			var inputSearchField = $('input[name ="' + autocompleteInputID + '"]');
			var inputField = $(inputSearchField).prev();

			// single user 
			if (inputField.is("input")) {
				// show the username in the serach field..
				inputSearchField.val(username);
				inputField.val(userid);

				// update data 
				inputSearchField.data('username', username);
			}
			// user list 
			if (inputField.is("textarea")) {
				var list = inputField.val();

				var list = inputField.val().split(/\r?\n/);
				var newList = new Array();
				$.each(list, function (key, value) {
					if (value != '') {
						if (!newList.includes(value)) {
							newList.push(value);
						}
					}
				});
				if (!newList.includes(userid)) {
					newList.push(userid);
				}

				var newValue = "";
				$.each(newList, function (key, value) {
					if (key == 0) {
						newValue = newValue + value;
					} else {
						newValue = newValue + "\n" + value;
					}
				});

				inputField.val(newValue);
				// trigger on change event
				inputField.trigger('change');
				// clear input
				inputSearchField.val('');
			}
		},

		/* Deletes a given user id form a usergroup list */
		deleteUserID = function (link) {
			// find the value field based on the given link.
			var parent = $(link).closest("span[id$='datalist']");
			var inputField = $(parent).prevAll('textarea');

			var userid = $(link).data('userid').trim();

			// only user list is supported
			if (inputField.is("textarea")) {
				var list = inputField.val().split(/\r?\n/).map(item => item.trim());
				var newList = new Array();
				$.each(list, function (key, value) {
					if (value.toLowerCase()!== userid.toLowerCase()) {
						newList.push(value);
					}
				});
				var newValue = newList.join("\n");
				inputField.val(newValue);
				// trigger on change event
				inputField.trigger('change');
			}
		},

		/*
		 * delay function
		 * see: https://stackoverflow.com/questions/1909441/how-to-delay-the-keyup-handler-until-the-user-stops-typing
		 */
		delay = function (callback, ms) {
			var timer = 0;
			return function () {
				var context = this, args = arguments;
				clearTimeout(timer);
				timer = setTimeout(function () {
					callback.apply(context, args);
				}, ms || 0);
			};
		};


	// public API
	return {

		userInputInit: userInputInit,
		userSearchShowResult: userSearchShowResult,
		deleteUserID: deleteUserID,
		selectUserID: selectUserID
	};

}());

// Define public namespace
var imixsMarty = IMIXS.org.imixs.marty;
