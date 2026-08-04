package org.mycompany.bizcom.log.param;

/**
 * 플로우를 기동시킨 트리거 종류.
 *
 * <p>enum 상수는 앱 XML 의 스키마 검증 대상이다. 상수를 변경하거나 제거하면 이미 배포된
 * 앱의 {@code triggerType="..."} 값이 기동 시점에 깨지므로 신중히 다룰 것.
 */
public enum TriggerType {

  /** 외부 API 호출로 기동 */
  API,

  /** 배치 / 스케줄러로 기동 */
  BATCH
}
