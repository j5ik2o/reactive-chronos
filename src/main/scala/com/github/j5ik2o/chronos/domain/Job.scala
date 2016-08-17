package com.github.j5ik2o.chronos.domain

import java.util.UUID

import akka.actor.Props
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder

case class Job(id: UUID, jobProps: Props, startBeforeFinished: Boolean = false) {
  val name: String = s"job-$id"

  override def toString: String = {
    new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
      .append("id", id)
      .append("name", name)
      .append("jobProps", jobProps)
      .append("startBeforeFinished", startBeforeFinished)
      .build()
  }
}
