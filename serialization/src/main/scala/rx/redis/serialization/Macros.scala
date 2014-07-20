package rx.redis.serialization

import rx.redis.util.Utf8

import java.util.Locale
import scala.language.higherKinds
import scala.reflect.macros.blackbox.Context


class Macros(val c: Context) {
  import c.universe._

  def writes[A: c.WeakTypeTag]: c.Tree = macroImpl[A, Bytes, Writes]

  private def fail(msg: String) =
    c.abort(c.enclosingPosition, "\n" + msg)

  private val ra = q"rx.redis.resp.RespArray"
  private val rb = q"rx.redis.resp.RespBytes"

  private class ArgType(tpe: Type, tc: Type, field: MethodSymbol) {
    val proper: Type = field.infoIn(tpe).resultType

    val x = q"x"
    val access = q"value.${field.name}"

    val isVarArgs =
      proper.typeArgs.size == 1 &&
      !proper.typeSymbol.isAbstract &&
      proper.getClass.getSimpleName.endsWith("ClassArgsTypeRef")

    val neededTypeClassType: Type =
      if (!isVarArgs) proper
      else proper.typeArgs.head

    val isTupleType =
      neededTypeClassType.typeArgs.nonEmpty
      neededTypeClassType.baseClasses.exists(_.asType.toType =:= typeOf[Product])

    val tupleSize =
      if (!isTupleType) q"1"
      else q"${neededTypeClassType.typeArgs.size}"

    val neededTypeClasses: List[Type] =
      if (!isTupleType)
        List(appliedType(tc.typeConstructor, neededTypeClassType :: Nil))
      else
        neededTypeClassType.typeArgs.map(t => appliedType(tc.typeConstructor, t :: Nil))

    def resolvedOneTypeClass(tc: Type): c.Tree = {
      val paramWrites = c.inferImplicitValue(tc)
      if (paramWrites == EmptyTree) {
        fail(
          "Missing implicit instance of " + tc + "\n" +
          "This is required to serialize instances of " + tc.typeArgs.head)
      }
      paramWrites
    }

    val resolvedTypeClasses: List[c.Tree] = {
      neededTypeClasses.map(resolvedOneTypeClass)
    }

    private def generateSingleArg(value: c.Tree, tc: c.Tree): c.Tree = {
      q"buf += $rb($tc.bytes($value))"
    }

    private def generateSimpleArg(value: c.Tree): c.Tree =
      generateSingleArg(value, resolvedTypeClasses.head)

    private def generateTupleArg(value: c.Tree): c.Tree = {
      val tuples =
        resolvedTypeClasses.zipWithIndex map { case (tcls, i) =>
          val tupleAccess = TermName(s"_${i + 1}")
          generateSingleArg(Select(value, tupleAccess), tcls)
        }
      q"..$tuples"
    }

    private def generateVariadicArgs(): c.Tree = {
      val items = fq"x <- $access"
      val singleArg = generateSimpleArg(x)
      q"""
      for ($items) {
        $singleArg
      }
      """
    }

    private def generateVariadicTupleArgs(): c.Tree = {
      val items = fq"x <- $access"
      val tupleArgs = generateTupleArg(x)
      q"""
      for ($items) {
        $tupleArgs
      }
      """
    }

    def generateArg(): c.Tree =
      if (!isVarArgs && !isTupleType)
        generateSimpleArg(access)
      else if (!isVarArgs)
        generateTupleArg(access)
      else if (!isTupleType)
        generateVariadicArgs()
      else
        generateVariadicTupleArgs()

    def generateSize(): Option[c.Tree] =
      if (!isVarArgs)
        if (!isTupleType) None
        else Some(tupleSize)
      else
        if (!isTupleType) Some(q"$access.size")
        else Some(q"$tupleSize * $access.size")
  }

  private def sizeHeader(args: List[ArgType]): c.Tree = {
    val argsSize = args.size
    val argSizeTrees = args flatMap (_.generateSize())
    val definiteSize = q"${1 + (argsSize - argSizeTrees.size)}"
    argSizeTrees.foldLeft(definiteSize) { (tree, sizeHint) =>
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
      case method: MethodSymbol if method.isCaseAccessor => new ArgType(tpe, tcaTag.tpe, method)
    }
    val argumentTrees = arguments map (_.generateArg())

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

    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
