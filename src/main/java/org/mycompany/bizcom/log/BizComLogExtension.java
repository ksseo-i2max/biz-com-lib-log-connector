package org.mycompany.bizcom.log;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.JavaVersionSupport;
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
 * 이것이 없으면 {@code vars.ctx} 를 {@code ee:transform} 에서 다룰 때, 또는 VM connector /
 * persistent object store 를 경유할 때 {@code ClassNotFoundException} 이 발생한다.
 */
@Extension(name = "Biz Com Log")
@Xml(prefix = "biz-log")
@JavaVersionSupport({JAVA_17})
@Configurations(BizComLogConfiguration.class)
@ErrorTypes(LogErrorType.class)
@Export(classes = {LogContext.class, TriggerType.class, Status.class})
public class BizComLogExtension {
}
