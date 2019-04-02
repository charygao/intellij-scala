package org.jetbrains.plugins.scala
package lang
package psi
package impl
package statements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{PsiModifierListOwnerExt, ifReadAllowed}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.base._
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.lang.psi.controlFlow.impl.statements.ScPatternDefinitionCfgBuildingImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScPropertyStub
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScPropertyElementType
import org.jetbrains.plugins.scala.lang.psi.types.ScLiteralType
import org.jetbrains.plugins.scala.lang.psi.types.result._

/**
  * @author Alexander Podkhalyuzin
  */
final class ScPatternDefinitionImpl private[psi](stub: ScPropertyStub[ScPatternDefinition],
                                                 nodeType: ScPropertyElementType[ScPatternDefinition],
                                                 node: ASTNode)
  extends ScalaStubBasedElementImpl(stub, nodeType, node)
    with ScPatternDefinition with ScPatternDefinitionCfgBuildingImpl {

  override def toString: String = ifReadAllowed {
    val names = declaredNames
    if (names.isEmpty) "ScPatternDefinition"
    else "ScPatternDefinition: " + declaredNames.mkString(", ")
  }("")

  def bindings: Seq[ScBindingPattern] = Option(pList).map(_.bindings).getOrElse(Seq.empty)

  def declaredElements: Seq[ScBindingPattern] = bindings

  def `type`(): TypeResult = typeElement match {
    case Some(te) => te.`type`()
    case _ =>
      expr.toRight {
        new Failure("Cannot infer type without an expression")
      }.flatMap {
        _.`type`()
      }.map {
        case literalType: ScLiteralType if this.hasFinalModifier => literalType
        case t => ScLiteralType.widenRecursive(t)
      }
  }

  def assignment: Option[PsiElement] = Option(findChildByType[PsiElement](ScalaTokenTypes.tASSIGN))

  def expr: Option[ScExpression] = byPsiOrStub(findChild(classOf[ScExpression]))(_.bodyExpression)

  def typeElement: Option[ScTypeElement] = byPsiOrStub(findChild(classOf[ScTypeElement]))(_.typeElement)

  def pList: ScPatternList = getStubOrPsiChild(ScalaElementType.PATTERN_LIST)
}