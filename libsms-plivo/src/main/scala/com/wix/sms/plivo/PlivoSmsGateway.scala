package com.wix.sms.plivo

import java.io.IOException

import com.google.api.client.http._
import com.google.api.client.util.Base64
import com.wix.sms.model.{Sender, SmsGateway}
import com.wix.sms.plivo.model.{SendMessageRequestParser, SendMessageResponseParser}
import com.wix.sms.{CommunicationException, SmsErrorException, SmsException}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object Endpoints {
  val production = "https://api.plivo.com/v1/"
}

class PlivoSmsGateway(requestFactory: HttpRequestFactory,
                      endpoint: String = Endpoints.production,
                      connectTimeout: Option[Duration] = None,
                      readTimeout: Option[Duration] = None,
                      numberOfRetries: Int = 0,
                      credentials: Credentials) extends SmsGateway {
  override def sendPlain(sender: Sender, destPhone: String, text: String): Try[String] = {
    send(
      sender = sender,
      destPhone = destPhone,
      text = text
    )
  }

  override def sendUnicode(sender: Sender, destPhone: String, text: String): Try[String] = {
    send(
      sender = sender,
      destPhone = destPhone,
      text = text
    )
  }

  private def send(sender: Sender, destPhone: String, text: String): Try[String] = {
    Try {
      val request = PlivoHelper.createSendMessageRequest(
        sender = sender,
        destPhone = destPhone,
        text = text
      )

      val requestJson = SendMessageRequestParser.stringify(request)

      val httpRequest = requestFactory.buildPostRequest(
        new GenericUrl(s"${endpoint}Account/${credentials.authId}/Message/"),
        new ByteArrayContent("application/json; charset=utf-8", requestJson.getBytes("UTF-8")))

      connectTimeout.foreach { duration => httpRequest.setConnectTimeout(duration.toMillis.toInt) }
      readTimeout.foreach { duration => httpRequest.setReadTimeout(duration.toMillis.toInt) }
      httpRequest.setNumberOfRetries(numberOfRetries)

      httpRequest.getHeaders.setAuthorization(createBasicAuthorization(
        user = credentials.authId,
        password = credentials.authToken
      ))

      val httpResponse = httpRequest.execute()
      val responseJson = extractAndCloseResponse(httpResponse)
      val response = SendMessageResponseParser.parse(responseJson)

      response.error match {
        case Some(error) => throw SmsErrorException(message = error)
        case None => response.message_uuid.get.head
      }
    } match {
      case Success(msgUuid) => Success(msgUuid)
      case Failure(e: SmsException) => Failure(e)
      case Failure(e: IOException) => Failure(CommunicationException(e.getMessage, e))
      case Failure(e) => Failure(SmsErrorException(e.getMessage, e))
    }
  }

  private def createBasicAuthorization(user: String, password: String): String = {
    s"Basic ${Base64.encodeBase64String(s"$user:$password".getBytes("UTF-8"))}"
  }

  private def extractAndCloseResponse(httpResponse: HttpResponse): String = {
    try {
      httpResponse.parseAsString()
    } finally {
      httpResponse.ignore()
    }
  }
}
