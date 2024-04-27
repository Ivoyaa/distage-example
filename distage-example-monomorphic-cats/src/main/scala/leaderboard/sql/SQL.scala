package leaderboard.sql

import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import leaderboard.model.QueryFailure

trait SQL {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): IO[Either[QueryFailure, A]]
}

object SQL {
  final class Impl(
    transactor: Transactor[IO[_]]
  ) extends SQL {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): IO[Either[QueryFailure, A]] = {
      transactor.trans
        .apply(conn)
        .redeem(ex => Left(QueryFailure(queryName, ex)), res => Right(res))
    }
  }
}
