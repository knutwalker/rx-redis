package rx.redis.serialization

import rx.redis.serialization.Writes.MWrites

import java.nio.charset.Charset
import java.util.Locale
import scala.language.higherKinds
import scala.reflect.macros.blackbox.Context


class Macros(val c: Context) {
  import c.universe._

  private val charset = Charset.defaultCharset()

  private def fail(msg: String) =
    c.abort(c.enclosingPosition, msg)

  private class ArgType(tpe: Type, tc: Type, field: MethodSymbol) {
    val proper: Type = field.infoIn(tpe).resultType
    val isVarArgs =
      proper.typeArgs.nonEmpty &&
        !proper.typeSymbol.isAbstract &&
        proper.getClass.getSimpleName.endsWith("ClassArgsTypeRef")

    val neededTypeClass: Type = {
      val tcType =
        if (!isVarArgs) proper
        else proper.typeArgs.head
      appliedType(tc.typeConstructor, tcType :: Nil)
    }

    val resolvedTypeClass: c.Tree = {
      val paramWrites = c.inferImplicitValue(neededTypeClass)
      if (paramWrites == EmptyTree) {
        fail("Could not find an implicit value for " + neededTypeClass)
      }
      paramWrites
    }

    private def generateSingleArg(value: c.Tree) =
      q"writeArg(buf, $value, $resolvedTypeClass)"

    private def generateArgOfFixedArity() =
      generateSingleArg(q"value.${field.name}")

    private def generateArgOfVariableArity() = {
      val enum = fq"x <- value.${field.name}"
      val singleArg = generateSingleArg(q"x")
      q"""
      for ($enum) {
        $singleArg
      }
      """
    }

    def generateArg(): c.Tree =
      if (!isVarArgs)
        generateArgOfFixedArity()
      else
        generateArgOfVariableArity()

    def generateSize(): Option[c.Tree] =
      if (!isVarArgs) None
      else Some(q"value.${field.name}.size")
  }

  def writes[A: c.WeakTypeTag]: c.Tree = macroImpl[A, Writes, MWrites]

  private def sizeHeader(args: List[ArgType]): c.Tree = {
    val argsSize = args.size
    val argSizeTrees = args flatMap (_.generateSize())
    val definiteSize = q"${1 + (argsSize - argSizeTrees.size)}"
    argSizeTrees.foldLeft(definiteSize) { (tree, sizeHint) =>
      q"$tree + $sizeHint"
    }
  }

  private def nameHeader(name: String): c.Tree = {
    val header = s"$$${name.getBytes(charset).length}\r\n${name.toUpperCase(Locale.ROOT)}\r\n"
    q"${header.getBytes(charset)}"
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

    val generated = q"""
    object $objectName extends $finalTpe {
      def sizeHint(value: $tpe): Long = ${sizeHeader(arguments)}
      def nameHeader: Array[Byte] = ${nameHeader(typeName)}
      def writeArgs(buf: io.netty.buffer.ByteBuf, value: $tpe): Unit = {
        ..$argumentTrees
      }
    }
    $objectName
    """

    c.info(c.enclosingPosition, "Generated code: \n\n" + showCode(generated), force = false)

    generated
  }
}
