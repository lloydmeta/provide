package com.beachape.annotations

import scala.reflect.macros._

/**
 * Created by Lloyd on 6/13/15.
 */

/**
 * Does a check to make sure things make sense
 */
object ProvideMacro {

  def provideImp(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    annottees.map(_.tree) match {
      case (d: ValOrDefDef) :: Nil => {
        checkedOrAbort(c)(d)
      }
      case x => c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }

  private def checkedOrAbort(c: blackbox.Context)(valOrDef: c.universe.ValOrDefDef): c.Expr[Any] = {
    if (isAbstract(c)(valOrDef)) {
      c.abort(c.enclosingPosition, "Method itself is abstract.")
    }
    val ancestorMethods = allAncestorDeclarations(c)(valOrDef)
    if (ancestorMethods.isEmpty)
      c.abort(c.enclosingPosition, "Method has no parent class definitions.")
    else if (!ancestorMethods.forall(m => m.isAbstract || m.isAbstractOverride))
      c.abort(c.enclosingPosition, "Method has parent class definitions that are not abstract")
    else
      c.Expr[Any](valOrDef)
  }

  private def ancestorTrees(c: blackbox.Context): List[c.universe.Tree] = {
    import c.universe._
    val ClassDef(_, _, _, Template(parents, _, _)) = c.enclosingClass
    parents
  }

  private def matchingSymbol(c: blackbox.Context)(valOrDef: c.universe.ValOrDefDef, unsafeTree: c.universe.Tree): c.universe.Symbol = {
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

  private def inputTypeParamsWithInputTypes(c: blackbox.Context)(valOrDef: c.universe.ValOrDefDef): (List[c.universe.TypeName], List[List[c.universe.Name]]) = {
    import c.universe._
    valOrDef match {
      case ValDef(_, _, _, _) => (Nil, Nil)
      case DefDef(_, _, typeParamss, vParamss, _, rhs) => {
        val typeParamNames = typeParamss.map(_.name.toTypeName)
        val inputTypees = vParamss.map { paramList =>
          val l1 = paramList.map {
            case ValDef(_, _, Ident(s), _) => s
            case ValDef(_, _, tpt @ AppliedTypeTree(Ident(s), _), _) => s
          }
          l1
        }
        (typeParamNames, inputTypees)
      }
    }
  }

  // valOrDef.symbol.isAbstract does not work, so we have to roll our own by checking the tree
  private def isAbstract(c: blackbox.Context)(valOrDef: c.universe.ValOrDefDef): Boolean = {
    import c.universe._
    valOrDef match {
      case ValDef(_, _, _, rhs) => rhs.isEmpty
      case DefDef(_, _, _, _, _, rhs) => rhs.isEmpty
    }
  }

  private def toTreeWithTpe(c: blackbox.Context)(unsafeTree: c.universe.Tree): c.universe.Tree = {
    import c.universe._
    if (unsafeTree.tpe == null) {
      val castedTreeIdent = TypeApply(Select(Literal(Constant(null)), TermName("asInstanceOf")), List(unsafeTree))
      c.typecheck(castedTreeIdent)
    } else
      unsafeTree
  }

  private def allAncestorDeclarations(c: blackbox.Context)(valOrDef: c.universe.ValOrDefDef): List[c.universe.Symbol] = {
    import c.universe._
    val trees = ancestorTrees(c)
    trees.map(tree => matchingSymbol(c)(valOrDef, tree)).filterNot(_ == NoSymbol)
  }

}