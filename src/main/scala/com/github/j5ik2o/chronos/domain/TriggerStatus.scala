package com.github.j5ik2o.chronos.domain

import java.util.UUID

import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.{ ToStringBuilder, ToStringStyle }
import org.sisioh.baseunits.scala.time.TimePoint

import scala.util.Try

case class TriggerStatus(id: UUID,
                         jobId: UUID,
                         triggerId: UUID,
                         computedFireAt: TimePoint,
                         startedAt: Option[TimePoint] = None,
                         finishedAt: Option[TimePoint] = None,
                         result: Option[Try[Any]] = None) {


  def isStarted(now: TimePoint): Boolean =
    startedAt.exists(_.isAfter(now))

  def isFinished(now: TimePoint): Boolean =
    finishedAt.exists(_.isAfter(now))

  override def toString: String =
    new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
      .append("id", id)
      .append("jobId", jobId)
      .append("triggerId", triggerId)
      .append("computedFireAt", computedFireAt)
      .append("startedAt", startedAt)
      .append("finishedAt", finishedAt)
      .append("result", result)
      .build
}
