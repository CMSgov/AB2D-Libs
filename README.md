# AB2D LIBS

This repository stores all dependencies for AB2D.
Confirm you have your gradle set, so you can connect to the cms repository locally . 

#Locally Build
```
gradle -b build.gradle
```

#Publishing
To publish any changes push changes. You can force an update if you want to publish by using the jenkins_force_publish file.
New jars won't be published unless you change the library version to a version that dosen't exist. 
Example of version for bfd in /ab2d-bfd/build.gradle
```
version = '0.0.1'
```

