package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.authorizationmanagement.model.persistence.KeysAdded

object AutorizationEventsSlickHandler {

  // TODO facciamolo come un DAO

  // def handleEvents(e: EventDelAuth): PartialFunction[Event,Future[Unit]] = {
  //        TODO usa slick e crea un DAO di cui fare il wiring nell'endpoint
  //   import slick.jdbc.PostgresProfile.api._
  //   val db         = Database.forConfig("postgres")
  //   val plainQuery = sql"select SUP_NAME from SUPPLIERS where STATE = 'ARGH'".as[String]
  //   db.run(plainQuery)
  // }

  // TODO patternmatch sugli event: Future.unit in tutti i casi tranne che KeysAdded e Keys Deleted
  // TODO nel caso di KeysAdded e Keys Deleted => Usa slick per scrive in tabella
  // TODO ogni evento di Add sono N scritture su postgres
  // TODO idea Schema: sortKey (seq auto-generated), kid, EventType (enum), eventDate

  // TODO AGGIUNGERE ANCHE FUNZIONE DI LETTURA SULLA TABELLA DA UTILIZZARSI NEGLI ENDPOINTS

}
