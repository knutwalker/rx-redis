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

import concurrent.duration.{ Deadline, FiniteDuration }

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
  val utf8 = q"rx.redis.util.Utf8"

  private[this] val asterisk = '*'.toInt
  private[this] val dollar = '$'.toInt
  private[this] val crlf = Array('\r'.toByte, '\n'.toByte)

  object CodeGens {

    private[serialization] def genByteArrayCode(value: c.Tree, bb: c.TermName) =
      q"""
      $bb.writeBytes($value.length.toString.getBytes($utf8))
      $bb.writeBytes($crlf)
      $bb.writeBytes($value)
      """

    private[serialization] def genStringCode(value: c.Tree, bb: c.TermName) =
      q"""
      $bb.writeBytes($value.length.toString.getBytes($utf8))
      $bb.writeBytes($crlf)
      $bb.writeBytes($value.getBytes($utf8))
      """

    private[serialization] def genLongCode(value: c.Tree, bb: c.TermName) = {
      val term = c.freshName(TermName("longValue"))
      q"""
      val $term = $value.toString
      $bb.writeBytes($term.length.toString.getBytes($utf8))
      $bb.writeBytes($crlf)
      $bb.writeBytes($term.getBytes($utf8))
      """
    }

    private[serialization] def genDurationCode(value: c.Tree, bb: c.TermName) = {
      val term = c.freshName(TermName("durationValue"))
      q"""
      val $term = $value.toSeconds.toString
      $bb.writeBytes($term.length.toString.getBytes($utf8))
      $bb.writeBytes($crlf)
      $bb.writeBytes($term.getBytes($utf8))
      """
    }

    private[serialization] def genDeadlineCode(value: c.Tree, bb: c.TermName) = {
      val term = c.freshName(TermName("deadlineValue"))
      q"""
      val $term = (($value.timeLeft.toMillis + java.lang.System.currentTimeMillis()) / 1000).toString
      $bb.writeBytes($term.length.toString.getBytes($utf8))
      $bb.writeBytes($crlf)
      $bb.writeBytes($term.getBytes($utf8))
      """
    }

    private[serialization] def genTypeClassCode(value: c.Tree, tc: c.Tree, bb: c.TermName) = {
      val knownSize = c.freshName(TermName("knownSize"))
      val contentHolder = c.freshName(TermName("contentHolder"))
      val content = c.freshName(TermName("content"))
      q"""
      if ($tc.hasKnownSize) {
        val $knownSize = $tc.knownSize($value)
        $bb.writeBytes($knownSize.toString.getBytes($utf8))
        $bb.writeBytes($crlf)
        $tc.toByteBuf($bb, $value)
      } else {
        val $contentHolder = $bb.alloc().buffer()
        val $content = $tc.toByteBuf($contentHolder, $value)
        $bb.writeBytes($content.readableBytes().toString.getBytes($utf8))
        $bb.writeBytes($crlf)
        $bb.writeBytes($content)
      }
      """
    }
  }

  private class ArgType(tpe: c.Type, tc: c.Type, field: MethodSymbol, valueName: c.TermName, bb: c.TermName) {
    private[this] val proper = field.infoIn(tpe).resultType
    private[this] val access = q"$valueName.${field.name}"

    private[this] val isRepeated =
      proper.resultType.typeSymbol == definitions.RepeatedParamClass

    private[this] val concreteType =
      if (!isRepeated) proper
      else proper.typeArgs.head

    private[this] val isTupleType =
      concreteType.typeArgs.nonEmpty &&
        definitions.TupleClass.seq.exists(t ⇒ concreteType.baseType(t) != NoType)

    private[this] val tupleSize =
      if (!isTupleType) 1
      else concreteType.typeArgs.size

    private[this] def typeClassType(underlying: c.Type) =
      appliedType(tc.typeConstructor, underlying :: Nil)

    private[this] def resolveTypeClass(tc: c.Type) = {
      val resolved = c.inferImplicitValue(tc)
      if (resolved == EmptyTree) {
        fail(
          s"""Missing implicit instance of $tc
             |This is required to serialize instances of ${tc.typeArgs.head}""".stripMargin)
      }
      resolved
    }

    private[this] def generateSingleArg(value: c.Tree, forType: c.Type) = {
      import CodeGens._
      val contentCode = forType match {
        case s if s =:= typeOf[Array[Byte]]    ⇒ genByteArrayCode(value, bb)
        case s if s =:= typeOf[String]         ⇒ genStringCode(value, bb)
        case s if s =:= typeOf[Long]           ⇒ genLongCode(value, bb)
        case s if s =:= typeOf[Int]            ⇒ genLongCode(value, bb)
        case s if s =:= typeOf[FiniteDuration] ⇒ genDurationCode(value, bb)
        case s if s =:= typeOf[Deadline]       ⇒ genDeadlineCode(value, bb)
        case _ ⇒
          val tc = resolveTypeClass(typeClassType(forType))
          genTypeClassCode(value, tc, bb)
      }
      q"""
      $bb.writeByte($dollar)
      $contentCode
      $bb.writeBytes($crlf)
      """
    }

    private[this] def generateSimpleArg(value: c.Tree) =
      generateSingleArg(value, concreteType)

    private[this] def generateTupleArg(value: c.Tree) = {
      val tuples =
        concreteType.typeArgs.zipWithIndex map {
          case (tplTpe, i) ⇒
            val tupleAccess = TermName(s"_${i + 1}")
            generateSingleArg(Select(value, tupleAccess), tplTpe)
        }
      q"..$tuples"
    }

    private[this] def generateRepeatedArgs() = {
      val x = c.freshName(TermName("x"))
      val items = fq"$x <- $access"
      val singleArg = generateSimpleArg(q"$x")
      q"""
      for ($items) {
        $singleArg
      }
      """
    }

    private[this] def generateRepeatedTupleArgs() = {
      val x = c.freshName(TermName("x"))
      val items = fq"$x <- $access"
      val tupleArgs = generateTupleArg(q"$x")
      q"""
      for ($items) {
        $tupleArgs
      }
      """
    }

    val tree: Tree =
      if (isRepeated)
        if (isTupleType) generateRepeatedTupleArgs()
        else generateRepeatedArgs()
      else if (isTupleType) generateTupleArg(access)
      else generateSimpleArg(access)

    val sizeHint: Option[Tree] =
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

  private[this] def sizeHeader(args: List[ArgType]) = {
    val argsSize = args.size
    val argSizeTrees = args flatMap (_.sizeHint)
    val definiteSize = q"${1 + (argsSize - argSizeTrees.length)}"
    argSizeTrees.foldLeft(definiteSize) { (tree, sizeHint) ⇒
      q"$tree + $sizeHint"
    }
  }

  private[this] def knownSizeHeader(args: List[ArgType], name: Array[Byte], bb: c.TermName) = {
    val knownSize: Option[Int] = args.foldLeft(Some(1): Option[Int])((sz, arg) ⇒
      for (s1 ← sz; s2 ← arg.knownSize) yield s1 + s2)

    knownSize match {
      case None ⇒ q"""
        $bb.writeByte($asterisk)
        $bb.writeBytes(${sizeHeader(args)}.toString.getBytes($utf8))
        $bb.writeBytes($name)
        """
      case Some(x) ⇒
        val length = x.toString.getBytes(Utf8)
        val header = Array(asterisk.toByte) ++ length ++ name
        q"$bb.writeBytes($header)"
    }
  }

  private[this] def nameHeaderResp(name: String): Array[Byte] = {
    val header = name.toUpperCase(Locale.ROOT).getBytes(Utf8)
    val length = name.length.toString.getBytes(Utf8)
    crlf ++ Array(dollar.toByte) ++ length ++ crlf ++ header ++ crlf
  }

  private[this] def macroImpl[A, TC[_], M[_]](implicit aTag: c.WeakTypeTag[A], tcaTag: c.WeakTypeTag[TC[A]], maTag: c.WeakTypeTag[M[A]]): c.Tree = {

    val tpe = aTag.tpe

    val finalTpe = appliedType(maTag.tpe.typeConstructor, tpe :: Nil)
    val typeName = tpe.typeSymbol.name.toString

    val objectName = c.freshName(TermName(typeName + "Writes"))
    val valueParam = c.freshName(TermName("value"))
    val bbParam = c.freshName(TermName("bb"))

    val arguments = tpe.decls.toList.collect {
      case method: MethodSymbol if method.isCaseAccessor ⇒ new ArgType(tpe, tcaTag.tpe, method, valueParam, bbParam)
    }
    val argumentTrees = arguments map (_.tree)

    val generated = q"""
    object $objectName extends $finalTpe {
      def write($bbParam: $bbuf, $valueParam: $tpe): $bbuf = {
        ${knownSizeHeader(arguments, nameHeaderResp(typeName), bbParam)}
        ..$argumentTrees
        $bbParam
      }
    }
    $objectName
    """

    //    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
