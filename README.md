# notification-sample-app
This sample application uses the Email Notification Framework on Predix as an App Monitoring Alert service.

## Service Requirements

* Predix UAA 
* Email Notification Framework
* Email Notification Framework Client SDK


## Build

```
mvn clean install
```

## Deploying on Predix


### Deploy Application

```
cf push -f manifest.yml
```

##Additioanl Resources

* https://jsonpath.herokuapp.com/ 

* http://www.thymeleaf.org/index.html