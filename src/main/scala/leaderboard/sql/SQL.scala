package leaderboard.sql

import cats.ApplicativeThrow
import cats.effect.Async
import cats.syntax.applicativeError.*
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import leaderboard.model.QueryFailure

trait SQL[F[_]] {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): F[Either[QueryFailure, A]]
}

object SQL {
  final class Impl[F[+_]: Async: ApplicativeThrow](
    transactor: Transactor[F[_]]
  ) extends SQL[F] {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): F[Either[QueryFailure, A]] = {
      transactor.trans
        .apply(conn)
        .redeem(ex => Left(QueryFailure(queryName, ex)), res => Right(res))
    }
  }
}
