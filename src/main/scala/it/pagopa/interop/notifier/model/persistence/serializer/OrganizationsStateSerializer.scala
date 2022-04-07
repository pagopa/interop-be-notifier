package it.pagopa.interop.notifier.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.notifier.model.persistence.OrganizationsState
import it.pagopa.interop.notifier.model.persistence.serializer.v1._

import java.io.NotSerializableException

class OrganizationsStateSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 20000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val className: String = classOf[OrganizationsState].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case s: OrganizationsState => serialize(s, className, currentVersion)
    case _                     =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[${manifest(o)}]], currentVersion: [[$currentVersion]] "
      )
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest.split('|').toList match {
      case `className` :: `version1` :: Nil =>
        deserialize(v1.state.OrganizationsStateV1, bytes, manifest, currentVersion)
      case _                                =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )
    }

}