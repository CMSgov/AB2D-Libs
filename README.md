# AB2D LIBS

This repository stores all dependencies for AB2D.
Confirm you have your gradle configured, so you can connect to the CMS repository locally. 

## Locally Build
```
gradle -b build.gradle
```

## Publishing
To publish any changes, You can force an update by using the jenkins_force_publish file.
New jars won't be published unless you change the library version to one that does not exist. 
Example of version for bfd in /ab2d-bfd/build.gradle
```
version = '1.0'
```

