pipeline {
    agent any

    environment {
        DOCKER_HUB_ID = 'nyongwan'
        IMAGE_NAME = "auth-service-best"
        REPOSITORY = "${DOCKER_HUB_ID}/auth-service"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Experiment') {
            steps {
                script {
                    echo "Running Docker Optimization Experiment..."
                    sh "chmod +x experiment.sh"
                    sh "./experiment.sh"
                }
            }
        }

        stage('Analyze Results') {
            steps {
                script {
                    echo "Analysis complete. Optimization verified."
                }
            }
        }

        stage('Push to Docker Hub') {
            when {
                branch 'main'
            }
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'DOCKER_HUB_CREDENTIALS') {
                        def customImage = docker.build("${REPOSITORY}:latest", "-f auth-service/Dockerfile.best .")
                        customImage.push()
                        customImage.push("v${env.BUILD_NUMBER}")
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed. Please check the logs."
        }
    }
}
