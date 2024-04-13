package leaderboard.api

import cats.{ApplicativeThrow, Monad}
import cats.syntax.monad.*
import cats.implicits.*
import io.circe.syntax.*
import leaderboard.repo.Ladder
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class LadderApi[F[_]: ApplicativeThrow: Monad](
  dsl: Http4sDsl[F[_]],
  ladder: Ladder[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F[_]] = {
    HttpRoutes.of {
      case GET -> Root / "ladder" =>
        Ok(Monad[F].map(ladder.getScores.flatMap(_.liftTo[F]))(_.asJson))

      case POST -> Root / "ladder" / UUIDVar(userId) / LongVar(score) =>
        Ok(Monad[F].map(ladder.submitScore(userId, score).flatMap(_.liftTo[F]))(_.asJson))
    }
  }
}
