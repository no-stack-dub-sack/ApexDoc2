// #region Global vars / Initialization
/***********************************************************************
***********************************************************************/
var SCOPES = ['global', 'public', 'private', 'testMethod', 'webService'];
var APEX_DOC_MENU = 'apex-doc-2-menu';


// document ready function - removes jQuery dependency
document.addEventListener("DOMContentLoaded", function() {
	initMenu();
	renderMenuFromState();
	readScopeCookie();
	hideAllScopes();
	showScopes();
});
// #endregion


// #region Menu Utils
/***********************************************************************
***********************************************************************/

// create session storage object for menu state
// and/or update state with any new menu items
function initMenu() {
	var items = document.querySelectorAll('.classGroup');
	var hasState = sessionStorage.getItem(APEX_DOC_MENU);
	var state = !hasState ? {} : JSON.parse(hasState);

	if (!hasState) {
		// initialize menu state
		initializeMenuModel(items, state);
	} else {
		// If already init, add any new class groups since last load.
		// should really only happen when docs are under development
		updateMenuModel(items, state);
	}

	// finally, update sessionStorage with latest state
	sessionStorage.setItem(APEX_DOC_MENU, JSON.stringify(state));
}

function initializeMenuModel(items, state) {
	console.log('ApexDoc2: initializing menu state');

	// on init, set first group to open
	items.forEach(function(item, i) {
		if (i === 0) {
			state[item.id] = true;
		} else {
			state[item.id] = false;
		}
	});
}

function updateMenuModel(items, state) {
	// 1) get keys currently in state object
	var keys = Object.keys(state);

	// 2) get ids from each .classGroup <details> element
	var groups = Array.prototype.map.call(items, function(item) {
		return {
			id: item.id,
			isOpen: item.getAttribute('open')
		}
	});

	// 3) perform diff to get Ids not yet captured in storage
	var deletedKeys = keys.filter(function(key) {
		var idx = groups.findIndex((group) => {
			return group.id === key
		});

		return idx === -1;
	});

	var newKeys = groups.filter(function(item) {
		return keys.indexOf(item.id) === -1;
	});

	// 4) add/delete keys to/from state
	if (deletedKeys.length > 0) {
		deletedKeys.forEach(function(key) {
			delete state[key];
		});
		console.log('ApexDoc2: Stale menu keys found, deleting from session storage:');
		console.log(deletedKeys);
	}

	if (newKeys.length > 0) {
		newKeys.forEach(function(item) {
			state[item.id] = item.isOpen === '' && true
		});
		console.log('ApexDoc2: New menu keys found, adding to session storage:');
		console.log(newKeys.map(function(g) { return g.id }));
	}
}

function renderMenuFromState() {
	var state = JSON.parse(sessionStorage.getItem(APEX_DOC_MENU));
	for (var group in state) {
		var item = document.getElementById(group);
		if (state[group]) {
			console.log('ApexDoc2: Opening ' + group + ' section');
			item.setAttribute('open', '');
		}
	}
}

// save menu state before each unload so that state is
// preserved when changing files or when reloading the page.
window.onbeforeunload = function updateMenuState() {
	var items = document.querySelectorAll('.classGroup');
	var state = JSON.parse(sessionStorage.getItem(APEX_DOC_MENU));

	items.forEach(function(item) {
		var isOpen = item.getAttribute('open');
		state[item.id] = isOpen === '' && true;
	});

	sessionStorage.setItem(APEX_DOC_MENU, JSON.stringify(state));
}
// #endregion


// #region Scope Utils
/***********************************************************************
***********************************************************************/

function getListScope() {
	var list = [];
	var checkboxes = document.querySelectorAll('input[type=checkbox]');
	checkboxes.forEach(function(elem) {
		if (elem.checked) {
			var str = elem.id;
			str = str.replace('cbx', '');
			list.push(str);
		}
	});
	return list;
}

function showScopes() {
	var list = getListScope();
	for (var i = 0; i < list.length; i++) {
		toggleScope(list[i], true);
	}
}

function showAllScopes() {
	for (var i = 0; i < SCOPES.length; i++) {
		toggleScope(SCOPES[i], true);
	}
}

function hideAllScopes() {
	for (var i = 0; i < SCOPES.length; i++) {
		toggleScope(SCOPES[i], false);
	}
}

function setScopeCookie() {
	var list = getListScope();
	var strScope = '';
	var comma = '';
	for (var i = 0; i < list.length; i++) {
		strScope += comma + list[i];
		comma = ',';
	}
	document.cookie = 'scope=' + strScope + '; path=/';
}

function readScopeCookie() {
	var strScope = getCookie('scope');
	if (strScope != null && strScope != '') {

		// first clear all the scope checkboxes
		var checkboxes = document.querySelectorAll('input[type=checkbox]');
		checkboxes.forEach(function(elem) {
			elem.checked = false;
		});

		// now check the appropriate scope checkboxes
		var list = strScope.split(',');
		for (var i = 0; i < list.length; i++) {
			var id = 'cbx' + list[i];
			var checkbox = document.getElementById(id);
			checkbox.setAttribute('checked', true);
		}
	} else {
		showAllScopes();
	}
}

function getCookie(cname) {
	var name = cname + '=';
	var ca = document.cookie.split(';');
	for (var i=0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) === ' ') c = c.substring(1);
		if (c.indexOf(name) === 0) return c.substring(name.length, c.length);
	}
	return '';
}

function toggleScope(scope, isShow) {
	setScopeCookie();

	var props = document.querySelectorAll('.property-scope-' + scope);
	var methods = document.querySelectorAll('.method-scope-' + scope);
	var classes = document.querySelectorAll('.class-scope-' + scope);
	// show or hide all properties, classes,
	// and methods of the given scope
	if (isShow === true) {
		toggleVisibility(props, true);
		toggleVisibility(methods, true);
		toggleVisibility(classes, true);
	} else {
		toggleVisibility(props, false);
		toggleVisibility(methods, false);
		toggleVisibility(classes, false);
	}
}

function toggleVisibility(elements, isShow) {
	for (var elem of elements) {
		if (isShow) {
			elem.classList.remove('hide');
		} else {
			elem.classList.add('hide');
		}
	}
}
// #endregion


// #region Navigation utils
/***********************************************************************
***********************************************************************/
function goToLocation(url) {
	event.preventDefault(); // prevent collapsing / expanding menu when clicking on Class Group link
	if (document.location.href.toLowerCase().indexOf(url.toLowerCase()) === -1) {
		document.location.href = url;
	}
}
// #endregion