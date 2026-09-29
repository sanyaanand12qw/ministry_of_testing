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

    agent any //run this pipeline on any available jenkins node/agent

    tools {  //"For this pipeline, use the JDK and Maven installations configured in Jenkins.
        jdk 'JDK21'
        maven 'Maven3'
    }

    parameters { //This creates parameters that you can select when starting the Jenkins build.
        choice(name: 'BROWSER', choices: ['chromium', 'firefox', 'webkit'],
                description: 'Browser engine to run against')
        booleanParam(name: 'HEADLESS', defaultValue: true,
                description: 'Uncheck only on an agent that has a display')
        string(name: 'SUITE_FILE', defaultValue: 'testng.xml',
                description: 'TestNG suite file to execute')
    }

    options { //Adds timestamps to Jenkins console logs.
        timestamps()
        //This prevents Jenkins from filling your disk.
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        //If the entire pipeline takes more than 30 minutes:This protects you from a test suite hanging forever.
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        stage('Checkout') {
            steps {
            //Checkout the source code configured for this Jenkins job.
                checkout scm
                //prints the latest Git commit.
                sh 'git --no-pager log -1 --oneline'
            }
        }

//This installs the Playwright browser required by your parameter.
        stage('Install Browsers') {
            steps {
                // Idempotent: a no-op once the binaries are cached.
                //
                // No PLAYWRIGHT_BROWSERS_PATH override on purpose. Playwright's
                // default cache is per-user (~/Library/Caches/ms-playwright on
                // macOS, ~/.cache/ms-playwright on Linux), and this Jenkins runs
                // as the same account you develop with, so the engines you already
                // downloaded locally are reused and this stage finishes instantly.
                //
                // '--with-deps' is deliberately not used: it shells out to the
                // Linux package manager and is not supported on a macOS agent.
                // params.BROWSER, not $BROWSER: Groovy interpolation, resolved before
                // the shell sees it. A single-quoted string would leave $BROWSER for
                // the shell, and on the very first parameterised build Jenkins has not
                // injected the parameters as env vars yet -- the arg would silently
                // become a bare "install", which downloads every engine.
              //  Get the value of the Jenkins parameter called BROWSER.
                sh "mvn -B -ntp exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args='install ${params.BROWSER}'"
            }
        }

//This is where your actual QA framework runs.
        stage('Test') {
            steps {
                // catchError marks the build FAILED but lets the pipeline carry on
                // to reporting, so the evidence for the failure is still published.

                //That's why catchError is used. So you don't lose your debugging evidence.
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

//and gives you a report tab in Jenkins.
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

//Run this after the pipeline, regardless of whether tests passed or failed.
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
