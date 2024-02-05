package uk.co.odinconsultants.sql
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.getquill.{SqlServerJdbcContext, UpperCase}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object MSSqlMain extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val TABLE_NAME         = "test_table"
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

  def insert(nRows: Int): Transactor[IO] => IO[Int] = { case xa: Transactor[IO] =>
    val inserts: List[ConnectionIO[Int]] = (1 to nRows).map { case i: Int =>
      val sql = s"insert into $TABLE_NAME values ('${s"name$i"}', $i)"
      println(sql)
      Fragment.const(sql).update.run
    }.toList
    inserts.map(_.transact(xa)).sequence.map(_.sum)
  }

  /** If the table does not exist, this prints:
    * Create rows = 0, insert rows = 10
    * If it does:
    * Create rows = -1, insert rows = 10
    * @return
    */
  override def run: IO[Unit] =
    for {
      _          <- IO.println("Creating table...")
      createRows <- createTable(xa)
      _          <- IO.println("Inserting...")
      insertRows <- insert(10)(xa)
    } yield println(s"Create rows = $createRows, insert rows = $insertRows")

}
