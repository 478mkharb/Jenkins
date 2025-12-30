pipeline {

    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'qa', 'prod'],
            description: 'Select target environment'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run automated tests?'
        )
    }

    environment {
        APP_NAME = "demo-app"
        BUILD_INFO = "Build-${BUILD_NUMBER}"
    }

    options {
        timestamps()
        skipDefaultCheckout()
    }

    stages {

        stage('Checkout Code') {
            steps {
                echo "Checking out source code..."
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building ${APP_NAME}"
                sh '''
                  echo "Compiling application..."
                  sleep 2
                  echo "Build successful"
                '''
            }
        }

        stage('Unit Tests') {
            when {
                expression { params.RUN_TESTS }
            }
            steps {
                echo "Running unit tests..."
                sh '''
                  echo "Executing tests..."
                  sleep 2
                  echo "All tests passed"
                '''
            }
        }

        stage('Parallel Quality Checks') {
            parallel {

                stage('Lint Check') {
                    steps {
                        sh '''
                          echo "Running lint check..."
                          sleep 1
                          echo "Lint passed"
                        '''
                    }
                }

                stage('Security Scan') {
                    steps {
                        sh '''
                          echo "Running security scan..."
                          sleep 1
                          echo "No vulnerabilities found"
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                echo "Deploying to ${params.ENVIRONMENT} environment"
                sh '''
                  echo "Deploying application..."
                  sleep 2
                  echo "Deployment successful"
                '''
            }
        }
    }

    post {

        always {
            echo "Pipeline finished for ${APP_NAME}"
            echo "Cleaning workspace..."
            cleanWs()
        }

        success {
            echo " BUILD SUCCESSFUL"
        }

        failure {
            echo " BUILD FAILED"
        }

        unstable {
            echo " BUILD UNSTABLE"
        }
    }
}

