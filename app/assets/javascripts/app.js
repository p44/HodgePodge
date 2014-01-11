'use strict';

/** app level module which depends on services and controllers */
angular.module('fishstoreOne', ['fishstoreOne.controllers', 'ngRoute']);

/** services module initialization, allows adding services to module in multiple files */
angular.module('fishstoreOne.services', []);
