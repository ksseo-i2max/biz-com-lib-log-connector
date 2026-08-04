package org.mycompany.bizcom.log.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mycompany.bizcom.log.param.Status.FAIL;
import static org.mycompany.bizcom.log.param.Status.SUCCESS;
import static org.mycompany.bizcom.log.param.TriggerType.API;
import static org.mycompany.bizcom.log.param.TriggerType.BATCH;

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
 */
public class LogContextTestCase {

  @Test
  public void retainsAllSixValues() {
    LogContext ctx = LogContext.of("v1", "TB_IF_LOG", API, "batch-user", "SFDC", SUCCESS);

    assertThat(ctx.getFlowVersion(), is("v1"));
    assertThat(ctx.getBaseTableName(), is("TB_IF_LOG"));
    assertThat(ctx.getTriggerType(), is(API));
    assertThat(ctx.getActor(), is("batch-user"));
    assertThat(ctx.getTargetAppName(), is("SFDC"));
    assertThat(ctx.getStatus(), is(SUCCESS));
  }

  @Test
  public void toMapKeepsInsertionOrderAndSerializesEnumsAsNames() {
    Map<String, Object> map =
        LogContext.of("v2", "TB_HIST", BATCH, "scheduler", "SAP", FAIL).toMap();

    assertThat(map.keySet(),
        contains("flowVersion", "baseTableName", "triggerType", "actor", "targetAppName", "status"));
    assertThat(map.get("flowVersion"), is("v2"));
    assertThat(map.get("triggerType"), is("BATCH"));
    assertThat(map.get("status"), is("FAIL"));
    assertThat(map.get("actor"), is("scheduler"));
  }

  /**
   * {@code flowVersion} 은 스키마 기본값 {@code "v1"} 을 가지므로 보통 비어 있지 않지만,
   * 사용자가 빈 문자열을 명시하면 런타임이 그대로 넘긴다. 그 구멍을
   * {@code BIZ-LOG:INVALID_CONTEXT} 로 막는지 확인한다.
   */
  @Test
  public void rejectsBlankStringsWithInvalidContext() {
    assertRejected(null, "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("", "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("   ", "TB_IF_LOG", "batch-user", "SFDC");
    assertRejected("v1", " ", "batch-user", "SFDC");
    assertRejected("v1", "TB_IF_LOG", " ", "SFDC");
    assertRejected("v1", "TB_IF_LOG", "batch-user", " ");
  }

  /** enum 파라미터는 null 이어도 컨텍스트 생성 자체는 막지 않는다 (스키마가 이미 필수 강제). */
  @Test
  public void allowsNullEnumsAndRendersThemAsNullInMap() {
    LogContext ctx = LogContext.of("v1", "TB_IF_LOG", null, "batch-user", "SFDC", null);

    assertThat(ctx.getTriggerType(), is((Object) null));
    assertThat(ctx.toMap().get("triggerType"), is((Object) null));
    assertThat(ctx.toMap().get("status"), is((Object) null));
  }

  @Test
  public void equalityIsValueBased() {
    LogContext a = LogContext.of("v1", "TB_IF_LOG", API, "batch-user", "SFDC", SUCCESS);
    LogContext b = LogContext.of("v1", "TB_IF_LOG", API, "batch-user", "SFDC", SUCCESS);
    LogContext c = LogContext.of("v1", "TB_IF_LOG", API, "batch-user", "SFDC", FAIL);
    LogContext d = LogContext.of("v2", "TB_IF_LOG", API, "batch-user", "SFDC", SUCCESS);

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(c)));
    assertThat(a, is(not(d)));
  }

  @Test
  public void toMapIsUnmodifiable() {
    Map<String, Object> map =
        LogContext.of("v1", "TB_IF_LOG", API, "batch-user", "SFDC", SUCCESS).toMap();
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
      LogContext.of(flowVersion, baseTableName, API, actor, targetAppName, SUCCESS);
      fail("공백 파라미터는 거부되어야 한다");
    } catch (ModuleException e) {
      assertThat(e.getType(), is(LogErrorType.INVALID_CONTEXT));
    }
  }
}
