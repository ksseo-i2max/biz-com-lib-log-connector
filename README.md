# biz-com-lib-log-connector

로그 컨텍스트를 Mule 이벤트에 주입하는 MuleSoft 커스텀 커넥터.

| 항목 | 값 |
|---|---|
| groupId / artifactId | `org.mycompany` / `biz-com-lib-log-connector` |
| Mule Runtime | 4.9.17 |
| JDK | 17 |
| Parent | `org.mule.extensions:mule-modules-parent:1.9.17` |
| 현재 버전 | `1.1.0-SNAPSHOT` |
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

### 1. Scope — Logging Context (`<biz-log:logging-context>`)

컨텍스트를 메시지 **attributes** 로 주입한 상태로 하위 컴포넌트를 실행합니다.

```xml
<flow name="scopeStyleFlow">
  <biz-log:logging-context flowVersion="${biz.log.flowVersion}"
      baseTableName="${biz.log.baseTableName}"
      triggerType="API" actor="SFDC"
      targetAppName="biz-com-exp-listener" status="SUCCESS">

    <logger message="#[attributes.actor ++ ' → ' ++ attributes.baseTableName]"/>
    <flow-ref name="businessFlow"/>
  </biz-log:logging-context>
</flow>
```

원본 payload 와 media type 은 그대로 보존됩니다.

> **Scope 에는 `config-ref` 가 없습니다.** Mule SDK 가 Scope 의 configuration 바인딩을
> 금지하기 때문입니다. Scope 메서드에 `@Config` 를 붙이면 빌드가 실패합니다.
>
> ```
> IllegalOperationModelDefinitionException:
>   Scope 'loggingContext' requires a config, but that is not allowed, remove such parameter
> ```
>
> 그래서 `flowVersion` / `baseTableName` 을 Scope 자체 파라미터로 받습니다. 앱마다 한 번만
> 정하려면 위 예시처럼 `${...}` property placeholder 를 쓰면 됩니다.

`includeRequestPayload` / `includeResponsePayload` 만 필수이고 나머지는 기본값이 있습니다.

```xml
<biz-log:logging-context includeRequestPayload="false" includeResponsePayload="false">
  ...
</biz-log:logging-context>
<!-- v1 / MULE_BIZ_INTERFACE_LOG / API / SFDC / biz-com-exp-listener / SUCCESS -->
```

### 2. Configuration + Operation — Build Context (`<biz-log:build-context>`)

Operation 은 config 바인딩이 허용되므로, 이 경로에서는 두 값을 Configuration 에서 가져옵니다.

```xml
<!-- 두 값 모두 기본값이 있어 이름만 주면 됩니다 -->
<biz-log:config name="BizLog_Config"/>

<!-- 다르게 쓰려면 명시 -->
<biz-log:config name="BizLog_Config_V2"
                flowVersion="v2"
                baseTableName="MULE_BIZ_INTERFACE_LOG_HIST"/>
```

두 파라미터 모두 기본값(`v1` / `MULE_BIZ_INTERFACE_LOG`)이 있어 생략 가능합니다.
단, 빈 문자열을 명시하면 `BIZ-LOG:INVALID_CONTEXT` 로 거부됩니다.

`target` 과 함께 쓰면 **진짜 flow variable** 이 되어 메시지가 교체되어도 살아남습니다.

```xml
<flow name="varStyleFlow">
  <biz-log:build-context config-ref="BizLog_Config"
      triggerType="BATCH" actor="SFDC"
      targetAppName="SAP" status="FAIL"
      target="ctx"/>

  <http:request .../>                      <!-- 메시지 교체됨 -->
  <logger message="#[vars.ctx.actor]"/>    <!-- 여전히 유효 -->
  <flow-ref name="businessFlow"/>
</flow>
```

## 파라미터

### 로그 대상 (Scope 파라미터 / Configuration)

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `flowVersion` | `v1` | 로그 스키마 / 플로우 버전 식별자 |
| `baseTableName` | `MULE_BIZ_INTERFACE_LOG` | 로그가 기록될 기준 테이블명 |

### 컨텍스트 (Scope / Operation 공통)

| 파라미터 | 타입 | 기본값 | 값 |
|---|---|---|---|
| `triggerType` | enum | `API` | `API` (외부 API 호출로 기동), `BATCH` (배치 / 스케줄러로 기동) |
| `actor` | String | `SFDC` | 작업 주체 (사용자 ID 또는 시스템 계정) |
| `targetAppName` | String | `biz-com-exp-listener` | 연동 대상 애플리케이션 명 |
| `status` | enum | `SUCCESS` | `SUCCESS` (정상 처리), `FAIL` (실패) |
| `includeRequestPayload` | boolean | **없음 (필수)** | 진입 직전 payload 를 `requestPayload` 로 기록 |
| `includeResponsePayload` | boolean | **없음 (필수)** | 응답 payload 기록 여부 (아래 참고) |

