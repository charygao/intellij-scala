package org.jetbrains.plugins.scala
package base
package libraryLoaders

import java.io.File
import java.{util => ju}

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{JarFileSystem, VirtualFile}
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.plugins.scala.project.{ModuleExt, ScalaLibraryProperties, ScalaLibraryType, template}
import org.junit.Assert._

case class ScalaSDKLoader(includeScalaReflect: Boolean = false) extends LibraryLoader {

  private object DependencyManager extends DependencyManagerBase {
    override protected val artifactBlackList = Set.empty[String]
  }

  import DependencyManagerBase._
  import ScalaSDKLoader._
  import template.Artifact.ScalaCompiler.{versionOf => ScalaCompilerVersion}

  protected def binaryDependencies(implicit version: ScalaVersion): List[DependencyDescription] =
    version match { // TODO maybe refactoring?
      case Scala_3_0 =>
        List(
          scalaCompilerDescription,
          scalaLibraryDescription,
          scalaLibraryDescription(Scala_2_13),
          DependencyDescription("ch.epfl.lamp", s"tasty-core_${version.major}", version.minor),
          DependencyDescription("ch.epfl.lamp", "dotty-interfaces", version.minor),
          DependencyDescription("org.scala-lang.modules", "scala-asm", "7.0.0-scala-1")
        )
      case _ =>
        if (includeScalaReflect)
          List(
            scalaCompilerDescription,
            scalaLibraryDescription,
            scalaReflectDescription
          )
        else
          List(
            scalaCompilerDescription,
            scalaLibraryDescription
          )
    }

  protected def sourcesDependency(implicit version: ScalaVersion): DependencyDescription =
    scalaLibraryDescription % Types.SRC

  final def sourceRoot(implicit version: ScalaVersion): VirtualFile = {
    val ResolvedDependency(_, file) = DependencyManager.resolveSingle(sourcesDependency)
    findJarFile(file)
  }

  override final def init(implicit module: Module, version: ScalaVersion): Unit = {
    val dependencies = binaryDependencies
    val resolved = DependencyManager.resolve(dependencies: _*)

    assertEquals(
      s"Failed to resolve scala sdk version $version, result:\n${resolved.mkString("\n")}",
      dependencies.size,
      resolved.size
    )

    val compilerClasspath = for {
      ResolvedDependency(_, file) <- resolved
      if file.exists()
    } yield file

    assertFalse(
      s"Local SDK files failed to verify for version $version:\n${resolved.mkString("\n")}",
      compilerClasspath.isEmpty
    )

    val classesRoots = {
      import scala.collection.JavaConverters._
      compilerClasspath.map(findJarFile).asJava
    }

    val library = PsiTestUtil.addProjectLibrary(
      module,
      s"scala-sdk-${version.minor}",
      classesRoots,
      ju.Collections.singletonList(sourceRoot)
    )

    Disposer.register(module, library)
    inWriteAction {
      val properties = ScalaLibraryProperties(
        ScalaCompilerVersion(compilerClasspath.head),
        compilerClasspath
      )

      val editor = new ExistingLibraryEditor(library, null)
      editor.setType(ScalaLibraryType())
      editor.setProperties(properties)
      editor.commit()

      val model = module.modifiableModel
      model.addLibraryEntry(library)
      model.commit()
    }
  }
}

object ScalaSDKLoader {

  private def findJarFile(file: File) =
    JarFileSystem.getInstance().refreshAndFindFileByPath {
      file.getCanonicalPath + "!/"
    }
}