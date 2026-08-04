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
        .baseTableName("TB_IF_LOG")
        .triggerType(API)
        .actor("batch-user")
        .targetAppName("SFDC")
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
    assertThat(ctx.getBaseTableName(), is("TB_IF_LOG"));
    assertThat(ctx.getTriggerType(), is(API));
    assertThat(ctx.getActor(), is("batch-user"));
    assertThat(ctx.getTargetAppName(), is("SFDC"));
    assertThat(ctx.getStatus(), is(SUCCESS));
    assertThat(ctx.getCorrelationId(), is("corr-0001"));
    assertThat(ctx.getStartTime(), is(FIXED));
    assertThat(ctx.getOriginPayload(), is(sameInstance(payload)));
    assertThat(ctx.getOriginAttributes(), is(sameInstance(attributes)));
  }

  /** {@code startTime} 을 지정하지 않으면 빌드 시점의 현재 시각으로 채워진다. */
  @Test
  public void stampsCurrentTimeWhenStartTimeOmitted() {
    LocalDateTime before = LocalDateTime.now();
    LogContext ctx = LogContext.builder()
        .flowVersion("v1")
        .baseTableName("TB_IF_LOG")
        .triggerType(API)
        .actor("batch-user")
        .targetAppName("SFDC")
        .status(SUCCESS)
        .build();
    LocalDateTime after = LocalDateTime.now();

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
        .targetAppName("SAP")
        .status(FAIL)
        .correlationId("corr-0002")
        .startTime(FIXED)
        .originPayload(payload)
        .build()
        .toMap();

    assertThat(map.keySet(), contains("flowVersion", "baseTableName", "triggerType", "actor",
        "targetAppName", "status", "correlationId", "startTime", "originPayload",
        "originAttributes"));
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

  /** 원본 메시지는 지정하지 않으면 null 이며, 그 자체로 오류는 아니다. */
  @Test
  public void allowsAbsentOriginMessage() {
    LogContext ctx = valid().build();

    assertThat(ctx.getOriginPayload(), is((Object) null));
    assertThat(ctx.getOriginAttributes(), is((Object) null));
  }

  /**
   * {@code correlationId} 는 검증 대상이 아니다. 런타임이 항상 값을 주지만, 이벤트 없이
   * 컨텍스트를 만드는 경우까지 실패로 막을 이유는 없다.
   */
  @Test
  public void allowsAbsentCorrelationId() {
    LogContext ctx = valid().correlationId(null).build();

    assertThat(ctx.getCorrelationId(), is((Object) null));
    assertThat(ctx.toMap().get("correlationId"), is((Object) null));
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
    LogContext differentPayload = valid().originPayload(new Object()).build();

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(differentStatus)));
    assertThat(a, is(not(differentVersion)));
    assertThat(a, is(not(differentStartTime)));
    assertThat(a, is(not(differentCorrelationId)));
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
    assertThat(rendered, containsString("actor='batch-user'"));
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
