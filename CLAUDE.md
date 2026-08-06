# CLAUDE.md

이 파일은 이 레포에서 작업하는 Claude Code 세션이 자동으로 읽습니다. README 는 커넥터를
**쓰는 사람**을 위한 문서이고, 이 파일은 커넥터를 **고치는 사람**을 위한 문서입니다.

## 무엇인가

Mule 4 커스텀 커넥터. 로그 컨텍스트(플로우 버전, 테이블명, 트리거 종류, 작업 주체, 출발 /
대상 앱, 처리 결과, correlation id, 시각, 원본 메시지)를 Mule 이벤트에 주입합니다.
**컴포넌트는 `logging-context` Scope 하나입니다** (1.2.0 에서 Operation 제거).

| | |
|---|---|
| 좌표 | `org.mycompany:biz-com-lib-log-connector` |
| 현재 버전 | `1.2.0-SNAPSHOT` (`pom.xml`, `README.md` 두 곳) |
| 타깃 | Mule Runtime 4.9.17 / JDK 17 |
| Parent | `org.mule.extensions:mule-modules-parent:1.9.17` |
| XML prefix | `biz-log` |
| 원격 | `git@github.com:ksseo-i2max/biz-com-lib-log-connector.git` |

## 빌드 / 테스트

```bash
mvn clean install          # 단위 테스트 15개 포함, 통과해야 커밋
```

**functional test 는 기본 빌드에서 제외돼 있습니다.** 테스트 러너가
`com.mulesoft.mule.distributions:mule-runtime-apis-split-loader-bom:pom:4.9.17` 을 요구하는데
이 아티팩트는 **MuleSoft EE 리포지터리 전용**입니다 (2026-08-03 확인: public 404,
`nexus-ee/.../releases-ee` 401). `pom.xml` 의 `functionalTestExclude` property 로 걸러내고
있습니다.

```bash
mvn verify -Pfunctional-tests    # EE 자격증명이 ~/.m2/settings.xml 에 있을 때만
```

**따라서 `mvn clean install` 이 통과해도 다음은 미검증 상태입니다** — 사용자에게 이 점을
분명히 알리세요.

- DSL 생성 결과 (Mule 4 는 XSD 를 런타임에 생성. `target/` 에서 스키마를 찾지 마세요)
- attributes / vars 전파
- 파라미터 기본값 및 필수 여부의 실제 스키마 강제
- `p('app.name')` 해석

parent POM 해석은 POM 안의 `<repositories>` 보다 먼저 일어나므로 `~/.m2/settings.xml` 에
MuleSoft public 리포지터리가 등록되어 있어야 합니다.

빌드 JDK 는 21 이어도 통과하지만(`release=17`), 커넥터가 `supportedJavaVersions: ["17"]` 로
선언되므로 JDK 21 런타임에서 테스트하면 거부될 수 있습니다.

## 구조

```
BizComLogExtension        @Extension @Xml(biz-log) @JavaVersionSupport(JAVA_17) @Export
                          @Operations 를 extension 에 직접 — Configuration 없음
BizComLogOperations       loggingContext(Scope) 하나
param/LogTargetParameters flowVersion / baseTableName  → Scope 파라미터
param/LogContextParameters 컨텍스트 파라미터 (@ParameterGroup)
param/TriggerType         API, BATCH
param/Status              SUCCESS, FAIL
model/LogContext          Serializable, Builder 패턴, 검증은 build() 한 곳
error/LogErrorType        BIZ-LOG:INVALID_CONTEXT, BIZ-LOG:EXECUTION
```

## Mule SDK 제약 — 전부 빌드로 실증한 것

여기 적힌 것은 추측이 아니라 이 프로젝트에서 실제로 부딪혀 확인한 사실입니다. 되돌리지
마세요.

