package uk.co.odinconsultants.sql
import cats.effect.{IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.getquill.{SqlServerJdbcContext, UpperCase}
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

object MSSqlMain extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val ctx                = new SqlServerJdbcContext(UpperCase, "ctx")
  val xa: Transactor[IO] = Transactor.fromDataSource[IO](ctx.dataSource, ExecutionContext.global)

  override def run: IO[Unit] = {
    val sql: Fragment =
      sql"""
          if not exists (select * from sysobjects where name='cars' and xtype='U')
    create table cars (
        Name varchar(64) not null
    )
          """

    val updated: doobie.Update0       = sql.update
    val ran: doobie.ConnectionIO[Int] = updated.run
    val transacted: IO[Int]           = ran.transact(xa)

    transacted.void
  }

}
