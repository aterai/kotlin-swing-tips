package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val accordion = Box.createVerticalBox()
  accordion.isOpaque = true
  accordion.background = Color(0xB4_B4_FF)
  accordion.border = BorderFactory.createEmptyBorder(10, 5, 5, 5)
  makeExpansionPanelList().forEach {
    accordion.add(it)
    accordion.add(Box.createVerticalStrut(5))
  }
  accordion.add(Box.createVerticalGlue())

  val scroll = JScrollPane(accordion)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scroll.verticalScrollBar.unitIncrement = 25

  val split = JSplitPane()
  split.resizeWeight = .5
  split.dividerSize = 2
  split.leftComponent = scroll
  split.rightComponent = JPanel()

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeExpansionPanelList() = listOf(
  object : AbstractExpansionPanel("System Tasks") {
    override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
      listOf("111", "222222222")
        .map { title -> JCheckBox(title) }
        .forEach { check ->
          check.isOpaque = false
          p.add(check)
        }
    }
  },
  object : AbstractExpansionPanel("Other Places") {
    override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
      listOf("Desktop", "My Network Places", "My Documents", "Shared Documents")
        .map { title -> JLabel(title) }
        .forEach { label -> p.add(label) }
    }
  },
  object : AbstractExpansionPanel("Details") {
    override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
      val bg = ButtonGroup()
      listOf("aaa", "bbb", "ccc", "ddd")
        .map { title -> JRadioButton(title) }
        .forEach { radio ->
          radio.isSelected = p.componentCount == 0
          radio.isOpaque = false
          p.add(radio)
          bg.add(radio)
        }
    }
  }
)

abstract class AbstractExpansionPanel(private val title: String) : JPanel(BorderLayout()) {
  private val titleBar = object : JLabel("▼ $title") {
    private val bgc = Color(0xC8_C8_FF)
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = GradientPaint(50f, 0f, Color.WHITE, width.toFloat(), height.toFloat(), bgc)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  private val panel: JPanel

  abstract fun makePanel(): JPanel

  init {
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        initPanel()
      }
    }
    titleBar.addMouseListener(ml)
    titleBar.foreground = Color.BLUE
    titleBar.border = BorderFactory.createEmptyBorder(2, 5, 2, 2)
    add(titleBar, BorderLayout.NORTH)

    panel = makePanel()
    panel.isVisible = false
    panel.isOpaque = true
    panel.background = Color(0xF0_F0_FF)
    val outBorder = BorderFactory.createMatteBorder(0, 2, 2, 2, Color.WHITE)
    val inBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    val border = BorderFactory.createCompoundBorder(outBorder, inBorder)
    panel.border = border
    add(panel)
  }

  override fun getPreferredSize(): Dimension? = titleBar.preferredSize?.also {
    panel.takeIf { it.isVisible }?.also { p ->
      it.height += p.preferredSize.height
    }
  }

  override fun getMaximumSize() = preferredSize?.also {
    it.width = Short.MAX_VALUE.toInt()
  }

  protected fun initPanel() {
    panel.also {
      it.isVisible = !it.isVisible
      val mark = if (it.isVisible) "△" else "▼"
      titleBar.text = "$mark $title"
      revalidate()
      EventQueue.invokeLater { it.scrollRectToVisible(it.bounds) }
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
