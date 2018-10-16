package ru.daron.mtproto.models

import scodec.Codec
import scodec.codecs._
import shapeless.HNil
import shapeless.syntax.singleton._


sealed trait PQMessage

final case class ReqPQ(nonce: Long) extends PQMessage

final case class ResPQ(nonce: Long, serverNonce: Long) extends PQMessage

final case class ReqDHParams(nonce: Long, serverNonce: Long) extends PQMessage

final case class ResDHParamsOk(nonce: Long, serverNonce: Long) extends PQMessage

final case class ResDHParamsFail(nonce: Long, serverNonce: Long) extends PQMessage

object PQMessage {

  implicit val codec: Codec[PQMessage] = {

    implicit def reqPQCodec: Codec[ReqPQ] =
      ("nonce" | int64).as[ReqPQ]
    implicit def resPqCodec: Codec[ResPQ] =
      {("nonce" | int64) :: ("server_nonce" | int64)}.as[ResPQ]
    implicit def reqDHCodec: Codec[ReqDHParams] =
      {("nonce" | int64) :: ("server_nonce" | int64)}.as[ReqDHParams]
    implicit def resDHOkCodec: Codec[ResDHParamsOk] =
      {("nonce" | int64) :: ("server_nonce" | int64)}.as[ResDHParamsOk]
    implicit def resDHFailCodec: Codec[ResDHParamsFail] =
      {("nonce" | int64) :: ("server_nonce" | int64)}.as[ResDHParamsFail]

    Codec
      .coproduct[PQMessage]
      .discriminatedBy(int32)
      .using(
        'ReqPQ ->> 0x60469778 ::
        'ResPQ ->> 0x05162463 ::
        'ReqDHParams ->> 0xd712e4be ::
        'ResDHParamsOk ->> 0xd0e8075c ::
        'ResDHParamsFail ->> 0x79cb045d ::
        HNil
      )
      .as[PQMessage]
  }
}
