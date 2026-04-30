package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun createUI(): Component {
  val sideMenuBox = Box.createVerticalBox()
  sideMenuBox.isOpaque = true
  sideMenuBox.background = Color(0xB4_B4_FF)
  sideMenuBox.border = BorderFactory.createEmptyBorder(10, 5, 5, 5)
  initAccordionSections().forEach {
    sideMenuBox.add(it)
    sideMenuBox.add(Box.createVerticalStrut(5))
  }
  sideMenuBox.add(Box.createVerticalGlue())

  val scroll = object : JScrollPane(sideMenuBox) {
    override fun updateUI() {
      super.updateUI()
      horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBar.unitIncrement = 25
    }
  }

  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).also {
    it.leftComponent = scroll
    it.rightComponent = JPanel()
    it.resizeWeight = .5
    it.dividerLocation = 160
    it.dividerSize = 2
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initAccordionSections() = listOf(
  object : AccordionSectionPanel("System Tasks") {
    override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
      listOf("111", "222222222")
        .map { title -> JCheckBox(title) }
        .forEach { check ->
          check.isOpaque = false
          p.add(check)
        }
    }
  },
  object : AccordionSectionPanel("Other Places") {
    override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
      listOf("Desktop", "My Network Places", "My Documents", "Shared Documents")
        .map { title -> JLabel(title) }
        .forEach { label -> p.add(label) }
    }
  },
  object : AccordionSectionPanel("Details") {
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
  },
)

abstract class AccordionSectionPanel(
  private val title: String,
) : JPanel(BorderLayout()) {
  private val headerLabel = object : JLabel("▼ $title") {
    private val bgc = Color(0xC8_C8_FF)

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val fw = width.toFloat()
      val fh = height.toFloat()
      g2.paint = GradientPaint(50f, 0f, Color.WHITE, fw, fh, bgc)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  private val contentPanel: JPanel

  init {
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        toggleExpansion()
      }
    }
    headerLabel.addMouseListener(ml)
    headerLabel.foreground = Color.BLUE
    headerLabel.border = BorderFactory.createEmptyBorder(2, 5, 2, 2)
    add(headerLabel, BorderLayout.NORTH)

    contentPanel = makePanel()
    contentPanel.isVisible = false
    contentPanel.isOpaque = true
    contentPanel.background = Color(0xF0_F0_FF)
    val outside = BorderFactory.createMatteBorder(0, 2, 2, 2, Color.WHITE)
    val inside = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    contentPanel.border = BorderFactory.createCompoundBorder(outside, inside)
    add(contentPanel)
  }

  abstract fun makePanel(): JPanel

  final override fun add(comp: Component?): Component = super.add(comp)

  final override fun add(comp: Component, constraints: Any?) {
    super.add(comp, constraints)
  }

  override fun getPreferredSize(): Dimension {
    val d = headerLabel.getPreferredSize()
    if (contentPanel.isVisible) {
      d.height += contentPanel.getPreferredSize().height
    }
    return d
  }

  override fun getMaximumSize() = preferredSize.also {
    it.width = Short.MAX_VALUE.toInt()
  }

  protected fun toggleExpansion() {
    contentPanel.also {
      it.isVisible = !it.isVisible
      val mark = if (it.isVisible) "△" else "▼"
      headerLabel.text = "$mark $title"
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
