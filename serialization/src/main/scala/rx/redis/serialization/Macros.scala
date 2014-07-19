package rx.redis.serialization

import scala.language.higherKinds

import java.nio.charset.Charset
import java.util.Locale
import scala.reflect.macros.blackbox.Context


class Macros(val c: Context) {
  import c.universe._

  val charset = Charset.defaultCharset()
  val StringMarker = '$'.toByte
  val Cr = '\r'.toByte
  val Lf = '\n'.toByte

  def writes[A: c.WeakTypeTag]: c.Tree =
    macroImpl[A, Writes]("write")

  def macroImpl[A, M[_]](methodName: String)(implicit atag: c.WeakTypeTag[A], matag: c.WeakTypeTag[M[A]]): c.Tree = {

    val tpe = atag.tpe
    val mtpe = matag.tpe
    val finalTpe = appliedType(mtpe.typeConstructor, tpe :: Nil)

    val arguments = tpe.decls.collect {
      case method: MethodSymbol if method.isCaseAccessor => method
    }.toList

    val argumentTrees = arguments map { m =>
      val properReturnType = m.infoIn(tpe).resultType
      val neededImplicitType = appliedType(mtpe.typeConstructor, properReturnType :: Nil)
      val paramWrites = c.inferImplicitValue(neededImplicitType)
      if (paramWrites == EmptyTree) {
        c.abort(c.enclosingPosition, "Could not find an implicit value for " + neededImplicitType)
      }
      q"""
          val content = value.${m.name}
          val contentBytes = $paramWrites.write(content, allocator)
          val contentLength = int2bytes(contentBytes.readableBytes())
          buf
            .writeByte($StringMarker)
            .writeBytes(contentLength)
            .writeByte($Cr).writeByte($Lf)
            .writeBytes(contentBytes)
            .writeByte($Cr).writeByte($Lf)
      """
    }

    val argumentsSize = arguments.size
    val size = 1 + argumentsSize

    val name = tpe.typeSymbol.name.toString

    val header = s"*$size\r\n$$${name.getBytes(charset).length}\r\n${name.toUpperCase(Locale.ROOT)}\r\n"
    val headerBytes = header.getBytes(charset)

    val objectName = c.freshName(TermName(name + "Writes"))

    val mName = TermName(methodName)

    val generated = q"""
    object $objectName extends $finalTpe {
      def $mName(value: $tpe, allocator: io.netty.buffer.ByteBufAllocator): io.netty.buffer.ByteBuf = {
        val buf = allocator.buffer(${headerBytes.length + argumentsSize * 16}).writeBytes($headerBytes)
        ..$argumentTrees
        buf
      }
    }
    $objectName
    """

    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
