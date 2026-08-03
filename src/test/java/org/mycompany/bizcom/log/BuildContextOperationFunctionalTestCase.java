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
        is("2.1.0|TB_IF_LOG_HIST|SCHEDULE|scheduler|SAP|RUNNING"));
  }

  @Test
  public void variableSurvivesMessageReplacement() throws Exception {
    CoreEvent event = flowRunner("variableSurvivesMessageReplacement").run();

    assertThat(event.getMessage().getPayload().getValue(), is("integration-user|DONE"));
  }

  @Test
  public void withoutTargetContextBecomesPayload() throws Exception {
    CoreEvent event = flowRunner("operationWithoutTargetReturnsPayload").run();

    assertThat(event.getMessage().getPayload().getValue(), is("ops|ADMIN"));
  }
}
