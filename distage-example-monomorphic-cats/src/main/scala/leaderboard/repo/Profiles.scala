package leaderboard.repo

import cats.effect.IO
import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{QueryFailure, UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogIO

import scala.collection.concurrent.TrieMap

trait Profiles {
  def setProfile(userId: UserId, profile: UserProfile): IO[Either[QueryFailure, Unit]]
  def getProfile(userId: UserId): IO[Either[QueryFailure, Option[UserProfile]]]
}

object Profiles {
  final class Dummy
    extends Lifecycle.LiftF[IO[_], Profiles](for {
      state <- IO.pure(TrieMap.empty[UserId, UserProfile])
    } yield {
      new Profiles {
        override def setProfile(userId: UserId, profile: UserProfile): IO[Either[Nothing, Unit]] =
          IO.pure(Right(state.update(userId, profile)))

        override def getProfile(userId: UserId): IO[Either[Nothing, Option[UserProfile]]] =
          IO.pure(Right(state.get(userId)))
      }
    })

  final class Postgres(
    sql: SQL,
    log: LogIO[IO],
  ) extends Lifecycle.LiftF[IO[_], Profiles](for {
      _ <- log.info("Creating Profile table")
      _ <- sql.execute("ddl-profiles") {
        sql"""create table if not exists profiles (
             |  user_id uuid not null,
             |  name text not null,
             |  description text not null,
             |  primary key (user_id)
             |) without oids
             |""".stripMargin.update.run
      }
    } yield new Profiles {
      override def setProfile(userId: UserId, profile: UserProfile): IO[Either[QueryFailure, Unit]] = {
        sql
          .execute("set-profile") {
            sql"""insert into profiles (user_id, name, description)
                 |values ($userId, ${profile.name}, ${profile.description})
                 |on conflict (user_id) do update set
                 |  name = excluded.name,
                 |  description = excluded.description
                 |""".stripMargin.update.run
          }.map(_.map(_ => ()))
      }

      override def getProfile(userId: UserId): IO[Either[QueryFailure, Option[UserProfile]]] = {
        sql.execute("get-profile") {
          sql"""select name, description from profiles
               |where user_id = $userId
               |""".stripMargin.query[UserProfile].option
        }
      }
    })
}
