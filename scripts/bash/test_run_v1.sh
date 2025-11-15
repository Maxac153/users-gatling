#!/bin/bash

export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# Функция проверки выполнения Java класса или Bash скрипта
error_check() {
  echo "Ошибка при выполенении Java клксса или Bash скритпа!"
  exit 1
}

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

  # Чтение параметров для Gatling (GRAPHITE_HOST, GRAPHITE_PORT, LOAD_GENERATOR)
  source env/gatling.properties

  # Чтение параметров для Jenkins
  source env/jenkins.properties
  # Функция для запуска pipeline test
  run_jenkins_pipeline() {
    local PROFILE_JSON=$(<"$1")

    # Запускаем джабу с параметрами
    http_code=$(curl --silent "$USERNAME:$API_TOKEN" \
      --data-urlencode "JSON=$PROFILE_JSON" \
      --data "token=$JOB_TOKEN&delay=0s" \
      -X POST \
      -o /dev/null -w "%{http_code}" \
      "$JENKINS_URL/job/test_runner/buildWithParameters")

    if [ "$http_code" -ne 201 ]; then
      echo "Error Status Code: $http_code"
      exit 1
    fi

    SECONDS=0
    # Ожидаем появления номера сборки
    while true; do
      build_number=$(curl -s --user "$USERNAME:$API_TOKEN" "$JNEKINS_URL/job/gatling/job/job/test_runner/api/json" | grep -Po '"number":\K\d+' | head -n 1)
      if [[ -n "$build_number" ]]; then
        echo "Build Started With Number: $build_number"
        break
      fi

      if (( SECONDS >= 30 )); then
        echo "Timeout Reached: Build Number Did Not Appear In 30 Seconds"
        exit 1
      fi

      echo "Waiting For Build Number..."
      sleep 2
    done

    build_url="$JENKINS_URL/job/gatling/job/test_runner/$build_number"
    echo "Build URL: $build_url"
    echo "Build In Progress..."

    # Отслеживаем статус сборки
    while true; do
      local status_json=$(curl -s --user "$USERNAME:$API_TOKEN" "$build_url/api/json")
      local building=$(echo "$status_json" | grep -Po '"building":\K(true|false)')

      if [[ "$building" == "false" ]]; then
        local result=$(echo "$status_json" | grep -Po '"result":"\K[^"]+')
        echo "Build Finished With Status: $result"
        if [[ "$result" != "SUCCESS" ]]; then
          exit 1
        else
          break
        fi
      else
        sleep 30
      fi
    done
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
  # Формат даты 2025-10-20_01-00-00
  # Если не нужна пауза передать 0
  local TARGET_DATE=$8
  local delay=${12}
  local date_time_now=${13}
  local index_step=2

  echo "================= RUN PARAMETERS ================="
  echo "MODULE_NAME: $MODULE_NAME"
  echo "DATE_NOW: $DATE_NOW"
  echo "JAVA_PATH: $JAVA_PATH"
  echo "JAR_PATH: $JAR_PATH"
  echo "JAR_K8S_PATH: $JAR_K8S_PATH"
  echo "TEST_PROFILE_PATH: $TEST_PROFILE_PATH"
  echo "CSV_FILE_PATH: $CSV_FILE_PATH"
  echo "MAX_TPS: $MAX_TPS"
  echo "GRAPHITE_HOST: $GRAPHITE_HOST"
  echo "GRAPHITE_PORT: $GRAPHITE_PORT"
  echo "LOAD_GENERATOR: $LOAD_GENERATOR"
  echo "SCRIPT_STEP: $SCRIPT_STEP"
  echo "TARGET_DATE: $TARGET_DATE"
  echo "DELAY: ${delay}"
  echo "=================================================="

  if [ "$TARGET_DATE" != "0" ]; then
    echo "Дата запуска теста: $TARGET_DATE"
    echo "Ждем $(convert_seconds $delay) до запуска теста."
    sleep $delay
  fi

  local server="http://localhost:8443"
  local namespace=(
    "test_1"
    "test_2"
  )
  local check_config_file_path=(
    "config_check/users/config_1.json"
    "config_check/users/config_2.json"
  )

  #1
  if [ $SCRIPT_STEP -le $index_step ]; then
    echo "$index_step. Проверка конфигурации K8S"
    for ((i=0; i<length; i++)); do
      echo -e "$login\n$password" | "$JAVA_PATH" -DMODULE_NAME
    done
  fi
  ((index_step++))

  echo "Wait 5M"
  sleep 5m

  #2
  if [ $SCRIPT_STEP -le $index_step ]; then
    echo "$index_step. Запуск теста"
    run_jenkins_pipeline "$TEST_PROFILE_PATH" || error_check
  fi
}

