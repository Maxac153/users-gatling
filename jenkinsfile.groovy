#!groovy
import groovy.json.JsonBuilder

import java.text.SimpleDateFormat

def testGenSet = [] as Set
def moduleName = ''
def logPath = ''

class Maven {
    String TEST_FOLDER_PATH
    String MODULE_NAME
    Double PERCENT_PROFILE = 100.0
}

class CommonSettings {
    Maven MAVEN
    Map PROPERTIES
}

class Job {
    String GENERATOR
    String TEST_NAME
    String TEST_PATH
}

class Steps {
    Double STAR_TPS
    Double END_TPS
    Double RAMP_TIME
    Double HOLD_TIME
}

class Profile {
    String SCENARIO_NAME
    Steps[] STEPS
}

class Settings {
    Job JOB
    Profile[] PROFILE
    Map PROPERTIES
}

class CreateScript {
    def static private jsonBuildSettings(Settings settings, Double percentProfile) {
        for (Profile profile in settings.PROFILE) {
            for (Steps step in profile.STEPS) {
                step.STAR_TPS *= percentProfile
                step.END_TPS *= percentProfile
            }
        }
        return new JsonBuilder(settings).toString()
    }

    def static private jsonBuildCommonSettings(CommonSettings commonSettings) {
        return new JsonBuilder(commonSettings).toString()
    }

    def static createScript(CommonSettings commonSettings, Settings settings) {
        def percentProfile = commonSettings.MAVEN.PERCENT_PROFILE / 100.0
        String script =
                // Если запуск на другой машине
                // "cd ${testParam.COMMON_SETTINGS.MAVEN.TEST_FOLDER_PATH}/${testParam.COMMON_SETTINGS.MAVEN.MODULE_NAME};\n" +
                "mvn gatling:test -Dgatling.simulationClass=${settings.JOB.TEST_PATH}.${settings.JOB.TEST_NAME} " +
                        '-DCOMMON_SETTINGS="' + jsonBuildCommonSettings(commonSettings) + '" -DTEST_SETTINGS="' + jsonBuildSettings(settings, percentProfile) + '"'

        return script
    }
}

// Вывод времени когда тест начался, когда закончится, длительность, время для графаны
def testDuration(Integer wait, Steps[] stepsProfile) {
    SimpleDateFormat sdfDateTime = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy")
    sdfDateTime.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

    int durationProfile = stepsProfile.inject(0) { acc, step ->
        acc + (step.RAMP_TIME + step.HOLD_TIME) * 60
    } as int

    def calendarStartRumUp = Calendar.getInstance()
    calendarStartRumUp.add(Calendar.SECOND, wait)
    def calendarStart = Calendar.getInstance()
    calendarStart.add(Calendar.SECOND, wait)
    calendarStart.add(Calendar.SECOND, stepsProfile[0].RAMP_TIME * 60 as int)
    def calendarEnd = Calendar.getInstance()
    calendarEnd.add(Calendar.SECOND, wait)
    calendarEnd.add(Calendar.SECOND, durationProfile)
    def diffInMillis = calendarEnd.getTimeInMillis() - calendarStartRumUp.getTimeInMillis()

    long diffInSeconds = (long) (diffInMillis / 1000)
    long diffInMinutes = (long) (diffInSeconds / 60)
    long diffInHours = (long) (diffInMinutes / 60)
    long diffInDays = (long) (diffInHours / 24)

    String delimiter = "|-----------------------------------------------------------|\n"
    String testStartTime = "|  Test Start Time:    ${sdfDateTime.format(calendarStartRumUp.getTime())}"
    String testEndTime = "|  Test End Time:      ${sdfDateTime.format(calendarEnd.getTime())}"
    String testDuration = "|  Test Duration:      ${diffInDays}d ${diffInHours % 24}h:${diffInMinutes % 60}m:${diffInSeconds % 60}s"
    String grafanaRumUp = "|  Grafana (RumUp):    from=${calendarStartRumUp.getTime().getTime()}&to=${calendarEnd.getTime().getTime()}"
    String grafana = "|  Grafana:            from=${calendarStart.getTime().getTime()}&to=${calendarEnd.getTime().getTime()}"

    echo delimiter +
            testStartTime + ' ' * (delimiter.size() - testStartTime.size() - 2) + '|\n' + delimiter +
            testEndTime + ' ' * (delimiter.size() - testEndTime.size() - 2) + '|\n' + delimiter +
            testDuration + ' ' * (delimiter.size() - testDuration.size() - 2) + '|\n' + delimiter +
            grafanaRumUp + ' ' * (delimiter.size() - grafana.size() - 2) + '|\n' + delimiter +
            grafana + ' ' * (delimiter.size() - grafana.size() - 2) + '|\n' + delimiter
}

