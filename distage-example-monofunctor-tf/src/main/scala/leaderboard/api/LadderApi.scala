package leaderboard.api

import cats.MonadThrow
import cats.implicits.*
import io.circe.syntax.*
import leaderboard.repo.Ladder
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class LadderApi[F[_]: MonadThrow](
  dsl: Http4sDsl[F[_]],
  ladder: Ladder[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F[_]] = {
    HttpRoutes.of {
      case GET -> Root / "ladder" =>
        Ok(for {
          resEither <- ladder.getScores
          res       <- resEither.liftTo[F]
        } yield res.asJson)

      case POST -> Root / "ladder" / UUIDVar(userId) / LongVar(score) =>
        Ok(for {
          resEither <- ladder.submitScore(userId, score)
          _         <- resEither.liftTo[F]
        } yield ())
    }
  }
}
