package org.jetbrains.plugins.scala.hierarchy

import java.util
import java.util.Comparator

import com.intellij.ide.hierarchy.JavaHierarchyUtil
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.newAPI.CallHierarchyBrowserBase._
import com.intellij.ide.hierarchy.newAPI.{CallHierarchyBrowserBase, HierarchyNodeDescriptor, HierarchyScopeType, HierarchyTreeStructure}
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, PsiMethod}
import com.intellij.ui.PopupHandler
import javax.swing.{JComponent, JTree}

final class ScalaCallHierarchyBrowser(project: Project, method: PsiMethod)
  extends CallHierarchyBrowserBase(project, method) {

  override protected def createTrees(type2TreeMap: util.Map[HierarchyScopeType, JTree]): Unit = {
    val group: ActionGroup = ActionManager.getInstance.getAction(IdeActions.GROUP_CALL_HIERARCHY_POPUP).asInstanceOf[ActionGroup]

    val tree1: JTree = createTree(false)
    PopupHandler.installPopupHandler(tree1, group, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP, ActionManager.getInstance)
    val forName: Class[_] = Class.forName("com.intellij.ide.hierarchy.CallHierarchyBrowserBase")
    val classes = forName.getDeclaredClasses
    var baseClass: Class[_] = null
    for (clazz <- classes if clazz.getName endsWith "BaseOnThisMethodAction")
      baseClass = clazz
    val constructor = baseClass.getConstructor()
    val inst: Any = constructor.newInstance()
    val method = baseClass.getMethod("registerCustomShortcutSet", classOf[ShortcutSet], classOf[JComponent])
    method.invoke(inst, ActionManager.getInstance.getAction(IdeActions.ACTION_CALL_HIERARCHY).getShortcutSet, tree1)
    type2TreeMap.put(getCalleeType, tree1)

    val tree2: JTree = createTree(false)
    PopupHandler.installPopupHandler(tree2, group, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP, ActionManager.getInstance)
    method.invoke(inst, ActionManager.getInstance.getAction(IdeActions.ACTION_CALL_HIERARCHY).getShortcutSet, tree2)
    type2TreeMap.put(getCallerType, tree2)
  }

  override protected def getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement =
    descriptor match {
      case nodeDescriptor: CallHierarchyNodeDescriptor => nodeDescriptor.getEnclosingElement
      case _ => null
    }

  override protected def getOpenFileElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement =
    descriptor match {
      case nodeDescriptor: CallHierarchyNodeDescriptor => nodeDescriptor.getTargetElement
      case _ => null
    }

  override protected def isApplicableElement(element: PsiElement): Boolean =
    element.isInstanceOf[PsiMethod]

  override def createHierarchyTreeStructure(typ: HierarchyScopeType, psiElement: PsiElement): HierarchyTreeStructure = {
    if (getCallerType.equals(typ))
      new ScalaCallerMethodsTreeStructure(myProject, psiElement.asInstanceOf[PsiMethod], getCurrentScopeType)
    else if (getCalleeType.equals(typ))
      new ScalaCalleeMethodsTreeStructure(myProject, psiElement.asInstanceOf[PsiMethod], getCurrentScopeType)
    else
      null
  }

  override protected def getComparator: Comparator[NodeDescriptor[_]] =
    JavaHierarchyUtil.getComparator(myProject)
}
