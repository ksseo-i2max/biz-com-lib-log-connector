package org.mycompany.bizcom.log;

import static org.mule.sdk.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.sdk.api.annotation.error.Throws;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.MediaType;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Summary;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.parameter.CorrelationInfo;
import org.mule.sdk.api.runtime.process.CompletionCallback;
import org.mule.sdk.api.runtime.route.Chain;
import org.mule.sdk.api.runtime.streaming.StreamingHelper;
import org.mycompany.bizcom.log.error.LogErrorProvider;
import org.mycompany.bizcom.log.model.LogContext;
import org.mycompany.bizcom.log.param.LogContextParameters;
import org.mycompany.bizcom.log.param.LogTargetParameters;

/**
 * 커넥터가 제공하는 컴포넌트 2종.
 *
 * <p>모든 타입은 {@code org.mule.sdk.api.*} 로 통일했다. 하나의 메서드 시그니처 안에서
 * {@code sdk-api} 와 구 {@code extensions-api} 타입을 섞으면 SDK 가 extension model
 * 생성 단계에서 거부한다.
 *
 * <p><b>주의 — Operation 메서드 인자에 {@code @Parameter} 를 붙이지 말 것.</b>
 * {@code sdk-api} 의 {@code @Parameter} 는 {@code @Target({FIELD})} 로 필드 전용이다
 * (구 {@code extensions-api} 와 달라진 부분). Operation 메서드 인자는 암시적으로
 * 파라미터로 인식되므로 애노테이션이 필요 없다.
 */
public class BizComLogOperations {

  private static final String TARGET_GROUP = "Log Target";
  private static final String CONTEXT_GROUP = "Context";

  /** {@code #[attributes]} — {@code Optional.PAYLOAD} 에 대응하는 attributes 쪽 기본값. */
  private static final String CURRENT_ATTRIBUTES = "#[attributes]";

