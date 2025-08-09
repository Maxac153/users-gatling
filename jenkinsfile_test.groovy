#!groovy

import groovy.json.JsonSlurperClassic
import groovy.json.JsonBuilder

class Run {
    String LOAD_GENERATOR
    String ENV
    String TEST_NAME
    String SIMULATION_CLASS
}

class Steps {
    Double TPS
    Double RAMP_TIME
    Double HOLD_TIME
}

class Profile {
    String SCENARIO_NAME
    String PACING
    Steps[] STEPS
}

class TestParam {
    Run RUN
    Profile[] PROFILE
    Map PROPERTIES
}

enum Datasource {
    INFLUXDB, PROMETHEUS, VICTORIA_METRICS
}

enum LoadTool {
    JMETER, GATLING, K6
}

class RunSettings {
    String MODULE_NAME
    String LEVEL_CONSOLE_LOG
    String LEVEL_FILE_LOG
    LoadTool LOAD_TOOL
    Datasource DATASOURCE
    String DATASOURCE_HOST
    Integer DATASOURCE_PORT
    Double PERCENT_PROFILE
}

class CommonSettings {
    RunSettings RUN_SETTINGS
    Map PROPERTIES
}

class TestsParam {
    TestParam[] TESTS_PARAM
    CommonSettings COMMON_SETTINGS
}

def groupedMap = [:]

pipeline {
    agent { label 'test2' }
    parameters {
        booleanParam(name: 'BUILD_JAR_ENABLE', defaultValue: false, description: 'Нужно ли пересобрать обновить исходники на генераторе')
        text(name: 'GIT_BRANCH', defaultValue: 'master', description: 'Название ветки из которой надо подтянуть изменения')
        text(name: 'JSON', defaultValue: '', description: 'JSON с параметрами запуска')
    }

    environment {
        // Креды пользователя для подключения по ssh и scp
        LOG_FOLDER_PATH = 'logs'
        JAR_NAME = 'performance-test-gatling.jar'
        GIT_URL = 'https://github.com/yourorg/yourrepo.git'
    }

    stages {
        // Обновляем исходники Jenkins
        stage("1. Update Jar On Generators") {
            when {
                expression { params.BUILD_JAR_ENABLE == true }
            }
            steps {
                script {
                    println '=========== UPDATE JAR ON GENERATORS ==========='

                    println '1. Build Jar'
                    git branch: "${params.GIT_BRANCH}", url: "${env.GIT_URL}"
                    sh 'mvn clean package -DskipTests'

                    println '2. Update Jar On Generators'
                    // sh ''
                }
            }
        }

        // Запуск тестов
        stage("2. Starting Tests") {
            when {
                expression { params.JSON != '' }
            }
            steps {
                script {
                    println '=========== RUN TESTS ON GENERATORS ==========='

                    def parsed = new JsonSlurperClassic().parseText(params.JSON)
                    def commonSettings = parsed.COMMON_SETTINGS
                    def preparedTasks = [:]
                    def parallelTasks = [:]

                    println '1. Group Tests By LOAD_GENERATOR'
                    parsed.TESTS_PARAM.each { testParam ->
                        def loadGenerator = testParam.RUN?.LOAD_GENERATOR
                        if (!groupedMap.containsKey(loadGenerator)) {
                            groupedMap[loadGenerator] = [TESTS_PARAM: [], COMMON_SETTINGS: commonSettings]
                        }
                        groupedMap[loadGenerator].TESTS_PARAM << testParam
                    }

                    println '2. Preparing The Launch Script'
                    groupedMap.each { loadGenerator, testProfile ->
                        def cleanProfile = [
                        TESTS_PARAM: testProfile.TESTS_PARAM.collect { testParam ->
                                    [ RUN: testParam.RUN, PROFILE: testParam.PROFILE, PROPERTIES: testParam.PROPERTIES]
                                },
                                COMMON_SETTINGS: testProfile.COMMON_SETTINGS
                        ]

                        def profileJson = new JsonBuilder(cleanProfile).toString()
                        def profilePath = "test_profile_${loadGenerator}.json"

                        def scriptRun = "java -DGRAPHITE_HOST=${testProfile.COMMON_SETTINGS.RUN_SETTINGS.DATASOURCE_HOST} " +
                                "-DGRAPHITE_PORT=${testProfile.COMMON_SETTINGS.RUN_SETTINGS.DATASOURCE_PORT} " +
                                "-DLOAD_GENERATOR=${loadGenerator} " +
                                "-DPROFILE=${profilePath} -cp performance-test-gatling.jar io.gatling.app.Gatling -s gatling.TestRunner"

                        preparedTasks[loadGenerator] = [
                                profileJson: profileJson,
                                scriptRun: scriptRun,
                                profilePath: profilePath
                        ]
                    }

                    // Запуск параллельных задач с уже готовыми сериализуемыми данными
                    println '3. Running Tests On Generators'
                    preparedTasks.eachWithIndex { loadGenerator, taskData, index ->
                        node(loadGenerator) {
                            parallelTasks["${index}-run-tests-generator-${loadGenerator}"] = {
                                println "Running Load Generator: ${loadGenerator}"
                                println "Command: ${taskData.scriptRun}"

                                // Сохраняем JSON профиль в файл
                                sh "echo '${taskData.profileJson}' > ${taskData.profilePath}"

                                // Запускаем тест на генераторе
                                sh 'java -DGRAPHITE_HOST=localhost -DGRAPHITE_PORT=2003 -DLOAD_GENERATOR=localhost -DPROFILE=./profiles/test_profile.json -cp performance-test-gatling.jar io.gatling.app.Gatling -s gatling.TestRunner'
                            }
                        }
                    }

                    parallel parallelTasks
                }
            }
        }
    }

    post {
        always {
            script {
                println '=========== CREATE LOG FOLDER JENKINS ==========='

                println '1. Archive Test Artifacts'
                archiveArtifacts artifacts: "${env.LOG_FOLDER_PATH}/**", allowEmptyArchive: true

                println '2. Delete Test Logs Folder'
                sh "rm -rf ${env.LOG_FOLDER_PATH}/*"

                println '3. Delete Jar'
                sh "rm -rf ${env.JAR_NAME}"
            }
        }

        unsuccessful {
            script {
                println '=========== STOP JAVA PROCESS ON GENERATORS ==========='
                groupedMap.each { generator, testProfile ->
                    node(generator) {
                        println "1. Kill Process Generator - ${generator}"
                        sh "sudo kill \$(pgrep -f ${testProfile.COMMON_SETTINGS.MODULE_NAME})"
                    }
                }
            }
        }
    }
}