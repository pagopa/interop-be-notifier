package it.pagopa.interop.notifier.service

import it.pagopa.interop.notifier.model.{DynamoQuery, DynamoRecord}

import scala.concurrent.Future

/**
  * Exposes methods for operations on DynamoDB tables
  */
trait DynamoService[Record <: DynamoRecord, Query <: DynamoQuery, ResultType] {

  def put(message: Record): Future[Unit]
  def get(limit: Int)(query: Query)(implicit contexts: Seq[(String, String)]): Future[List[ResultType]]
  def getOne(query: Query)(implicit contexts: Seq[(String, String)]): Future[Option[ResultType]]

}
