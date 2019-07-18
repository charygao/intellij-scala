package org.jetbrains.plugins.scala
package codeInspection
package syntacticClarification

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration

/**
  * Author: Svyatoslav Ilinskiy
  * Date: 10/14/15.
  */
class AutoTuplingInspectionTest extends ScalaQuickFixTestBase {

  import EditorTestUtil.{SELECTION_END_TAG => END, SELECTION_START_TAG => START}

  override protected val classOfInspection: Class[_ <: LocalInspectionTool] =
    classOf[AutoTuplingInspection]

  override protected val description: String =
    AutoTuplingInspection.message

  val hint = MakeTuplesExplicitFix.hint


  override protected def setUp(): Unit = {
    super.setUp()

    // experimental activates SAM in 2.11
    if (version == Scala_2_11) {
      val defaultProfile = ScalaCompilerConfiguration.instanceIn(getProject).defaultProfile
      val newSettings = defaultProfile.getSettings
      newSettings.experimental = true
      defaultProfile.setSettings(newSettings)
    }
  }

  def testBasic(): Unit = {
    val text =
      s"""
        |def foo(a: Any) = {}
        |foo$START(1, 2)$END
      """.stripMargin
    checkTextHasError(text)

    val code =
      """
        |def foo(a: Any) = {}
        |foo(<caret>1, 2)
      """.stripMargin
    val result =
      """
        |def foo(a: Any) = {}
        |foo((1, 2))
      """.stripMargin
    testQuickFix(code, result, hint)
  }

  def testSAMNotHighlighted(): Unit = {
    val text =
      """
        |trait SAM {
        |  def foo(): Int
        |}
        |def foo(s: SAM) = ()
        |foo(() => 2)
      """.stripMargin
    checkTextHasNoErrors(text)
  }

  def testAutoTupledSAMsAreHighlighted(): Unit = {
    val text =
      s"""
         |def foo(a: Any) = {}
         |foo$START(() => println("foo"),  () => 2)$END
      """.stripMargin
    checkTextHasError(text)

    val code =
      """
        |def foo(a: Any) = {}
        |foo(<caret>() => println("foo"),  () => 2)
      """.stripMargin
    val result =
      """
        |def foo(a: Any) = {}
        |foo((() => println("foo"), () => 2))
      """.stripMargin
    testQuickFix(code, result, hint)
  }

  def testSAMNotHighlightedWhenTypesOfParametersOfAnonymousFunctionAreInferred(): Unit = {
    val text =
      """
        |trait SAM {
        |  def foo(s: String): Char
        |}
        |def bar(sam: SAM) = s.foo("foo")
        |
        |bar(j => j.charAt(0))
      """.stripMargin
    checkTextHasNoErrors(text)
  }

}
