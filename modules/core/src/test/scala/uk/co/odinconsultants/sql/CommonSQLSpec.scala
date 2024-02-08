package uk.co.odinconsultants.sql

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.scalatest.GivenWhenThen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import uk.co.odinconsultants.documentation_utils.SpecPretifier
import uk.co.odinconsultants.sql.MSSqlMain.{xa, ctx}
import uk.co.odinconsultants.sql.SqlServerUtils.ddlIfTableDoesNotExist
import uk.co.odinconsultants.sql.SqlUtils.ddlFields
import io.getquill.*
import io.getquill.autoQuote

case class Address(id: Int, location: String)

case class Customer(id: Int, name: String, address: Int)

class CommonSQLSpec extends SpecPretifier with GivenWhenThen {

  import ctx.*

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def tableNameFor(x: Class[?]) = x.getSimpleName.replace("$", "_")

  def execute(sql: String, xa: Transactor[IO]): IO[Int] =
    IO.println(s"Running: $sql") *> Fragment
      .const(sql)
      .update
      .run
      .transact(xa)

  def someAddresses(n: Int): Seq[Address] = (0 until n).map(i => Address(i, s"address $i"))

  def someCustomers(n: Int, addressMod: Int): Seq[Customer] = (0 until n).map(i => Customer(i, s"name $i", i % addressMod))

  def dropAfter(sql: String, dropTable: String): Resource[IO, Unit] = Resource
      .make {
        execute(sql, xa).void
      }(_ => execute(s"DROP TABLE $dropTable", xa) *> IO.println(s"Dropped $dropTable"))

  "Created table" should {
    "allow inserts" in {
      val customerTable             = tableNameFor(classOf[Customer])
      val addressTable              = tableNameFor(classOf[Address])
      val addressSQL: String        =
        s"CREATE TABLE $addressTable (${ddlFields(classOf[Address])}, PRIMARY KEY (id))"
      val addressDDL                = ddlIfTableDoesNotExist(addressTable, addressSQL)
      val createCustomerSQL: String =
        s"""CREATE TABLE $customerTable (${ddlFields(classOf[Customer])},
           |PRIMARY KEY (id),
           |CONSTRAINT fk_$addressTable FOREIGN KEY(address) REFERENCES $addressTable(id))""".stripMargin
      val customerDDL               = ddlIfTableDoesNotExist(customerTable, createCustomerSQL)
      val program = (
          for {
            _ <- Resource.make(IO(Given(s"SQL:\n${formatSQL(addressDDL)}\n${formatSQL(customerDDL)}")))(_ => IO.unit)
            _ <- Resource.make(IO(When("we execute the SQL")))(_ => IO.unit)
            _ <- dropAfter(addressDDL, addressTable)
            _ <- dropAfter(customerDDL, customerTable)
          } yield ()
        ).use { case _ =>
          for {
            _ <- IO(And("we populate those tables"))
            n = 5
            _ <- IO.println(s"Creating $n addresses") *> IO {
              val q = quote {
                liftQuery(someAddresses(n).toList).foreach(a => query[Address].insertValue(a))
              }
              ctx.run(q)
            }
            _ <- IO.println(s"Creating $n customers") *> IO {
              val q = quote {
                liftQuery(someCustomers(n, n).toList).foreach(a => query[Customer].insertValue(a))
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
