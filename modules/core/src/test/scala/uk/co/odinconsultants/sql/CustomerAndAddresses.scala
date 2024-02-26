package uk.co.odinconsultants.sql
import cats.effect.{IO, Resource}
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.scalatest.GivenWhenThen
import uk.co.odinconsultants.sql.MSSqlMain.{ctx, xa}
import uk.co.odinconsultants.sql.SqlServerUtils.ddlIfTableDoesNotExist
import uk.co.odinconsultants.sql.SqlUtils.ddlFields
import uk.co.odinconsultants.sql.MSSqlMain.ctx
import doobie.*
import doobie.implicits.*
import io.getquill.*
import org.scalatest.GivenWhenThen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import uk.co.odinconsultants.documentation_utils.SpecPretifier
import uk.co.odinconsultants.sql.MSSqlMain.ctx

trait CustomerAndAddresses {
  this: GivenWhenThen =>

  import ctx.*

  def execute(sql: String, xa: Transactor[IO]): IO[Int] =
    IO.println(s"Running: $sql") *> Fragment
      .const(sql)
      .update
      .run
      .transact(xa)

  def dropAfter(sql: String, dropTable: String): Resource[IO, Unit] = Resource
    .make {
      execute(sql, xa).void
    }(_ => execute(s"DROP TABLE $dropTable", xa) *> IO.println(s"Dropped $dropTable"))

  def tableNameFor(x: Class[?]) = x.getSimpleName.replace("$", "_")

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

  val createTables: Resource[IO, Unit] = for {
    _ <- dropAfter(addressDDL, addressTable)
    _ <- dropAfter(customerDDL, customerTable)
  } yield ()


  def someAddresses(n: Int): Seq[Address] = (0 until n).map(i => Address(i, s"address $i"))

  def someCustomers(n: Int, addressMod: Int): Seq[Customer] =
    (0 until n).map(i => Customer(i, s"name $i", i % addressMod))

  def createAddresses(n: Int) = IO {
      val q = quote {
        liftQuery(someAddresses(n).toList).foreach(a => query[Address].insertValue(a))
      }
      ctx.run(q)
    }
}
