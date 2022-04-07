package it.pagopa.interop.notifier.model.persistence.serializer.v1

import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganization
import it.pagopa.interop.notifier.model.persistence.serializer.v1.organization.OrganizationV1

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.util.Try

object protobufUtils {

  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def toPersistentOrganization(org: OrganizationV1): Either[Throwable, PersistentOrganization] = {
    for {
      uuid <- Try {
        UUID.fromString(org.organizationId)
      }.toEither
    } yield PersistentOrganization(
      organizationId = uuid,
      notificationURL = org.notificationURL,
      audience = org.audience
    )
  }

  def toProtobufOrganization(org: PersistentOrganization): OrganizationV1 =
    OrganizationV1(
      organizationId = org.organizationId.toString,
      notificationURL = org.notificationURL,
      audience = org.audience
    )

  def fromTime(timestamp: OffsetDateTime): String = timestamp.format(formatter)
  def toTime(timestamp: String): OffsetDateTime   = {
    OffsetDateTime.of(LocalDateTime.parse(timestamp, formatter), ZoneOffset.UTC)
  }
}
