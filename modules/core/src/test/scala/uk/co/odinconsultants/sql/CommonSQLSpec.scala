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
import uk.co.odinconsultants.sql.MSSqlMain.xa
import uk.co.odinconsultants.sql.SqlServerUtils.ddlIfTableDoesNotExist
import uk.co.odinconsultants.sql.SqlUtils.ddlFields

case class Address(id: Int, location: String)

case class Customer(id: Int, name: String, address: Int)

class CommonSQLSpec extends SpecPretifier with GivenWhenThen {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def tableNameFor(x: Class[?]) = x.getSimpleName.replace("$", "_")

  def execute(sql: String, xa: Transactor[IO]): IO[Int] =
    Fragment
      .const(sql)
      .update
      .run
      .transact(xa)

   def someAddresses(n: Int): Seq[Address] = (0 to n).map(i => Address(i, s"address $i"))

   def someCustomers(n: Int, addressMod: Int): Seq[Customer] = (0 to n).map(i => Customer(i, s"name $i", i % addressMod))

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
      val program                   = for {
        _ <- IO(Given(s"SQL:\n${formatSQL(addressDDL)}\n${formatSQL(customerDDL)}"))
        _ <- IO(When("we execute the SQL"))
        _ <- execute(addressDDL, xa)
        _ <- execute(customerDDL, xa)
      } yield ()
      program.unsafeRunSync()
    }
  }
}
