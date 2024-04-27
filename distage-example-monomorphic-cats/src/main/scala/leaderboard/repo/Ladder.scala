package leaderboard.repo

import cats.effect.IO
import cats.implicits.*
import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{QueryFailure, Score, UserId}
import leaderboard.sql.SQL
import logstage.LogIO

import scala.collection.concurrent.TrieMap

trait Ladder {
  def submitScore(userId: UserId, score: Score): IO[Either[QueryFailure, Unit]]
  def getScores: IO[Either[QueryFailure, List[(UserId, Score)]]]
}

object Ladder {
  final class Dummy
    extends Lifecycle.LiftF[IO[_], Ladder](for {
      state <- IO.pure(TrieMap.empty[UserId, Score])
    } yield {
      new Ladder {
        override def submitScore(userId: UserId, score: Score): IO[Either[Nothing, Unit]] =
          IO.pure(Right(state.update(userId, score)))

        override def getScores: IO[Either[Nothing, List[(UserId, Score)]]] =
          IO.pure(Right(state.toList.sortBy(_._2)(Ordering[Score].reverse)))
      }
    })

  final class Postgres(
    sql: SQL,
    log: LogIO[IO],
  ) extends Lifecycle.LiftF[IO[_], Ladder](for {
      _ <- log.info(s"Creating Ladder table")
      _ <- sql.execute("ladder-ddl") {
        sql"""create table if not exists ladder (
             | user_id uuid not null,
             | score bigint not null,
             | primary key (user_id)
             |) without oids
             |""".stripMargin.update.run
      }
      res = new Ladder {
        override def submitScore(userId: UserId, score: Score): IO[Either[QueryFailure, Unit]] =
          sql
            .execute("submit-score") {
              sql"""insert into ladder (user_id, score) values ($userId, $score)
                   |on conflict (user_id) do update set
                   |  score = excluded.score
                   |""".stripMargin.update.run
            }.map(_.map(_ => ()))

        override val getScores: IO[Either[QueryFailure, List[(UserId, Score)]]] =
          sql.execute("get-leaderboard") {
            sql"""select user_id, score from ladder order by score DESC
                 |""".stripMargin.query[(UserId, Score)].to[List]
          }
      }
    } yield res)
}
