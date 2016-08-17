package com.github.j5ik2o.chronos.scheduler

import com.github.j5ik2o.chronos.domain.{ JobStatus, TriggerStatus }
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder

case class JobControlContext(jobStatus: JobStatus, triggerStatus: TriggerStatus) {
  private lazy val tsb = new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
  override def toString: String = tsb
    .append("jobStatus", jobStatus)
    .append("triggerStatus", triggerStatus)
    .build
}

