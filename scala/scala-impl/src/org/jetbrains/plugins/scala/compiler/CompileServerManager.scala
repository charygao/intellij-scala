package org.jetbrains.plugins.scala
package compiler

import java.awt.Point
import java.awt.event.{ActionEvent, ActionListener, MouseEvent}

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, DefaultActionGroup, Separator}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.{StatusBar, StatusBarWidget, WindowManager}
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import javax.swing.{Icon, Timer}
import org.jetbrains.plugins.scala.compiler.CompileServerManager._
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project._
import org.jetbrains.plugins.scala.settings.ShowSettingsUtilImplExt

/**
 * @author Pavel Fatin
 */
final class CompileServerManager(project: Project) extends ProjectComponent {

  private val IconRunning = Icons.COMPILE_SERVER
  private val IconStopped = IconLoader.getDisabledIcon(IconRunning)

  private val timer = new Timer(1000, TimerListener)

  override def getComponentName: String = getClass.getSimpleName

  override def projectOpened() {
    if (ApplicationManager.getApplication.isUnitTestMode) return

    configureWidget()
    timer.setRepeats(true)
    timer.start()
  }

  override def projectClosed(): Unit = {
    if (ApplicationManager.getApplication.isUnitTestMode) return

    configureWidget()
    timer.stop()
  }

  private def applicable: Boolean = running ||
    ScalaCompileServerSettings.getInstance.COMPILE_SERVER_ENABLED &&
      project.hasScala

  private def running: Boolean = launcher.running

  private var installed = false

  private def launcher = CompileServerLauncher

  private def statusBar = Option(WindowManager.getInstance.getStatusBar(project))

  private def title = "Scala Compile Server"

  private def configureWidget(): Unit = {
    if (ApplicationManager.getApplication.isUnitTestMode) return

    (applicable, installed) match {
      case (false, false) => // do nothing
      case (true, true) => // do nothing
      case (true, false) =>
        statusBar.foreach { b =>
          b.addWidget(Widget, "before Position", project)
          installed = true
        }
      case (false, true) =>
        removeWidget()
    }
  }

  private def removeWidget(): Unit = {
    if (installed) {
      statusBar.foreach(_.removeWidget(Widget.ID))
      installed = false
    }
  }

  private def updateWidget(): Unit = {
    statusBar.foreach(_.updateWidget(Widget.ID))
  }

  private object Widget extends StatusBarWidget {
    override def ID = "Compile server"

    override def getPresentation: Presentation.type = Presentation

    override def install(statusBar: StatusBar): Unit = {}

    override def dispose(): Unit = {}

    object Presentation extends StatusBarWidget.IconPresentation {
      override def getIcon: Icon = if(running) IconRunning else IconStopped

      override def getClickConsumer: Consumer[MouseEvent] = ClickConsumer

      override def getTooltipText: String = title + launcher.port.map(_.formatted(" (TCP %d)")).getOrElse("")

      private object ClickConsumer extends Consumer[MouseEvent] {
        override def consume(t: MouseEvent): Unit = toggleList(t)
      }
    }
  }

  private def toggleList(e: MouseEvent): Unit = {
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val group = new DefaultActionGroup(Start, Stop, Separator.getInstance, Configure)
    val context = DataManager.getInstance.getDataContext(e.getComponent)
    val popup = JBPopupFactory.getInstance.createActionGroupPopup(title, group, context, mnemonics, true)
    val dimension = popup.getContent.getPreferredSize
    val at = new Point(0, -dimension.height)
    popup.show(new RelativePoint(e.getComponent, at))
  }

  private object Start extends AnAction("&Run", "Start compile server", AllIcons.Actions.Execute) with DumbAware {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(!launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      launcher.tryToStart(project)
  }

  private object Stop extends AnAction("&Stop", "Shutdown compile server", AllIcons.Actions.Suspend) with DumbAware {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      launcher.stop(e.getProject)
  }

  private object Configure extends AnAction("&Configure...", "Configure compile server", AllIcons.General.Settings) with DumbAware {
    override def actionPerformed(e: AnActionEvent): Unit =
      showCompileServerSettingsDialog(project)
  }

  private object TimerListener extends ActionListener {
    private var wasRunning: Option[Boolean] = None

    override def actionPerformed(e: ActionEvent): Unit = {
      val nowRunning = running

      configureWidget()

      if (installed || nowRunning)
        updateWidget()

      wasRunning = Some(nowRunning)

      val errors = launcher.errors()

      if (errors.nonEmpty) {
        Notifications.Bus.notify(new Notification(title, title, errors.mkString, NotificationType.ERROR), project)
      }
    }
  }
}

object CompileServerManager {

  def configureWidget(project: Project): Unit = {
    if (!project.isDisposed) {
      val instance = project.getComponent(classOf[CompileServerManager])
      instance.configureWidget()
    }
  }

  def showCompileServerSettingsDialog(project: Project, filter: String = ""): Unit =
    ShowSettingsUtilImplExt.showSettingsDialog(project, classOf[ScalaCompileServerForm], filter)

  def enableCompileServer(project: Project): Unit = {
    val settings = ScalaCompileServerSettings.getInstance()
    settings.COMPILE_SERVER_ENABLED = true
  }
}