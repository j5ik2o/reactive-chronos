package com.github.j5ik2o.chronos.domain

import java.util.UUID

import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.{ ToStringBuilder, ToStringStyle }

case class JobStatus(id: UUID, jobId: UUID, running: Boolean) {

  override def toString: String = {
    new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
      .append("id", id)
      .append("jobId", jobId)
      .append("running", running)
      .build()
  }
}

