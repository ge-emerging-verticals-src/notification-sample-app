# notification-sample-app
This sample application uses the Email Notification Framework on Predix as an App Monitoring Alert service.

## What is the Email Notification Framework?

The Email Notification Framework is a service on [Predix](https://www.predix.io) that allows users to send emails using any SMTP.  The service features: 

*	Integration with Predix UAA
*	Plain-Text and HTML support
*	30 Day email logs
*	Support for service webhooks
*	Secure SMTP configuration storage 
*	Email Templates for custom email notification

## Sample Application

Suppose we have and app on Predix with a variety of endpoints.  A common use case for monitoring an app will be to count how many times an endpoint is called and notify someone when a certain threshold is reached for either an individual endpoint or all endpoints.  Additionally, different personas may care about different endpoints and thresholds.  For instance, a Marketing team may care that Endpoint A is being called much more than Endpoint B so they can monitor which features bring more value to their end user.  However, the IT team may want an alert when the total number of requests reaches a certain threshold so can plan and scale out infrastructure for the future.  

In this post, we will add an email alert feature that notifies different people according to different scenarios.  A sample application with some simple endpoints has been provided and I will walk through adding the following functionality to this sample app.

* Configuring the Email Notification Service
* Creating a daily usage report
*	Creating an email template for different recipients

## Application Requirements

* [Predix UAA](https://www.predix.io/services/service.html?id=1172)
* [Email Notification Framework](https://www.predix.io/services/service.html?id=2284)
* [Email Notification Framework Client SDK](https://github.com/ge-emerging-verticals-src/email-notification-framework-service-client-sdk)
