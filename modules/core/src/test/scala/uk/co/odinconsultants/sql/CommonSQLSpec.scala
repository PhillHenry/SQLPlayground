package uk.co.odinconsultants.sql
import cats.effect.IO
import org.scalatest.GivenWhenThen
import uk.co.odinconsultants.documentation_utils.SpecPretifier
import cats.effect.unsafe.implicits.global
import uk.co.odinconsultants.sql.SqlServerUtils.ddlIfTableDoesNotExist
import uk.co.odinconsultants.sql.SqlUtils.ddlFields

case class Address(id: Int, location: String)

case class Customer(id: Int, name: String, address: Int)

class CommonSQLSpec extends SpecPretifier with GivenWhenThen {

  def tableNameFor(x: Class[?]) = x.getSimpleName.replace("$", "_")

  "Created table" should {
    "allow inserts" in {
      val customerTable     = tableNameFor(classOf[Customer])
      val createSQL: String = s"CREATE TABLE $customerTable (${ddlFields(classOf[Customer])})"
      val sql               = ddlIfTableDoesNotExist(customerTable, createSQL)
      val program           = for {
        _ <- IO(Given(s"SQL:\n${formatSQL(sql)}"))
      } yield ()
      program.unsafeRunSync()
    }
  }
}