`sourceAppName` 은 **DSL 파라미터가 아닙니다** — 커넥터가 Java 에서 `app.name` 프로퍼티를
읽어 채웁니다. 아래 "sourceAppName" 절을 참고하세요.

`includeRequestPayload` / `includeResponsePayload` 는 **필수 속성**이라 항상 명시해야
합니다. Mule SDK 에서 필수와 기본값은 동시에 성립하지 않으므로 스키마 기본값은 없습니다.

```xml
<biz-log:logging-context includeRequestPayload="false" includeResponsePayload="false">
  ...
</biz-log:logging-context>
```

enum 상수는 앱 XML 의 스키마 검증 대상입니다. 상수를 변경하거나 제거하면 이미 배포된
앱의 `triggerType="..."` / `status="..."` 값이 기동 시점에 깨집니다.

### "필수" 와 "기본값" 에 대해

Mule SDK 에서 이 둘은 **동시에 성립하지 않습니다.** `@Optional(defaultValue = ...)` 을
붙이면 스키마상 required 가 아니게 되어 XML 에서 생략할 수 있습니다.

`includeRequestPayload` / `includeResponsePayload` 두 개만 **스키마 레벨 필수**이고
나머지는 기본값이 있습니다.

```xml
<biz-log:logging-context includeRequestPayload="false" includeResponsePayload="false">
  ...
</biz-log:logging-context>
<!-- v1 / MULE_BIZ_INTERFACE_LOG / API / SFDC / biz-com-exp-listener / SUCCESS -->
```

필수 보장은 도메인 레벨에만 있습니다. `LogContext` 빌더가 null / 공백을
`BIZ-LOG:INVALID_CONTEXT` 로 거부하므로 **컨텍스트에 값이 비는 일은 없습니다.**
빈 문자열(`" "`)을 명시하는 경우도 거부됩니다.

> **알고 쓸 것.** 파라미터를 하나도 안 써도 앱이 기동하므로, 오타로 파라미터 이름을
> 틀리거나 빠뜨렸을 때 기동 시점에 잡히지 않고 **기본값이 조용히 기록됩니다.**
> 특히 `status` 는 기본값이 `SUCCESS` 라서 실패 경로에서 `status="FAIL"` 을 빠뜨리면
> 성공으로 남습니다. 값이 반드시 명시되어야 하는 파라미터가 있으면 그것만 기본값을
> 빼는 편이 안전합니다.

## 컨텍스트에 실리는 항목

`attributes` (Scope) 또는 `vars.<target>` (Operation) 으로 아래 13개가 노출됩니다.

| 항목 | 타입 | 출처 |
|---|---|---|
| `flowVersion` | String | 파라미터 / Configuration (기본 `v1`) |
| `baseTableName` | String | 파라미터 / Configuration (기본 `MULE_BIZ_INTERFACE_LOG`) |
| `triggerType` | `TriggerType` | 파라미터 (기본 `API`) |
| `actor` | String | 파라미터 (기본 `SFDC`) |
| `sourceAppName` | String | **커넥터가 `app.name` 프로퍼티에서 자동 기록** (DSL 파라미터 아님) |
| `targetAppName` | String | 파라미터 (기본 `biz-com-exp-listener`) |
| `status` | `Status` | 파라미터 (기본 `SUCCESS`) |
| `correlationId` | String | **기존** Mule 이벤트의 correlation id |
| `startTime` | `LocalDateTime` | 커넥터가 자동 기록 (**UTC 기준**) |
| `includeRequestPayload` | boolean | 파라미터 (필수) |
| `includeResponsePayload` | boolean | 파라미터 (필수) |
| `requestPayload` | Object | 컴포넌트 진입 **전** payload. `includeRequestPayload=true` 일 때만 |
| `originAttributes` | Object | 컴포넌트 진입 **전** attributes |

```xml
<logger message="#[attributes.correlationId]"/>
<logger message="#[attributes.startTime]"/>
<logger message="#[attributes.requestPayload]"/>             <!-- 플래그를 켰을 때만 -->
<logger message="#[attributes.originAttributes.headers]"/>   <!-- 예: HTTP 요청 헤더 -->
```

