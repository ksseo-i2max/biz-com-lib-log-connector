package org.mycompany.bizcom.log.param;

import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Example;
import org.mule.sdk.api.annotation.param.display.Summary;

/**
 * Scope 전용 로그 대상 파라미터 ({@code flowVersion} / {@code baseTableName}).
 *
 * <p><b>왜 Configuration 이 아니라 Scope 파라미터인가:</b> Mule SDK 는 Scope 가
 * configuration 에 바인딩되는 것을 금지한다. Scope 메서드에 {@code @Config} 를 붙이면
 * extension model 생성 단계에서 아래 오류로 빌드가 실패한다.
 *
 * <pre>
 * IllegalOperationModelDefinitionException:
 *   Scope 'loggingContext' requires a config, but that is not allowed, remove such parameter
 * </pre>
 *
 * <p>따라서 Scope 는 {@link org.mycompany.bizcom.log.BizComLogConfiguration} 의 값을
 * 읽을 수 없고, 두 값을 자체 파라미터로 받는다. {@code flowVersion} 은 Configuration 과
 * 동일하게 기본값 {@code "v1"} 을 가지므로 생략할 수 있다. 앱마다 다른 값을 한 번만
 * 정하고 싶다면 Mule 의 property placeholder 를 쓰면 된다.
 *
 * <pre>{@code
 * <biz-log:logging-context baseTableName="TB_IF_LOG"
 *                       triggerType="API" ... >
 *
 * <biz-log:logging-context flowVersion="${biz.log.flowVersion}"
 *                       baseTableName="${biz.log.baseTableName}"
 *                       triggerType="BATCH" ... >
 * }</pre>
 *
 * <p>Configuration 은 {@code <biz-log:build-context>} Operation 경로에서 그대로 쓰인다.
 * (Operation 은 config 바인딩이 허용된다.)
 */
public class LogTargetParameters {

  @Parameter
  @Optional(defaultValue = "v1")
  @DisplayName("Flow Version")
  @Summary("로그 스키마 / 플로우 버전 식별자")
  @Example("v1")
  private String flowVersion;

  @Parameter
  @Optional(defaultValue = "MULE_BIZ_INTERFACE_LOG")
  @DisplayName("Base Table Name")
  @Summary("로그가 기록될 기준 테이블명 (기본값 MULE_BIZ_INTERFACE_LOG)")
  @Example("MULE_BIZ_INTERFACE_LOG")
  private String baseTableName;

  public String getFlowVersion() {
    return flowVersion;
  }

  public String getBaseTableName() {
    return baseTableName;
  }
}
