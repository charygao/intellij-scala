package org.jetbrains.plugins.scala.lang.resolve2.bug

import org.jetbrains.plugins.scala.lang.resolve2.ResolveTestBase
import junit.framework.Assert


/**
 * Pavel.Fatin, 02.02.2010
 */

class BugTest extends ResolveTestBase {
  override def getTestDataPath: String = {
    super.getTestDataPath + "bug/"
  }

  //TODO
//  def testBug1 = doTest

  //TODO
//  def testIncomplete = doTest

  def testSimplePrivateAccess = doTest
  def testPrivateThis = doTest
  def testProtectedThis = doTest
  //TODO
//  def testGetOrElse = doTest
}