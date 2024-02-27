package uk.co.odinconsultants.sql

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import io.getquill.*
import org.scalatest.GivenWhenThen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import uk.co.odinconsultants.documentation_utils.SpecPretifier
import uk.co.odinconsultants.sql.MSSqlMain.{ctx, xa}

class CommonSQLSpec extends SpecPretifier with GivenWhenThen with CustomerAndAddresses {

  import ctx.*

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  "Created table" should {
    "allow inserts" in {
      val program = (
        for {
          _ <- Resource
                 .make(
                   IO(Given(s"SQL:\n${formatSQL(addressDDL)}\n${formatSQL(customerDDL)}")) *> IO(
                     When("we execute the SQL")
                   )
                 )(_ => IO.unit)
                 .flatMap(_ => createTables)
        } yield ()
      ).use { case _ =>
        for {
          _ <- IO(And("we populate those tables"))
          n  = 5
          _ <- IO.println(s"Creating $n addresses") *> createAddresses(n)
          _ <- IO.println(s"Creating $n customers") *> createCusomters(n)
          _ <- IO(Then(s"the tables $customerTable and $addressTable have $n rows in them"))
          _ <- IO(assert(ctx.run(query[Customer]).size == n))
          _ <- IO(assert(ctx.run(query[Address]).size == n))
        } yield ()
      }
      program.unsafeRunSync()
    }
  }
  "Query plan" should {
    "give metrics for select" in {
      val n       = 5
      val program = (
        for {
          _ <- createTables
          _ <- Resource.make(IO(Given("fully populated tables")) *> createAddresses(n) *> createCusomters(n))(_ => IO.unit)
        } yield ()
      ).use { case _ =>
        for {
          _ <- execute("SET SHOWPLAN_ALL ON", xa).void
          _ <- IO(Then(s"the tables $customerTable and $addressTable have $n rows in them"))
          _ <- queryWithLogHandler("SELECT * FROM Address", xa).void
        } yield ()
      }
      program.unsafeRunSync()
    }
  }
}
