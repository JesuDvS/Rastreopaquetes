pipeline {
    agent any
    stages {
        stage('Clonar repositorio') {
            steps {
                git branch: 'master', url: "https://github.com/JesuDvS/Rastreopaquetes.git"
            }
        }
        stage('Construir aplicación') {
            steps {
                bat './mvnw.cmd clean package'
            }
        }
        stage('Archivar artefacto') {
            steps {
                archiveArtifacts artifacts: 'target/Rastreopaquetes-0.0.1-SNAPSHOT.jar', fingerprint: true
            }
        }
    }
}