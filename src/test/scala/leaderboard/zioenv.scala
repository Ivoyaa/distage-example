package leaderboard

import leaderboard.model.*
import leaderboard.repo.{Ladder}
//import leaderboard.repo.{Ladder, Profiles}
//import leaderboard.services.Ranks
import org.scalacheck.Arbitrary
import zio.{IO, Task, RIO, ZIO}

object zioenv {

  object ladder extends Ladder[RIO[Ladder[Task], _]] {
    def submitScore(userId: UserId, score: Score): RIO[Ladder[Task], Either[QueryFailure, Unit]] = ZIO.serviceWithZIO(_.submitScore(userId, score))
    def getScores: RIO[Ladder[Task], Either[QueryFailure, List[(UserId, Score)]]]                = ZIO.serviceWithZIO(_.getScores)
  }
//
//  object profiles extends Profiles[ZIO[Profiles[IO], _, _]] {
//    override def setProfile(userId: UserId, profile: UserProfile): ZIO[Profiles[IO], QueryFailure, Unit] = ZIO.serviceWithZIO(_.setProfile(userId, profile))
//    override def getProfile(userId: UserId): ZIO[Profiles[IO], QueryFailure, Option[UserProfile]]        = ZIO.serviceWithZIO(_.getProfile(userId))
//  }
//
//  object ranks extends Ranks[ZIO[Ranks[IO], _, _]] {
//    override def getRank(userId: UserId): ZIO[Ranks[IO], QueryFailure, Option[RankedProfile]] = ZIO.serviceWithZIO(_.getRank(userId))
//  }

  object rnd extends Rnd[RIO[Rnd[Task], _]] {
    override def apply[A: Arbitrary]: RIO[Rnd[Task], A] = ZIO.serviceWithZIO(_.apply[A])
  }

}
