'use strict';

//http://docs.angularjs.org/api/ng.$http
angular.module('fishstoreOne.services', []).
service('blankModel', function () {
   console.log('fishstoreOne.services.blankModel')
});

angular.module('fishstoreTwo.services', []).
service('blankModel', function () {
   console.log('fishstoreTwo.services.blankModel')
});

angular.module('fishstoreThree.services', []).
service('blankModel', function () {
   console.log('fishstoreThree.services.blankModel')
});