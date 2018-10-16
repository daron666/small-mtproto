package ru.daron.mtproto.models

import scodec.Codec
import scodec.codecs._

final case class UnencryptedMessage(messageId: Long = System.currentTimeMillis(),
                                    message: PQMessage)

object UnencryptedMessage {
  val unencrypted = constant(0x00000000)

  val codec: Codec[UnencryptedMessage] = {
    ("auth_key_id" | unencrypted) :: ("message_Id" | int64) :: ("message" | PQMessage.codec)
  }.as[UnencryptedMessage]

  implicit val variableSizeBitsCodes: Codec[UnencryptedMessage] = variableSizeBytesLong(uint32, codec)
}
