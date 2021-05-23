package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val model = SpinnerNumberModel(5, 0, 10, 1)
  val spinner0 = JSpinner(model)

  UIManager.put("Spinner.arrowButtonSize", Dimension(60, 0))
  val spinner1 = JSpinner(model)

  val spinner2 = object : JSpinner(model) {
    override fun updateUI() {
      super.updateUI()
      descendants(this)
        .filterIsInstance<JButton>()
        .forEach {
          val d = it.preferredSize
          d.width = 40
          it.preferredSize = d
        }
    }
  }

  val spinner3 = object : JSpinner(model) {
    override fun setLayout(mgr: LayoutManager?) {
      super.setLayout(SpinnerLayout())
    }
  }

  val p = JPanel(GridLayout(2, 2))
  p.add(makeTitledPanel("default", spinner0))
  p.add(makeTitledPanel("Spinner.arrowButtonSize", spinner1))
  p.add(makeTitledPanel("setPreferredSize", spinner2))
  p.add(makeTitledPanel("setLayout", spinner3))

  val spinner4 = object : JSpinner(model) {
    override fun updateUI() {
      super.updateUI()
      font = font.deriveFont(32f)
      descendants(this)
        .filterIsInstance<JButton>()
        .forEach {
          val d = it.preferredSize
          d.width = 50
          it.preferredSize = d
        }
    }
  }

  val box = Box.createVerticalBox()
  box.add(p)
  box.add(makeTitledPanel("setPreferredSize + setFont", spinner4))

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class SpinnerLayout : LayoutManager {
  private var nextButton: Component? = null
  private var previousButton: Component? = null
  private var editor: Component? = null
  override fun addLayoutComponent(name: String, c: Component) {
    when (name) {
      "Next" -> nextButton = c
      "Previous" -> previousButton = c
      "Editor" -> editor = c
    }
  }

  override fun removeLayoutComponent(c: Component) {
    when (c) {
      nextButton -> nextButton = null
      previousButton -> previousButton = null
      editor -> editor = null
    }
  }

  override fun preferredLayoutSize(parent: Container): Dimension {
    val nextD = preferredSize(nextButton)
    val previousD = preferredSize(previousButton)
    val editorD = preferredSize(editor)

    // Force the editors height to be a multiple of 2
    editorD.height = (editorD.height + 1) / 2 * 2
    val size = Dimension(editorD.width, editorD.height)
    size.width += nextD.width.coerceAtLeast(previousD.width)
    val insets = parent.insets
    size.width += insets.left + insets.right
    size.height += insets.top + insets.bottom
    return size
  }

  override fun minimumLayoutSize(parent: Container) = preferredLayoutSize(parent)

  override fun layoutContainer(parent: Container) {
    val width = parent.width
    val height = parent.height
    val ins = parent.insets
    if (nextButton == null && previousButton == null) {
      setBounds(editor, ins.left, ins.top, width - ins.left - ins.right, height - ins.top - ins.bottom)
      return
    }

    // Dimension nextD = preferredSize(nextButton);
    // Dimension previousD = preferredSize(previousButton);
    val buttonsWidth = 100 // Math.max(nextD.width, previousD.width);
    val editorHeight = height - (ins.top + ins.bottom)

    // The arrowButtonInsets value is used instead of the JSpinner's
    // insets if not null. Defining this to be (0, 0, 0, 0) causes the
    // buttons to be aligned with the outer edge of the spinner's
    // border, and leaving it as "null" places the buttons completely
    // inside the spinner's border.
    val buttonInsets = UIManager.getInsets("Spinner.arrowButtonInsets") ?: ins

    // Deal with the spinner's componentOrientation property.
    val editorX: Int
    val editorWidth: Int
    val buttonsX: Int
    if (parent.componentOrientation.isLeftToRight) {
      editorX = ins.left
      editorWidth = width - ins.left - buttonsWidth - buttonInsets.right
      buttonsX = width - buttonsWidth - buttonInsets.right
    } else {
      buttonsX = buttonInsets.left
      editorX = buttonsX + buttonsWidth
      editorWidth = width - buttonInsets.left - buttonsWidth - ins.right
    }
    val nextY = buttonInsets.top
    val nextHeight = height / 2 + height % 2 - nextY
    val previousY = buttonInsets.top + nextHeight
    val previousHeight = height - previousY - buttonInsets.bottom
    setBounds(editor, editorX, ins.top, editorWidth, editorHeight)
    setBounds(nextButton, buttonsX, nextY, buttonsWidth, nextHeight)
    setBounds(previousButton, buttonsX, previousY, buttonsWidth, previousHeight)
  }

  companion object {
    private fun preferredSize(c: Component?) = c?.preferredSize ?: Dimension()

    private fun setBounds(c: Component?, x: Int, y: Int, width: Int, height: Int) {
      c?.setBounds(x, y, width, height)
    }
  }
}

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
