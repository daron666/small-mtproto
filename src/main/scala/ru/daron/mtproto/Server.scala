package ru.daron.mtproto

import java.nio.ByteOrder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.util.ByteString
import ru.daron.mtproto.models._
import scodec.Codec
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.util.Random

object Server extends App {

  import akka.stream.scaladsl.Framing

  implicit val system = ActorSystem.apply("new")
  implicit val materializer = ActorMaterializer()

  implicit val messageCodec = UnencryptedMessage.variableSizeBitsCodes

  val (host, port) = ("0.0.0.0", 8888)

  val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind(host, port)
  connections.runForeach { connection =>

    println(s"Server: New connection from: ${connection.remoteAddress}")
    val proc = Flow[ByteString]
      .via(Framing.lengthField(4, 0, Int.MaxValue, ByteOrder.BIG_ENDIAN))
      .map { byteString =>
        BitVector.apply(byteString.toArray)
      }
      .map { bits =>

        println(s"Server: Got bits : $bits")
        messageCodec.decode(bits).require.value match {
          case UnencryptedMessage(messageId, ReqPQ(nonce)) =>
            println(s"Server: Got request pq message with message Id : $messageId and nonce : $nonce")
          case UnencryptedMessage(messageId, ReqDHParams(nonce, serverNonce)) =>
            println(s"Server: Got request dh params message with message Id : " +
              s"$messageId and nonce : $nonce and serverNonce : $serverNonce")
          case _ =>
            println("Server: got nothing interesting.")
        }
        messageCodec.decode(bits).require.value.message
      }
      .via(pqFlow)
      //really don't know how to close this connection from this place.
      //that's why I put close connection logic into client.
      .map(res => UnencryptedMessage(System.currentTimeMillis(), res).asByteString)

    connection.handleWith(proc)
  }

  //naive state like implementation. no validations, no strings attached.
  val pqFlow = Flow[PQMessage].statefulMapConcat { () =>
    var data: Map[Long, Long] = Map.empty[Long, Long]

    {
      case ReqPQ(nonce) =>
        println("Server: Generating server nonce.")
        val serverNonce = Random.nextLong()
        println(s"Server: Server nonce is $serverNonce.")
        data = data.updated(nonce, serverNonce)
        List(ResPQ(nonce, serverNonce))
      case ReqDHParams(nonce, serverNonce) =>
        println(s"Server: Asking for DH params for nonce $nonce and serverNonce $serverNonce.")
        val isOk = data.get(nonce).contains(serverNonce)
        if (isOk) {
          List(ResDHParamsOk(nonce, serverNonce))
        } else {
          List(ResDHParamsFail(nonce, serverNonce))
        }
    }
  }

  implicit class MessageToByteString(m: UnencryptedMessage) {
    val codec = Codec[UnencryptedMessage]
    def asByteString: ByteString = ByteString(codec.encode(m).require.toByteArray)
  }

  //simple client to check that all is good
  system.actorOf(Client.props)
}
