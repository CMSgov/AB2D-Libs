pipeline {

    agent {
        label 'build'
    }

    tools {
        gradle "gradle-7.2"
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

        stage ('Test Gradle') {
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
    }
}