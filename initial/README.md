# Getting Started
Before we start, let’s make a few assumptions.  First, I will assume you have a Predix account (if not, sign-up!).  Secondly, we will be using a free Sparkpost account for our SMTP needs. Next, we will be adding our feature to Spring-based Java so we can take advantage of the Java client SDK, but I will also provide a Postman collection with every API call we use so you can migrate to any language you want.  Finally, the sample app for you to follow along is located here, but if you want to skip to the end, you can download the complete sample.

Now, let’s get started.

## Create an instance of the Email Notification Framework service.  
The Email Notification Framework service will require you provide a [UAA instance](https://www.predix.io/services/service.html?id=1172) as a token issuer.  You can create an instance of the service using the Cloud Foundry CLI and following the steps [here](https://docs.predix.io/en-US/content/service/operations/notification/get-started-with-the-email-notification-framework).  

## Configuring and deploying the Notification Sample App
A manifest.yml has been provided for your convenience, but we need to modify it with your settings:

```
applications:
- name: {sample-app-name}
  memory: 1G
  instances: 1
  timeout: 180
  host: {sample-app-name}
  path: ./target/ev-notification-sample-1.0.0.jar
  buildpack: https://github.com/cloudfoundry/java-buildpack.git
  services:
    - {uaa-instance-name}
    - {notification-instance-name}
  env:
    accessTokenEndpointUrl: "{uua_token_endpoint}"
    accessTokenAuthString: "{uaa_auth_string}"
    notificationServiceName: "{notification-instance-name}"
```

Parameters:
*	{sample-app-name} - Set to a unique app name.  This will also be your hostname
*	{uaa-instance-name} -Set to your UAA instance name from the previous section
*	{notification-instance-name} – Set to your Email Notification Framework instance name from the previous section.
*	{uaa_token_endpoint} – Set to you UAA get token url (e.g. https://{uaa-url}/oauth/token)
*	{uaa_auth_string} – Set to your base64 encoded client/secret for your UAA instance.

Once you’ve updated this manifest you can try pushing to Predix for the first time.

We will be monitoring the 3 endpoints in this application and counting how many times each API is called.  

*	/A
*	/B
*	/C

(Very creative, I know ☺)

The code to track each API call is already been implemented the sample application and you can review it at your leisure.  We will be adding all of our code in EndpointMonitor.java.  

The first thing we can add to EndpointMonitor is an instance of ServiceEnvironment from the client SDK.  This allows us to obtain the service information from our applications VCAP environment variables.  At the end of the init() function we add the following lines:

```
//Create Notification Service Environment from VCAP
NotificationServiceEnvironmentElement notificationServiceEnvironmentElement = null;
try
{
    ServiceEnvironment serviceEnvironment = new ServiceEnvironment();
    notificationServiceEnvironmentElement = serviceEnvironment.getNotificationServiceElementByName(notificationServiceName);
}
catch (ServiceEnvironmentException e)
{
    e.printStackTrace();
}
```

We should also define a bean for notificationServiceName at the top of our class so we can pass in the service instance name from our system environment variables.

```
@Value("${notificationServiceName}")
String notificationServiceName;
```

This will make our life much easier for the next sections. 

Next we will create an instance of NotificationServiceClient to allow us to interact with our service.

```
//Create Notifcation Service Client
notificationServiceClient = new NotificationServiceClientBuilder(notificationServiceEnvironmentElement).build();
```

We also define a member variable at the top of our class: 

```
private NotificationServiceClient notificationServiceClient;
```

Also to make our life easier we’ve created a simple component that can retrieve tokens from our UAA instance, which we will be using throughout this post.  To obtain a token, we simply make this call:

```
String token = uaaTokenRequester.getToken();
```

We may omit some of these lines from the sample source code in this post for brevity, but the complete source code will contain the code.

Now we’re ready to interact with the service.

## Creating an email configuration
Email configurations are required by the service to send emails.  The configurations are encrypted with 128-bit encryption and stored in Predix.   To add an email configuration we add the following on lines to the end of the init() function:

```
//Create Configuration
    Configuration configuration = new Configuration.ConfigurationBuilder()
        .setProtocol("smtp")
        .setHost("smtp.sparkpostmail.com")
        .setPort(587)
        .setSmtpAuth(true)
        .setSmtpStarttlsEnable(true)
        .setMailFrom("ev.notification.sample@sparkpost.com")
        .setMailUsername("SMTP_Injection")
        .setMailPassword("6fb83e32b23ae24d230742a15f09180c92219747")
        .build();
    
    //Create Configuration Request Body
    CreateConfigurationRequestBody configurationRequestBody = new CreateConfigurationRequestBody.CreateConfigurationRequestBodyBuilder()
        .addConfigurations(configuration)
        .build();

    //Send Create Configuration Request
    try {
      this.configuration = notificationServiceClient.createConfiguration(token, configurationRequestBody);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    } catch (RequestException e) {
      e.printStackTrace();
    }
```

At this point we could send a test email to make sure emails are sending.

## Creating an hourly alert email 
Now that we can send emails, let’s create an hourly notification that email that sends some usage information.  We will be using Spring Scheduler to schedule our functionality.  First, we create a function called SendHourlyReport: 

```
@Scheduled(fixedDelay = 1000*60*60L) //every hour
public void SendHourlyReport() {}
```

In this function we will add the code to send the usage information.  This will be a simple email that just gives a total of the number of requests for each API. Update our scheduler function with the following code:   

```
@Scheduled(fixedDelay = 1000*60*60L)
public void SendHourlyReport()
{
    StringBuilder emailBody = new StringBuilder("Here is your daily usage snapshot.\n");
    this.requests.keySet().stream().forEach(s -> emailBody.append(s + "=" + this.requests.get(s) + "\n"));

    //Create recipients
    SendEmailRequestBodyRecipient sendEmailRequestBodyRecipient = new SendEmailRequestBodyRecipient.SendEmailRequestBodyRecipientBuilder("SysAdmin", "sysadmin@ev.notification.ge.com", RecipientType.to).build();

    //Create email body
    SendEmailRequestBody sendEmailRequestBody = new SendEmailRequestBody.SendEmailRequestBodyBuilder()
        .setBody(emailBody.toString())
        .setFromEmail("ev.notification.sample@sparkpostbox.com")
        .setFromName("Endpoint Monitor")
        .addRecipients(sendEmailRequestBodyRecipient)
        .setSubject("Snapshot - " + new Date())
        .build();

    //Send email
    try
    {
      String token = uaaTokenRequester.getToken();
      SendEmailResponse sendEmailResponse = notificationServiceClient.sendEmail(token, this.configuration, sendEmailRequestBody);
    }
    catch (RequestException e)
    {
      e.printStackTrace();
    }
    catch (NotificationClientException e) {
      e.printStackTrace();
    }
  }
```

This is a decent feature.  Getting an hourly update from our application will certainly give us useful information, but the feature as implemented is too rigid and the email isn’t very pretty.  As discussed in the introduction, we want to tailor our feature to send useful emails based on different thresholds to different people.  The template feature will allow us to do that.

## Creating custom alerts with Templates
The template feature of the Email Notification Framework allows us to send template- emails with user-defined values in the email body. In our case, we will create a usage alert with two levels: LOW and HIGH.  And based on this content, we will send an email to a different user.  

Here is a sample template as an example. 

```
<!DOCTYPE html>
<html>
<head>
</head>
<body>
<span th:text="${alert}">1</span><br>
<span th:text="${heap_usage}">1</span><br>
<span th:text="${endpoint_hits}">1</span><br>
</body>
</html>
```

Using this template we can define alert, heap_usage, and endpoint_hits when we invoke the service programmatically.

First let’s create our thresholds and define our conditions for sending an email.  For this exercise, we will assign some harmless thresholds.  A HIGH alert level will be 20 hits to our endpoints and a LOW alert level will be 10 hits to our endpoints.  We add the following function called alertOnThreshold():

```
private void alertOnThreshold()
{
    AlertLevel alertLevel = AlertLevel.NONE;
    if (this.hits >= 20)
    {
        alertLevel = AlertLevel.HIGH;
    }
    else if (this.hits >= 10)
    {
        alertLevel = AlertLevel.LOW;
    }  
    if (alertLevel != AlertLevel.NONE)
    {
        SendAlert(alertLevel);
    }
}
```

Also we need to create the SendAlert() method

```
private void SendAlert(AlertLevel alertLevel){}
```

Don't worry, we will implement this method shortly.  Also don’t forget to call our new alertOnThreshold() method from the TrackRequests method.

Next we will by create our template. 
A LOW alert might go to a general IT inbox, like it-alert@ev.notification.ge.com. A HIGH alert will be escalated to the IT Director for further review.
Sample template files are provided for you in the src/main/resources/ directory.  We will use it_template first.

Since creating templates are a one-time thing like creating a configuration we will go back to our init() function:

```
//Load template file
InputStream in = null;
try {
    in = new FileInputStream(new   File(this.getClass().getClassLoader().getResource("it_template.txt").getFile()));
}
catch (IOException e) {
    e.printStackTrace();
}

//Send Create Template Request
try 
{
  TemplateRequestBody templateRequestBody = new TemplateRequestBody.TemplateRequestBodyBuilder("ev.notification.ge.com", "usage_alert", "Usage alert template" )
          .setSubjectTemplate("Usage Report")
          .build();
      this.template = notificationServiceClient.createTemplate(token, templateRequestBody, in);
}
catch (RequestException e)
{
    e.printStackTrace();
}
catch (NotificationClientException e)
{
    e.printStackTrace();
}
```

Now we need to configure our Email Notification Service to send this template email based on these conditions.  If you recall, we want to send LOW alerts to a generic mail box, but HIGH alerts to the IT Director.  We can do this by creating Matchers with Recipient lists.  A Matcher is a pattern condition that associates the contents of a template email to a list of recipients.  For instance, if our email has the alert HIGH in its contents, it will automatically be send to the recipients associated with that Matcher, in our case the IT Director.  If our email has the alert LOW in its contents, to will be sent to those recipients. 
We will create our Matchers and Recipients in the init() function again: 
We will be creating 2 different matchers for our alerts

```
//Create a matcher for HIGH alerts
    try {
      CreateMatchersRequestBody createMatchersRequestBody = new CreateMatchersRequestBody.CreateMatchersRequestBodyBuilder("$.[?(@.alert in ['HIGH'])]").build();
      this.highAlertMatcher = notificationServiceClient.createMatcher(token, this.template createMatchersRequestBody);
    }
    catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Create a matcher for LOW alerts
    try {
      CreateMatchersRequestBody createMatchersRequestBody = new CreateMatchersRequestBody.CreateMatchersRequestBodyBuilder("$.[?(@.alert in ['LOW'])]").build();
      this.lowAlertMatcher = notificationServiceClient.createMatcher(token, this.template, createMatchersRequestBody);
    }
    catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }
```

Next we will create a recipient list for each Matcher.  Please note I’ve restructured the code a little to make it cleaner for this section:

```
//Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient("it.director@ev.notification.ge.com").build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.highAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

//Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient("it@ev.notification.ge.com").build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.lowAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }
```

## Putting it all Togther
Finally, we can implement our SendAlert() function to work with the template.  
The first part of our function defines our template parameters:

```
//Set template email parameters
SendTemplateEmailRequestBody sendTemplateEmailRequestBody = new SendTemplateEmailRequestBody.SendTemplateEmailRequestBodyBuilder()
        .addKeyValue("heap_usage", heap_usage)
        .addKeyValue("endpoint_hits", hits)
        .addKeyValue("a_count", this.requests.get(ENDPOINT_A))
        .addKeyValue("b_count", this.requests.get(ENDPOINT_B))
        .addKeyValue("c_count", this.requests.get(ENDPOINT_C))
        .addKeyValue("alert", alertLevel)
        .build();
```

Pay close attention to the alert parameter.  Based on this parameter, the email service will determine which recipients to send the email to by the Matcher we’ve created.

The next section sends the email using our template

```
//Send template email request
    try {
      String token = uaaTokenRequester.getToken();
      SendEmailResponse sendEmailResponse = notificationServiceClient.sendTemplateEmail(token, this.configuration,  this.template, sendTemplateEmailRequestBody);
    }
    catch (RequestException e)
    {
      e.printStackTrace();
    }
    catch (NotificationClientException e)
    {
      e.printStackTrace();
    }
```

That’s it.  We can build and deploy our sample app on Predix and start receiving emails.  Please note, I’ve use 2 email addresses in this article that do not exist.  You should replace *it.director@ev.notification.ge.com* and *it@ev.notification.ge.com* with real email addresses that exist when testing.

##Extra Credit
If you download the complete code, there are two extra functions that we did not cover printEmailEvents and configureTenant.  These functions utilize some extra functionality with referenced in the intro but did not cover as a part of this exercise, but are very useful when using the service.  For more information you can reach out to me directly at dat.nguyen@ge.com or the team at ev-notification@ge.com.

## Notes
1. You can always manually configure the Tenant by getting the information directly using the cloud foundry CLI.

2. Send Email and Send Template Email request can be found in the include [Postman collection](https://github.com/ge-emerging-verticals-src/notification-sample-app/tree/master/Postman).

3. Templates use Thymeleaf Java template engine. More info can be found [here](http://www.thymeleaf.org/.)

4. Create Template and Upload Template request can be found in the include [Postman collection](https://github.com/ge-emerging-verticals-src/notification-sample-app/tree/master/Postman).

5. Matchers uses [Jayway JsonPath](https://github.com/json-path/JsonPath) implementation to evaluate email contents.

6. Create Matchers request can be found in the include [Postman collection](https://github.com/ge-emerging-verticals-src/notification-sample-app/tree/master/Postman).

7. Create Recipients request can be found in the include [Postman collection](https://github.com/ge-emerging-verticals-src/notification-sample-app/tree/master/Postman)

