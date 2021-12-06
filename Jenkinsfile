pipeline {
    environment {
        ARTIFACTORY_URL = credentials('ARTIFACTORY_URL')
    }

    agent {
        label 'build'
    }

    tools {
        gradle "gradle-7.2"
        jdk 'adoptjdk13'
    }

    stages {
        stage ('Build and Test Libraries') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh 'gradle clean test --info -b build.gradle'
                }
            }
        }

        stage ('Build Jars') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh 'gradle jar --info -b build.gradle'
                }
            }
        }

        stage ('Publish Libraries') {
            when {
                branch 'main'
            }

            steps {

                def versionInfo = sh (
                            script: 'gradle -q lookForArtifacts -p fhir',
                            returnStdout: true
                        ).trim()
                echo versionInfo
                if (versionInfo == 'false') {
                   echo 'execute'
                } else {
                   echo 'skip'
                }

//                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
//
//                    sh 'gradle artifactoryPublish -b build.gradle'
//                }
            }
        }
    }
}