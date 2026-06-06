pipeline {
    agent any

    environment {
        IMAGE_NAME      = 'taskportal'
        IMAGE_TAG       = "${BUILD_NUMBER}"
        SONAR_HOST_URL  = 'http://sonarqube:9000'
        REGISTRY        = 'docker.io/yourdockerhubuser'   // change this
    }

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-17'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '── Checking out source ──'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '── Building with Maven ──'
                sh 'mvn clean package -DskipTests -q'
            }
        }

        stage('Unit Tests') {
            steps {
                echo '── Running unit tests ──'
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                echo '── Generating JaCoCo coverage report ──'
                sh 'mvn jacoco:report'
                jacoco(
                    execPattern: '**/target/jacoco.exec',
                    classPattern: '**/target/classes',
                    sourcePattern: '**/src/main/java'
                )
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '── Running SonarQube analysis ──'
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=taskportal \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo '── Waiting for SonarQube Quality Gate ──'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo '── Building Docker image ──'
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            steps {
                echo '── Pushing image to Docker Hub ──'
                withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker tag ${IMAGE_NAME}:latest      ${REGISTRY}/${IMAGE_NAME}:latest
                        docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${REGISTRY}/${IMAGE_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo '── Deploying to Kubernetes ──'
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh """
                        kubectl apply -f k8s/
                        kubectl set image deployment/taskportal-app \
                            taskportal=${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \
                            -n taskportal
                        kubectl rollout status deployment/taskportal-app -n taskportal --timeout=120s
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check the logs above.'
        }
        always {
            cleanWs()
        }
    }
}
