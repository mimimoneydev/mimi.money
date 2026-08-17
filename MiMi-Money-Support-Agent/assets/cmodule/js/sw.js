self.addEventListener('push', function (event) {
    console.log('Received a push message', event);

    var title = 'Yay a message.';
    var body = 'We have received a push message.';
    var icon = 'images/touch/chrome-touch-icon-192x192.png';
    var tag = 'simple-push-demo-notification-tag';

    event.waitUntil(self.registration.showNotification(title, {
        body: body,
        icon: icon
    }));

    // Since there is no payload data with the first version  
    // of push messages, we'll grab some data from  
    // an API and use it to populate a notification  
    /*event.waitUntil(fetch(API_ENDPOINT).then(function (response) {
     if (response.status !== 200) {
     // Either show a message to the user explaining the error  
     // or enter a generic message and handle the
     // onnotificationclick event to direct the user to a web page  
     console.log('Looks like there was a problem. Status Code: ' + response.status);
     throw new Error();
     }
     
     // Examine the text in the response  
     return response.json().then(function (data) {
     if (data.error || !data.notification) {
     console.error('The API returned an error.', data.error);
     throw new Error();
     }
     
     var title = data.notification.title;
     var message = data.notification.message;
     var icon = data.notification.icon;
     var data = data.notification.data;
     
     return self.registration.showNotification(title, {
     body: message,
     icon: icon,
     data: data
     });
     });
     }).catch(function (err) {
     console.error('Unable to retrieve data', err);
     
     var title = 'An error occurred';
     var message = 'We were unable to get the information for this push message';
     var icon = DEFAULT_ICON;
     var notificationTag = 'notification-error';
     return self.registration.showNotification(title, {
     body: message,
     icon: icon,
     tag: notificationTag
     });
     }));*/
});

/*self.addEventListener('push', function (event) {
 console.log('Received a push message', event);
 if (event.data) {
 console.log('message data', event.data);
 console.log('message data', event.data.text);
 var output = event.data.text();
 console.log(output);
 }
 
 var title = 'Yay a message.';
 var body = 'We have received a push message.';
 var icon = 'images/touch/chrome-touch-icon-192x192.png';
 var tag = 'simple-push-demo-notification-tag';
 
 event.waitUntil(
 self.registration.showNotification(title, {
 body: body,
 icon: icon
 })
 );
 });*/

self.addEventListener('notificationclick', function (event) {
    var url = event.notification.data.url;
    event.notification.close();
    event.waitUntil(clients.openWindow(url));
});
