package com.beachape.annotations

import scala.reflect.macros._

/**
 * Created by Lloyd on 6/13/15.
 */

/**
 * Does a check to make sure things make sense
 */
object ProvideMacro {

  def provideImp(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    annottees.map(_.tree) match {
      case (v @ ValDef(m @ Modifiers(fSet, modeName, annotations), name, tpt, rhs)) :: (cdef @ ClassDef(cdMods, cdName, cdTypeDefs, Template(parents, cdSelf, body))) :: xs => {
        c.warning(
          c.enclosingPosition,
          """
            |Looks like you're trying to use @provide with a (case) class constructor param, which is not yet supported.
            |The annotation will be skipped.
          """.stripMargin)
        c.Expr[Any](cdef)
      }
      case (d: ValOrDefDef) :: xs => checkedOrAbort(c)(d, None)
      case x => c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }

  private def checkedOrAbort(c: whitebox.Context)(valOrDef: c.universe.ValOrDefDef, maybeParents: Option[List[c.universe.Tree]]): c.Expr[Any] = {
    if (isAbstract(c)(valOrDef)) {
      c.abort(c.enclosingPosition, "Method itself is abstract.")
    }
    val ancestorMethods = allAncestorDeclarations(c)(valOrDef, maybeParents)
    if (ancestorMethods.isEmpty)
      c.abort(c.enclosingPosition, "Method has no parent class definitions.")
    else if (!ancestorMethods.forall(m => m.isAbstract || m.isAbstractOverride))
      c.abort(c.enclosingPosition, "Method has parent class definitions that are not abstract.")
    else
      c.Expr[Any](valOrDef)
  }

  private def ancestorTrees(c: whitebox.Context): List[c.universe.Tree] = {
    import c.universe._
    val ClassDef(_, _, _, Template(parents, _, _)) = c.enclosingClass
    parents
  }

  private def matchingSymbol(c: whitebox.Context)(valOrDef: c.universe.ValOrDefDef, unsafeTree: c.universe.Tree): c.universe.Symbol = {
    import c.universe._
    val tree = toTreeWithTpe(c)(unsafeTree)
    val (inputTypeParams, valOrDefInputTypess) = inputTypeParamsWithInputTypes(c)(valOrDef)
    tree.tpe.members.filter(_.name == valOrDef.name).find { member =>
      val memberTypeParams = member.typeSignature.typeParams.map(_.name)
      val memberTypeNameToInputTypeParams = memberTypeParams.zip(inputTypeParams).toMap
      val memberParamListTypess = member.typeSignature.paramLists.map { paramList =>
        val l2 = paramList.map { p =>
          val n = p.info.typeSymbol.name
          memberTypeNameToInputTypeParams.getOrElse(n, n)
        }
        l2
      }
      val paramListsTypesMatch = valOrDefInputTypess == memberParamListTypess
      member.isMethod && paramListsTypesMatch
    } getOrElse NoSymbol
  }

  private def inputTypeParamsWithInputTypes(c: whitebox.Context)(valOrDef: c.universe.ValOrDefDef): (List[c.universe.TypeName], List[List[c.universe.Name]]) = {
    import c.universe._
    valOrDef match {
      case ValDef(_, _, _, _) => (Nil, Nil)
      case DefDef(_, _, typeParamss, vParamss, _, rhs) => {
        val typeParamNames = typeParamss.map(_.name.toTypeName)
        val inputTypees = vParamss.map { paramList =>
          val l1 = paramList.map {
            case ValDef(_, _, Ident(s), _) => s
            case ValDef(_, _, tpt @ AppliedTypeTree(Ident(s), _), _) => s
            case ValDef(_, _, tpt, _) => {
              val typeTree = Typed(Ident(TermName("$qmark$qmark$qmark")), tpt)
              c.typecheck(q"??? : $tpt").tpe.typeSymbol.name
            }
          }
          l1
        }
        (typeParamNames, inputTypees)
      }
    }
  }

  // valOrDef.symbol.isAbstract does not work, so we have to roll our own by checking the tree
  private def isAbstract(c: whitebox.Context)(valOrDef: c.universe.ValOrDefDef): Boolean = {
    import c.universe._
    valOrDef match {
      case v @ ValDef(mods, _, _, rhs) => {
        !mods.hasFlag(Flag.CASEACCESSOR | Flag.PARAMACCESSOR) && rhs.isEmpty
      }
      case DefDef(_, _, _, _, _, rhs) => rhs.isEmpty
    }
  }

  private def toTreeWithTpe(c: whitebox.Context)(unsafeTree: c.universe.Tree): c.universe.Tree = {
    import c.universe._
    if (unsafeTree.tpe == null) {
      val castedTreeIdent = TypeApply(Select(Literal(Constant(null)), TermName("asInstanceOf")), List(unsafeTree))
      c.typecheck(castedTreeIdent)
    } else
      unsafeTree
  }

  private def allAncestorDeclarations(c: whitebox.Context)(valOrDef: c.universe.ValOrDefDef, maybeParents: Option[List[c.universe.Tree]]): List[c.universe.Symbol] = {
    import c.universe._
    val trees = maybeParents.getOrElse(ancestorTrees(c))
    trees.map(tree => matchingSymbol(c)(valOrDef, tree)).filterNot(_ == NoSymbol)
  }

}