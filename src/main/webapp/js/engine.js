$(function () {
    "use strict";
    var messageId = 0;
    var content = $('#content');
    var numberInput = $("#numberInput");
    var submitNumber = $('#submitNumber');
    var getResult = $('#getResult');

    function subscribeForNotifications(taskId) {
        if (typeof(EventSource) !== 'undefined') {
            // Yes! Server-sent events support!
            var source = new EventSource('api/tasks/' + taskId + '/notifications');
            source.onmessage = function (event) {
                display('Received message: ' + event.data, 'info');
                console.log('Received unnamed event: ' + JSON.stringify(event));
            };

            source.addEventListener('STARTED', function(event) {
                display('Calculation STARTED: ' + event.data, 'info');
                console.log('Received event ' + JSON.stringify(event));
            }, false);

            source.addEventListener('FINISHED', function(event) {
                display('Calculation Finished: ' + event.data, 'info');
                console.log('Received event ' + JSON.stringify(event));

                var options = {
                    'type': 'GET',
                    'url': '/api/tasks/' + JSON.parse(event.data).taskId,
                    'success' : function(response) {
                                    display('Got Result ' + JSON.stringify(response), 'success');
                                }
                }

                $.ajax(options);
            }, false);

            source.onopen = function (event) {
                display('connected to SSE', 'info')
            };

            source.onerror = function (event) {
                display('Got error ' + event.data, 'error');
                console.log('Received error event: ' + JSON.stringify(event));
            };
        } else {
            display('SSE not supported by browser.', 'error');
        }
    }

    submitNumber.click(function () {
        var data = JSON.stringify({'number': numberInput.val()});

        var options = {
            'type': 'POST',
            'url':'/api/tasks',
            'data': data,
            'contentType': 'application/json',
            'dataType': 'json',
            'success': function(response) {
                display('Queued task ' + JSON.stringify(response), 'success');
                subscribeForNotifications(response.taskId);
            }
        };

        $.ajax(options)
    });

    getResult.click(function() {
        var error = function(error) {
            if (404 == error.status) {
                display('No result found for number ' + numberInput.val(), 'error');
            } else {
                display('Ooops, unexpected error from the server', 'error');
                throw new Error(JSON.stringify(error))
            }
        };

        var number = numberInput.val();
        var options = {
            'type': 'GET',
            'url':'/api/results/' + number,
            'success': function(response) { display('Got result ' + JSON.stringify(response), 'success'); },
            'error': error
        };

        $.ajax(options)
    });

    var levels = {
    'success': 'success',
    'info': 'info',
    'error': 'danger'
    }
    function display(text, level) {
        var css = levels[level || 'info'];
        content.append('<tr class=\"' + css + '\"> <th scope="row">'+ ++messageId +'</th> <td>' + text + '</td></tr>');
    }
});