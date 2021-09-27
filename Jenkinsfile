def server
def rtGradle = Artifactory.newGradleBuild()
def buildInfo = Artifactory.newBuildInfo()

pipeline {

    agent {
        label 'build'
    }

    tools {
        jdk 'adoptjdk13'
    }

    stages {

        stage ('Artifactory configuration') {
            steps {
                server = Artifactory.server 'CMSArtifactory'

                rtGradle.tool = "filtersGradle"
                rtGradle.deployer repo:'ab2d-filters', server: server
                rtGradle.resolver repo:'ab2d-filters-remove', server: server
            }
        }

        stage ('Build info') {
            steps {
                buildInfo.env.capture = true
            }
        }

        stage ('Gradle configs') {
            steps {
                rtGradle.deployer.artifactDeploymentPatterns.addExclude("*.war")
                rtGradle.usesPlugin = true
                rtGradle.useWrapper = true
            }
        }

        stage ('Test') {
            steps {
                rtGradle.run rootDir: '.', buildFile: 'build.gradle', tasks: 'clean test'
            }
        }

        stage ('Publish Gradle') {
            when {
                branch 'master'
            }

            rtGradle.run rootDir: ".", buildFile: 'build.gradle',
                "tasks": 'clean artifactoryPublish', buildInfo: buildInfo
        }
    }
}