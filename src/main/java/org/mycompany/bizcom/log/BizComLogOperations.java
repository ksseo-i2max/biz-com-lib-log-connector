package org.mycompany.bizcom.log;

import static org.mule.sdk.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.sdk.api.annotation.error.Throws;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.MediaType;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Summary;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.process.CompletionCallback;
import org.mule.sdk.api.runtime.route.Chain;
import org.mycompany.bizcom.log.error.LogErrorProvider;
import org.mycompany.bizcom.log.model.LogContext;
import org.mycompany.bizcom.log.param.LogContextParameters;

/**
 * 커넥터가 제공하는 컴포넌트 2종.
 *
 * <p>모든 타입은 {@code org.mule.sdk.api.*} 로 통일했다. 하나의 메서드 시그니처 안에서
 * {@code sdk-api} 와 구 {@code extensions-api} 타입을 섞으면 SDK 가 extension model
 * 생성 단계에서 거부한다.
 */
public class BizComLogOperations {

  private static final String CONTEXT_GROUP = "Context";

  /**
   * <b>Scope</b> — 감싼 하위 컴포넌트 체인을 실행하면서 로그 컨텍스트를 메시지
   * attributes 로 주입한다.
   *
   * <pre>{@code
   * <biz-log:with-context config-ref="BizLog_Config"
   *     triggerType="EVENT" actor="batch-user"
   *     targetAppName="SFDC" status="READY">
   *   <logger message="#[attributes.actor]"/>
   *   <flow-ref name="businessFlow"/>
   * </biz-log:with-context>
   * }</pre>
   *
   * <p>설계상 두 가지가 중요하다.
   *
   * <p>1. {@code payload} 파라미터. {@link Chain} 에 attributes 를 실어 보내려면 payload 도
   * 함께 넘겨야 하는데, 여기에 임의 값을 넣으면 사용자의 원본 payload 가 조용히 사라진다.
   * {@code #[payload]} 를 기본값으로 받아 원본 payload 와 media type 을 그대로 되돌려주어
   * pass-through 를 보장한다. 기본값이 있으므로 사용자가 DSL 에 명시할 필요는 없다.
   *
   * <p>2. 에러 전파. 체인 내부 예외는 감싸지 않고 {@code callback.error(throwable)} 로
   * 그대로 넘긴다. 그래야 원본 에러 타입이 유지되어 사용자의 {@code <error-handler>} 가
   * {@code HTTP:CONNECTIVITY} 등을 정상적으로 잡을 수 있다.
   *
   * <p><b>주의:</b> 스코프 안에서 메시지를 교체하는 컴포넌트(HTTP Request, DB Select,
   * Transform 등)를 지나면 attributes 는 소실된다. 그런 경우
   * {@link #buildContext(BizComLogConfiguration, LogContextParameters)} 를
   * {@code target} 과 함께 쓰거나, 스코프 첫 줄에
   * {@code <set-variable variableName="ctx" value="#[attributes]"/>} 를 넣는다.
   */
  @DisplayName("With Context")
  @Summary("로그 컨텍스트를 attributes 로 주입한 상태로 하위 컴포넌트를 실행한다")
  @MediaType(value = ANY, strict = false)
  @Throws(LogErrorProvider.class)
  public void withContext(@Config BizComLogConfiguration config,
                          @ParameterGroup(name = CONTEXT_GROUP) LogContextParameters params,
                          @Parameter @Optional(defaultValue = Optional.PAYLOAD)
                          @Summary("체인으로 그대로 전달할 payload. 기본값은 현재 payload 다.")
                          TypedValue<Object> payload,
                          Chain operations,
                          CompletionCallback<Object, Object> callback) {

    LogContext context = LogContext.of(config, params);

    Result<Object, Object> input = Result.<Object, Object>builder()
        .output(payload.getValue())
        .mediaType(payload.getDataType().getMediaType())
        .attributes(context)
        .build();

    operations.process(
        input,
        result -> callback.success(asObjectResult(result)),
        (throwable, result) -> callback.error(throwable));
  }

  /**
   * <b>Operation</b> — 로그 컨텍스트를 만들어 반환한다. {@code target} 과 함께 쓰면
   * 진짜 flow variable 이 되어 메시지가 교체되어도 살아남는다.
   *
   * <pre>{@code
   * <biz-log:build-context config-ref="BizLog_Config"
   *     triggerType="SCHEDULE" actor="scheduler"
   *     targetAppName="SAP" status="RUNNING"
   *     target="ctx"/>
   *
   * <http:request .../>                    <!-- 메시지 교체됨 -->
   * <logger message="#[vars.ctx.actor]"/>  <!-- 여전히 유효 -->
   * }</pre>
   *
   * <p>{@code @MediaType} 을 붙이지 않은 것은 의도된 것이다. POJO 를 반환하면 SDK 가
   * {@code application/java} 로 추론한다. ({@code sdk-api} 의 {@code MediaType} 에는
   * {@code APPLICATION_JAVA} 상수가 없다. 해당 애노테이션은 {@code String} /
   * {@code InputStream} 을 반환할 때만 필수다.)
   */
  @DisplayName("Build Context")
  @Summary("로그 컨텍스트 객체를 생성한다. target 과 함께 쓰면 flow variable 로 저장된다")
  @Throws(LogErrorProvider.class)
  public LogContext buildContext(@Config BizComLogConfiguration config,
                                 @ParameterGroup(name = CONTEXT_GROUP) LogContextParameters params) {
    return LogContext.of(config, params);
  }

  /**
   * {@link Chain#process} 의 콜백은 raw {@code Result} 를 넘겨주므로
   * {@link CompletionCallback#success} 가 요구하는 {@code Result<Object, Object>} 로
   * 좁혀 준다. 런타임이 넣어주는 값은 항상 {@code Object} 로 안전하게 볼 수 있다.
   */
  @SuppressWarnings("unchecked")
  private static Result<Object, Object> asObjectResult(Result<?, ?> result) {
    return (Result<Object, Object>) result;
  }
}
