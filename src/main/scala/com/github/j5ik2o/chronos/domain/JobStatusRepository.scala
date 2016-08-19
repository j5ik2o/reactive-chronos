package com.github.j5ik2o.chronos.domain

import java.util.{ NoSuchElementException, UUID }

import scala.util.{ Success, Try }

case class JobStatusRepository(entities: Map[UUID, JobStatus] = Map.empty) {

  def store(aggregate: JobStatus): Try[JobStatusRepository] = Try {
    copy(entities = this.entities + (aggregate.id -> aggregate))
  }

  def storeMulti(aggregates: Seq[JobStatus]): Try[JobStatusRepository] = Try {
    copy(entities = this.entities ++ aggregates.map { e => e.id -> e }.toMap)
  }

  def delete(id: UUID): Try[JobStatusRepository] = Try {
    copy(entities = this.entities.filterNot { case (k, _) => k == id })
  }

  def resolveBy(id: UUID): Try[JobStatus] = Try {
    entities(id)
  }

  def resolveByJobId(jobId: UUID): Try[JobStatus] = Try {
    iterator.find(_.jobId == jobId).get
  }

  def iterator: Iterator[JobStatus] = entities.map { case (_, v) => v }.toIterator

}

object JobStatusRepository {

  implicit def toOption(t: Try[JobStatusRepository]) = {
    t.map(Some(_)).recoverWith {
      case ex: NoSuchElementException => Success(None)
    }
  }

}
