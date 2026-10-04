# FleetCheck – Gradle version (Worksheet Build Systems, Part 8)

Mesmo código-fonte, recursos e testes do projeto Maven (FleetCheck_Starter); só o sistema de build mudou.
Ambiente: JDK 21 (Temurin 21.0.12), Gradle 9.8.0.

Nota: no Gradle 9 é obrigatório declarar `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`
para os testes JUnit 5 correrem (deixou de ser adicionado automaticamente), por isso acrescentei essa linha ao build.gradle da ficha.

## 8.1 – Build sem Jackson
`gradle clean build` →
App.java:4: error: package com.fasterxml.jackson.databind does not exist
Dependência em falta: com.fasterxml.jackson.core:jackson-databind (usada pelos imports de ObjectMapper e TypeReference).

## 8.2 – Dependência adicionada e grafo de dependências
`gradle clean build` → BUILD SUCCESSFUL (testes passam, o defeito >= já tinha sido corrigido na parte Maven).

`gradle dependencies --configuration runtimeClasspath`:
\--- com.fasterxml.jackson.core:jackson-databind:2.22.2
     +--- com.fasterxml.jackson.core:jackson-annotations:2.22
     +--- com.fasterxml.jackson.core:jackson-core:2.22.2
     \--- com.fasterxml.jackson:jackson-bom:2.22.2 (*)

Direta: jackson-databind (declarada com implementation). Transitivas: jackson-core e jackson-annotations.
Comparação com mvn dependency:tree: as dependências são as mesmas (mesmos artefactos e versões).
Mudar o sistema de build não mudou as dependências da aplicação, só a forma de as declarar e mostrar
(o Gradle mostra ainda o jackson-bom como "constraint" (c), que alinha as versões do Jackson).

## 8.3 – JAR executável
Antes: `java -jar build/libs/fleetcheck-1.0.0.jar` → "no main manifest attribute, in build/libs/fleetcheck-1.0.0.jar"
Depois (plugin application + configuração do jar):
FleetCheck 1.0
Vehicles loaded: 4
Vehicles requiring service: 2
Average mileage: 37000 km

O que mudou: o manifest passou a ter Main-Class: pt.upt.fleetcheck.App e o bloco `from { configurations.runtimeClasspath ... zipTree }`
descompacta as classes do Jackson (databind, core, annotations) para dentro do JAR. Passou a ser um "fat JAR" autocontido,
equivalente ao fleetcheck-1.0.0-all.jar do Shade no Maven (aqui substitui o JAR normal em vez de criar um segundo).

## 8.4 – Gradle Wrapper
`gradle wrapper` → gradlew, gradlew.bat, gradle/wrapper/ (fixado no Gradle 9.8.0). `.\gradlew.bat clean build` → BUILD SUCCESSFUL.
Suposição removida: que cada máquina tem o Gradle instalado, no PATH e numa versão compatível. O wrapper fixa a versão
em gradle-wrapper.properties e descarrega-a automaticamente (como se viu: "Downloading .../gradle-9.8.0-bin.zip").

## 8.5 – GitHub Actions
Workflow "Gradle Build" com sucesso (Status: Success, artefacto fleetcheck-gradle-build, 2.07 MB):
https://github.com/tokas10/FleetCheck-Gradle/actions/runs/37224102816

## 8.6 – SBOM com Gradle
`.\gradlew.bat cyclonedxBom` → build/reports/cyclonedx/bom.json (e bom.xml). Contém jackson-databind, jackson-core e jackson-annotations.
Porque contém dependências que não escrevi no build.gradle? Porque o SBOM descreve o grafo de dependências resolvido e não só
o que foi declarado. Só declarei jackson-databind; jackson-core e jackson-annotations são transitivas e fazem parte do software entregue.
Diferença observada: o SBOM do Gradle inclui também as dependências de teste (junit-jupiter, opentest4j, ...), marcadas como test,
enquanto o plugin Maven por omissão só incluiu compile/runtime (3 componentes).

## 8.7 – Comparação Maven vs Gradle
| Tarefa | Maven | Gradle |
|---|---|---|
| Configuração do build | pom.xml (XML declarativo) | build.gradle (DSL Groovy) + settings.gradle |
| Build limpo | mvnw.cmd clean verify | gradlew.bat clean build |
| Adicionar dependência | `<dependency>...</dependency>` | `implementation 'group:artifact:version'` |
| Ver dependências | mvn dependency:tree | gradle dependencies |
| Wrapper | mvnw.cmd | gradlew.bat |
| Output do build | target/ | build/ |
| Localização do JAR | target/ | build/libs/ |
| JAR executável | maven-shade-plugin (fleetcheck-1.0.0-all.jar) | plugin application + configuração do jar |
| SBOM | CycloneDX Maven plugin → target/bom.json | CycloneDX Gradle plugin → build/reports/cyclonedx/bom.json |

## Pergunta final
O que mudou foi o processo de build, não o software. O código-fonte, os recursos, os testes, as dependências
(jackson-databind 2.22.2 + transitivas) e o resultado da aplicação são exatamente os mesmos nas duas versões.
Mudou a forma de descrever e executar o build: a linguagem de configuração, os comandos, as pastas de output,
os plugins para empacotar e gerar o SBOM, e o workflow de CI.
