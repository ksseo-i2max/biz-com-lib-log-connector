package org.mycompany.bizcom.log.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.mule.sdk.api.exception.ModuleException;
import org.mycompany.bizcom.log.BizComLogConfiguration;
import org.mycompany.bizcom.log.error.LogErrorType;
import org.mycompany.bizcom.log.param.LogContextParameters;
import org.mycompany.bizcom.log.param.LogTargetParameters;
import org.mycompany.bizcom.log.param.Status;
import org.mycompany.bizcom.log.param.TriggerType;

/**
 * 로그 대상 정보 2개와 컨텍스트 파라미터 4개를 합친 로그 컨텍스트.
 *
 * <p>Scope 에서는 메시지의 <b>attributes</b> 로, Operation 에서는 {@code target} 을 통해
 * <b>flow variable</b> 로 실려 나간다. 두 경로 모두 동일한 타입이므로 DataWeave 접근
 * 경로의 모양이 같다.
 *
 * <pre>
 *   Scope     : #[attributes.actor]
 *   Operation : #[vars.ctx.actor]
 * </pre>
 *
 * <p>{@link Serializable} 은 VM connector, persistent object store, 클러스터를
 * 경유할 때 필요하다.
 */
public class LogContext implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String flowVersion;
  private final String baseTableName;
  private final TriggerType triggerType;
  private final String actor;
  private final String targetAppName;
  private final Status status;

  private LogContext(String flowVersion,
                     String baseTableName,
                     TriggerType triggerType,
                     String actor,
                     String targetAppName,
                     Status status) {
    this.flowVersion = flowVersion;
    this.baseTableName = baseTableName;
    this.triggerType = triggerType;
    this.actor = actor;
    this.targetAppName = targetAppName;
    this.status = status;
  }

  /**
   * 정규 팩토리. 아래 두 오버로드가 모두 이 메서드로 위임하므로 검증 규칙이 한 곳에만 있다.
   *
   * <p>Mule 은 required 파라미터의 <i>존재</i>만 보장하므로 빈 문자열은 통과한다.
   * 여기서 공백 여부를 검증해 {@link LogErrorType#INVALID_CONTEXT} 로 승격시킨다.
   *
   * @throws ModuleException {@code BIZ-LOG:INVALID_CONTEXT} — 문자열 파라미터가 공백일 때
   */
  public static LogContext of(String flowVersion,
                              String baseTableName,
                              TriggerType triggerType,
                              String actor,
                              String targetAppName,
                              Status status) {
    requireNonBlank(flowVersion, "flowVersion");
    requireNonBlank(baseTableName, "baseTableName");
    requireNonBlank(actor, "actor");
    requireNonBlank(targetAppName, "targetAppName");

    return new LogContext(flowVersion, baseTableName, triggerType, actor, targetAppName, status);
  }

  /** Operation({@code build-context}) 경로 — 로그 대상 정보를 Configuration 에서 가져온다. */
  public static LogContext of(BizComLogConfiguration config, LogContextParameters params) {
    return of(config.getFlowVersion(),
        config.getBaseTableName(),
        params.getTriggerType(),
        params.getActor(),
        params.getTargetAppName(),
        params.getStatus());
  }

  /**
   * Scope({@code with-context}) 경로 — Scope 는 config 에 바인딩될 수 없으므로
   * 로그 대상 정보를 자체 파라미터로 받는다. 자세한 이유는
   * {@link LogTargetParameters} 참고.
   */
  public static LogContext of(LogTargetParameters target, LogContextParameters params) {
    return of(target.getFlowVersion(),
        target.getBaseTableName(),
        params.getTriggerType(),
        params.getActor(),
        params.getTargetAppName(),
        params.getStatus());
  }

  private static void requireNonBlank(String value, String name) {
    if (value == null || value.trim().isEmpty()) {
      throw new ModuleException(
          "로그 컨텍스트 파라미터 '" + name + "' 는 비어 있을 수 없습니다.",
          LogErrorType.INVALID_CONTEXT);
    }
  }

  public String getFlowVersion() {
    return flowVersion;
  }

  public String getBaseTableName() {
    return baseTableName;
  }

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

  /**
   * DB insert 파라미터 등으로 바로 넘기기 좋은 형태로 변환한다.
   * 삽입 순서가 유지되도록 {@link LinkedHashMap} 을 사용한다.
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("flowVersion", flowVersion);
    map.put("baseTableName", baseTableName);
    map.put("triggerType", triggerType == null ? null : triggerType.name());
    map.put("actor", actor);
    map.put("targetAppName", targetAppName);
    map.put("status", status == null ? null : status.name());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LogContext)) {
      return false;
    }
    LogContext that = (LogContext) o;
    return Objects.equals(flowVersion, that.flowVersion)
        && Objects.equals(baseTableName, that.baseTableName)
        && triggerType == that.triggerType
        && Objects.equals(actor, that.actor)
        && Objects.equals(targetAppName, that.targetAppName)
        && status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(flowVersion, baseTableName, triggerType, actor, targetAppName, status);
  }

  @Override
  public String toString() {
    return "LogContext{"
        + "flowVersion='" + flowVersion + '\''
        + ", baseTableName='" + baseTableName + '\''
        + ", triggerType=" + triggerType
        + ", actor='" + actor + '\''
        + ", targetAppName='" + targetAppName + '\''
        + ", status=" + status
        + '}';
  }
}
