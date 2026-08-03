package org.mycompany.bizcom.log.param;

/**
 * 로그 시점의 처리 상태.
 *
 * <p><b>주의:</b> 아래 상수는 임시안이다. 실제 도메인 값으로 교체할 것.
 * enum 상수를 변경하면 이미 작성된 앱 XML 이 스키마 검증에서 깨지므로
 * 가급적 초기에 확정하는 것이 좋다.
 */
public enum Status {

  /** 처리 대기 */
  READY,

  /** 처리 중 */
  RUNNING,

  /** 정상 완료 */
  DONE,

  /** 실패 */
  FAILED
}
