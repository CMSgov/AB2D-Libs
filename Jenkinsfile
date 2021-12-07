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
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    script {
                        def deployScript = '';
                        def versionPublishedList = sh(
                                script: 'gradle -q lookForArtifacts',
                                returnStdout: true
                        ).trim().split("'''")

                        for (int i = 1; i < versionPublishedList.size(); i++) {
                            def artifactoryInfo = versionPublishedList[i].split(":")
                            if (artifactoryInfo[1] == 'false') {
                                echo "Deploying ${artifactoryInfo[0]}"
                                deployScript += "${artifactoryInfo[0]}:artifactoryPublish "
                            }
                        }

                        sh "gradle ${deployScript} -b build.gradle"
                    }
                }
            }
        }
    }
}