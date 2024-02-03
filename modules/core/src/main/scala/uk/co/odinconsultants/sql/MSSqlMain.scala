package uk.co.odinconsultants.sql
import cats.effect.{IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.getquill.SqlServerJdbcContext

object MSSqlMain extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run: IO[Unit] = {

  }
}
