package models

import org.encryfoundation.common.transaction.Proof
import org.encryfoundation.prismlang.compiler.CompiledContract
import scorex.crypto.authds.ADKey

case class ParsedInput(key: ADKey, contract: Option[(CompiledContract, Seq[Proof])] = None)