> **1.1.0 에서 `originPayload` 가 제거됐습니다.** `requestPayload` 와 내용이 같으면서
> 게이팅 없이 항상 담겨, `includeRequestPayload="false"` 로 꺼도 요청 본문이 컨텍스트에
> 남았습니다. 원본 payload 가 필요하면 `includeRequestPayload="true"` 로 켜고
> `requestPayload` 를 쓰세요.

### correlationId

커넥터가 새로 만들지 않고 **현재 Mule 이벤트의 correlation id 를 그대로** 담습니다
(`CorrelationInfo` 주입). 따라서 아래가 성립합니다.

```xml
<logger message="#[attributes.correlationId == correlationId]"/>   <!-- 항상 true -->
```

같은 이벤트에서 나온 로그끼리 이 값으로 묶을 수 있고, `flow-ref` 로 플로우 경계를 넘어도
동일합니다. HTTP Listener 는 요청의 `X-Correlation-ID` 헤더를 이 값으로 받아들이므로
호출측 시스템의 추적 id 와도 이어집니다.

### sourceAppName — Java 에서 채웁니다

DSL 파라미터로 노출하지 않습니다. 커넥터가 `ConfigurationProperties` 를 주입받아
`app.name` 프로퍼티에서 읽어 채우므로 **사용자가 값을 지정할 여지가 없고 항상 현재 Mule
앱 이름이 기록됩니다.**

```java
@Inject
private ConfigurationProperties configurationProperties;
// ...
configurationProperties.resolveStringProperty("app.name").orElse(null)
```

프로퍼티가 없는 환경에서는 `null` 이 됩니다. 자동 파생 값 때문에 로깅이 실패하면 안 되므로
검증하지 않습니다 (`correlationId` 와 같은 취급).

### startTime — UTC 기준

컨텍스트 생성 시각이 `LocalDateTime.now(ZoneOffset.UTC)` 로 채워집니다. Scope 의 경우
**하위 체인 실행 전**, 즉 스코프 진입 시각입니다.

서버 로컬 타임존으로 찍으면 여러 환경 / 리전의 로그를 한 테이블에서 비교할 수 없으므로
UTC 로 고정했습니다. 앞으로 추가되는 시각 항목도 같은 기준을 따릅니다
(`LogContext.LOG_TIME_ZONE` 한 곳에서 관리).

```
2026-08-04T06:15:30.123
```

> **타입이 `LocalDateTime` 이라 값에 오프셋이 남지 않습니다.** "이 커넥터의 시각은 UTC"
> 라는 약속으로만 성립하므로, 컬럼 주석이나 API 문서에 함께 남겨 두세요. 값에 오프셋을
> 새기려면 `OffsetDateTime` 이어야 하지만, JDBC 가 `TIMESTAMP WITH TIME ZONE` 으로
> 바인딩하게 되어 오프셋 없는 `TIMESTAMP` 컬럼에 그대로 넣을 수 없습니다. 일반적인 로그
> 테이블 컬럼에 바로 들어가는 쪽을 택했습니다.

처리 소요시간을 재려면 종료 시각이 필요합니다. 스코프가 체인 실행을 마친 뒤 `endTime` 을
채우는 방식이 되며, 그 값은 체인 **안에서는** 볼 수 없고 스코프가 반환하는 attributes 에만
실립니다. 필요하시면 추가하겠습니다.

### requestPayload — includeRequestPayload 로 게이팅

`includeRequestPayload="true"` 일 때만 진입 직전 payload 가 `requestPayload` 에 담기고,
꺼져 있으면 `null` 입니다. 기본값 없이 **필수**라 매번 명시해야 합니다 — 요청 본문을
로그에 담는 것은 용량과 민감정보 노출 비용이 있는 선택이기 때문입니다.

```xml
<biz-log:logging-context includeRequestPayload="true" includeResponsePayload="false">
  <logger message="#[attributes.requestPayload]"/>
</biz-log:logging-context>
```

값은 빌드 시점에 게이팅되므로 플래그와 값이 어긋날 수 없습니다 (`requestPayload` 를
직접 넣는 setter 가 없습니다).

**1.1.0 부터 이 플래그가 요청 본문이 컨텍스트에 남는지를 결정하는 유일한 스위치입니다.**
이전에는 게이팅 없이 항상 담기는 `originPayload` 가 함께 있어, 플래그를 꺼도 본문이
그대로 남았습니다.

Scope 는 attributes 를 교체하는 컴포넌트를 지나면 컨텍스트가 소실되므로, 체인 뒤에서
원본을 참조하려면 진입 직후 `vars` 로 옮겨 둡니다.

