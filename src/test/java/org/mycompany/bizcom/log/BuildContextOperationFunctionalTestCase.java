package org.mycompany.bizcom.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@code <biz-log:build-context>} Operation 검증.
 *
 * <p>Scope 의 attributes 방식과 달리 {@code target} 으로 저장된 flow variable 은
 * 메시지가 교체되어도 살아남아야 한다 — 하이브리드 설계의 존재 이유다.
 */
public class BuildContextOperationFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "biz-log-operation-test.xml";
  }

  @Test
  public void storesContextInTargetVariable() throws Exception {
    CoreEvent event = flowRunner("operationStoresContextInVariable").run();

    assertThat(event.getMessage().getPayload().getValue(),
        is("v2|TB_IF_LOG_HIST|BATCH|scheduler|SAP|FAIL"));
  }

  @Test
  public void variableSurvivesMessageReplacement() throws Exception {
    CoreEvent event = flowRunner("variableSurvivesMessageReplacement").run();

    assertThat(event.getMessage().getPayload().getValue(), is("integration-user|SUCCESS"));
  }

  /** 컴포넌트 진입 전 payload 가 {@code vars.ctx.originPayload} 로 실려야 한다. */
  @Test
  public void exposesOriginPayload() throws Exception {
    CoreEvent event = flowRunner("operationExposesOriginMessage")
        .withPayload("INCOMING-BODY")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("INCOMING-BODY"));
  }

  /** {@code startTime} 이 커넥터에 의해 자동으로 채워져야 한다. */
  @Test
  public void stampsStartTime() throws Exception {
    CoreEvent event = flowRunner("operationStampsStartTime").run();

    assertThat(event.getMessage().getPayload().getValue(), is("T"));
  }

  /** Configuration 에서 {@code flowVersion} 생략 시 기본값 {@code "v1"} 이 적용되어야 한다. */
  @Test
  public void appliesFlowVersionDefault() throws Exception {
    CoreEvent event = flowRunner("configAppliesFlowVersionDefault").run();

    assertThat(event.getMessage().getPayload().getValue(), is("v1"));
  }

  @Test
  public void withoutTargetContextBecomesPayload() throws Exception {
    CoreEvent event = flowRunner("operationWithoutTargetReturnsPayload").run();

    assertThat(event.getMessage().getPayload().getValue(), is("ops|ADMIN"));
  }
}