// Загружаем логи на генератор
def uploadingGenLog(logPath, logFolder, moduleName) {
    SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    sdfDateTime.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"))
    String nowDateTime = sdfDateTime.format(new Date())
    String archiveName = "${moduleName}_${nowDateTime}.tar"

    sshagent(credentials: [env.CREDENTIAL]) {
        sh "tar -cvf ${archiveName} ${logPath}"
        sh "ssh -o 'StrictHostKeyChecking=no' -o 'UserKnownHostsFile=/dev/null' ${env.USERNAME}@${env.GENERATOR_LOGS} 'mkdir -p ${logFolder}'"
        sh "scp -rp ${archiveName} ${env.USERNAME}@${env.GENERATOR_LOGS}:${logFolder}"
    }
}

// Скачивание логов с генератора в домашнюю директорию hitachi_client
def downloadJenkinsLog(logPath, logFolder, generator) {
    try {
        sshagent(credentials: [env.CREDENTIAL]) {
            sh "scp -r ${env.USERNAME}@${generator}:${logPath}/* ${logFolder}"
        }
    } catch (Exception e) {
        echo "ERROR: ${e.toString()}"
    }
}

pipeline {
    agent any
    parameters {
        text(name: 'JSON', description: 'JSON с параметрами запуска', defaultValue: '')
    }

    environment {
        // Креды пользователя для подключения по ssh и scp
        USERNAME = ''
        CREDENTIAL = ''
        GENERATOR_LOGS = 'localhost'
    }

    stages {
        // Запуск тестов
        stage("Starting Tests") {
            when {
                expression { params.JSON != '' }
            }
            steps {
                script {
                    Map<String, List<Object>> testsScripts = [:]
                    def TESTS_PARAM = readJSON text: params.JSON
                    CommonSettings commonSettings = new CommonSettings(TESTS_PARAM.COMMON_SETTINGS)
                    moduleName = commonSettings.MAVEN.MODULE_NAME
                    logPath = "${commonSettings.MAVEN.TEST_FOLDER_PATH}/${commonSettings.MAVEN.MODULE_NAME}/"

                    TESTS_PARAM.TESTS_PARAM.eachWithIndex { testParam, index ->
                        Settings settings = new Settings(testParam)
                        testGenSet.add(settings.JOB.GENERATOR)

                        Integer wait = commonSettings.PROPERTIES['WAIT'].toInteger()
                        for (Profile profile in settings.PROFILE) {
                            testDuration(wait, profile.STEPS)
                        }

                        String generator = settings.JOB.GENERATOR
                        String testName = "Module: ${commonSettings.MAVEN.MODULE_NAME} Test: ${settings.JOB.TEST_NAME} #${index + 1}"
                        String script = CreateScript.createScript(commonSettings, settings)
                        echo "${testName}:\n${script}"
                        def scriptName = "${commonSettings.MAVEN.MODULE_NAME}_${index + 1}.sh"
                        writeFile file: scriptName, text: script

                        if (!testsScripts.containsKey(generator))
                            testsScripts[generator] = []

                        testsScripts[generator] << [testName: testName, scriptName: scriptName]
                    }

                    def parallelTasks = [:]
                    testsScripts.each { generator, testsParam ->
                        parallelTasks[generator] = {
                            // Если надо запустить на нодах
                            // node(generator) {
                            def parallelStages = testsParam.collectEntries {
                                [(it): {
                                    stage(it.testName) {
                                        sh "bash ${it.scriptName}"
                                    }
                                }]
                            }
                            parallel parallelStages
                            // }
                        }
                    }
                    parallel parallelTasks
                }
            }
        }
    }

    post {
        success {
            script {
                archiveArtifacts artifacts: 'target/gatling/**', allowEmptyArchive: true
                sh 'rm -rf target/gatling/*'
                deleteDir()
            }
        }
    }
}