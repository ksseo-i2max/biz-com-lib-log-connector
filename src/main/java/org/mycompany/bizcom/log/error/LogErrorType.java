package org.mycompany.bizcom.log.error;

import org.mule.sdk.api.error.ErrorTypeDefinition;

/**
 * 커넥터가 노출하는 에러 타입. XML 상에서는 {@code BIZ-LOG:INVALID_CONTEXT},
 * {@code BIZ-LOG:EXECUTION} 으로 참조된다.
 *
 * <p>Scope <b>내부</b>에서 발생한 예외는 여기에 포함되지 않는다. 그런 예외는
 * 원본 에러 타입({@code HTTP:CONNECTIVITY}, {@code DB:QUERY_EXECUTION} 등)을 그대로
 * 유지한 채 전파되므로 사용자의 {@code <error-handler>} 가 정상적으로 잡을 수 있다.
 */
public enum LogErrorType implements ErrorTypeDefinition<LogErrorType> {

  /** 컨텍스트 파라미터 값이 유효하지 않음 (예: actor 가 공백) */
  INVALID_CONTEXT,

  /** 컨텍스트 구성 자체의 실패 */
  EXECUTION
}
