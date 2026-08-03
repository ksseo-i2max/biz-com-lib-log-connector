package org.mycompany.bizcom.log.param;

/**
 * 플로우를 기동시킨 트리거 종류.
 *
 * <p><b>주의:</b> 아래 상수는 임시안이다. 실제 도메인 값으로 교체할 것.
 * enum 상수를 변경하면 이미 작성된 앱 XML 이 스키마 검증에서 깨지므로
 * 가급적 초기에 확정하는 것이 좋다.
 */
public enum TriggerType {

  /** 외부 이벤트/메시지 수신으로 기동 */
  EVENT,

  /** 스케줄러에 의한 주기 기동 */
  SCHEDULE,

  /** 사용자 수동 기동 */
  MANUAL
}
