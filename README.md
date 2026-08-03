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

## 제공 컴포넌트

### Configuration

```xml
<biz-log:config name="BizLog_Config"
                flowVersion="1.0.0"
                baseTableName="TB_IF_LOG"/>
```

`flowVersion`, `baseTableName` 모두 **필수**입니다. 누락 시 앱 기동 시점에 검증 실패합니다.

### 1. Scope — `<biz-log:with-context>`

컨텍스트를 메시지 **attributes** 로 주입한 상태로 하위 컴포넌트를 실행합니다.

```xml
<flow name="scopeStyleFlow">
  <biz-log:with-context config-ref="BizLog_Config"
      triggerType="EVENT" actor="batch-user"
      targetAppName="SFDC" status="READY">

    <logger message="#[attributes.actor ++ ' → ' ++ attributes.baseTableName]"/>
    <flow-ref name="businessFlow"/>
  </biz-log:with-context>
</flow>
```

원본 payload 와 media type 은 그대로 보존됩니다.

### 2. Operation — `<biz-log:build-context>`

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

| | 접근 | 메시지 교체 후 생존 |
|---|---|---|
| Scope | `#[attributes.actor]` | ❌ |
| Operation + `target` | `#[vars.ctx.actor]` | ✅ |

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
| `BIZ-LOG:INVALID_CONTEXT` | 문자열 파라미터가 공백 |
| `BIZ-LOG:EXECUTION` | 컨텍스트 구성 실패 |

Scope **내부**에서 발생한 예외는 감싸지 않고 원본 에러 타입을 유지한 채 전파됩니다.

## 빌드

```bash
mvn clean install
```

### 전제 조건

1. **JDK 17.** 현재 개발 머신은 JDK 21 입니다. `maven.compiler.release=17` 로
   바이트코드는 맞출 수 있지만, extension model 생성 단계는 **빌드 JDK 위에서**
   실행되므로 JDK 17 로 빌드하는 것을 권장합니다.

   ```bash
   export JAVA_HOME=/path/to/jdk-17
   mvn clean install
   ```

   또는 `~/.m2/toolchains.xml` 로 컴파일 JDK 를 17 로 고정하세요.

2. **MuleSoft 공개 리포지터리.** parent POM 해석은 POM 안의 `<repositories>` 보다
   먼저 일어나므로, `~/.m2/settings.xml` 에 MuleSoft 공개 리포지터리를 등록해야
   합니다.

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

## Mule 앱에서 사용

```xml
<dependency>
  <groupId>org.mycompany</groupId>
  <artifactId>biz-com-lib-log-connector</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <classifier>mule-plugin</classifier>
</dependency>
```

## TODO

- [ ] `TriggerType` / `Status` enum 상수를 실제 도메인 값으로 교체
      (상수 변경은 이미 작성된 앱 XML 을 깨뜨리므로 초기에 확정)
- [ ] JDK 17 환경에서 `mvn clean install` 및 테스트 실행 검증
