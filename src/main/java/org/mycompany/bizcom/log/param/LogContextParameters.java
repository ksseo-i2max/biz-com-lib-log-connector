package org.mycompany.bizcom.log.param;

import org.mule.sdk.api.annotation.param.Optional;
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
 * <p><b>필수 여부에 대해.</b> Mule SDK 에서 "필수"와 "기본값"은 동시에 성립하지 않는다.
 * {@code @Optional(defaultValue = ...)} 을 붙이면 스키마상 required 가 아니게 되어
 * XML 에서 생략할 수 있다. 그래서 두 층으로 나눴다.
 *
 * <ul>
 *   <li>{@code targetAppName} — {@code @Optional} 없음. 스키마 레벨 필수. 연동 대상은
 *       사용처마다 달라서 기본값을 정할 수 없다.</li>
 *   <li>{@code triggerType} / {@code actor} / {@code status} — 기본값 {@code API} /
 *       {@code SFDC} / {@code SUCCESS}. XML 에서 생략 가능하지만 값이 비는 일은 없고,
 *       {@link org.mycompany.bizcom.log.model.LogContext} 빌더가 null / 공백을
 *       {@code BIZ-LOG:INVALID_CONTEXT} 로 거부하므로 도메인 레벨에서는 필수다.</li>
 * </ul>
 */
public class LogContextParameters {

  @Parameter
  @Optional(defaultValue = "API")
  @DisplayName("Trigger Type")
  @Summary("플로우를 기동시킨 트리거 종류 (기본값 API)")
  private TriggerType triggerType;

  @Parameter
  @Optional(defaultValue = "SFDC")
  @DisplayName("Actor")
  @Summary("작업 주체 (사용자 ID 또는 시스템 계정, 기본값 SFDC)")
  @Example("SFDC")
  private String actor;

  @Parameter
  @DisplayName("Target App Name")
  @Summary("연동 대상 애플리케이션 명")
  @Example("SFDC")
  private String targetAppName;

  @Parameter
  @Optional(defaultValue = "SUCCESS")
  @DisplayName("Status")
  @Summary("처리 결과 SUCCESS / FAIL (기본값 SUCCESS)")
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
