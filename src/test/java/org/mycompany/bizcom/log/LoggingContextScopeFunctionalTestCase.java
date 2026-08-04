package org.mycompany.bizcom.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@code <biz-log:logging-context>} Scope 검증.
 *
 * <p>에러 타입 검증은 테스트 프레임워크의 matcher 대신 XML {@code <error-handler>} 에서
 * 잡은 에러 타입 문자열을 payload 로 되돌려 단정한다. 의존하는 테스트 API 가 줄어들고,
 * 실제 사용자가 겪는 경로(error-handler 통과)를 그대로 검증할 수 있다.
 */
public class LoggingContextScopeFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "biz-log-scope-test.xml";
  }

  @Test
  public void exposesConfigAndParametersAsAttributes() throws Exception {
    CoreEvent event = flowRunner("scopeExposesAttributes").run();

    assertThat(event.getMessage().getPayload().getValue(),
        is("v1|MULE_BIZ_INTERFACE_LOG|API|SFDC|SFDC|SUCCESS"));
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

  /**
   * {@code correlationId} 는 새로 만들지 않고 <b>현재 이벤트의 값을 그대로</b> 담아야 한다.
   * 커넥터가 자체 id 를 생성하면 로그를 이벤트 단위로 묶을 수 없다.
   */
  @Test
  public void reusesEventCorrelationId() throws Exception {
    CoreEvent event = flowRunner("scopeReusesEventCorrelationId").run();

    assertThat(event.getMessage().getPayload().getValue(), is("SAME"));
  }

  /** {@code startTime} 이 커넥터에 의해 자동으로 채워져야 한다. */
  @Test
  public void stampsStartTime() throws Exception {
    CoreEvent event = flowRunner("scopeStampsStartTime").run();

    assertThat(event.getMessage().getPayload().getValue(), is("T"));
  }

  /**
   * {@code sourceAppName} 은 DSL 파라미터가 아니라 Java 에서 app.name 프로퍼티로
   * 채워져야 한다. 앱 이름은 환경마다 다르므로 값을 직접 단정하지 않고 플로우 안에서
   * 같은 표현식과 비교한다.
   */
  @Test
  public void derivesSourceAppNameFromAppNameProperty() throws Exception {
    CoreEvent event = flowRunner("scopeDerivesSourceAppNameFromProperty").run();

    assertThat(event.getMessage().getPayload().getValue(), is("SAME"));
  }

  /** {@code includeRequestPayload="true"} 면 진입 직전 payload 가 실려야 한다. */
  @Test
  public void includesRequestPayloadWhenEnabled() throws Exception {
    CoreEvent event = flowRunner("scopeIncludesRequestPayloadWhenEnabled")
        .withPayload("INCOMING-BODY")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("INCOMING-BODY"));
  }

  /**
   * {@code includeRequestPayload="false"} 면 {@code requestPayload} 가 비어야 한다.
   * {@code originPayload} 는 플래그와 무관하게 항상 담긴다.
   */
  @Test
  public void omitsRequestPayloadWhenDisabled() throws Exception {
    CoreEvent event = flowRunner("scopeOmitsRequestPayloadWhenDisabled")
        .withPayload("INCOMING-BODY")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("NULL|INCOMING-BODY"));
  }

  /** 기본값이 있는 파라미터를 생략하면 문서화된 기본값이 적용되어야 한다. */
  @Test
  public void appliesDefaultsWhenAllOmitted() throws Exception {
    CoreEvent event = flowRunner("scopeAppliesDefaults").run();

    assertThat(event.getMessage().getPayload().getValue(),
        is("v1|MULE_BIZ_INTERFACE_LOG|API|SFDC|biz-com-exp-listener|SUCCESS"));
  }

  @Test
  public void rejectsBlankParameterWithInvalidContext() throws Exception {
    CoreEvent event = flowRunner("scopeRejectsBlankParameter").run();

    assertThat(event.getMessage().getPayload().getValue(), is("BIZ-LOG:INVALID_CONTEXT"));
  }
}
