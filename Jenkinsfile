agent {
      label 'build'
  }

node {
    // Get Artifactory server instance, defined in the Artifactory Plugin administration page.
    def server = Artifactory.server "CMSArtifactory"

    stage('Clone sources') {
        git url: 'https://github.com/CMSgov/AB2D-Filters.git'
    }

    stage('Artifactory configuration') {
        // Tool name from Jenkins configuration
        rtGradle.tool = "filtersGradle"
        // Set Artifactory repositories for dependencies resolution and artifacts deployment.
        rtGradle.deployer repo:'ab2d-filters', server: server
        rtGradle.resolver repo:'ab2d-filters-remote', server: server
    }

    stage('Gradle build') {
        buildInfo = rtGradle.run buildFile: 'build.gradle', tasks: 'clean artifactoryPublish'
    }

    stage('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
