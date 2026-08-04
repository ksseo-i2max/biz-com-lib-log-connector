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
 * <p>두 파라미터 모두 기본값이 있으므로 이름만 주고 쓸 수 있다. 다만 빈 문자열을
 * 명시하면 {@code BIZ-LOG:INVALID_CONTEXT} 로 거부된다
 * ({@link org.mycompany.bizcom.log.model.LogContext} 참고).
 *
 * <pre>{@code
 * <biz-log:config name="BizLog_Config"/>
 * <biz-log:config name="BizLog_Config_V2" flowVersion="v2" baseTableName="TB_IF_LOG_HIST"/>
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
