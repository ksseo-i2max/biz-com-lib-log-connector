package org.mycompany.bizcom.log.param;

import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.display.DisplayName;
import org.mule.sdk.api.annotation.param.display.Example;
import org.mule.sdk.api.annotation.param.display.Summary;

/**
 * Scope 와 Operation 이 공유하는 컨텍스트 파라미터 묶음.
 *
 * <p>{@code @ParameterGroup} 으로 주입되므로 파라미터 정의가 두 컴포넌트에서
 * 중복되지 않는다. Studio 에서는 "Context" 그룹으로 묶여 렌더링되고,
 * enum 타입인 {@code triggerType} / {@code status} 는 드롭다운으로 표시된다.
 *
 * <p><b>필수 여부에 대해.</b> Mule SDK 에서 "필수"와 "기본값"은 동시에 성립하지 않는다.
 * {@code @Optional(defaultValue = ...)} 을 붙이면 스키마상 required 가 아니게 되어
 * XML 에서 생략할 수 있다.
 *
 * <p>모든 파라미터에 기본값이 있으므로 <b>스키마 레벨 필수는 하나도 없다.</b>
 * 대신 {@link org.mycompany.bizcom.log.model.LogContext} 빌더가 null / 공백을
 * {@code BIZ-LOG:INVALID_CONTEXT} 로 거부하므로 값이 비는 일은 없다 — 필수 보장은
 * 도메인 레벨에만 있다. 예외는 {@code sourceAppName} 으로, 자동 파생 값이라
 * 검증하지 않는다.
 *
 * <p>부작용을 알고 쓸 것: 파라미터를 하나도 안 써도 앱이 기동하므로, 오타로 파라미터를
 * 빠뜨렸을 때 기동 시점에 잡히지 않고 기본값이 조용히 기록된다.
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

  /**
   * 기본값이 리터럴이 아니라 <b>표현식</b>이다. {@code p('app.name')} 은 Mule 이 배포된
   * 애플리케이션 이름을 돌려주므로, 커넥터를 쓰는 앱이 자기 이름을 따로 적지 않아도
   * 출발지가 채워진다.
   *
   * <p>단, 이 값은 검증하지 않는다. 프로퍼티가 없는 환경(일부 테스트 하네스 등)에서는
   * {@code p()} 가 null 을 돌려주는데, 자동 파생 값 때문에 로깅이 실패하면 안 된다.
   * {@code correlationId} 와 같은 취급이다.
   */
  @Parameter
  @Optional(defaultValue = "#[p('app.name')]")
  @DisplayName("Source App Name")
  @Summary("호출 출발 애플리케이션 명 (기본값: 현재 Mule 앱 이름 p('app.name'))")
  @Example("biz-com-exp-listener")
  private String sourceAppName;

  @Parameter
  @Optional(defaultValue = "biz-com-exp-listener")
  @DisplayName("Target App Name")
  @Summary("연동 대상 애플리케이션 명 (기본값 biz-com-exp-listener)")
  @Example("biz-com-exp-listener")
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

  public String getSourceAppName() {
    return sourceAppName;
  }

  public String getTargetAppName() {
    return targetAppName;
  }

  public Status getStatus() {
    return status;
  }
}
