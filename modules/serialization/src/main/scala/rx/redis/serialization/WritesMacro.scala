/*
 * Copyright 2014 – 2015 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.redis.serialization

import java.util.Locale
import scala.language.higherKinds
import scala.reflect.macros.blackbox

import rx.redis.util.Utf8

class WritesMacro(val c: blackbox.Context) {
  import c.universe._

  def writes[A: c.WeakTypeTag]: c.Tree = macroImpl[A, ByteBufWriter, Writes]

  private[this] def fail(msg: String) =
    c.abort(c.enclosingPosition, "\n" + msg)

  val bbuf = tq"io.netty.buffer.ByteBuf"

  private[this] val asterisk = '*'.toInt
  private[this] val dollar = '$'.toInt
  private[this] val crlf = Array('\r'.toByte, '\n'.toByte)

  private class ArgType(tpe: Type, tc: Type, field: MethodSymbol) {
    private[this] val proper: Type = field.infoIn(tpe).resultType
    private[this] val access = q"value.${field.name}"

    private[this] val isRepeated =
      proper.resultType.typeSymbol == definitions.RepeatedParamClass

    private[this] val neededTypeClassType: Type =
      if (!isRepeated) proper
      else proper.typeArgs.head

    private[this] val isTupleType =
      neededTypeClassType.typeArgs.nonEmpty &&
        definitions.TupleClass.seq.exists(t ⇒ neededTypeClassType.baseType(t) != NoType)

    private[this] val tupleSize =
      if (!isTupleType) 1
      else neededTypeClassType.typeArgs.size

    private[this] val neededTypeClasses: List[Type] =
      if (!isTupleType)
        List(appliedType(tc.typeConstructor, neededTypeClassType :: Nil))
      else
        neededTypeClassType.typeArgs.map(t ⇒ appliedType(tc.typeConstructor, t :: Nil))

    private[this] def resolvedOneTypeClass(tc: Type): c.Tree = {
      val paramWrites = c.inferImplicitValue(tc)
      if (paramWrites == EmptyTree) {
        fail(
          "Missing implicit instance of " + tc + "\n" +
            "This is required to serialize instances of " + tc.typeArgs.head)
      }
      paramWrites
    }

    private[this] val resolvedTypeClasses: List[c.Tree] = {
      neededTypeClasses.map(resolvedOneTypeClass)
    }

    private[this] def generateSingleArgBBUf(value: c.Tree, tc: c.Tree): c.Tree =
      q"""
      bb.writeByte($dollar)
      if ($tc.hasKnownSize) {
        val knownSize = $tc.knownSize($value)
        bb.writeBytes(knownSize.toString.getBytes(rx.redis.util.Utf8))
        bb.writeBytes($crlf)
        $tc.toByteBuf(bb, $value)
      } else {
        val contentHolder = bb.alloc().buffer()
        val content = $tc.toByteBuf(contentHolder, $value)
        bb.writeBytes(content.readableBytes().toString.getBytes(rx.redis.util.Utf8))
        bb.writeBytes($crlf)
        bb.writeBytes(content)
      }
      bb.writeBytes($crlf)
      """

    private[this] def generateSimpleArgBBUf(value: c.Tree): c.Tree =
      generateSingleArgBBUf(value, resolvedTypeClasses.head)

    private[this] def generateTupleArgBBUf(value: c.Tree): c.Tree = {
      val tuples =
        resolvedTypeClasses.zipWithIndex map {
          case (tcls, i) ⇒
            val tupleAccess = TermName(s"_${i + 1}")
            generateSingleArgBBUf(Select(value, tupleAccess), tcls)
        }
      q"..$tuples"
    }

    private[this] def generateRepeatedArgsBBUf(): c.Tree = {
      val items = fq"x <- $access"
      val singleArg = generateSimpleArgBBUf(q"x")
      q"""
      for ($items) {
        $singleArg
      }
      """
    }

    private[this] def generateRepeatedTupleArgsBBUf(): c.Tree = {
      val items = fq"x <- $access"
      val tupleArgs = generateTupleArgBBUf(q"x")
      q"""
      for ($items) {
        $tupleArgs
      }
      """
    }

    val tree: c.Tree =
      if (isRepeated)
        if (isTupleType) generateRepeatedTupleArgsBBUf()
        else generateRepeatedArgsBBUf()
      else if (isTupleType) generateTupleArgBBUf(access)
      else generateSimpleArgBBUf(access)

    val sizeHint: Option[c.Tree] =
      if (isRepeated)
        if (isTupleType) Some(q"$tupleSize * $access.length")
        else Some(q"$access.length")
      else if (isTupleType) Some(q"$tupleSize")
      else None

    val knownSize: Option[Int] =
      if (isRepeated) None
      else if (isTupleType) Some(tupleSize)
      else Some(1)
  }

  private[this] def sizeHeader(args: List[ArgType]): c.Tree = {
    val argsSize = args.size
    val argSizeTrees = args flatMap (_.sizeHint)
    val definiteSize = q"${1 + (argsSize - argSizeTrees.length)}"
    argSizeTrees.foldLeft(definiteSize) { (tree, sizeHint) ⇒
      q"$tree + $sizeHint"
    }
  }

  private[this] def knownSizeHeader(args: List[ArgType], name: Array[Byte]): c.Tree = {
    val knownSize: Option[Int] = args.foldLeft(Some(1): Option[Int])((sz, arg) ⇒
      for (s1 ← sz; s2 ← arg.knownSize) yield s1 + s2)

    knownSize match {
      case None ⇒ q"""
        bb.writeByte($asterisk)
        bb.writeBytes(${sizeHeader(args)}.toString.getBytes(rx.redis.util.Utf8))
        bb.writeBytes($name)
        """
      case Some(x) ⇒
        val length = x.toString.getBytes(Utf8)
        val headerLength = 1 + length.length + name.length
        val header = new Array[Byte](headerLength)
        header(0) = asterisk.toByte
        System.arraycopy(length, 0, header, 1, length.length)
        System.arraycopy(name, 0, header, 1 + length.length, name.length)
        q"bb.writeBytes($header)"
    }
  }

  private[this] def nameHeaderResp(name: String): Array[Byte] = {
    val header = name.toUpperCase(Locale.ROOT).getBytes(Utf8)
    val length = name.length.toString.getBytes(Utf8)

    val hlen = header.length
    val llen = length.length

    val headerLength = 2 + 1 + llen + 2 + hlen + 2
    val headerBytes = new Array[Byte](headerLength)

    System.arraycopy(crlf, 0, headerBytes, 0, 2) // bb.writeBytes($crlf)
    headerBytes(2) = dollar.toByte // bb.writeByte($dollar)
    System.arraycopy(length, 0, headerBytes, 3, llen) // bb.writeBytes(${nameHeader(typeName)}.length.toString.getBytes())
    System.arraycopy(crlf, 0, headerBytes, 3 + llen, 2) // bb.writeBytes($crlf)
    System.arraycopy(header, 0, headerBytes, 5 + llen, header.length) // bb.writeBytes(${nameHeader(typeName)})
    System.arraycopy(crlf, 0, headerBytes, 5 + llen + hlen, 2) // bb.writeBytes($crlf)

    headerBytes
  }

  private[this] def macroImpl[A, TC[_], M[_]](implicit aTag: c.WeakTypeTag[A], tcaTag: c.WeakTypeTag[TC[A]], maTag: c.WeakTypeTag[M[A]]): c.Tree = {

    val tpe = aTag.tpe

    val finalTpe = appliedType(maTag.tpe.typeConstructor, tpe :: Nil)
    val typeName = tpe.typeSymbol.name.toString
    val objectName = c.freshName(TermName(typeName + "Writes"))

    val arguments = tpe.decls.toList.collect {
      case method: MethodSymbol if method.isCaseAccessor ⇒ new ArgType(tpe, tcaTag.tpe, method)
    }
    val argumentTrees = arguments map (_.tree)

    val generated = q"""
    object $objectName extends $finalTpe {
      def write(bb: $bbuf, value: $tpe): $bbuf = {
        ${knownSizeHeader(arguments, nameHeaderResp(typeName))}
        ..$argumentTrees
        bb
      }
    }
    $objectName
    """

    //    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
