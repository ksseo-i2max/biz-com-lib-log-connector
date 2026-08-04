package org.mycompany.bizcom.log;

import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Example;
import org.mule.sdk.api.annotation.param.display.Summary;

/**
 * 커넥터 Configuration.
 *
 * <p>{@code baseTableName} 은 {@code @Optional} 이 없으므로 <b>필수(required)</b> 로
 * 스키마에 반영된다. 값이 빠지면 런타임 NPE 가 아니라 <b>앱 기동 시점</b>에 검증
 * 실패로 잡힌다.
 *
 * <p>{@code flowVersion} 은 기본값 {@code "v1"} 을 가지므로 생략할 수 있다. 다만 빈
 * 문자열을 명시하면 {@code BIZ-LOG:INVALID_CONTEXT} 로 거부된다
 * ({@link org.mycompany.bizcom.log.model.LogContext} 참고).
 *
 * <pre>{@code
 * <biz-log:config name="BizLog_Config" baseTableName="TB_IF_LOG"/>
 * <biz-log:config name="BizLog_Config_V2" flowVersion="v2" baseTableName="TB_IF_LOG"/>
 * }</pre>
 */
@Configuration(name = "config")
@Operations(BizComLogOperations.class)
public class BizComLogConfiguration {

  @Parameter
  @Optional(defaultValue = "v1")
  @DisplayName("Flow Version")
  @Summary("로그 스키마 / 플로우 버전 식별자")
  @Example("v1")
  private String flowVersion;

  @Parameter
  @DisplayName("Base Table Name")
  @Summary("로그가 기록될 기준 테이블명")
  @Example("TB_IF_LOG")
  private String baseTableName;

  public String getFlowVersion() {
    return flowVersion;
  }

  public String getBaseTableName() {
    return baseTableName;
  }
}
