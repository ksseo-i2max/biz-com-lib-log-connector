package org.mycompany.bizcom.log;

import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Example;
import org.mule.sdk.api.annotation.param.display.Summary;

/**
 * 커넥터 Configuration.
 *
 * <p>두 파라미터 모두 {@code @Optional} 을 붙이지 않았으므로 <b>필수(required)</b> 로
 * 스키마에 반영된다. 따라서 값이 빠지면 런타임 NPE 가 아니라 <b>앱 기동 시점</b>에
 * 검증 실패로 잡힌다.
 *
 * <pre>{@code
 * <biz-log:config name="BizLog_Config" flowVersion="1.0.0" baseTableName="TB_IF_LOG"/>
 * }</pre>
 */
@Configuration(name = "config")
@Operations(BizComLogOperations.class)
public class BizComLogConfiguration {

  @Parameter
  @DisplayName("Flow Version")
  @Summary("로그 스키마 / 플로우 버전 식별자")
  @Example("1.0.0")
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
