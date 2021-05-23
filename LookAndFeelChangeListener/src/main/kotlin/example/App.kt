package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

private val check1 = ActionCommandCheckBox(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON)
private val check2 = ActionCommandCheckBox(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR)
private val textArea = JTextArea()

fun makeUI(): Component {
  append("MainPanel: init")
  UIManager.addPropertyChangeListener { e ->
    if ("lookAndFeel" == e.propertyName) {
      updateCheckBox("UIManager: propertyChange")
    }
  }
  EventQueue.invokeLater {
    val al = ActionListener { e ->
      append("JMenuItem: actionPerformed")
      (e.source as? JRadioButtonMenuItem)?.takeIf { it.isSelected }?.also {
        updateCheckBox("JMenuItem: actionPerformed: invokeLater")
      }
    }
    val menuBar = JMenuBar()
    menuBar.add(LookAndFeelUtil.createLookAndFeelMenu())
    textArea.rootPane.jMenuBar = menuBar
    descendants(menuBar)
      .filterIsInstance<JRadioButtonMenuItem>()
      .forEach { it.addActionListener(al) }
  }
  val np = JPanel(GridLayout(2, 1))
  np.add(check1)
  np.add(check2)
  val p = JPanel(GridLayout(2, 1))
  val tree = JTree()
  p.add(JScrollPane(tree))
  p.add(JScrollPane(textArea))
  return JPanel(BorderLayout()).also {
    it.add(np, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class TreeDraws(private val key: String) {
  DRAWS_FOCUS_BORDER_AROUND_ICON("Tree.drawsFocusBorderAroundIcon"),
  DRAW_DASHED_FOCUS_INDICATOR("Tree.drawDashedFocusIndicator");

  override fun toString() = key
}

private fun descendants(me: MenuElement): List<MenuElement> =
  me.subElements.flatMap { listOf(it) + descendants(it) }

private fun updateCheckBox(str: String) {
  EventQueue.invokeLater {
    append("--------\n$str")
    val focusKey = TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString()
    append(focusKey + ": " + UIManager.getBoolean(focusKey))
    check1.isSelected = UIManager.getBoolean(focusKey)
    val dashedKey = TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString()
    append(dashedKey + ": " + UIManager.getBoolean(dashedKey))
    check2.isSelected = UIManager.getBoolean(dashedKey)
  }
}

private fun append(str: String) {
  textArea.append("$str\n")
}

private class ActionCommandCheckBox(key: TreeDraws) : JCheckBox(key.toString()) {
  init {
    action = object : AbstractAction(key.toString()) {
      override fun actionPerformed(e: ActionEvent) {
        (e.source as? JCheckBox)?.also {
          UIManager.put(key.toString(), it.isSelected)
          SwingUtilities.updateComponentTreeUI(it.rootPane)
        }
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    updateCheckBox("JCheckBox: updateUI")
  }
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
