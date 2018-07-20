package loggingSystem

import akka.stream.Materializer
import javax.inject.Inject
import play.api.Logger
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

class LoggingFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result]) (requestHeader: RequestHeader): Future[Result] =
    if (requestHeader.uri.startsWith("/assets")) nextFilter(requestHeader)
    else {
      val startTime: Long = System.currentTimeMillis
      nextFilter(requestHeader).map { result =>
        val requestTime: Long = System.currentTimeMillis - startTime
        Logger.info(s"Method: ${requestHeader.method}, " +
          s"ip: ${requestHeader.domain}, " +
          s"uri: ${requestHeader.uri}, " +
          s"response time: ${requestTime}ms, " +
          s"code of response = [${result.header.status}].")
        result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
}
