pipeline {
    environment {
        ARTIFACTORY_URL = credentialsId('ARTIFACTORY_URL')
    }

    agent {
        label 'build'
    }

    tools {
        gradle "filtersGradle"
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

        stage ('Publish Libraries') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh 'gradle artifactoryPublish -b build.gradle'
                }
            }
        }
    }
}