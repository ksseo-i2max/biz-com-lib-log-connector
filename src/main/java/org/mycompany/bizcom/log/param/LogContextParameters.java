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

  /**
   * {@code true} 면 컴포넌트 진입 직전 payload 가 {@code attributes.requestPayload} 로
   * 실린다. {@code false} 면 그 항목이 {@code null} 이다.
   *
   * <p>요청 본문을 로그 컨텍스트에 담는 것은 용량과 민감정보 노출 비용이 있는 선택이므로
   * 기본값 없이 <b>필수</b>로 두어 매번 명시하게 했다. 1.1.0 에서 게이팅 없이 항상 담기던
   * {@code originPayload} 를 제거했으므로, 이 플래그가 요청 본문이 컨텍스트에 남는지를
   * 결정하는 <b>유일한</b> 스위치다.
   */
  @Parameter
  @DisplayName("Include Request Payload")
  @Summary("진입 직전 payload 를 requestPayload 로 기록할지 여부")
  @Example("false")
  private boolean includeRequestPayload;

  /**
   * 응답 payload 기록 여부.
   *
   * <p><b>현재는 플래그만 컨텍스트로 전달되고 {@code responsePayload} 항목은 채워지지
   * 않는다.</b> 응답 payload 는 스코프의 하위 체인이 끝나야 정해지는 값이라, 스코프가
   * 반환하는 attributes 를 교체하는 방식이 필요하다 — {@code endTime} 과 같은 제약이고
   * 그 방식은 아직 채택하지 않았다.
   */
  @Parameter
  @DisplayName("Include Response Payload")
  @Summary("응답 payload 기록 여부. 현재는 플래그만 전달된다")
  @Example("false")
  private boolean includeResponsePayload;

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

  public boolean isIncludeRequestPayload() {
    return includeRequestPayload;
  }

  public boolean isIncludeResponsePayload() {
    return includeResponsePayload;
  }
}
