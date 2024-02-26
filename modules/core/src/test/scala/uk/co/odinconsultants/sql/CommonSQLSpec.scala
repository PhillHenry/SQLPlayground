package uk.co.odinconsultants.sql

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.getquill.*
import org.scalatest.GivenWhenThen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import uk.co.odinconsultants.documentation_utils.SpecPretifier
import uk.co.odinconsultants.sql.MSSqlMain.ctx

case class Address(id: Int, location: String)

case class Customer(id: Int, name: String, address: Int)

class CommonSQLSpec extends SpecPretifier with GivenWhenThen with CustomerAndAddresses {

  import ctx.*

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  "Created table" should {
    "allow inserts" in {
      val program                   = (
        for {
          _ <- Resource.make(
                 IO(Given(s"SQL:\n${formatSQL(addressDDL)}\n${formatSQL(customerDDL)}")) *> IO(When("we execute the SQL"))
               )(_ => IO.unit).flatMap(_ => createTables)
        } yield ()
      ).use { case _ =>
        for {
          _ <- IO(And("we populate those tables"))
          n  = 5
          _ <- IO.println(s"Creating $n addresses") *> createAddresses(n)
          _ <- IO.println(s"Creating $n customers") *> IO {
                 val q = quote {
                   liftQuery(someCustomers(n, n).toList).foreach(a =>
                     query[Customer].insertValue(a)
                   )
                 }
                 ctx.run(q)
               }
          _ <- IO(Then(s"the tables $customerTable and $addressTable have $n rows in them"))
          _ <- IO(assert(ctx.run(query[Customer]).size == n))
          _ <- IO(assert(ctx.run(query[Address]).size == n))
        } yield ()
      }
      program.unsafeRunSync()
    }
  }
}
