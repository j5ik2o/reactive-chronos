package com.github.j5ik2o.chronos.scheduler

import com.github.j5ik2o.chronos.domain.{ Job, JobStatus, Trigger, TriggerStatus }
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder

case class JobControlContext(
    job:           Job,
    trigger:       Trigger,
    jobStatus:     JobStatus,
    triggerStatus: TriggerStatus
) {

  override def toString: String =
    new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
      .append("job", job)
      .append("trigger", trigger)
      .append("jobStatus", jobStatus)
      .append("triggerStatus", triggerStatus)
      .build
}

