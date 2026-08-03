# biz-com-lib-log-connector

로그 컨텍스트를 Mule 이벤트에 주입하는 MuleSoft 커스텀 커넥터.

| 항목 | 값 |
|---|---|
| groupId / artifactId | `org.mycompany` / `biz-com-lib-log-connector` |
| Mule Runtime | 4.9.17 |
| JDK | 17 |
| Parent | `org.mule.extensions:mule-modules-parent:1.9.17` |
| XML prefix | `biz-log` |
| Base package | `org.mycompany.bizcom.log` |

`mule-modules-parent` 버전은 타깃 Mule 버전과 1:1 대응합니다. `1.9.17` 이
`mule.version=4.9.17`, `mule.sdk.api.version=1.0.0`, `java.release.version=17` 을
확정해 주므로 별도 버전 property 를 둘 필요가 없습니다.

빌드 산출물 `mule-artifact.json` 으로 확인된 값:

```json
{ "minMuleVersion": "4.9.17", "supportedJavaVersions": ["17"], "requiredProduct": "MULE" }
```

## 제공 컴포넌트

### 1. Scope — `<biz-log:with-context>`

컨텍스트를 메시지 **attributes** 로 주입한 상태로 하위 컴포넌트를 실행합니다.

```xml
<flow name="scopeStyleFlow">
  <biz-log:with-context flowVersion="${biz.log.flowVersion}"
      baseTableName="${biz.log.baseTableName}"
      triggerType="EVENT" actor="batch-user"
      targetAppName="SFDC" status="READY">

    <logger message="#[attributes.actor ++ ' → ' ++ attributes.baseTableName]"/>
    <flow-ref name="businessFlow"/>
  </biz-log:with-context>
</flow>
```

원본 payload 와 media type 은 그대로 보존됩니다.

> **Scope 에는 `config-ref` 가 없습니다.** Mule SDK 가 Scope 의 configuration 바인딩을
> 금지하기 때문입니다. Scope 메서드에 `@Config` 를 붙이면 빌드가 실패합니다.
>
> ```
> IllegalOperationModelDefinitionException:
>   Scope 'withContext' requires a config, but that is not allowed, remove such parameter
> ```
>
> 그래서 `flowVersion` / `baseTableName` 을 Scope 자체 파라미터로 받습니다. 앱마다 한 번만
> 정하려면 위 예시처럼 `${...}` property placeholder 를 쓰면 됩니다.

### 2. Configuration + Operation — `<biz-log:build-context>`

Operation 은 config 바인딩이 허용되므로, 이 경로에서는 두 값을 Configuration 에서 가져옵니다.

```xml
<biz-log:config name="BizLog_Config"
                flowVersion="1.0.0"
                baseTableName="TB_IF_LOG"/>
```

`flowVersion`, `baseTableName` 모두 **필수**입니다. 누락 시 앱 기동 시점에 검증 실패합니다.

`target` 과 함께 쓰면 **진짜 flow variable** 이 되어 메시지가 교체되어도 살아남습니다.

```xml
<flow name="varStyleFlow">
  <biz-log:build-context config-ref="BizLog_Config"
      triggerType="SCHEDULE" actor="scheduler"
      targetAppName="SAP" status="RUNNING"
      target="ctx"/>

  <http:request .../>                      <!-- 메시지 교체됨 -->
  <logger message="#[vars.ctx.actor]"/>    <!-- 여전히 유효 -->
  <flow-ref name="businessFlow"/>
</flow>
```

## 어느 쪽을 쓸지

| | 로그 대상 정보 출처 | 접근 | 메시지 교체 후 생존 |
|---|---|---|---|
| Scope | Scope 파라미터 (`${...}` 권장) | `#[attributes.actor]` | ❌ |
| Operation + `target` | Configuration (`config-ref`) | `#[vars.ctx.actor]` | ✅ |

스코프 안에서 메시지를 교체하는 컴포넌트(HTTP Request, DB Select, Transform 등)를
지나면 attributes 는 소실됩니다. 그런 경우 Operation 을 쓰거나, 스코프 첫 줄에
아래 한 줄을 넣으세요.

```xml
<set-variable variableName="ctx" value="#[attributes]"/>
```

### 왜 Scope 가 직접 variable 을 세팅하지 않는가

Mule SDK 의 `Chain` 인터페이스는 `process(payload, attributes, …)` 만 제공하고
**variable 주입 API 가 없습니다.** `CoreEvent.builder(event).addVariable(...)` 는
`org.mule.runtime.core.*` 를 요구하는데 이 패키지는 extension 클래스로더에 export
되지 않고, privileged API 허용 아티팩트 목록은 런타임 배포본에 고정되어 있어
서드파티 커넥터가 접근할 수 없습니다. 리플렉션 우회는 4.x 마이너 업그레이드마다
깨지므로 채택하지 않았습니다. 이것이 Scope + Operation 하이브리드 구성의 이유입니다.

