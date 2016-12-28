package com.wix.sms.plivo.it

import com.google.api.client.http.javanet.NetHttpTransport
import com.wix.sms.SmsErrorException
import com.wix.sms.model.{Sender, SmsGateway}
import com.wix.sms.plivo.testkit.PlivoDriver
import com.wix.sms.plivo.{Credentials, PlivoSmsGateway}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class PlivoSmsGatewayIT extends SpecWithJUnit {
  private val plivoPort = 10006

  val driver = new PlivoDriver(port = plivoPort)
  step {
    driver.start()
  }

  sequential

  trait Ctx extends Scope {
    val requestFactory = new NetHttpTransport().createRequestFactory()

    val someCredentials = Credentials(
      authId = "someAuthId",
      authToken = "someAuthToken"
    )
    val someDestPhone = "+12125551234"
    val someSender = Sender(
      phone = Some("+12125554321")
    )
    val somePlainText = "some plain text"
    val someUnicodeText = "some יוניקוד text"
    val someMessageId = "someMessageId"

    val plivo: SmsGateway = new PlivoSmsGateway(
      requestFactory = requestFactory,
      endpoint = s"http://localhost:$plivoPort/",
      credentials = someCredentials
    )

    driver.reset()
  }

  "sendPlain" should {
    "successfully yield a message ID on valid request" in new Ctx {
      driver.aSendMessageFor(
        credentials = someCredentials,
        sender = someSender,
        destPhone = someDestPhone,
        text = somePlainText
      ) returns(
          msgId = someMessageId
      )

      plivo.sendPlain(
        sender = someSender,
        destPhone = someDestPhone,
        text = somePlainText
      ) must beASuccessfulTry(
        check = ===(someMessageId)
      )
    }

    "gracefully fail on error" in new Ctx {
      val someError = "some error"

      driver.aSendMessageFor(
        credentials = someCredentials,
        sender = someSender,
        destPhone = someDestPhone,
        text = somePlainText
      ) failsWith(
        error = someError
      )

      plivo.sendPlain(
        sender = someSender,
        destPhone = someDestPhone,
        text = somePlainText
      ) must beAFailedTry.like {
        case e: SmsErrorException => e.message must contain(someError)
      }
    }
  }

  "sendUnicode" should {
    "successfully yield a message ID on valid request" in new Ctx {
      driver.aSendMessageFor(
        credentials = someCredentials,
        sender = someSender,
        destPhone = someDestPhone,
        text = someUnicodeText
      ) returns(
        msgId = someMessageId
      )

      plivo.sendUnicode(
        sender = someSender,
        destPhone = someDestPhone,
        text = someUnicodeText
      ) must beASuccessfulTry(
        check = ===(someMessageId)
      )
    }

    "gracefully fail on error" in new Ctx {
      val someError = "some error"

      driver.aSendMessageFor(
        credentials = someCredentials,
        sender = someSender,
        destPhone = someDestPhone,
        text = someUnicodeText
      ) failsWith(
        error = someError
        )

      plivo.sendUnicode(
        sender = someSender,
        destPhone = someDestPhone,
        text = someUnicodeText
      ) must beAFailedTry.like {
        case e: SmsErrorException => e.message must contain(someError)
      }
    }
  }

  step {
    driver.stop()
  }
}
