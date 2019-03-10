// #region Global vars / Life-Cycle Functions
/***********************************************************************
***********************************************************************/
const SCOPES = ['global', 'public', 'private', 'protected', 'testMethod', 'webService'];
const APEX_DOC_MENU = 'apex-doc-2-menu';
const APEX_DOC_ACTIVE_EL = 'apex-doc-2-active-el';

// document ready function - removes jQuery dependency
document.addEventListener("DOMContentLoaded", () => {
	initMenu();
	initHighlightJs();
	renderMenuFromState();
	setActiveElement();
	readScopeCookie();
	hideAllScopes();
	showScopes();
});

// fire un-mounting functions
window.onbeforeunload = () => {
	updateMenuState();
	updateActiveElement();
}
// #endregion

// #region Initialization & Menu Utils
/***********************************************************************
***********************************************************************/
function initHighlightJs() {
	const selectors = [
		'pre code', '.methodSignature', '.methodAnnotations', '.classSignature',
		'.classAnnotations', '.attrSignature', '.propAnnotations'
	];
	// initialize highlighting for code examples and
	// signatures for methods, classes, props and enums
	selectors.forEach(selector => {
		document.querySelectorAll(selector).forEach(block => {
			hljs.highlightBlock(block);
		});
	});
}

// create session storage object for menu state
// and/or update state with any new menu items
function initMenu() {
	const hasState = sessionStorage.getItem(APEX_DOC_MENU);
	let items = document.querySelectorAll('.groupName');
	let state = !hasState ? {} : JSON.parse(hasState);

	if (!hasState) {
		// initialize menu state
		console.log('ApexDoc2: initializing menu state');
		items.forEach(item => state[item.id] = false);
	} else {
		// If already init, add any new class groups since last load.
		// should really only happen when docs are under development
		updateMenuModel(items, state);
	}

	// finally, update sessionStorage with latest state
	sessionStorage.setItem(APEX_DOC_MENU, JSON.stringify(state));
}

function updateMenuModel(items, state) {
	// 1) get keys currently in state object
	let keys = Object.keys(state);

	// 2) get ids from each .groupName <details> element
	let groups = Array.prototype.map.call(items, item => ({
		id: item.id,
		isOpen: item.getAttribute('open')
	}));

	// 3) perform diff to get Ids not yet captured in storage
	let deletedKeys = keys.filter(key =>
		groups.findIndex(group => group.id === key) === -1);

	let newKeys = groups.filter(item => keys.indexOf(item.id) === -1);

	// 4) add/delete keys to/from state
	if (deletedKeys.length > 0) {
		deletedKeys.forEach(key => {
			delete state[key];
		});
		console.log('ApexDoc2: Stale menu keys found, deleting from session storage:');
		console.log(deletedKeys);
	}

	if (newKeys.length > 0) {
		newKeys.forEach(item => state[item.id] = item.isOpen === '' && true);
		console.log('ApexDoc2: New menu keys found, adding to session storage:');
		console.log(newKeys.map(function(g) { return g.id }));
	}
}

function renderMenuFromState() {
	let state = JSON.parse(sessionStorage.getItem(APEX_DOC_MENU));
	for (let group in state) {
		let item = document.getElementById(group);
		if (state[group]) {
			console.log('ApexDoc2: Opening ' + group + ' section');
			item.setAttribute('open', '');
		}
	}
}

// save menu state before each unload so that state is
// preserved when changing files or when reloading the page.
function updateMenuState() {
	let items = document.querySelectorAll('.groupName');
	let state = JSON.parse(sessionStorage.getItem(APEX_DOC_MENU));

	items.forEach(item => {
		let isOpen = item.getAttribute('open');
		state[item.id] = isOpen === '' && true;
	});

	sessionStorage.setItem(APEX_DOC_MENU, JSON.stringify(state));
}

// preserve active menu item across loads
function updateActiveElement() {
	let active = document.querySelector('.active');
	active && sessionStorage.setItem(APEX_DOC_ACTIVE_EL, active.id);
}

// set active element from storage
function setActiveElement() {
	const id = sessionStorage.getItem(APEX_DOC_ACTIVE_EL);
	if (id) {
		var item = document.getElementById(id);
		item.classList.add('active');
		// focus element as well so tab
		// navigation can pick up where it left off
		if (item.firstElementChild && item.firstElementChild.tagName === 'A') {
			item.firstElementChild.focus();
		} else {
			item.focus();
		}
	}
}
// #endregion


