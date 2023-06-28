package it.pagopa.interop.notifier.model

import cats.syntax.all._
import it.pagopa.interop.notifier.model.NotificationMessage._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scanamo.DynamoValue

class NotificationObjectTypeSpec extends AnyWordSpecLike with Matchers {

  "NotificationObjectType values" should {
    "be correctly deserialized for all object types" in {

      import scala.reflect.runtime.{universe => ru}

      val tpe    = ru.typeOf[NotificationObjectType]
      val clazz  = tpe.typeSymbol.asClass
      val result = clazz.knownDirectSubclasses.toList.traverse(s =>
        formatNotificationObjectType.read(DynamoValue.fromString(s.name.toString))
      )

      result shouldBe a[Right[_, _]]
    }
  }
}
