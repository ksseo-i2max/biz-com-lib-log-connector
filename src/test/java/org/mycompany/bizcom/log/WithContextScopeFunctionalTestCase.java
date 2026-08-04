package org.mycompany.bizcom.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@code <biz-log:with-context>} Scope 검증.
 *
 * <p>에러 타입 검증은 테스트 프레임워크의 matcher 대신 XML {@code <error-handler>} 에서
 * 잡은 에러 타입 문자열을 payload 로 되돌려 단정한다. 의존하는 테스트 API 가 줄어들고,
 * 실제 사용자가 겪는 경로(error-handler 통과)를 그대로 검증할 수 있다.
 */
public class WithContextScopeFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "biz-log-scope-test.xml";
  }

  @Test
  public void exposesConfigAndParametersAsAttributes() throws Exception {
    CoreEvent event = flowRunner("scopeExposesAttributes").run();

    assertThat(event.getMessage().getPayload().getValue(),
        is("v1|TB_IF_LOG|API|batch-user|SFDC|SUCCESS"));
  }

  @Test
  public void preservesOriginalPayload() throws Exception {
    CoreEvent event = flowRunner("scopePreservesPayload")
        .withPayload("ORIGINAL-PAYLOAD")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("ORIGINAL-PAYLOAD"));
  }

  /**
   * 스코프 내부 에러는 {@code BIZ-LOG:*} 로 감싸이지 않고 원본 타입으로 전파되어야 한다.
   * 감싸이면 사용자의 {@code <error-handler type="HTTP:CONNECTIVITY">} 등이 동작하지 않는다.
   */
  @Test
  public void propagatesInnerErrorTypeUnwrapped() throws Exception {
    CoreEvent event = flowRunner("scopePropagatesInnerErrorType").run();

    String errorType = (String) event.getMessage().getPayload().getValue();
    assertThat(errorType, not(startsWith("BIZ-LOG:")));
    assertThat(errorType, is("MULE:EXPRESSION"));
  }

  /** 스코프 진입 전 payload 가 {@code attributes.originPayload} 로 실려야 한다. */
  @Test
  public void exposesOriginPayload() throws Exception {
    CoreEvent event = flowRunner("scopeExposesOriginMessage")
        .withPayload("INCOMING-BODY")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("INCOMING-BODY"));
  }

  /** 체인이 payload 를 교체한 뒤에도 {@code originPayload} 로 원본을 되찾을 수 있어야 한다. */
  @Test
  public void originPayloadSurvivesReplacementInsideChain() throws Exception {
    CoreEvent event = flowRunner("scopeOriginPayloadSurvivesReplacement")
        .withPayload("INCOMING-BODY")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("INCOMING-BODY"));
  }

  /** {@code startTime} 이 커넥터에 의해 자동으로 채워져야 한다. */
  @Test
  public void stampsStartTime() throws Exception {
    CoreEvent event = flowRunner("scopeStampsStartTime").run();

    assertThat(event.getMessage().getPayload().getValue(), is("T"));
  }

  /** {@code flowVersion} 생략 시 스키마 기본값 {@code "v1"} 이 적용되어야 한다. */
  @Test
  public void appliesFlowVersionDefault() throws Exception {
    CoreEvent event = flowRunner("scopeAppliesFlowVersionDefault").run();

    assertThat(event.getMessage().getPayload().getValue(), is("v1"));
  }

  @Test
  public void rejectsBlankParameterWithInvalidContext() throws Exception {
    CoreEvent event = flowRunner("scopeRejectsBlankParameter").run();

    assertThat(event.getMessage().getPayload().getValue(), is("BIZ-LOG:INVALID_CONTEXT"));
  }
}
