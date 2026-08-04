package org.mycompany.bizcom.log.model;

import java.io.Serializable;
import java.time.LocalDateTime;
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
 * 로그 대상 정보 2개, 컨텍스트 파라미터 4개, 시각 2개, 원본 메시지 2개를 합친 로그 컨텍스트.
 *
 * <p>Scope 에서는 메시지의 <b>attributes</b> 로, Operation 에서는 {@code target} 을 통해
 * <b>flow variable</b> 로 실려 나간다. 두 경로 모두 동일한 타입이므로 DataWeave 접근
 * 경로의 모양이 같다.
 *
 * <pre>
 *   Scope     : #[attributes.actor], #[attributes.eventTime], #[attributes.originPayload]
 *   Operation : #[vars.ctx.actor],   #[vars.ctx.eventTime],   #[vars.ctx.originPayload]
 * </pre>
 *
 * <p><b>{@code eventTime} 과 {@code startTime}</b> 은 둘 다 컨텍스트 생성 시각으로 채워지며
 * 같은 값이다. 컨텍스트 하나를 만드는 동안 {@link LocalDateTime#now()} 를 한 번만 호출해
 * 양쪽에 넣기 때문에 미세하게 어긋나지 않는다. 두 값을 다르게 두려면 빌더에서 각각
 * 지정하면 된다.
 *
 * <p><b>{@code originPayload} / {@code originAttributes}</b> 는 컴포넌트에 진입하기 <i>전</i>
 * 의 payload / attributes 다. Scope 의 경우 하위 체인이 실행되기 전 값이므로, 체인 안에서
 * 메시지가 어떻게 바뀌든 원본을 계속 참조할 수 있다.
 *
 * <p><b>직렬화 주의.</b> {@link Serializable} 을 선언하고 있고 나머지 필드는 모두
 * 직렬화 가능하지만, {@code originPayload} / {@code originAttributes} 는 사용자의 임의
 * 객체다. 이 컨텍스트를 VM connector / persistent object store / 클러스터로 넘길 계획이면
 * 해당 값이 직렬화 가능한지 확인해야 한다. 스트리밍 payload 는 커넥터가
 * {@code StreamingHelper.resolveCursorProvider(...)} 로 반복 조회 가능한 형태로 바꿔
 * 담지만, {@code CursorProvider} 자체는 직렬화 대상이 아니다.
 */
public class LogContext implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String flowVersion;
  private final String baseTableName;
  private final TriggerType triggerType;
  private final String actor;
  private final String targetAppName;
  private final Status status;
  private final LocalDateTime eventTime;
  private final LocalDateTime startTime;
  private final Object originPayload;
  private final Object originAttributes;

  private LogContext(Builder builder) {
    this.flowVersion = builder.flowVersion;
    this.baseTableName = builder.baseTableName;
    this.triggerType = builder.triggerType;
    this.actor = builder.actor;
    this.targetAppName = builder.targetAppName;
    this.status = builder.status;
    this.eventTime = builder.eventTime;
    this.startTime = builder.startTime;
    this.originPayload = builder.originPayload;
    this.originAttributes = builder.originAttributes;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Operation({@code build-context}) 경로 — 로그 대상 정보를 Configuration 에서 가져온다. */
  public static LogContext of(BizComLogConfiguration config,
                              LogContextParameters params,
                              Object originPayload,
                              Object originAttributes) {
    return builder()
        .flowVersion(config.getFlowVersion())
        .baseTableName(config.getBaseTableName())
        .from(params)
        .originPayload(originPayload)
        .originAttributes(originAttributes)
        .build();
  }

  /**
   * Scope({@code with-context}) 경로 — Scope 는 config 에 바인딩될 수 없으므로
   * 로그 대상 정보를 자체 파라미터로 받는다. 자세한 이유는
   * {@link LogTargetParameters} 참고.
   */
  public static LogContext of(LogTargetParameters target,
                              LogContextParameters params,
                              Object originPayload,
                              Object originAttributes) {
    return builder()
        .flowVersion(target.getFlowVersion())
        .baseTableName(target.getBaseTableName())
        .from(params)
        .originPayload(originPayload)
        .originAttributes(originAttributes)
        .build();
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

  /** 컨텍스트가 만들어진 시각. */
  public LocalDateTime getEventTime() {
    return eventTime;
  }

  /** 처리 시작 시각. 기본 팩토리에서는 {@link #getEventTime()} 과 같은 값이다. */
  public LocalDateTime getStartTime() {
    return startTime;
  }

  /** 컴포넌트 진입 전 payload. Scope 의 경우 하위 체인 실행 전 값이다. */
  public Object getOriginPayload() {
    return originPayload;
  }

  /** 컴포넌트 진입 전 attributes. Scope 의 경우 하위 체인 실행 전 값이다. */
  public Object getOriginAttributes() {
    return originAttributes;
  }

  /**
   * DB insert 파라미터 등으로 바로 넘기기 좋은 형태로 변환한다.
   * 삽입 순서가 유지되도록 {@link LinkedHashMap} 을 사용한다.
   *
   * <p>enum 은 {@code name()} 문자열로 바꾸지만 두 시각은 {@link LocalDateTime} 객체를
   * 그대로 둔다. JDBC 가 이를 {@code TIMESTAMP} 로 바인딩하므로 문자열로 바꾸면
   * 오히려 타입 정보를 잃는다. {@code originPayload} / {@code originAttributes} 도
   * 변환하지 않고 그대로 둔다 — 어떤 형태로 기록할지는 호출측이 결정할 일이다.
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("flowVersion", flowVersion);
    map.put("baseTableName", baseTableName);
    map.put("triggerType", triggerType == null ? null : triggerType.name());
    map.put("actor", actor);
    map.put("targetAppName", targetAppName);
    map.put("status", status == null ? null : status.name());
    map.put("eventTime", eventTime);
    map.put("startTime", startTime);
    map.put("originPayload", originPayload);
    map.put("originAttributes", originAttributes);
    return Collections.unmodifiableMap(map);
  }

  /**
   * {@code originPayload} / {@code originAttributes} 는 비교에서 <b>제외</b>한다.
   * 사용자의 임의 객체이므로 {@code equals} 가 identity 기반인 경우가 많아, 포함시키면
   * 논리적으로 같은 컨텍스트도 거의 항상 다르다고 판정된다.
   */
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
        && status == that.status
        && Objects.equals(eventTime, that.eventTime)
        && Objects.equals(startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flowVersion, baseTableName, triggerType, actor, targetAppName, status,
        eventTime, startTime);
  }

  /**
   * 원본 payload / attributes 는 <b>값을 찍지 않고 타입만</b> 남긴다. 이 객체는 로그로
   * 흘러갈 가능성이 높은데, 요청 본문 전체를 로그에 쏟으면 용량 문제와 민감정보 노출로
   * 이어진다.
   */
  @Override
  public String toString() {
    return "LogContext{"
        + "flowVersion='" + flowVersion + '\''
        + ", baseTableName='" + baseTableName + '\''
        + ", triggerType=" + triggerType
        + ", actor='" + actor + '\''
        + ", targetAppName='" + targetAppName + '\''
        + ", status=" + status
        + ", eventTime=" + eventTime
        + ", startTime=" + startTime
        + ", originPayload=" + describe(originPayload)
        + ", originAttributes=" + describe(originAttributes)
        + '}';
  }

  private static String describe(Object value) {
    return value == null ? "null" : "<" + value.getClass().getSimpleName() + ">";
  }

  /**
   * 필드가 10개여서 정적 팩토리 대신 빌더를 쓴다. 검증은 {@link #build()} 한 곳에만 있다.
   */
  public static final class Builder {

    private String flowVersion;
    private String baseTableName;
    private TriggerType triggerType;
    private String actor;
    private String targetAppName;
    private Status status;
    private LocalDateTime eventTime;
    private LocalDateTime startTime;
    private Object originPayload;
    private Object originAttributes;

    private Builder() {
    }

    public Builder flowVersion(String flowVersion) {
      this.flowVersion = flowVersion;
      return this;
    }

    public Builder baseTableName(String baseTableName) {
      this.baseTableName = baseTableName;
      return this;
    }

    public Builder triggerType(TriggerType triggerType) {
      this.triggerType = triggerType;
      return this;
    }

    public Builder actor(String actor) {
      this.actor = actor;
      return this;
    }

    public Builder targetAppName(String targetAppName) {
      this.targetAppName = targetAppName;
      return this;
    }

    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    /** 지정하지 않으면 {@link #build()} 에서 현재 시각으로 채워진다. */
    public Builder eventTime(LocalDateTime eventTime) {
      this.eventTime = eventTime;
      return this;
    }

    /** 지정하지 않으면 {@link #build()} 에서 현재 시각으로 채워진다. */
    public Builder startTime(LocalDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder originPayload(Object originPayload) {
      this.originPayload = originPayload;
      return this;
    }

    public Builder originAttributes(Object originAttributes) {
      this.originAttributes = originAttributes;
      return this;
    }

    /** {@link LogContextParameters} 의 4개 값을 한 번에 채운다. */
    public Builder from(LogContextParameters params) {
      return triggerType(params.getTriggerType())
          .actor(params.getActor())
          .targetAppName(params.getTargetAppName())
          .status(params.getStatus());
    }

    /**
     * 검증 후 컨텍스트를 만든다.
     *
     * <p>Mule 은 required 파라미터의 <i>존재</i>만 보장하므로 빈 문자열은 통과한다.
     * 여기서 공백 여부를 검증해 {@link LogErrorType#INVALID_CONTEXT} 로 승격시킨다.
     *
     * <p>{@code eventTime} / {@code startTime} 이 지정되지 않았으면
     * {@link LocalDateTime#now()} 를 <b>한 번만</b> 호출해 비어 있는 쪽을 채운다.
     *
     * @throws ModuleException {@code BIZ-LOG:INVALID_CONTEXT} — 문자열 파라미터가 공백일 때
     */
    public LogContext build() {
      requireNonBlank(flowVersion, "flowVersion");
      requireNonBlank(baseTableName, "baseTableName");
      requireNonBlank(actor, "actor");
      requireNonBlank(targetAppName, "targetAppName");

      if (eventTime == null || startTime == null) {
        LocalDateTime now = LocalDateTime.now();
        if (eventTime == null) {
          eventTime = now;
        }
        if (startTime == null) {
          startTime = now;
        }
      }

      return new LogContext(this);
    }

    private static void requireNonBlank(String value, String name) {
      if (value == null || value.trim().isEmpty()) {
        throw new ModuleException(
            "로그 컨텍스트 파라미터 '" + name + "' 는 비어 있을 수 없습니다.",
            LogErrorType.INVALID_CONTEXT);
      }
    }
  }
}