## 에러 타입

| 타입 | 발생 조건 |
|---|---|
| `BIZ-LOG:INVALID_CONTEXT` | 문자열 파라미터가 공백 (Mule 은 required 파라미터의 *존재*만 보장하고 빈 문자열은 통과시킴) |
| `BIZ-LOG:EXECUTION` | 컨텍스트 구성 실패 |

Scope **내부**에서 발생한 예외는 감싸지 않고 원본 에러 타입을 유지한 채 전파됩니다.

## 빌드

```bash
mvn clean install
```

단위 테스트 6개가 함께 실행됩니다 (`LogContextTestCase` — Mule 컨테이너 불필요).

### functional test 는 별도 실행이 필요합니다

`MuleArtifactFunctionalTestCase` 기반 테스트(`*FunctionalTestCase`)는 테스트 러너가
격리된 컨테이너 클래스로더를 구성하기 위해 아래 아티팩트를 요구합니다.

```
com.mulesoft.mule.distributions:mule-runtime-apis-split-loader-bom:pom:4.9.17
```

이 아티팩트는 **MuleSoft EE 리포지터리 전용**입니다 (public: 404, releases-ee: 401).
엔터프라이즈 자격증명이 필요하므로 기본 빌드에서는 제외했고, 자격증명이 설정된
환경에서만 실행합니다.

```bash
mvn verify -Pfunctional-tests
```

`~/.m2/settings.xml` 에 EE 리포지터리와 credentials 를 등록해야 합니다.

### 전제 조건

1. **MuleSoft 공개 리포지터리.** parent POM 해석은 POM 안의 `<repositories>` 보다
   먼저 일어나므로, `~/.m2/settings.xml` 에 등록해야 합니다.

   ```xml
   <profiles>
     <profile>
       <id>mule</id>
       <activation><activeByDefault>true</activeByDefault></activation>
       <repositories>
         <repository>
           <id>mulesoft-public</id>
           <url>https://repository.mulesoft.org/nexus/content/repositories/public</url>
           <releases><enabled>true</enabled></releases>
           <snapshots><enabled>false</enabled></snapshots>
         </repository>
       </repositories>
     </profile>
   </profiles>
   ```

2. **JDK.** JDK 21 에서도 `mvn clean install` 은 통과합니다 (`release=17` 로 컴파일).
   단, 커넥터가 `supportedJavaVersions: ["17"]` 로 선언되어 있어 JDK 21 런타임에서
   테스트/배포하면 아래 경고가 뜨고 거부될 수 있습니다.

   ```
   Extension 'Biz Com Log' does not support Java 21. Supported versions are: [17]
   ```

   JDK 21 도 지원하려면 `BizComLogExtension` 의 `@JavaVersionSupport({JAVA_17})` 를
   `{JAVA_17, JAVA_21}` 로 확장하세요.

## Mule 앱에서 사용

```xml
<dependency>
  <groupId>org.mycompany</groupId>
  <artifactId>biz-com-lib-log-connector</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <classifier>mule-plugin</classifier>
</dependency>
```

## SDK 관련 메모 (같은 실수 반복 방지)

- `sdk-api` 의 `@Parameter` 는 `@Target({FIELD})` — **필드 전용**입니다. Operation 메서드
  인자는 암시적으로 파라미터이므로 애노테이션을 붙이면 컴파일 에러가 납니다.
  (구 `extensions-api` 와 달라진 부분)
- `sdk-api` 의 `MediaType` 에는 `APPLICATION_JAVA` 상수가 **없습니다**. POJO 를 반환하는
  Operation 은 `@MediaType` 을 생략하면 SDK 가 `application/java` 로 추론합니다.
  해당 애노테이션은 `String` / `InputStream` 반환 시에만 필수입니다.
- Scope 는 `@Config` 를 받을 수 없습니다 (위 참고).
- Mule 4 는 XSD 를 빌드 산출물로 만들지 않고 **런타임에 생성**합니다. `target/` 에서
  스키마를 찾지 마세요.

## TODO

- [ ] `TriggerType` / `Status` enum 상수를 실제 도메인 값으로 교체
      (상수 변경은 이미 작성된 앱 XML 을 깨뜨리므로 초기에 확정)
- [ ] EE 자격증명 확보 후 `mvn verify -Pfunctional-tests` 로 DSL / 이벤트 전파 검증
