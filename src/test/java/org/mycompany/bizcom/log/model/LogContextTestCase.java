package org.mycompany.bizcom.log.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mycompany.bizcom.log.param.Status.FAIL;
import static org.mycompany.bizcom.log.param.Status.SUCCESS;
import static org.mycompany.bizcom.log.param.TriggerType.API;
import static org.mycompany.bizcom.log.param.TriggerType.BATCH;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.Test;
import org.mule.sdk.api.exception.ModuleException;
import org.mycompany.bizcom.log.error.LogErrorType;

/**
 * {@link LogContext} 단위 테스트.
 *
 * <p>Mule 컨테이너를 띄우지 않으므로 EE 리포지터리 접근 없이 어디서나 실행된다.
 * 컨테이너가 필요한 DSL / 이벤트 전파 / 파라미터 기본값 검증은 {@code *FunctionalTestCase}
 * 에 있고, 그쪽은 EE 자격증명이 있을 때 {@code -Pfunctional-tests} 로 실행한다.
 *
 * <p>{@code startTime} 이 걸린 단정은 빌더로 명시해 결정적으로 만든다. 자동 스탬핑은
 * 별도 테스트에서 범위로만 검증한다.
 */
public class LogContextTestCase {

  private static final LocalDateTime FIXED = LocalDateTime.of(2026, 8, 4, 10, 15, 30, 123_000_000);

  /** 필수값만 채운 빌더. 각 테스트가 필요한 것만 덧붙인다. */
  private static LogContext.Builder valid() {
    return LogContext.builder()
        .flowVersion("v1")
        .baseTableName("MULE_BIZ_INTERFACE_LOG")
        .triggerType(API)
        .actor("SFDC")
        .sourceAppName("biz-com-exp-listener")
        .targetAppName("biz-com-exp-listener")
        .status(SUCCESS)
        .correlationId("corr-0001")
        .startTime(FIXED);
  }

  @Test
  public void retainsAllValues() {
    Object payload = "REQUEST-BODY";
    Object attributes = new Object();

    LogContext ctx = valid()
        .originPayload(payload)
        .originAttributes(attributes)
        .build();

    assertThat(ctx.getFlowVersion(), is("v1"));
    assertThat(ctx.getBaseTableName(), is("MULE_BIZ_INTERFACE_LOG"));
    assertThat(ctx.getTriggerType(), is(API));
    assertThat(ctx.getActor(), is("SFDC"));
    assertThat(ctx.getSourceAppName(), is("biz-com-exp-listener"));
    assertThat(ctx.getTargetAppName(), is("biz-com-exp-listener"));
    assertThat(ctx.getStatus(), is(SUCCESS));
    assertThat(ctx.getCorrelationId(), is("corr-0001"));
    assertThat(ctx.getStartTime(), is(FIXED));
    assertThat(ctx.getOriginPayload(), is(sameInstance(payload)));
    assertThat(ctx.getOriginAttributes(), is(sameInstance(attributes)));
  }

  /** {@code startTime} 을 지정하지 않으면 빌드 시점의 현재 시각으로 채워진다. */
  @Test
  public void stampsCurrentTimeWhenStartTimeOmitted() {
    LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
    LogContext ctx = LogContext.builder()
        .flowVersion("v1")
        .baseTableName("MULE_BIZ_INTERFACE_LOG")
        .triggerType(API)
        .actor("SFDC")
        .targetAppName("biz-com-exp-listener")
        .status(SUCCESS)
        .build();
    LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);

