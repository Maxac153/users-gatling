#!/bin/bash

export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

run_test() {
  # Функция проверки выполнения Java класса или Bash скрипта
  error_check() {
    echo "Ошибка при выполенении Java клксса или Bash скритпа!"
    exit 1
  }

  # Функция для конвертации секунд в дни, часы, минуты и секунды
  convert_seconds() {
    local total_seconds=$1
    local days=$((total_seconds / 86400))
    local hours=$(( (total_seconds % 86400) / 3600 ))
    local minutes=$(( (total_seconds % 3600) / 60 ))
    local seconds=$(( total_seconds % 60 ))
    local result=""
    (( days > 0 )) && result="$result$days дн. "
    (( hours > 0 )) && result="$result$hours ч. "
    (( minutes > 0 )) && result="$result$minutes мин. "
    result="$result$seconds сек."
    echo "$result"
  }

  # Чтение параметров для Jenkins
  source env/jenkins.properties
  # Функция для запуска pipeline test
  run_jenkins_pipeline() {
    local PROFILE_JSON=$(<"$1")

    # Запускаем джабу с параметрами
    http_code=$(curl --silent "$USERNAME:$API_TOKEN" \
      --data-urlencode "JSON=$PROFILE_JSON" \
      --data "token=$JOB_TOKEN&deley=0s" \
      -X POST \
      -o /dev/null -w "%{http_code}" \
      "$JENKINS_URL/job/test_runner/buildWithParameters")

    if [ "$http_code" -ne 201 ]; then
      echo "Error Status Code: $http_code"
      exit 1
    fi

    SECONDS=0
  }

  local MODULE_NAME="moduel_name"
  local DATE_NOW="$(date "+%Y-%m-%d")"
  local JAVA_PATH=$1
  local JAR_PATH=$2
  local JAR_K8S_PATH=$3
  local TEST_PROFILE_PATH=$4
  local CSV_FILE_PATH=$5
  local MAX_TPS=$6
  local SCRIPT_STEP=$7
  local TARGET_DATE=$8

  echo "================= RUN PARAMETERS ================="
  echo "MODULE_NAME: $MODULE_NAME"
  echo "DATE_NOW: $DATE_NOW"
  echo "JAVA_PATH: $JAVA_PATH"
  echo "JAR_PATH: $JAR_PATH"
  echo "JAR_K8S_PATH: $JAR_K8S_PATH"
  echo "TEST_PROFILE_PATH: $TEST_PROFILE_PATH"
  echo "CSV_FILE_PATH: $CSV_FILE_PATH"
  echo "MAX_TPS: $MAX_TPS"
  echo "SCRIPT_STEP: $SCRIPT_STEP"
  echo "TARGET_DATE: $TARGET_DATE"
  echo "=================================================="
}

# Функция для валидации даты
validate_date() {
  local target_date=$1

  # Выходим если 0
  if [ "$target_date" == "0" ]; then
    return 0
  fi

  # Регулярное выражение для проверки формата ГГГГ-ММ-ДД_ЧЧ-ММ-СС
  if ! [[ $target_date =~ ^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])_([01][0-9]|2[0-3])-([0-5][0-9])-([0-5][0-9])$ ]]; then
    echo "Error: Invalid date format. Expected YYYY-MM-DD_HH-MM-SS, got $target_date" >&2
    exit 1
  fi

  # Заменяем подчеркивание на пробел и дефисы в времени на двоеточия
  local date_time="${target_date/_/ }"
  date_time="${date_time:0:13}:${date_time:14:2}:${date_time:17:2}"

  # Проверяем, является ли дата валидной с помощью команды date
  if ! date -d "$date_time" >/dev/null 2>&1; then
    echo "Error: Invalid date or time values in $target_date" >&2
    exit 1
  fi
}

EXPECTED_ARGS=11
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Ошибка ожидалось $EXPECTED_ARGS параметра(ов), передано $#."
  echo "Использование: <JAVA_PATH> <JAR_PATH> ..."
  exit 1
fi

validate_date "${11}"

export -f run_test

echo "+----------+"
echo "| AUTH K8S |"
echo "+----------+"
read -p "Login: " K8S_LOGIN
read -sp "Password: " K8S_PASSWORD
echo

mkdir -p "logs"
LOGFILE="logs/test_$(date +%Y-%m-%d_%H-%M-%S).log"
if [ ! -f "$LOGFILE" ]; then
  touch "$LOGFILE"
fi

# Запуск теста
[[ -n "$K8S_LOGIN" ]] && export K8S_LOGIN
[[ -n "$K8S_PASSWORD" ]] && export K8S_PASSWORD
nouhup bash -c 'run_test "$@"' _ "$@" > "$LOGFILE" 2>&1 &
unset K8S_LOGIN K8S_PASSWORD

PID=$!
echo "PID=$PID LOGFILE=$LOGFILE"
tail -f "$LOGFILE"