```xml
<biz-log:logging-context includeRequestPayload="true" includeResponsePayload="false">
  <set-variable variableName="ctx" value="#[attributes]"/>
  <http:request .../>                               <!-- 메시지 교체 -->
  <logger message="#[vars.ctx.requestPayload]"/>    <!-- 원본 요청 본문 -->
</biz-log:logging-context>
```

스트리밍 payload 는 커넥터가 `StreamingHelper.resolveCursorProvider(...)` 로 반복 조회
가능한 형태로 바꿔 담습니다. 원본 커서를 그대로 들고 있으면 체인이 소비한 뒤 다시 읽을
수 없기 때문입니다.

### includeResponsePayload — 플래그만 전달됩니다

**`responsePayload` 항목은 아직 없습니다.** 응답 payload 는 스코프의 하위 체인이 끝나야
정해지는 값이라, 스코프가 반환하는 attributes 를 교체하는 방식이 필요합니다 — 취소하신
`endTime` 과 같은 제약입니다. 현재는 플래그만 컨텍스트에 실려 downstream 이 "응답을
기록할 의도였는지"를 알 수 있습니다.

### originAttributes

Scope 는 attributes 를 로그 컨텍스트로 **교체**하므로, 원래 attributes 는
`attributes.originAttributes` 에서 꺼내 씁니다. 체인 안에서 메시지가 어떻게 바뀌든
진입 시점 값을 계속 참조할 수 있습니다.

```xml
<biz-log:logging-context baseTableName="MULE_BIZ_INTERFACE_LOG" triggerType="API"
    actor="SFDC" targetAppName="biz-com-exp-listener" status="SUCCESS"
    includeRequestPayload="false" includeResponsePayload="false">

  <logger message="#[attributes.originAttributes.headers]"/>   <!-- 원본 HTTP 헤더 -->
</biz-log:logging-context>
```

`requestPayload` 와 달리 게이팅이 없어 항상 담깁니다. attributes 는 보통 헤더 / 쿼리
파라미터 수준이라 본문만큼의 용량 부담이 없기 때문입니다 — 다만 헤더에 토큰이 실릴 수는
있습니다.

> **직렬화 주의.** `LogContext` 는 `Serializable` 이고 나머지 필드는 모두 직렬화 가능하지만,
> `requestPayload` / `originAttributes` 는 사용자의 임의 객체입니다. 이 컨텍스트를
> VM connector / persistent object store / 클러스터로 넘길 계획이면 해당 값이 직렬화
> 가능한지 확인하세요. `CursorProvider` 는 직렬화 대상이 아닙니다.

> **로그 노출 주의.** `LogContext.toString()` 은 원본 payload 의 **값을 찍지 않고 타입만**
> 남깁니다 (`requestPayload=<String>`). 요청 본문 전체가 로그에 쏟아지면 용량 문제와
> 민감정보 노출로 이어지기 때문입니다. DB 에 기록할 때는 `toMap()` 이 원본 객체를 그대로
> 주므로 마스킹 여부를 호출측에서 결정하세요.

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

## 버전 정책

기능이 추가될 때마다 **patch 자리를 하나** 올립니다.

```
1.0.6-SNAPSHOT  →  1.0.7-SNAPSHOT  →  1.0.8-SNAPSHOT  ...
```

`minor` / `major` 자리는 호환성이 깨지는 변경에 남겨 둡니다. 예를 들어 enum 상수 제거,
파라미터 이름 변경, 컨텍스트 항목 제거처럼 이미 배포된 앱의 XML 이나 DataWeave 표현식을
깨뜨리는 변경이 그렇습니다.

**1.0.8 → 1.1.0** 이 그 경우입니다. `originPayload` 컨텍스트 항목을 제거했으므로
`#[attributes.originPayload]` / `#[vars.ctx.originPayload]` 를 쓰던 앱은 고쳐야 합니다
(`includeRequestPayload="true"` + `requestPayload`).

버전은 `pom.xml` 과 이 README 두 곳에 있습니다 (`.idea/` 는 IDE 생성물이라 gitignore 대상).

## Mule 앱에서 사용

```xml
<dependency>
  <groupId>org.mycompany</groupId>
  <artifactId>biz-com-lib-log-connector</artifactId>
  <version>1.1.0-SNAPSHOT</version>
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

- [ ] EE 자격증명 확보 후 `mvn verify -Pfunctional-tests` 로 DSL / 이벤트 전파 검증