    assertThat(ctx.getStartTime(), is(notNullValue()));
    assertFalse("startTime 이 빌드 이전이면 안 된다", ctx.getStartTime().isBefore(before));
    assertFalse("startTime 이 빌드 이후이면 안 된다", ctx.getStartTime().isAfter(after));
  }

  /** 명시한 {@code startTime} 은 덮어써지지 않는다. */
  @Test
  public void keepsExplicitStartTime() {
    assertThat(valid().build().getStartTime(), is(FIXED));
  }

  /**
   * 자동 스탬핑은 <b>UTC 기준</b>이어야 한다. 시스템 로컬 타임존을 쓰면 여러 환경 / 리전의
   * 로그를 한 테이블에서 비교할 수 없다.
   *
   * <p>{@link LocalDateTime} 은 오프셋을 담지 않으므로 값을 UTC 기준 시각 범위와 비교하는
   * 것 말고는 확인할 방법이 없다. 따라서 이 테스트는 <b>로컬 타임존이 UTC 가 아닌
   * 머신에서만</b> 회귀를 잡아낸다 (개발 환경은 KST 라 유효하다).
   */
  @Test
  public void stampsStartTimeInUtc() {
    LocalDateTime utcBefore = LocalDateTime.now(ZoneOffset.UTC);
    LocalDateTime stamped = LogContext.builder()
        .flowVersion("v1")
        .baseTableName("MULE_BIZ_INTERFACE_LOG")
        .triggerType(API)
        .actor("SFDC")
        .targetAppName("biz-com-exp-listener")
        .status(SUCCESS)
        .build()
        .getStartTime();
    LocalDateTime utcAfter = LocalDateTime.now(ZoneOffset.UTC);

    assertFalse("UTC 기준 시각보다 이르면 안 된다", stamped.isBefore(utcBefore));
    assertFalse("UTC 기준 시각보다 늦으면 안 된다", stamped.isAfter(utcAfter));
  }

  /**
   * {@code startTime} 은 {@link LocalDateTime} 객체로, 원본 메시지는 변환 없이 그대로 남긴다.
   * JDBC 가 시각을 TIMESTAMP 로 바인딩하므로 문자열로 바꾸면 타입 정보를 잃는다.
   */
  @Test
  public void toMapKeepsInsertionOrderAndSerializesEnumsAsNames() {
    Object payload = "REQUEST-BODY";

    Map<String, Object> map = LogContext.builder()
        .flowVersion("v2")
        .baseTableName("TB_HIST")
        .triggerType(BATCH)
        .actor("scheduler")
        .sourceAppName("biz-com-exp-listener")
        .targetAppName("SAP")
        .status(FAIL)
        .correlationId("corr-0002")
        .startTime(FIXED)
        .originPayload(payload)
        .build()
        .toMap();

    assertThat(map.keySet(), contains("flowVersion", "baseTableName", "triggerType", "actor",
        "sourceAppName", "targetAppName", "status", "correlationId", "startTime",
        "includeRequestPayload", "includeResponsePayload", "requestPayload",
        "originPayload", "originAttributes"));
    assertThat(map.get("flowVersion"), is("v2"));
    assertThat(map.get("triggerType"), is("BATCH"));
    assertThat(map.get("status"), is("FAIL"));
    assertThat(map.get("correlationId"), is("corr-0002"));
    assertThat(map.get("startTime"), is(FIXED));
    assertThat(map.get("originPayload"), is(sameInstance(payload)));
    assertThat(map.get("originAttributes"), is((Object) null));
  }

  /**
   * {@code flowVersion} 은 스키마 기본값 {@code "v1"} 을 가지므로 보통 비어 있지 않지만,
   * 사용자가 빈 문자열을 명시하면 런타임이 그대로 넘긴다. 그 구멍을
   * {@code BIZ-LOG:INVALID_CONTEXT} 로 막는지 확인한다.
   */
  @Test
  public void rejectsBlankStringsWithInvalidContext() {
    assertRejected(valid().flowVersion(null));
    assertRejected(valid().flowVersion(""));
    assertRejected(valid().flowVersion("   "));
    assertRejected(valid().baseTableName(" "));
    assertRejected(valid().actor(" "));
    assertRejected(valid().targetAppName(" "));
  }

  /**
   * {@code triggerType} / {@code status} 는 DSL 기본값({@code API} / {@code SUCCESS})이
   * 있어 XML 에서는 비지 않지만, 프로그램에서 직접 빌드하는 경로를 위해 null 을 거부한다.
   * 이것이 "필수"를 도메인 레벨에서 보장하는 지점이다.
   */
  @Test
  public void rejectsNullEnumsWithInvalidContext() {
    assertRejected(valid().triggerType(null));
    assertRejected(valid().status(null));
  }

  /**
   * {@code includeRequestPayload} 가 켜지면 {@code originPayload} 와 <b>같은 인스턴스</b>가
   * {@code requestPayload} 에도 담긴다. 값이 빌드 시점에 파생되므로 둘이 어긋날 수 없다.
   */
  @Test
  public void copiesOriginPayloadIntoRequestPayloadWhenIncluded() {
    Object payload = "REQUEST-BODY";

    LogContext ctx = valid()
        .originPayload(payload)
        .includeRequestPayload(true)
        .build();

    assertThat(ctx.isIncludeRequestPayload(), is(true));
    assertThat(ctx.getRequestPayload(), is(sameInstance(payload)));
    assertThat(ctx.getOriginPayload(), is(sameInstance(payload)));
    assertThat(ctx.toMap().get("requestPayload"), is(sameInstance(payload)));
  }

  /** 플래그가 꺼져 있으면 originPayload 가 있어도 requestPayload 는 null 이다. */
  @Test
  public void leavesRequestPayloadNullWhenNotIncluded() {
    LogContext ctx = valid()
        .originPayload("REQUEST-BODY")
        .includeRequestPayload(false)
        .build();

    assertThat(ctx.isIncludeRequestPayload(), is(false));
    assertThat(ctx.getRequestPayload(), is((Object) null));
    assertThat("originPayload 는 플래그와 무관하게 담긴다",
        ctx.getOriginPayload(), is((Object) "REQUEST-BODY"));
  }

  /**
   * DSL 에서는 두 플래그가 필수 속성이지만, 빌더에서 지정하지 않으면 primitive boolean
   * 기본값인 {@code false} 가 된다 — 프로그램에서 직접 빌드하는 경로의 안전한 기본이다.
   */
  @Test
  public void builderLeavesPayloadFlagsOffWhenUnset() {
    LogContext ctx = LogContext.builder()
        .flowVersion("v1")
        .baseTableName("MULE_BIZ_INTERFACE_LOG")
        .triggerType(API)
        .actor("SFDC")
        .targetAppName("biz-com-exp-listener")
        .status(SUCCESS)
        .originPayload("REQUEST-BODY")
        .build();

    assertThat(ctx.isIncludeRequestPayload(), is(false));
    assertThat(ctx.isIncludeResponsePayload(), is(false));
    assertThat(ctx.getRequestPayload(), is((Object) null));
  }

  /** 원본 메시지는 지정하지 않으면 null 이며, 그 자체로 오류는 아니다. */
  @Test
  public void allowsAbsentOriginMessage() {
    LogContext ctx = valid().build();

    assertThat(ctx.getOriginPayload(), is((Object) null));
    assertThat(ctx.getOriginAttributes(), is((Object) null));
  }

  /**
   * {@code correlationId} 와 {@code sourceAppName} 은 검증 대상이 아니다. 둘 다 커넥터가
   * 자동 파생하는 값이고, {@code sourceAppName} 의 DSL 기본값 {@code #[p('app.name')]} 은
   * 프로퍼티가 없는 환경에서 null 이 될 수 있다. 자동 파생 값 때문에 로깅이 실패하면 안 된다.
   */
  @Test
  public void allowsAbsentAutoDerivedValues() {
    LogContext ctx = valid().correlationId(null).sourceAppName(null).build();

    assertThat(ctx.getCorrelationId(), is((Object) null));
    assertThat(ctx.getSourceAppName(), is((Object) null));
    assertThat(ctx.toMap().get("correlationId"), is((Object) null));
    assertThat(ctx.toMap().get("sourceAppName"), is((Object) null));
  }

  /**
   * {@code startTime} 은 값 비교에 참여하지만 {@code originPayload} /
   * {@code originAttributes} 는 제외된다 — 사용자의 임의 객체라 identity 기반
   * {@code equals} 인 경우가 많다.
   */
  @Test
  public void equalityCoversStartTimeButNotOriginMessage() {
    LogContext a = valid().build();
    LogContext b = valid().build();
    LogContext differentStatus = valid().status(FAIL).build();
    LogContext differentVersion = valid().flowVersion("v2").build();
    LogContext differentStartTime = valid().startTime(FIXED.plusNanos(1)).build();
    LogContext differentCorrelationId = valid().correlationId("corr-9999").build();
    LogContext differentSourceApp = valid().sourceAppName("other-app").build();
    LogContext differentFlag = valid().includeRequestPayload(true).build();
    LogContext differentPayload = valid().originPayload(new Object()).build();

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(differentStatus)));
    assertThat(a, is(not(differentVersion)));
    assertThat(a, is(not(differentStartTime)));
    assertThat(a, is(not(differentCorrelationId)));
    assertThat(a, is(not(differentSourceApp)));
    assertThat(a, is(not(differentFlag)));
    assertThat("원본 메시지는 비교 대상이 아니다", a, is(differentPayload));
  }

  /**
   * 이 객체는 로그로 흘러갈 가능성이 높으므로 원본 payload 의 <b>값</b>이 찍혀서는 안 된다.
   * 요청 본문 전체가 로그에 쏟아지면 용량 문제와 민감정보 노출로 이어진다.
   */
  @Test
  public void toStringDoesNotLeakOriginPayloadContent() {
    String secret = "password=hunter2";
    String rendered = valid().originPayload(secret).build().toString();

    assertThat(rendered, not(containsString(secret)));
    assertThat(rendered, containsString("originPayload=<String>"));
    assertThat(rendered, containsString("actor='SFDC'"));
  }

  @Test
  public void toMapIsUnmodifiable() {
    Map<String, Object> map = valid().build().toMap();
    try {
      map.put("injected", "value");
      fail("toMap() 결과는 수정 불가여야 한다");
    } catch (UnsupportedOperationException expected) {
      // 기대한 동작
    }
  }

  private static void assertRejected(LogContext.Builder builder) {
    try {
      builder.build();
      fail("공백 파라미터는 거부되어야 한다");
    } catch (ModuleException e) {
      assertThat(e.getType(), is(LogErrorType.INVALID_CONTEXT));
    }
  }
}
