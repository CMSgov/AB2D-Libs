pipeline {
    agent {
        label: 'build'
    }

    agent {
        label 'build'
    }

    tools {
        maven 'maven-3.6.3'
        jdk 'adoptjdk13'
    }

    stages {

        stage ('Artifactory configuration') {
            steps {
                rtGradleDeployer (
                    id: "GRADLE_DEPLOYER",
                    serverId: "CMSArtifactory",
                    repo: "ab2d-filters",
                    excludePatterns: ["*.war"],
                )

                rtGradleResolver (
                    id: "GRADLE_RESOLVER",
                    serverId: "CMSArtifactory",
                    repo: "ab2d-filters-repo"
                )
            }
        }

        stage ('Config Build Info') {
            steps {
                rtBuildInfo (
                    captureEnv: true,
                )
            }
        }

        stage ('Test Gradle') {
            when {
                branch 'master'
            }

            steps {
                rtGradleRun (
                    usesPlugin: true, // Artifactory plugin already defined in build script
                    useWrapper: true,
                    tool: 'filtersGradle', // Tool name from Jenkins configuration
                    rootDir: ".",
                    buildFile: 'build.gradle',
                    tasks: 'clean test',
                    resolverId: "GRADLE_RESOLVER"
                )
            }
        }

        stage ('Exec Gradle') {
            when {
                branch 'master'
            }

            steps {
                rtGradleRun (
                    usesPlugin: true, // Artifactory plugin already defined in build script
                    useWrapper: true,
                    tool: 'filtersGradle', // Tool name from Jenkins configuration
                    rootDir: ".",
                    buildFile: 'build.gradle',
                    tasks: 'clean artifactoryPublish',
                    deployerId: "GRADLE_DEPLOYER",
                    resolverId: "GRADLE_RESOLVER"
                )
            }
        }

        stage ('Publish build info') {
            steps {
                rtPublishBuildInfo (
                    serverId: "CMSArtifactory"
                )
            }
        }
    }
}