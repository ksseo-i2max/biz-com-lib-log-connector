package org.mycompany.bizcom.log.param;

/**
 * 처리 결과.
 *
 * <p>enum 상수는 앱 XML 의 스키마 검증 대상이다. 상수를 변경하거나 제거하면 이미 배포된
 * 앱의 {@code status="..."} 값이 기동 시점에 깨지므로 신중히 다룰 것.
 */
public enum Status {

  /** 정상 처리 */
  SUCCESS,

  /** 실패 */
  FAIL
}
