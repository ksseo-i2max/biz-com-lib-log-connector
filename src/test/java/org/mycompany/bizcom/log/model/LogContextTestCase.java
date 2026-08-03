package org.mycompany.bizcom.log.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mycompany.bizcom.log.param.Status.READY;
import static org.mycompany.bizcom.log.param.Status.RUNNING;
import static org.mycompany.bizcom.log.param.TriggerType.EVENT;
import static org.mycompany.bizcom.log.param.TriggerType.SCHEDULE;

import java.util.Map;

import org.junit.Test;
import org.mule.sdk.api.exception.ModuleException;
import org.mycompany.bizcom.log.error.LogErrorType;

/**
 * {@link LogContext} 단위 테스트.
 *
 * <p>Mule 컨테이너를 띄우지 않으므로 EE 리포지터리 접근 없이 어디서나 실행된다.
 * 컨테이너가 필요한 DSL / 이벤트 전파 검증은 {@code *FunctionalTestCase} 에 있고,
 * 그쪽은 EE 자격증명이 있을 때 {@code -Pfunctional-tests} 로 실행한다.
 */
public class LogContextTestCase {

  @Test
  public void retainsAllSixValues() {
    LogContext ctx = LogContext.of("1.0.0", "TB_IF_LOG", EVENT, "batch-user", "SFDC", READY);

    assertThat(ctx.getFlowVersion(), is("1.0.0"));
    assertThat(ctx.getBaseTableName(), is("TB_IF_LOG"));
    assertThat(ctx.getTriggerType(), is(EVENT));
    assertThat(ctx.getActor(), is("batch-user"));
    assertThat(ctx.getTargetAppName(), is("SFDC"));
    assertThat(ctx.getStatus(), is(READY));
  }

  @Test
  public void toMapKeepsInsertionOrderAndSerializesEnumsAsNames() {
    Map<String, Object> map =
        LogContext.of("2.1.0", "TB_HIST", SCHEDULE, "scheduler", "SAP", RUNNING).toMap();

    assertThat(map.keySet(),
        contains("flowVersion", "baseTableName", "triggerType", "actor", "targetAppName", "status"));
    assertThat(map.get("triggerType"), is("SCHEDULE"));
    assertThat(map.get("status"), is("RUNNING"));
    assertThat(map.get("actor"), is("scheduler"));
  }

  /**
   * Mule 은 required 파라미터의 존재만 보장하고 빈 문자열은 통과시킨다.
   * 그 구멍을 {@code BIZ-LOG:INVALID_CONTEXT} 로 막는지 확인한다.
   */
  @Test
  public void rejectsBlankStringsWithInvalidContext() {
    assertRejected(null, "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("", "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("   ", "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("1.0.0", " ", "batch-user", "SFDC");
    assertRejected("1.0.0", "TB_IF_LOG", " ", "SFDC");
    assertRejected("1.0.0", "TB_IF_LOG", "batch-user", " ");
  }

  /** enum 파라미터는 null 이어도 컨텍스트 생성 자체는 막지 않는다 (스키마가 이미 필수 강제). */
  @Test
  public void allowsNullEnumsAndRendersThemAsNullInMap() {
    LogContext ctx = LogContext.of("1.0.0", "TB_IF_LOG", null, "batch-user", "SFDC", null);

    assertThat(ctx.getTriggerType(), is((Object) null));
    assertThat(ctx.toMap().get("triggerType"), is((Object) null));
    assertThat(ctx.toMap().get("status"), is((Object) null));
  }

  @Test
  public void equalityIsValueBased() {
    LogContext a = LogContext.of("1.0.0", "TB_IF_LOG", EVENT, "batch-user", "SFDC", READY);
    LogContext b = LogContext.of("1.0.0", "TB_IF_LOG", EVENT, "batch-user", "SFDC", READY);
    LogContext c = LogContext.of("1.0.0", "TB_IF_LOG", EVENT, "batch-user", "SFDC", RUNNING);

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(c)));
  }

  @Test
  public void toMapIsUnmodifiable() {
    Map<String, Object> map =
        LogContext.of("1.0.0", "TB_IF_LOG", EVENT, "batch-user", "SFDC", READY).toMap();
    try {
      map.put("injected", "value");
      fail("toMap() 결과는 수정 불가여야 한다");
    } catch (UnsupportedOperationException expected) {
      // 기대한 동작
    }
  }

  private static void assertRejected(String flowVersion,
                                     String baseTableName,
                                     String actor,
                                     String targetAppName) {
    try {
      LogContext.of(flowVersion, baseTableName, EVENT, actor, targetAppName, READY);
      fail("공백 파라미터는 거부되어야 한다");
    } catch (ModuleException e) {
      assertThat(e.getType(), is(LogErrorType.INVALID_CONTEXT));
    }
  }
}
