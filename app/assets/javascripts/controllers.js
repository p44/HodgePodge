'use strict';

/** Controllers */
angular.module('fishstoreOne.controllers', ['fishstoreOne.services']).
controller('FishStoreOneCtrl',function ($scope, $http, $location, $timeout) {
	$scope.latest_catch_empty = JSON.parse('[]');
	$scope.latest_catch = $scope.latest_catch_empty; // this will be modified with $$hashkey
	$scope.latest_catch_raw = $scope.latest_catch_empty;
	$scope.latest_catch_size = 0;
	$scope.delivery_result_none = JSON.parse('{"message": "", "time": ""}');
	$scope.delivery_result_pending = JSON.parse('{"message": "...", "time": "..."}');
	$scope.delivery_result_error = JSON.parse('{"message": "Error", "time": ""}');
	$scope.delivery_result = $scope.delivery_result_none;
	
	$scope.catchExists = function() {
		return $scope.latest_catch_size > 0;
	}
	$scope.deliveryResultExists = function() {
		return ($scope.delivery_result.message != "" && $scope.delivery_result.message != "...");
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
		     $scope.delivery_result = $scope.delivery_result_none;
	   }).error(function(data, status, headers, config) {
		     console.log('GET ' + url + ' ERROR ' + status)
		     $scope.latest_catch = $scope.latest_catch_empty;
		     $scope.latest_catch_raw = $scope.latest_catch_empty;
		     $scope.latest_catch_size = 0;
		     $scope.delivery_result = $scope.delivery_result_none;
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



angular.module('fishstoreTwo.controllers', ['fishstoreTwo.services']).
controller('FishStoreTwoCtrl',function ($scope, $http, $location, $timeout) {
	$scope.latest_catch_empty = JSON.parse('[]');
	$scope.latest_catch = $scope.latest_catch_empty; // this will be modified with $$hashkey
	$scope.latest_catch_raw = $scope.latest_catch_empty;
	$scope.latest_catch_size = 0;
	// DeliveryReceipt(id: Long, fishCount: Int, totalWeight: Double, payment: Double, time: String)
	$scope.delivery_result_none = JSON.parse('{"id": 0, "fishCount": 0, "totalWeight": 0, "payment": 0, "time": "", "message": ""}');
	$scope.delivery_result_pending = JSON.parse('{"id": 0, "fishCount": 0, "totalWeight": 0, "payment": 0, "time": "...", "message": ""}');
	$scope.delivery_result_error = JSON.parse('{"id": 0, "fishCount": 0, "totalWeight": 0, "payment": 0, "time": "", "message": "Error"}');
	$scope.delivery_result = $scope.delivery_result_none;
	$scope.delivery_feed_msgs = []; // array of dropped fish
	
	$scope.catchExists = function() {
		return $scope.latest_catch_size > 0;
	}
	$scope.deliveryResultExists = function() {
		return ($scope.delivery_result.message != "" && $scope.delivery_result.message != "...");
	}
	$scope.getLatestCatch = function() {
       var url = '/fish_store_two/catch/latest';
	   $http({method: 'GET', url: url
	   }).success(function(data, status, headers, config) {
		     console.log(url);
		     console.log(data);
		     $scope.latest_catch = data;
		     $scope.latest_catch_raw = data;
		     $scope.latest_catch_size = data.length;
		     $scope.delivery_result = $scope.delivery_result_none;
	   }).error(function(data, status, headers, config) {
		     console.log('GET ' + url + ' ERROR ' + status)
		     $scope.latest_catch = $scope.latest_catch_empty;
		     $scope.latest_catch_raw = $scope.latest_catch_empty;
		     $scope.latest_catch_size = 0;
		     $scope.delivery_result = $scope.delivery_result_none;
	   });
	}
	$scope.deliverLatestCatch = function() {
		$scope.delivery_result = $scope.delivery_result_pending;
		var url = '/fish_store_two/delivery';
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
	
	/** handle incoming delivery feed messages: add to messages array */
    $scope.addDeliveryFeedMsg = function (msg) { 
    	var msgobj = JSON.parse(msg.data);
    	console.log('Got DeliveryFeedMsg' + msg.data);
        $scope.$apply(function () { 
        	$scope.delivery_feed_msgs.push(msgobj); // add to array
        });
    };
    
	/** start listening to the deliery feed for the fish store */
    $scope.listen = function () {
    	$scope.delivery_feed = new EventSource("/feed/fish_store_two/delivery"); 
        $scope.delivery_feed.addEventListener("message", $scope.addDeliveryFeedMsg, false);
    };
    
    $scope.listen();
});