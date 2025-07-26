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

// Обновление исходников
def updateSources() {

}

// Создаём скрипт запуска
def createScriptRun(loadGenerator, profile) {
    // mvn -DGRAPHITE_HOST=localhost -DGRAPHITE_PORT=2003 -DLOAD_GENERATOR=localhost -DPROFILE=./profiles/test_profile.json gatling:test -Dgatling.simulationClass=gatling.TestRunner
    // mvn -DGRAPHITE_HOST=localhost -DGRAPHITE_PORT=2003 -DLOAD_GENERATOR=localhost -DPROFILE=./profiles/test_profile.json gatling:test -Dgatling.simulationClass=gatling.TestRunner
}

// Скачиваем логи c генератора в Jenkins
def downloadJenkinsLog() {

}

pipeline {
    agent any
    parameters {
        // TODO Git Branch
        booleanParam(name: 'BUILD_JAR_ENABLE', defaultValue: false, description: 'Нужно ли пересобрать обновить исходники на генераторе')
        text(name: 'JSON', defaultValue: '', description: 'JSON с параметрами запуска')
    }

    environment {
        // Креды пользователя для подключения по ssh и scp
        USERNAME = ''
        CREDENTIAL = ''
        GENERATOR_LOGS = 'logs'
    }

    stages {
        // Обновляем исходники Jenkins
        stage("Update Sources") {
            when {
                expression { params.BUILD_JAR_ENABLE == true }
            }
            steps {
                script {
                    println 'Build Jar'
                    // git branch: 'develop', url: 'https://github.com/yourorg/yourrepo.git'
                }
            }
        }

        // Запуск тестов
        stage("Starting Tests") {
            when {
                expression { params.JSON != '' }
            }
            steps {
                script {
                    def parsed = new JsonSlurperClassic().parseText(params.JSON)
                    def commonSettings = parsed.COMMON_SETTINGS
                    def groupedMap = [:]

                    // Группируем тесты по LOAD_GENERATOR
                    parsed.TESTS_PARAM.each { testParam ->
                        def loadGen = testParam.RUN?.LOAD_GENERATOR
                        if (!groupedMap.containsKey(loadGen)) {
                            groupedMap[loadGen] = [TESTS_PARAM: [], COMMON_SETTINGS: commonSettings]
                        }
                        groupedMap[loadGen].TESTS_PARAM << testParam
                    }

                    // Вынесем подготовку сериализуемых данных и строк для запуска до параллели
                    def preparedTasks = [:]
                    groupedMap.each { loadGenerator, testProfile ->
                        def cleanProfile = [
                                TESTS_PARAM: testProfile.TESTS_PARAM.collect { testParam ->
                                    [ RUN: testParam.RUN, PROFILE: testParam.PROFILE, PROPERTIES: testParam.PROPERTIES]
                                },
                                COMMON_SETTINGS: testProfile.COMMON_SETTINGS
                        ]

                        def profileJson = new JsonBuilder(cleanProfile).toString()
                        def profilePath = "test_profile_${loadGenerator}.json"

                        def scriptRun = "mvn -DGRAPHITE_HOST=${testProfile.COMMON_SETTINGS.RUN_SETTINGS.DATASOURCE_HOST} " +
                                "-DGRAPHITE_PORT=${testProfile.COMMON_SETTINGS.RUN_SETTINGS.DATASOURCE_PORT} " +
                                "-DLOAD_GENERATOR=${loadGenerator} " +
                                "-DPROFILE=${profilePath} gatling:test -Dgatling.simulationClass=gatling.TestRunner"

                        preparedTasks[loadGenerator] = [profileJson: profileJson, scriptRun: scriptRun, profilePath: profilePath]
                    }

                    // Запуск параллельных задач с уже готовыми сериализуемыми данными
                    def parallelTasks = [:]
                    preparedTasks.each { loadGenerator, taskData ->
                        parallelTasks["run-tests-gen-${loadGenerator}"] = {
                            echo "Running load generator: ${loadGenerator}"
                            echo "Command: ${taskData.scriptRun}"
                            // Сохраняем JSON профиль в файл
                            sh "echo '${taskData.profileJson}' > ${taskData.profilePath}"


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
            }
        }

        success {
            script {
                println '=========== CREATE LOG FOLDER JENKINS ==========='
            }
        }

        unsuccessful {
            script {
                println '=========== STOP JAVA PROCESS TO GENERATORS ==========='
            }
        }
    }
}