package leaderboard.repo

import cats.{Applicative, Monad}
import cats.implicits.*
import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{QueryFailure, Score, UserId}
import leaderboard.sql.SQL
import logstage.LogIO

import scala.collection.concurrent.TrieMap

trait Ladder[F[_]] {
  def submitScore(userId: UserId, score: Score): F[Either[QueryFailure, Unit]]
  def getScores: F[Either[QueryFailure, List[(UserId, Score)]]]
}

object Ladder {
  final class Dummy[F[+_] : Applicative]
    extends Lifecycle.LiftF[F[_], Ladder[F]](for {
      state <- Applicative[F].pure(TrieMap.empty[UserId, Score])
    } yield {
      new Ladder[F] {
        override def submitScore(userId: UserId, score: Score): F[Either[Nothing, Unit]] =
          Applicative[F].pure(Right(state.update(userId, score)))

        override def getScores: F[Either[Nothing, List[(UserId, Score)]]] =
          Applicative[F].pure(Right(state.toList.sortBy(_._2)(Ordering[Score].reverse)))
      }
    })

  final class Postgres[F[+_]: Monad](
    sql: SQL[F],
    log: LogIO[F],
  ) extends Lifecycle.LiftF[F[_], Ladder[F]](for {
    _ <- log.info(s"Creating Ladder table")
    _ <- sql.execute("ladder-ddl") {
        sql"""create table if not exists ladder (
             | user_id uuid not null,
             | score bigint not null,
             | primary key (user_id)
             |) without oids
             |""".stripMargin.update.run
      }
      res = new Ladder[F] {
        override def submitScore(userId: UserId, score: Score): F[Either[QueryFailure, Unit]] =
          sql
            .execute("submit-score") {
              sql"""insert into ladder (user_id, score) values ($userId, $score)
                   |on conflict (user_id) do update set
                   |  score = excluded.score
                   |""".stripMargin.update.run
            }.map(_.map(_ => ()))

        override val getScores: F[Either[QueryFailure, List[(UserId, Score)]]] =
          sql.execute("get-leaderboard") {
            sql"""select user_id, score from ladder order by score DESC
                 |""".stripMargin.query[(UserId, Score)].to[List]
          }
      }
    } yield res)
}
