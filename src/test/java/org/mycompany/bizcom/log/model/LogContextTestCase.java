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
 * <p>시각이 걸린 단정은 빌더로 {@code eventTime} / {@code startTime} 을 명시해 결정적으로
 * 만든다. 자동 스탬핑은 별도 테스트에서 범위로만 검증한다.
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
        .eventTime(FIXED)
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
    assertThat(ctx.getEventTime(), is(FIXED));
    assertThat(ctx.getStartTime(), is(FIXED));
    assertThat(ctx.getOriginPayload(), is(sameInstance(payload)));
    assertThat(ctx.getOriginAttributes(), is(sameInstance(attributes)));
  }

  /**
   * 두 시각을 지정하지 않으면 현재 시각으로 채워지고, {@code now()} 를 한 번만 호출하므로
   * 둘이 <b>정확히 같아야</b> 한다. 두 번 호출하면 미세하게 어긋나 버그처럼 보인다.
   */
  @Test
  public void stampsBothTimesFromASingleNowCall() {
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

    assertThat(ctx.getEventTime(), is(notNullValue()));
    assertThat(ctx.getStartTime(), is(ctx.getEventTime()));
    assertFalse("eventTime 이 호출 이전이면 안 된다", ctx.getEventTime().isBefore(before));
    assertFalse("eventTime 이 호출 이후이면 안 된다", ctx.getEventTime().isAfter(after));
  }

  /** 한쪽만 지정하면 나머지 한쪽만 현재 시각으로 채워진다. */
  @Test
  public void fillsOnlyTheMissingTime() {
    LogContext ctx = LogContext.builder()
        .flowVersion("v1")
        .baseTableName("TB_IF_LOG")
        .triggerType(API)
        .actor("batch-user")
        .targetAppName("SFDC")
        .status(SUCCESS)
        .startTime(FIXED)
        .build();

    assertThat(ctx.getStartTime(), is(FIXED));
    assertThat(ctx.getEventTime(), is(notNullValue()));
    assertThat(ctx.getEventTime(), is(not(FIXED)));
  }

  /**
   * 시각은 {@link LocalDateTime} 객체로, 원본 메시지는 변환 없이 그대로 남긴다.
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
        .eventTime(FIXED)
        .startTime(FIXED)
        .originPayload(payload)
        .build()
        .toMap();

    assertThat(map.keySet(), contains("flowVersion", "baseTableName", "triggerType", "actor",
        "targetAppName", "status", "eventTime", "startTime", "originPayload", "originAttributes"));
    assertThat(map.get("flowVersion"), is("v2"));
    assertThat(map.get("triggerType"), is("BATCH"));
    assertThat(map.get("status"), is("FAIL"));
    assertThat(map.get("eventTime"), is(FIXED));
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

  /** enum 파라미터는 null 이어도 컨텍스트 생성 자체는 막지 않는다 (스키마가 이미 필수 강제). */
  @Test
  public void allowsNullEnumsAndRendersThemAsNullInMap() {
    LogContext ctx = valid().triggerType(null).status(null).build();

    assertThat(ctx.getTriggerType(), is((Object) null));
    assertThat(ctx.toMap().get("triggerType"), is((Object) null));
    assertThat(ctx.toMap().get("status"), is((Object) null));
  }

  /** 원본 메시지는 지정하지 않으면 null 이며, 그 자체로 오류는 아니다. */
  @Test
  public void allowsAbsentOriginMessage() {
    LogContext ctx = valid().build();

    assertThat(ctx.getOriginPayload(), is((Object) null));
    assertThat(ctx.getOriginAttributes(), is((Object) null));
  }

  /**
   * 두 시각은 값 비교에 참여하지만 {@code originPayload} / {@code originAttributes} 는
   * 제외된다 — 사용자의 임의 객체라 identity 기반 {@code equals} 인 경우가 많다.
   */
  @Test
  public void equalityCoversTimesButNotOriginMessage() {
    LogContext a = valid().build();
    LogContext b = valid().build();
    LogContext differentStatus = valid().status(FAIL).build();
    LogContext differentVersion = valid().flowVersion("v2").build();
    LogContext differentEventTime = valid().eventTime(FIXED.plusNanos(1)).build();
    LogContext differentStartTime = valid().startTime(FIXED.plusSeconds(1)).build();
    LogContext differentPayload = valid().originPayload(new Object()).build();

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(differentStatus)));
    assertThat(a, is(not(differentVersion)));
    assertThat(a, is(not(differentEventTime)));
    assertThat(a, is(not(differentStartTime)));
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
