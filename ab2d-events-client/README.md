# Events Client

This library allow other services can send Events to the Event Service.

#Apply Client

Gradle
```
implementation 'gov.cms.ab2d:ab2d-events-client:1.0'
```

Maven
```
<dependency>
    <groupId>gov.cms.ab2d</groupId>
    <artifactId>ab2d-events-client</artifactId>
    <version>1.0</version>
</dependency>
```
\
#Enable

By default the ability to send sqs messages is disabled. To enable all you have to do is set the property.
```
feature.sqs.enabled=true
```

