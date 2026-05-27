# Project structure notes

## Backend / Maven

`src/` — исходный код Spring Boot-приложения: Java-классы, ресурсы, тесты, миграции.
`pom.xml` — главный файл Maven-проекта: зависимости, плагины, версия Java, настройки сборки.
`target/` — результат сборки Maven: `.class`, `.jar`, отчёты тестов.

`mvnw` — Maven Wrapper для Linux/macOS. Позволяет запускать Maven без установленного Maven.
`mvnw.cmd` — Maven Wrapper для Windows.
`.mvn/wrapper/maven-wrapper.properties` — указывает, какую версию Maven скачать и использовать.

## IDE

`.idea/` — настройки IntelliJ IDEA для проекта.
`*.iml` — файл модуля IntelliJ IDEA. Хранит локальное представление структуры проекта для IDE.

## Git

`.gitignore` — список файлов и папок, которые Git не должен отслеживать.

`.gitattributes` — правила обработки файлов Git-ом, чаще всего переносы строк.
`/mvnw text eol=lf` — `mvnw` должен быть с Linux-переносами строк.
`*.cmd text eol=crlf` — Windows `.cmd`-файлы должны быть с Windows-переносами строк.

## Environment

`.env` — локальные переменные окружения: пароли, логины, порты.
`.env.example` — пример `.env` без реальных секретов.

## Docker / Infra

`compose.yaml` — Docker Compose-конфиг для запуска PostgreSQL, Redis, Kafka и других сервисов.
`Dockerfile` — инструкция для сборки Docker-образа приложения.

`infra/` — инфраструктурные файлы проекта.
`k6/` — нагрузочные тесты через k6.

## Frontend / Node.js

`frontend/` — frontend-часть проекта.

`package.json` — зависимости и npm-скрипты frontend/Node.js.
`package-lock.json` — точные версии npm-зависимостей.

`frontend/node_modules/` — зависимости frontend-а. 
`node_modules/` в корне — зависимости корневых инструментов, например типы для k6.