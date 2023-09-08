package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val list = listOf<JComponent>(
    JMenuItem("Open"),
    JMenuItem("Save"),
    JSeparator(),
    JMenuItem(ExitAction()),
  )

  val popup = JPopupMenu()
  list.forEach { popup.add(it) }

  val menu = JMenu("File")
  list.forEach { menu.add(it) }

  val bar = JMenuBar()
  bar.add(menu)

  val toolBar = JToolBar()
  toolBar.add(JLabel("Floatable JToolBar:"))
  toolBar.add(Box.createGlue())
  toolBar.add(ExitAction())

  val tree = JTree()
  tree.componentPopupMenu = popup

  return JPanel(BorderLayout()).also {
    it.add(bar, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.add(toolBar, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ExitAction : AbstractAction("Exit") {
  override fun actionPerformed(e: ActionEvent) {
    val p = SwingUtilities.getUnwrappedParent(e.source as? Component) ?: return
    val root = when (p) {
      is JPopupMenu -> SwingUtilities.getRoot(p.invoker)
      is JToolBar -> getToolBarRoot(p)
      else -> SwingUtilities.getRoot(p)
    }
    (root as? Window)?.also { window ->
      window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }
  }

  private fun getToolBarRoot(b: JToolBar) = if ((b.ui as? BasicToolBarUI)?.isFloating == true) {
    SwingUtilities.getWindowAncestor(b).owner
  } else {
    SwingUtilities.getRoot(b)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
