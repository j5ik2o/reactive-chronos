package com.github.j5ik2o.chronos.domain

import java.util.UUID

import scala.util.Try

case class TriggerRepository(entites: Map[UUID, Trigger] = Map.empty) {

  def store(aggregate: Trigger): Try[TriggerRepository] = Try {
    copy(entites = this.entites + (aggregate.id -> aggregate))
  }

  def storeMulti(aggregates: Seq[Trigger]): Try[TriggerRepository] = Try {
    copy(entites = this.entites ++ aggregates.map { e => e.id -> e }.toMap)
  }

  def delete(id: UUID): Try[TriggerRepository] = Try {
    copy(entites = this.entites.filterNot { case (k, _) => k == id })
  }

  def deleteMulti(ids: Seq[UUID]): Try[TriggerRepository] = Try {
    copy(entites = this.entites.filterNot { case (k, _) => ids.contains(k) })
  }

  def resolveBy(id: UUID): Try[Trigger] = Try {
    entites(id)
  }

  def iterator: Iterator[Trigger] = entites.map { case (_, v) => v }.toList.sortWith(_.nextFireTimePoint < _.nextFireTimePoint).toIterator

}
