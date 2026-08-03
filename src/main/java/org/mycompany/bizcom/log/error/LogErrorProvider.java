package org.mycompany.bizcom.log.error;

import java.util.HashSet;
import java.util.Set;

import org.mule.sdk.api.annotation.error.ErrorTypeProvider;
import org.mule.sdk.api.error.ErrorTypeDefinition;

/**
 * {@code @Throws} 로 지정되어, 각 컴포넌트가 던질 수 있는 에러 타입을
 * extension model 에 등록한다. 이 등록이 있어야 Studio 의 error mapping UI 와
 * {@code <on-error-propagate type="BIZ-LOG:INVALID_CONTEXT">} 같은 참조가 동작한다.
 */
public class LogErrorProvider implements ErrorTypeProvider {

  @SuppressWarnings("rawtypes")
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(LogErrorType.INVALID_CONTEXT);
    errors.add(LogErrorType.EXECUTION);
    return errors;
  }
}
