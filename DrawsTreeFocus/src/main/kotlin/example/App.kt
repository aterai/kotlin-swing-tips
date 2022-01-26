package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private var fbaCheck: JCheckBox? = null
private var dfiCheck: JCheckBox? = null

fun makeUI(): Component {
  val tree = JTree()

  fbaCheck = JCheckBox(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString()).also {
    it.isSelected = UIManager.getBoolean(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString())
    it.addActionListener { e ->
      val b = (e.source as? JCheckBox)?.isSelected == true
      UIManager.put(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString(), b)
      SwingUtilities.updateComponentTreeUI(tree)
    }
  }

  dfiCheck = JCheckBox(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString()).also {
    it.isSelected = UIManager.getBoolean(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString())
    it.addActionListener { e ->
      val b = (e.source as? JCheckBox)?.isSelected == true
      UIManager.put(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString(), b)
      SwingUtilities.updateComponentTreeUI(tree)
    }
  }

  val np = JPanel(GridLayout(2, 1))
  np.add(fbaCheck)
  np.add(dfiCheck)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  EventQueue.invokeLater { np.rootPane.jMenuBar = mb }

  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      fbaCheck?.isSelected = UIManager.getBoolean(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString())
      dfiCheck?.isSelected = UIManager.getBoolean(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString())
    }
  }
  p.add(np, BorderLayout.NORTH)
  p.add(JScrollPane(tree))
  p.preferredSize = Dimension(320, 240)
  return p
}

private enum class TreeDraws(private val key: String) {
  DRAWS_FOCUS_BORDER_AROUND_ICON("Tree.drawsFocusBorderAroundIcon"),
  DRAW_DASHED_FOCUS_INDICATOR("Tree.drawDashedFocusIndicator");

  override fun toString() = key
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
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
