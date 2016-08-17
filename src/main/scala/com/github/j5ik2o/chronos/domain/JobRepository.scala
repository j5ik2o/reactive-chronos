package com.github.j5ik2o.chronos.domain

import java.util.UUID

import scala.util.Try

case class JobRepository(entities: Map[UUID, Job] = Map.empty) {

  def store(aggregate: Job): Try[JobRepository] = Try {
    copy(entities = this.entities + (aggregate.id -> aggregate))
  }

  def storeMulti(aggregates: Seq[Job]): Try[JobRepository] = Try {
    copy(entities = this.entities ++ aggregates.map { e => e.id -> e }.toMap)
  }

  def delete(id: UUID): Try[JobRepository] = Try {
    copy(entities = this.entities.filterNot { case (k, _) => k == id })
  }

  def resolveBy(id: UUID): Try[Job] = Try {
    entities(id)
  }

  def iterator: Iterator[Job] = entities.map { case (_, v) => v }.toIterator

}
