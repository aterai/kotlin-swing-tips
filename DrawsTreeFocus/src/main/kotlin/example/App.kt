package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tree = JTree()

  val fbaCheck = JCheckBox(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString()).also {
    it.isSelected = UIManager.getBoolean(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString())
    it.addActionListener { e ->
      val b = (e.source as? JCheckBox)?.isSelected == true
      UIManager.put(TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString(), b)
      SwingUtilities.updateComponentTreeUI(tree)
    }
  }

  val dfiCheck = JCheckBox(TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString()).also {
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
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { np.rootPane.jMenuBar = mb }

  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      fbaCheck.isSelected = UIManager.getBoolean(
        TreeDraws.DRAWS_FOCUS_BORDER_AROUND_ICON.toString(),
      )
      dfiCheck.isSelected = UIManager.getBoolean(
        TreeDraws.DRAW_DASHED_FOCUS_INDICATOR.toString(),
      )
    }
  }
  p.add(np, BorderLayout.NORTH)
  p.add(JScrollPane(tree))
  p.preferredSize = Dimension(320, 240)
  return p
}

private enum class TreeDraws(private val key: String) {
  DRAWS_FOCUS_BORDER_AROUND_ICON("Tree.drawsFocusBorderAroundIcon"),
  DRAW_DASHED_FOCUS_INDICATOR("Tree.drawDashedFocusIndicator"),
  ;

  override fun toString() = key
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
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
