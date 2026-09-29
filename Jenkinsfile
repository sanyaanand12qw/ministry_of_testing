// Declarative pipeline for the Ministry of Testing UI suite.
//
// Runs on a normal Jenkins node (no Docker). Prerequisites on the controller:
//   * Global Tool Configuration -> JDK named  'JDK21'
//   * Global Tool Configuration -> Maven named 'Maven3'
//   * Plugins: "HTML Publisher" (report tab) and "Pipeline Utility" not required
//
// The important design point is at the bottom: evidence collection lives in
// post { always }, so a failing test can never skip it.

pipeline {

    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    parameters {
        choice(name: 'BROWSER', choices: ['chromium', 'firefox', 'webkit'],
                description: 'Browser engine to run against')
        booleanParam(name: 'HEADLESS', defaultValue: true,
                description: 'Uncheck only on an agent that has a display')
        string(name: 'SUITE_FILE', defaultValue: 'testng.xml',
                description: 'TestNG suite file to execute')
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        // Keep the browser download inside the workspace-independent Jenkins home
        // so it is downloaded once and reused by every build.
        PLAYWRIGHT_BROWSERS_PATH = "${env.JENKINS_HOME}/playwright-browsers"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git --no-pager log -1 --oneline'
            }
        }

        stage('Install Browsers') {
            steps {
                // Idempotent: a no-op once the binaries are cached.
                sh 'mvn -B -ntp exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps ${BROWSER}"'
            }
        }

        stage('Test') {
            steps {
                // catchError marks the build FAILED but lets the pipeline carry on
                // to reporting, so the evidence for the failure is still published.
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh """
                        mvn -B -ntp clean test \
                          -DsuiteFile=${params.SUITE_FILE} \
                          -Dbrowser=${params.BROWSER} \
                          -Dheadless=${params.HEADLESS}
                    """
                }
            }
        }

        stage('Publish Report') {
            steps {
                // The whole extent-report folder is published, not just index.html,
                // because the screenshots/videos/traces live inside it and are
                // linked with relative paths.
                publishHTML(target: [
                        reportDir            : 'target/extent-report',
                        reportFiles          : 'index.html',
                        reportName           : 'Extent Report',
                        keepAll              : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing         : false
                ])
            }
        }
    }

    post {
        always {
            // Feeds Jenkins' own test trend graph and per-test history.
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            // Downloadable evidence, kept even when the suite failed.
            archiveArtifacts artifacts: 'target/extent-report/**, target/logs/*.log',
                    allowEmptyArchive: true, fingerprint: false
        }
        failure {
            echo """
            Tests failed. Evidence for this build:
              * Extent Report tab  -> failing step, screenshot and stack trace
              * Artifacts          -> target/extent-report/videos/<test>.webm
              * Artifacts          -> target/extent-report/traces/<test>.zip
                                      open with: npx playwright show-trace <file>
              * Artifacts          -> target/logs/automation.log
            """
        }
        success {
            echo 'All tests passed. Video and trace were discarded by design (retain-on-failure).'
        }
    }
}