# Функция для проверки существования файлов и папок
check_paths() {
  local paths=("$@")
  local error_flag="0"

  for path in "${paths[@]}"
  do
    if [ -f "$path" ]; then
      :
    elif [ -d "$path" ]; then
      :
    else
      echo "$path - не существования!"
      error_flag="1"
    fi
  done

  return "$error_flag"
}

# Функция для проверки значения, что это число
is_number() {
  local filed_name=$1
  local number=$2
  local re='^-?[0-9]+$'
  if [[ $number =~ $re ]]; then
    return 0
  else
    echo "$filed_name: $number - не число!"
    exit 1
  fi
}

# Функция для валидации даты
validate_date() {
  local target_date=$1

  # Выходим если 0
  if [ "$target_date" == "0" ]; then
    echo "0"
    return
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

  local target_ts=$(date -d "$date_time" +%s)
  local current_ts=$(date +%s)
  local delay=$((target_ts - current_ts))

  # Дата уже прошла
  if (( delay < 0 )); then
    echo "The Set Date Has Already Passed!"
    exit 1
  else
    echo "$delay"
    return
  fi
}

EXPECTED_ARGS=11
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Ошибка ожидалось $EXPECTED_ARGS параметра(ов), передано $#."
  echo "Использование: <JAVA_PATH> <JAR_PATH> <TEST_PROFILE_PATH> <MAX_TPS> <SCRIPT_STEP> <DATE_TEST_START> ..."
  exit 1
fi

JAR_PATH=$2
JAR_K8S_PATH=$3
TEST_PROFILE_PATH=$4

# Проверка существования файлов и папок перед запуском теста
paths=(
  "$JAR_PATH"
  "$JAR_K8S_PATH"
  "env/redis.json"
)

echo "Какие фыйлы и папки нужны для запуска теста"
for path in "${paths[@]}"
do
  echo "$path"
done

check_paths "${paths[@]}"
result=$?
if [ "$result" = "0" ]; then
  echo "Проверка существования файлов и папок прошла успешно"
else
  exit 1
fi

# Проверка что значение число
echo "Проверка что значение число."
is_number "MAX_TPS" "$6"
# ...
echo "Проверка что занчение число прошла успешно"

# Проверка формата даты
delay=$(validate_date "${11}")
date_time_now="$(date "+%Y-%m-%d_%H-%M-%S")"

export -f run_test

echo "+----------+"
echo "| AUTH K8S |"
echo "+----------+"
read -p "Login: " K8S_LOGIN
read -sp "Password: " K8S_PASSWORD
echo
[[ -n "$K8S_LOGIN" ]] && export K8S_LOGIN
[[ -n "$K8S_PASSWORD" ]] && export K8S_PASSWORD

# Проверка авторизации перед запуском теста
JAVA_PATH=$1
JAR_K8S_PATH=$3
echo "0. Проверка авторизации K8S перед запуском теста"
echo -e "$login\n$password" | "$JAVA_PATH" -DMODULE_NAME="users" -DDATE_NOW="$(date "+%Y-%m-%d")" -DDATE_TIME_NOW="$date_time_now" -DLOG_FILE_NAME=0_check_auth_k8s -cp "$JAR_K8S_PATH" kubernetes_check_auth.KubernetesCheckAuth "https://localhost:8443" || error_check

# Создаём папку logs
mkdir -p "logs"
LOGFILE="logs/test_$(date +%Y-%m-%d_%H-%M-%S).log"
if [ ! -f "$LOGFILE" ]; then
  touch "$LOGFILE"
fi

# Запуск теста
nouhup bash -c 'run_test "$@" "$delay" "$date_time_now"' _ "$@" "$delay" "$date_time_now" > "$LOGFILE" 2>&1 &
unset K8S_LOGIN K8S_PASSWORD

PID=$!
echo "PID=$PID LOGFILE=$LOGFILE"
tail -f "$LOGFILE"