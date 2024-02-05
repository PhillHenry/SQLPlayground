package uk.co.odinconsultants.sql
import cats.effect.{IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.getquill.{SqlServerJdbcContext, UpperCase}
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.getquill.autoQuote
import cats.*
import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.traverse.*

import scala.concurrent.ExecutionContext

object MSSqlMain extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val TABLE_NAME = "test_table"
  val ctx                = new SqlServerJdbcContext(UpperCase, "ctx")
  val xa: Transactor[IO] = Transactor.fromDataSource[IO](ctx.dataSource, ExecutionContext.global)

  val CREATE_TABLE_SQL = {
    val raw = s"""
               |    if not exists (select * from sysobjects where name='${TABLE_NAME}' and xtype='U')
               |        create table ${TABLE_NAME} (
               |          Name varchar(64) not null,
               |          id SMALLINT not null
               |      )
               |            """.stripMargin
    println(raw)
    Fragment.const(raw)
  }

  val createTable: Transactor[IO] => IO[Int] = { case xa: Transactor[IO] =>
    val updated: doobie.Update0       = CREATE_TABLE_SQL.update
    val ran: doobie.ConnectionIO[Int] = updated.run
    val transacted: IO[Int]           = ran.transact(xa)
    transacted
  }

  def insert(nRows: Int): Transactor[IO] => IO[Unit] = { case xa: Transactor[IO] =>
    val inserts: List[ConnectionIO[Int]] = (0 to nRows).map { case i: Int =>
      val sql = s"insert into $TABLE_NAME values ('${s"name$i"}', $i)"
      println(sql)
      Fragment.const(sql).update.run
    }.toList
    inserts.map(_.transact(xa)).sequence.map(_.sum)
  }

  override def run: IO[Unit] = {
//    List(IO.println("Creating table"), createTable, insert(10)).map(_(xa)).reduce(_ *> _).void
    for {
      _ <- IO.println("Creating table...")
      _ <- createTable(xa)
      _ <- IO.println("Inserting...")
      _ <- insert(10)(xa)
    } yield {
      println("Finished")
    }
  }

}
