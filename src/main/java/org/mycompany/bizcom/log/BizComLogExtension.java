package org.mycompany.bizcom.log;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.error.ErrorTypes;
import org.mycompany.bizcom.log.error.LogErrorType;
import org.mycompany.bizcom.log.model.LogContext;
import org.mycompany.bizcom.log.param.Status;
import org.mycompany.bizcom.log.param.TriggerType;

/**
 * 커넥터 진입점.
 *
 * <p>XML namespace 는 {@code http://www.mulesoft.org/schema/mule/biz-log} 이며
 * DSL 상에서는 {@code <biz-log:...>} 로 노출된다.
 *
 * <p>{@code @JavaVersionSupport} 를 생략하면 생성되는 {@code mule-artifact.json} 의
 * {@code javaSpecificationVersions} 가 하위 버전으로 떨어져 JDK 17 기반 Runtime 4.9 에서
 * 배포 경고 또는 거부가 발생할 수 있으므로 명시한다. JDK 21 도 지원하려면
 * {@code {JAVA_17, JAVA_21}} 로 확장하면 된다.
 *
 * <p>{@code @Export} 는 {@link LogContext} 와 두 enum 을 앱 클래스로더에 노출한다.
 * 이것이 없으면 컨텍스트를 {@code ee:transform} 에서 다룰 때, 또는 VM connector /
 * persistent object store 를 경유할 때 {@code ClassNotFoundException} 이 발생한다.
 *
 * <p><b>Configuration 이 없다.</b> 1.2.0 에서 {@code build-context} Operation 을 제거하면서
 * 유일한 소비자를 잃은 {@code BizComLogConfiguration} 도 함께 삭제했다. 남은 컴포넌트인
 * {@code logging-context} 는 Scope 라 SDK 가 config 바인딩을 금지하므로, config 를 두면
 * DSL 에 쓰이지 않는 {@code <biz-log:config>} 요소만 남는다. {@code @Operations} 를
 * extension 에 직접 달면 SDK 가 암시적 기본 configuration 을 만들어 주므로
 * {@code config-ref} 없이 동작한다.
 */
@Extension(name = "Biz Com Log")
@Xml(prefix = "biz-log")
@JavaVersionSupport({JAVA_17})
@Operations(BizComLogOperations.class)
@ErrorTypes(LogErrorType.class)
@Export(classes = {LogContext.class, TriggerType.class, Status.class})
public class BizComLogExtension {
}