| 사실 | 결과 |
|---|---|
| `sdk-api` 의 `@Parameter` 는 `@Target({FIELD})` — **필드 전용** | Operation 메서드 인자에 붙이면 컴파일 에러. 인자는 암시적으로 파라미터다 (구 `extensions-api` 와 다름) |
| **Scope 는 `@Config` 를 받을 수 없다** | `IllegalOperationModelDefinitionException: Scope 'loggingContext' requires a config, but that is not allowed`. 그래서 Scope 는 `LogTargetParameters` 로 두 값을 직접 받는다 |
| `sdk-api` 의 `MediaType` 에 `APPLICATION_JAVA` 상수가 **없다** | POJO 반환 Operation 은 `@MediaType` 생략 → SDK 가 `application/java` 로 추론. 애노테이션은 `String` / `InputStream` 반환 시에만 필수 |
| **"필수"와 "기본값"은 동시에 성립하지 않는다** | `@Optional(defaultValue=...)` 을 붙이면 스키마상 required 가 아니게 된다. 사용자가 이걸 세 번 요청했고 매번 설명이 필요했다 |
| required primitive `boolean` 은 **허용된다** | `@Optional` 없는 `boolean` 파라미터로 모델 생성 통과 |
| `Chain` 인터페이스에 **variable 주입 API 가 없다** | `process(payload, attributes, …)` 만 있다. Scope 는 flow variable 을 세팅할 수 없다 → 사용자가 스코프 첫 줄에 `<set-variable value="#[attributes]"/>` 를 넣는다 |
| `@Operations` 를 **extension 클래스에 직접** 달면 Configuration 없이 빌드된다 | SDK 가 암시적 기본 config 를 만든다. 1.2.0 에서 실증 — 생성된 `biz-com-log-extension-descriptions.xml` 에 `loggingContext` 만 있고 `<configuration>` 요소는 없다 |
| Mule 4 는 XSD 를 **런타임에 생성**한다 | 빌드 산출물에 스키마가 없다 |

주입 가능한 것들 (모두 검증됨):

| 타입 | 용도 |
|---|---|
| `org.mule.sdk.api.runtime.parameter.CorrelationInfo` | `getCorrelationId()` — 기존 이벤트 값 재사용 |
| `org.mule.sdk.api.runtime.streaming.StreamingHelper` | `resolveCursorProvider()` — 스트림을 재조회 가능 형태로 |
| `org.mule.runtime.api.component.ConfigurationProperties` (`@Inject`, `javax.inject`) | `resolveStringProperty("app.name")` |
| `org.mule.sdk.api.annotation.param.Optional.PAYLOAD` | `"#[payload]"` 상수 |

`java.util.Optional` 을 import 하면 `sdk-api` 의 `@Optional` 과 단순명이 충돌합니다.
완전수식명을 쓰세요.

## 의도된 설계 — "고치지" 마세요

- **Scope 단일 컴포넌트 (1.2.0).** `build-context` Operation 과 `BizComLogConfiguration`
  을 제거했습니다 (사용자 요청). Scope 의 attributes 는 메시지를 교체하는 컴포넌트를
  지나면 소실되지만, 스코프 첫 줄의 `<set-variable variableName="ctx"
  value="#[attributes]"/>` 가 Operation + `target` 과 동일한 flow variable 을 만들고
  스코프를 빠져나온 뒤에도 유지됩니다 — 두 경로가 기능적으로 겹쳐 하나로 줄였습니다.
  되살리지 마세요. Configuration 을 다시 두면 Scope 는 config 를 못 받으므로 쓰이지
  않는 `<biz-log:config>` 요소만 남습니다.
- **에러는 감싸지 않습니다.** 체인 내부 예외를 `callback.error(throwable)` 로 그대로
  넘겨 원본 에러 타입을 유지합니다. `BIZ-LOG:*` 로 감싸면 사용자의
  `<error-handler type="HTTP:CONNECTIVITY">` 가 동작하지 않습니다.
- **payload pass-through.** Scope 는 `#[payload]` 기본값 파라미터로 원본 payload 와
  media type 을 받아 되돌려줍니다. `Chain.process` 에 임의 payload 를 넣으면 사용자
  데이터가 조용히 사라집니다.
- **`LogContext.toString()` 은 원본 payload 값을 찍지 않습니다** (`requestPayload=<String>`).
  로그로 흘러가는 객체라 요청 본문 전체가 쏟아지면 용량 / 민감정보 문제가 됩니다.
  테스트로 고정돼 있습니다.
- **`equals`/`hashCode` 에서 `requestPayload` / `originAttributes` 제외.**
  사용자의 임의 객체라 identity 기반 `equals` 인 경우가 많아 포함시키면 논리적으로 같은
  컨텍스트도 거의 항상 다르다고 판정됩니다.
- **`requestPayload` 는 `build()` 에서 `Builder.payload` 를 플래그로 게이팅해 만듭니다.**
  직접 넣는 setter 가 없어 플래그와 값이 어긋날 수 없습니다.
