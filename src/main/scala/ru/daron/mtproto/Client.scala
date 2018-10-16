package ru.daron.mtproto

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import ru.daron.mtproto.Client._
import ru.daron.mtproto.models._
import scodec.Codec
import scodec.bits.BitVector

class Client extends Actor {

  import akka.io.Tcp._
  import context.system

  val address = new InetSocketAddress("0.0.0.0", 8888)
  IO(Tcp) ! Connect(address)

  val codec = Codec[UnencryptedMessage]
  val req = ReqPQ(101L)

  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      println("Actor: Connection failed.")

    case _ @ Connected(_, _) =>
      println("Actor: connected")
      val connection = sender()
      connection ! Register(self)
      println(s"Actor sending message $req.")
      connection ! Write(encode(req))
    case Received(data) =>
      val resp = decode(data)
      println(s"Actor: Got data from server: $resp.")
      val conn = sender()

      resp match {
        case ResPQ(n, s) =>
          println(s"Actor: Got server nonce $s.")
          self ! AskDH(n, s, conn)
        case ResDHParamsOk(_, _) =>
          println("Actor: DH params ok.")
          self ! Stop(conn)
        case ResDHParamsFail(_, _) =>
          println("Actor: DH params fail.")
          self ! Stop(conn)
      }
    case AskDH(n, s, conn) =>
      conn ! Write(encode(ReqDHParams(n, s)))
    case Stop(conn) =>
      conn ! Close
    case Closed =>
      println("Actor: Connection closed, stopping client.")
      context.stop(self)

  }

  private def decode(data: ByteString): PQMessage = {
    codec.decode(BitVector(data.toArray)).require.value.message
  }

  private def encode(req: PQMessage): ByteString = {
    ByteString(codec.encode(UnencryptedMessage(message = req)).require.toByteArray)
  }
}

object Client {
  def props = Props(classOf[Client])

  final case class AskDH(n: Long, s: Long, conn: ActorRef)

  final case class Stop(conn: ActorRef)
}
