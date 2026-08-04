package org.mycompany.bizcom.log.param;

import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Example;
import org.mule.sdk.api.annotation.param.display.Summary;

/**
 * Scope 와 Operation 이 공유하는 컨텍스트 파라미터 묶음.
 *
 * <p>{@code @ParameterGroup} 으로 주입되므로 4개 파라미터 정의가 두 컴포넌트에서
 * 중복되지 않는다. Studio 에서는 "Context" 그룹으로 묶여 렌더링되고,
 * enum 타입인 {@code triggerType} / {@code status} 는 드롭다운으로 표시된다.
 *
 * <p>네 파라미터 모두 {@code @Optional} 이 없으므로 필수다.
 */
public class LogContextParameters {

  @Parameter
  @DisplayName("Trigger Type")
  @Summary("플로우를 기동시킨 트리거 종류")
  private TriggerType triggerType;

  @Parameter
  @DisplayName("Actor")
  @Summary("작업 주체 (사용자 ID 또는 시스템 계정)")
  @Example("batch-user")
  private String actor;

  @Parameter
  @DisplayName("Target App Name")
  @Summary("연동 대상 애플리케이션 명")
  @Example("SFDC")
  private String targetAppName;

  @Parameter
  @DisplayName("Status")
  @Summary("처리 결과 (SUCCESS / FAIL)")
  private Status status;

  public TriggerType getTriggerType() {
    return triggerType;
  }

  public String getActor() {
    return actor;
  }

  public String getTargetAppName() {
    return targetAppName;
  }

  public Status getStatus() {
    return status;
  }
}