// #region Scope Utils
/***********************************************************************
***********************************************************************/
function getListScope() {
	let list = [];
	let checkboxes = document.querySelectorAll('input[type=checkbox]');
	checkboxes.forEach(elem => {
		if (elem.checked) {
			let str = elem.id;
			str = str.replace('cbx', '');
			list.push(str);
		}
	});
	return list;
}

function showScopes() {
	let list = getListScope();
	for (let i = 0; i < list.length; i++) {
		toggleScope(list[i], true);
	}
}

function showAllScopes() {
	for (let i = 0; i < SCOPES.length; i++) {
		toggleScope(SCOPES[i], true);
	}
}

function hideAllScopes() {
	for (let i = 0; i < SCOPES.length; i++) {
		toggleScope(SCOPES[i], false);
	}
}

function setScopeCookie() {
	const list = getListScope();
	let strScope = '';
	let comma = '';
	for (let i = 0; i < list.length; i++) {
		strScope += comma + list[i];
		comma = ',';
	}
	document.cookie = 'scope=' + strScope + '; path=/';
}

function readScopeCookie() {
	const strScope = getCookie('scope');
	if (strScope != null && strScope != '') {

		// first clear all the scope checkboxes
		let checkboxes = document.querySelectorAll('input[type=checkbox]');
		checkboxes.forEach(elem => elem.checked = false);

		// now check the appropriate scope checkboxes
		let list = strScope.split(',');
		for (let i = 0; i < list.length; i++) {
			let id = 'cbx' + list[i];
			let checkbox = document.getElementById(id);
			checkbox.setAttribute('checked', true);
		}
	} else {
		showAllScopes();
	}
}

function getCookie(cname) {
	let name = cname + '=';
	let ca = document.cookie.split(';');
	for (let i=0; i < ca.length; i++) {
		let c = ca[i];
		while (c.charAt(0) === ' ') c = c.substring(1);
		if (c.indexOf(name) === 0) return c.substring(name.length, c.length);
	}
	return '';
}

function toggleScope(scope, isShow) {
	setScopeCookie();
	let enumTable = document.querySelectorAll('.properties');
	let propTable = document.querySelectorAll('.enums');
	let props = document.querySelectorAll('.property.' + scope);
	let enums = document.querySelectorAll('.enum.' + scope);
	let methods = document.querySelectorAll('.method.' + scope);
	let classes = document.querySelectorAll('.class.' + scope);
	// show or hide all props, classes, & methods of a given scope
	if (isShow === true) {
		// show props table if its been hidden
		toggleVisibility(enumTable, true);
		toggleVisibility(propTable, true);
		toggleVisibility(props, true);
		toggleVisibility(enums, true);
		toggleVisibility(methods, true);
		toggleVisibility(classes, true);
	} else {
		toggleVisibility(props, false);
		toggleVisibility(enums, false);
		toggleVisibility(methods, false);
		toggleVisibility(classes, false);
		// hide props table if there all props have been hidden
		maybeHideTable('.properties', '.property');
		maybeHideTable('.enums', '.enum');
	}
}

function toggleVisibility(elements, isShow) {
	for (let elem of elements) {
		if (isShow) {
			elem.classList.remove('hide');
		} else {
			elem.classList.add('hide');
		}
	}
}

function maybeHideTable(tableSelector, itemSelector) {
	let props, table = document.querySelectorAll(tableSelector);
	if (props = document.querySelectorAll(itemSelector)) {
		for (let prop of props) {
			if (!prop.classList.contains('hide')) {
				return;
			}
		}
		toggleVisibility(table, false);
	}
}
// #endregion


// #region Navigation utils
/***********************************************************************
***********************************************************************/
function toggleActiveClass(elem) {
	// remove isActive from current active element
	let item = document.querySelector('.active');
	item && item.classList.remove('active');

	// add to new active element
	elem.classList.add('active');
}

function goToLocation(url) {
	// prevent collapsing / expanding menu when clicking on Class Group link
	event.preventDefault();
	toggleActiveClass(event.currentTarget);
	if (document.location.href.toLowerCase().indexOf(url.toLowerCase()) === -1) {
		document.location.href = url;
	}
}
// #endregion