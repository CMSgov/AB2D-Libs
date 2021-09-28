pipeline {

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

        stage ('Exec Gradle') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh 'gradle artifactoryPublish -b build.gradle'
                }
            }
        }
    }
}