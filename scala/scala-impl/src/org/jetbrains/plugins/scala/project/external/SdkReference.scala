package org.jetbrains.plugins.scala.project.external

import java.io.File

import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable, Sdk}
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.serialization.PropertyMapping
import org.jetbrains.plugins.scala.extensions.inReadAction

import scala.language.implicitConversions
import scala.collection.JavaConverters._


/**
 * @author Nikolay Obedin
 * @since 7/14/15.
 */
sealed abstract class SdkReference

final case class JdkByName @PropertyMapping(Array("name")) (name: String) extends SdkReference
final case class JdkByHome @PropertyMapping(Array("home")) (home: File) extends SdkReference
final case class JdkByVersion @PropertyMapping(Array("version")) (version: String) extends SdkReference
final case class AndroidJdk @PropertyMapping(Array("version")) (version: String) extends SdkReference

object SdkUtils {
  def findProjectSdk(sdk: SdkReference): Option[Sdk] = {
    SdkResolver.EP_NAME.getExtensions
      .view
      .flatMap(_.sdkOf(sdk))
      .headOption
      .orElse {
        sdk match {
          case JdkByVersion(version) => findMostRecentJdk(sdk => Option(sdk.getVersionString).exists(_.contains(version)))
          case JdkByName(version) => findMostRecentJdk(_.getName == version).orElse(findMostRecentJdk(_.getName.contains(version)))
          case JdkByHome(homeFile) => findMostRecentJdk(sdk => FileUtil.comparePaths(homeFile.getCanonicalPath, sdk.getHomePath) == 0)
          case _ => None
        }
      }
  }

  private def findMostRecentJdk(condition: Sdk => Boolean): Option[Sdk] = {
    import scala.math.Ordering.comparatorToOrdering
    val sdkType = JavaSdk.getInstance()

    inReadAction {
      val jdks = ProjectJdkTable.getInstance()
        .getSdksOfType(JavaSdk.getInstance())
        .asScala
        .filter(condition)

      if (jdks.isEmpty) None
      else Option(jdks.max(comparatorToOrdering(sdkType.versionComparator())))
    }
  }

  def mostRecentJdk: Option[Sdk] =
    findMostRecentJdk(_ => true)

  def defaultJavaLanguageLevelIn(jdk: Sdk): Option[LanguageLevel] =
    Option(LanguageLevel.parse(jdk.getVersionString))

  def javaLanguageLevelFrom(javacOptions: Seq[String]): Option[LanguageLevel] = {
    for {
      sourcePos <- Option(javacOptions.indexOf("-source")).filterNot(_ == -1)
      sourceValue <- javacOptions.lift(sourcePos + 1)
      languageLevel <- Option(LanguageLevel.parse(sourceValue))
    } yield languageLevel
  }
}
