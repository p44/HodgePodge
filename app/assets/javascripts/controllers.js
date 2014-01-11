'use strict';

/** Controllers */
angular.module('fishstoreOne.controllers', ['fishstoreOne.services']).
controller('FishStoreOneCtrl',function ($scope, $http, $location, $timeout) {
	$scope.latest_catch_empty = JSON.parse('[]');
	$scope.latest_catch = $scope.latest_catch_empty; // this will be modified with $$hashkey
	$scope.latest_catch_raw = $scope.latest_catch_empty;
	$scope.latest_catch_size = 0;
	$scope.delivery_result_none = JSON.parse('{"message": "None"}');
	$scope.delivery_result_pending = JSON.parse('{"message": "..."}');
	$scope.delivery_result_error = JSON.parse('{"message": "Error"}');
	$scope.delivery_result = $scope.delivery_result_none;
	$scope.callback_msgs = [];
	
	$scope.catchExists = function() {
		return $scope.latest_catch_size > 0;
	}
	$scope.getLatestCatch = function() {
       var url = '/fish_store_one/catch/latest';
	   $http({method: 'GET', url: url
	   }).success(function(data, status, headers, config) {
		     console.log(url);
		     console.log(data);
		     $scope.latest_catch = data;
		     $scope.latest_catch_raw = data;
		     $scope.latest_catch_size = data.length;
	   }).error(function(data, status, headers, config) {
		     console.log('GET ' + url + ' ERROR ' + status)
		     $scope.latest_catch = $scope.latest_catch_empty;
		     $scope.latest_catch_raw = $scope.latest_catch_empty;
		     $scope.latest_catch_size = 0;
	   });
	}
	$scope.deliverLatestCatch = function() {
		$scope.delivery_result = $scope.delivery_result_pending;
		var url = '/fish_store_one/delivery';
		$http({method: 'POST', url: url,
		   headers: {'Content-Type': 'application/json'},
		   data: JSON.stringify($scope.latest_catch_raw)
		   // data: {'consumerId': $scope.consumer_simulated.id, 'url': $scope.simulator_callback_url}
		}).success(function(data, status, headers, config) {
		      console.log(url);
		      console.log(data);
		      $scope.latest_catch = $scope.latest_catch_empty;
			  $scope.latest_catch_size = 0;
			  $scope.delivery_result = data;
		}).error(function(data, status, headers, config) {
		      console.log('POST ' + url + ' ERROR ' + status)
		      $scope.latest_catch = $scope.latest_catch_empty;
			  $scope.latest_catch_size = 0;
			  $scope.delivery_result = $scope.delivery_result_error;
	    });
	}
});