  /**
   * <b>Scope</b> — 감싼 하위 컴포넌트 체인을 실행하면서 로그 컨텍스트를 메시지
   * attributes 로 주입한다.
   *
   * <pre>{@code
   * <biz-log:logging-context flowVersion="v1" baseTableName="TB_IF_LOG"
   *     triggerType="API" actor="batch-user"
   *     targetAppName="SFDC" status="SUCCESS">
   *   <logger message="#[attributes.actor]"/>
   *   <flow-ref name="businessFlow"/>
   * </biz-log:logging-context>
   * }</pre>
   *
   * <p>설계상 세 가지가 중요하다.
   *
   * <p>1. <b>{@code config-ref} 가 없다.</b> Mule SDK 는 Scope 의 config 바인딩을 금지한다
   * ({@code @Config} 를 붙이면 {@code IllegalOperationModelDefinitionException: Scope
   * 'loggingContext' requires a config, but that is not allowed} 로 빌드 실패). 그래서
   * {@code flowVersion} / {@code baseTableName} 을 {@link LogTargetParameters} 로 직접
   * 받는다. 앱마다 한 번만 정하려면 {@code ${...}} property placeholder 를 쓰면 된다.
   *
   * <p>2. {@code payload} 파라미터. {@link Chain} 에 attributes 를 실어 보내려면 payload 도
   * 함께 넘겨야 하는데, 여기에 임의 값을 넣으면 사용자의 원본 payload 가 조용히 사라진다.
   * {@code #[payload]} 를 기본값으로 받아 원본 payload 와 media type 을 그대로 되돌려주어
   * pass-through 를 보장한다. 같은 값이 {@code attributes.originPayload} 로도 실린다.
   * 기본값이 있으므로 사용자가 DSL 에 명시할 필요는 없다.
   *
   * <p>3. 에러 전파. 체인 내부 예외는 감싸지 않고 {@code callback.error(throwable)} 로
   * 그대로 넘긴다. 그래야 원본 에러 타입이 유지되어 사용자의 {@code <error-handler>} 가
   * {@code HTTP:CONNECTIVITY} 등을 정상적으로 잡을 수 있다.
   *
   * <p>스코프의 attributes 는 {@link LogContext} 로 <b>교체</b>되므로, 원래 attributes 는
   * {@code attributes.originAttributes} 에서 꺼내 쓴다.
   *
   * <p><b>주의:</b> 스코프 안에서 메시지를 교체하는 컴포넌트(HTTP Request, DB Select,
   * Transform 등)를 지나면 attributes 는 소실된다. 그런 경우
   * {@code build-context} 를 {@code target} 과 함께 쓰거나, 스코프 첫 줄에
   * {@code <set-variable variableName="ctx" value="#[attributes]"/>} 를 넣는다.
   */
  @DisplayName("Logging Context")
  @Summary("로그 컨텍스트를 attributes 로 주입한 상태로 하위 컴포넌트를 실행한다")
  @MediaType(value = ANY, strict = false)
  @Throws(LogErrorProvider.class)
  public void loggingContext(@ParameterGroup(name = TARGET_GROUP) LogTargetParameters target,
                          @ParameterGroup(name = CONTEXT_GROUP) LogContextParameters params,
                          @Optional(defaultValue = Optional.PAYLOAD)
                          @Summary("체인으로 전달하고 originPayload 로도 기록할 payload."
                              + " 기본값은 현재 payload 다.")
                          TypedValue<Object> payload,
                          @Optional(defaultValue = CURRENT_ATTRIBUTES)
                          @Summary("originAttributes 로 기록할 attributes."
                              + " 기본값은 현재 attributes 다.")
                          TypedValue<Object> originAttributes,
                          CorrelationInfo correlationInfo,
                          StreamingHelper streamingHelper,
                          Chain operations,
                          CompletionCallback<Object, Object> callback) {

    // 스트림 payload 를 체인 실행 후에도 다시 읽을 수 있도록 repeatable provider 로 바꾼다.
    // 원본 커서를 그대로 들고 있으면 체인이 소비한 뒤 읽을 수 없다.
    Object originPayload = streamingHelper.resolveCursorProvider(valueOf(payload));

    LogContext context = LogContext.from(target, params)
        .correlationId(correlationIdOf(correlationInfo))
        .originPayload(originPayload)
        .originAttributes(valueOf(originAttributes))
        .build();

    // 체인에 넘기는 payload 도 위에서 resolve 한 값을 그대로 써서, 체인과 originPayload 가
    // 같은 repeatable provider 를 가리키게 한다.
    Result<Object, Object> input = Result.<Object, Object>builder()
        .output(originPayload)
        .mediaType(mediaTypeOf(payload))
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
   * <p>Operation 은 config 바인딩이 허용되므로 이 경로에서는 {@code flowVersion} /
   * {@code baseTableName} 을 Configuration 에서 가져온다.
   *
   * <pre>{@code
   * <biz-log:build-context config-ref="BizLog_Config"
   *     triggerType="BATCH" actor="scheduler"
   *     targetAppName="SAP" status="FAIL"
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
                                 @ParameterGroup(name = CONTEXT_GROUP) LogContextParameters params,
                                 @Optional(defaultValue = Optional.PAYLOAD)
                                 @Summary("originPayload 로 기록할 payload."
                                     + " 기본값은 현재 payload 다.")
                                 TypedValue<Object> payload,
                                 @Optional(defaultValue = CURRENT_ATTRIBUTES)
                                 @Summary("originAttributes 로 기록할 attributes."
                                     + " 기본값은 현재 attributes 다.")
                                 TypedValue<Object> originAttributes,
                                 CorrelationInfo correlationInfo,
                                 StreamingHelper streamingHelper) {
    return LogContext.from(config, params)
        .correlationId(correlationIdOf(correlationInfo))
        .originPayload(streamingHelper.resolveCursorProvider(valueOf(payload)))
        .originAttributes(valueOf(originAttributes))
        .build();
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

  /**
   * {@code #[attributes]} 는 attributes 가 없는 이벤트에서 null 을 담은 {@link TypedValue}
   * 로 올 수도 있고, 파라미터 자체가 null 로 올 수도 있다. 양쪽을 함께 흡수한다.
   */
  private static Object valueOf(TypedValue<Object> typedValue) {
    return typedValue == null ? null : typedValue.getValue();
  }

  /**
   * 현재 이벤트의 correlation id 를 꺼낸다. 새로 만들지 않고 <b>기존 값을 그대로</b>
   * 쓰므로, 같은 이벤트에서 나온 로그끼리 이 값으로 묶이고 플로우 경계를 넘어도 동일하다.
   */
  private static String correlationIdOf(CorrelationInfo correlationInfo) {
    return correlationInfo == null ? null : correlationInfo.getCorrelationId();
  }

  /** 원본 payload 의 media type 을 보존한다. 알 수 없으면 {@code null} (런타임이 추론). */
  private static org.mule.runtime.api.metadata.MediaType mediaTypeOf(TypedValue<Object> payload) {
    if (payload == null || payload.getDataType() == null) {
      return null;
    }
    return payload.getDataType().getMediaType();
  }
}
