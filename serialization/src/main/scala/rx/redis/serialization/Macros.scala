/*
 * Copyright 2014 Paul Horn
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

import rx.redis.util.Utf8

import java.util.Locale
import scala.language.higherKinds
import scala.reflect.macros.blackbox.Context

class Macros(val c: Context) {
  import c.universe._

  def writes[A: c.WeakTypeTag]: c.Tree = macroImpl[A, BytesFormat, Writes]

  private def fail(msg: String) =
    c.abort(c.enclosingPosition, "\n" + msg)

  private val ra = q"rx.redis.resp.RespArray"
  private val rb = q"rx.redis.resp.RespBytes"

  private class ArgType(tpe: Type, tc: Type, field: MethodSymbol) {
    private val proper: Type = field.infoIn(tpe).resultType
    private val access = q"value.${field.name}"

    private val isRepeated =
      proper.resultType.typeSymbol == definitions.RepeatedParamClass

    private val neededTypeClassType: Type =
      if (!isRepeated) proper
      else proper.typeArgs.head

    private val isTupleType =
      neededTypeClassType.typeArgs.nonEmpty &&
        definitions.TupleClass.seq.exists(t ⇒ neededTypeClassType.baseType(t) != NoType)

    private val tupleSize =
      if (!isTupleType) q"1"
      else q"${neededTypeClassType.typeArgs.size}"

    private val neededTypeClasses: List[Type] =
      if (!isTupleType)
        List(appliedType(tc.typeConstructor, neededTypeClassType :: Nil))
      else
        neededTypeClassType.typeArgs.map(t ⇒ appliedType(tc.typeConstructor, t :: Nil))

    private def resolvedOneTypeClass(tc: Type): c.Tree = {
      val paramWrites = c.inferImplicitValue(tc)
      if (paramWrites == EmptyTree) {
        fail(
          "Missing implicit instance of " + tc + "\n" +
            "This is required to serialize instances of " + tc.typeArgs.head)
      }
      paramWrites
    }

    private val resolvedTypeClasses: List[c.Tree] = {
      neededTypeClasses.map(resolvedOneTypeClass)
    }

    private def generateSingleArg(value: c.Tree, tc: c.Tree): c.Tree = {
      q"buf += $rb($tc.bytes($value))"
    }

    private def generateSimpleArg(value: c.Tree): c.Tree =
      generateSingleArg(value, resolvedTypeClasses.head)

    private def generateTupleArg(value: c.Tree): c.Tree = {
      val tuples =
        resolvedTypeClasses.zipWithIndex map {
          case (tcls, i) ⇒
            val tupleAccess = TermName(s"_${i + 1}")
            generateSingleArg(Select(value, tupleAccess), tcls)
        }
      q"..$tuples"
    }

    private def generateRepeatedArgs(): c.Tree = {
      val items = fq"x <- $access"
      val singleArg = generateSimpleArg(q"x")
      q"""
      for ($items) {
        $singleArg
      }
      """
    }

    private def generateRepeatedTupleArgs(): c.Tree = {
      val items = fq"x <- $access"
      val tupleArgs = generateTupleArg(q"x")
      q"""
      for ($items) {
        $tupleArgs
      }
      """
    }

    val tree: c.Tree =
      if (!isRepeated && !isTupleType)
        generateSimpleArg(access)
      else if (!isRepeated)
        generateTupleArg(access)
      else if (!isTupleType)
        generateRepeatedArgs()
      else
        generateRepeatedTupleArgs()

    val sizeHint: Option[c.Tree] =
      if (!isRepeated)
        if (!isTupleType) None
        else Some(tupleSize)
      else if (!isTupleType) Some(q"$access.size")
      else Some(q"$tupleSize * $access.size")
  }

  private def sizeHeader(args: List[ArgType]): c.Tree = {
    val argsSize = args.size
    val argSizeTrees = args flatMap (_.sizeHint)
    val definiteSize = q"${1 + (argsSize - argSizeTrees.size)}"
    argSizeTrees.foldLeft(definiteSize) { (tree, sizeHint) ⇒
      q"$tree + $sizeHint"
    }
  }

  private def nameHeader(name: String): c.Tree = {
    val header = name.toUpperCase(Locale.ROOT).getBytes(Utf8)
    q"$header"
  }

  private def macroImpl[A, TC[_], M[_]](implicit aTag: c.WeakTypeTag[A], tcaTag: c.WeakTypeTag[TC[A]], maTag: c.WeakTypeTag[M[A]]): c.Tree = {

    val tpe = aTag.tpe

    val finalTpe = appliedType(maTag.tpe.typeConstructor, tpe :: Nil)
    val typeName = tpe.typeSymbol.name.toString
    val objectName = c.freshName(TermName(typeName + "Writes"))

    val arguments = tpe.decls.toList.collect {
      case method: MethodSymbol if method.isCaseAccessor ⇒ new ArgType(tpe, tcaTag.tpe, method)
    }
    val argumentTrees = arguments map (_.tree)

    val dt = tq"rx.redis.resp.DataType"
    val generated = q"""
    object $objectName extends $finalTpe {
      def write(value: $tpe): $dt = {
        val buf = new scala.collection.mutable.ArrayBuffer[$dt](${sizeHeader(arguments)})
        buf += $rb(${nameHeader(typeName)})
        ..$argumentTrees
        $ra(buf.toArray)
      }
    }
    $objectName
    """

    //    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