- **`originPayload` 는 1.1.0 에서 제거했습니다** (사용자 요청). `requestPayload` 와 내용이
  같으면서 게이팅이 없어, 플래그를 꺼도 요청 본문이 컨텍스트에 남는 것이 이유였습니다.
  되살리지 마세요 — `includeRequestPayload` 가 요청 본문 기록 여부의 유일한 스위치라는
  것이 현재 설계입니다. `originAttributes` 는 그대로 남아 있고 게이팅이 없습니다.
- **`startTime` 은 `LocalDateTime.now(ZoneOffset.UTC)`.** `LOG_TIME_ZONE` 상수 한 곳에서
  관리합니다. `OffsetDateTime` 으로 바꿨다가(`79ebe94`) 사용자 요청으로 되돌렸습니다
  (`283c0ac`) — JDBC 가 오프셋 없는 `TIMESTAMP` 로 바인딩되는 쪽을 택했습니다. 대가는
  값만 보고 UTC 인지 알 수 없다는 점입니다.
- **`sourceAppName` 은 DSL 파라미터가 아닙니다.** `ConfigurationProperties` 로 `app.name` 을
  읽어 Java 에서 채웁니다. 사용자가 지정할 여지를 없앤 것이 의도입니다.
- **자동 파생 값은 검증하지 않습니다** (`correlationId`, `sourceAppName`). 프로퍼티나
  이벤트가 없는 환경에서 null 일 수 있고, 그것 때문에 로깅이 실패하면 안 됩니다.

## 파라미터 현황

`targetAppName` 을 포함해 대부분 기본값이 있고, **`includeRequestPayload` /
`includeResponsePayload` 두 개만 스키마 레벨 필수**입니다.

```
flowVersion            v1
baseTableName          MULE_BIZ_INTERFACE_LOG
triggerType            API            (enum: API, BATCH)
actor                  SFDC
targetAppName          biz-com-exp-listener
status                 SUCCESS        (enum: SUCCESS, FAIL)
includeRequestPayload   필수 — 기본값 없음
includeResponsePayload  필수 — 기본값 없음
```

필수 보장은 대부분 **도메인 레벨**에 있습니다 — `LogContext.Builder.build()` 가 null /
공백을 `BIZ-LOG:INVALID_CONTEXT` 로 거부합니다. 부작용: 파라미터 이름을 틀려도 기동
시점에 안 잡히고 기본값이 조용히 기록됩니다. 특히 `status` 기본값이 `SUCCESS` 라 실패
경로에서 `status="FAIL"` 을 빠뜨리면 성공으로 남습니다.

## 작업 규칙

- **기능 추가마다 patch 버전을 하나 올립니다** — `pom.xml` 과 `README.md` 두 곳.
  `minor`/`major` 는 호환성 파괴(enum 상수 제거, 파라미터 이름 변경, 컨텍스트 항목 제거)용.
- **빌드 통과 후 커밋·푸시까지 한 턴에 마칩니다.** 사용자가 `push` 를 따로 요청하지
  않아도 됩니다.
- 주석 / javadoc / 커밋 메시지 본문은 **한국어**. 코드 식별자는 영어.
- 파라미터를 추가하면 손봐야 할 곳: `LogContextParameters`(또는 `LogTargetParameters`),
  `LogContext`(필드 / 생성자 / getter / builder / `from(params)` / `toMap` / `equals` /
  `hashCode` / `toString`), 단위 테스트의 `toMap` 키 순서 단정,
  `biz-log-scope-test.xml`, `README.md`, 버전.

## 미해결 / 보류

- **`endTime`** — 가능하지만 사용자가 취소. 체인 완료 시점에 찍어 스코프 **출력**
  attributes 로만 실을 수 있습니다. 체인 안에서는 볼 수 없고(논리적 불가), 현재의
  pass-through 동작이 깨지며, 에러 경로에서는 attributes 를 실을 수 없습니다.
- **`responsePayload`** — `includeResponsePayload` 플래그만 전달되고 값은 채워지지
  않습니다. `endTime` 과 같은 제약입니다.
- ~~**`requestPayload` vs `originPayload` 중복**~~ — 1.1.0 에서 `originPayload` 제거로
  해소했습니다.
- **`startTime` UTC 테스트의 한계** — `LocalDateTime` 에는 검사할 오프셋이 없어 UTC 기준
  범위 비교만 합니다. 로컬 타임존이 UTC 인 CI 머신에서는 회귀를 놓칩니다.
- **DB 컬럼 타입 미확인** — `MULE_BIZ_INTERFACE_LOG` 의 실제 스키마를 본 적 없습니다.
- **EE 자격증명 확보 후 `mvn verify -Pfunctional-tests`** 로 위 미검증 항목들을 검증할 것.
