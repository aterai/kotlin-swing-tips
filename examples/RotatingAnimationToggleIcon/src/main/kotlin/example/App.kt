package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs

fun makeUI(): Component {
  val accordion = Box.createVerticalBox()
  accordion.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  makeExpansionPanelList().forEach {
    accordion.add(it)
    accordion.add(Box.createVerticalStrut(5))
  }
  accordion.add(Box.createVerticalGlue())
  val scroll = JScrollPane(accordion)
  scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  scroll.getVerticalScrollBar().setUnitIncrement(25)
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, JPanel())
  split.setResizeWeight(.5)
  split.setDividerLocation(160)
  split.setDividerSize(2)
  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeExpansionPanelList() = listOf(
  object : AbstractExpansionPanel("System Tasks") {
    override fun makePanel(): JPanel {
      val p = JPanel(GridLayout(0, 1))
      listOf("1111", "222222")
        .map { JCheckBox(it) }
        .forEach { p.add(it) }
      return p
    }
  },
  object : AbstractExpansionPanel("Other Places") {
    override fun makePanel(): JPanel {
      val p = JPanel(GridLayout(0, 1))
      listOf("Desktop", "My Network Places", "My Documents", "Shared Documents")
        .map { JLabel(it) }
        .forEach { p.add(it) }
      return p
    }
  },
  object : AbstractExpansionPanel("Details") {
    override fun makePanel(): JPanel {
      val p = JPanel(GridLayout(0, 1))
      val bg = ButtonGroup()
      listOf("aaa", "bbb", "ccc", "ddd")
        .map { JRadioButton(it) }
        .forEach {
          it.setSelected(p.componentCount == 0)
          p.add(it)
          bg.add(it)
        }
      return p
    }
  },
)

private abstract class AbstractExpansionPanel(
  title: String,
) : JPanel(BorderLayout()) {
  private val toggleButton = object : JToggleButton() {
    override fun updateUI() {
      super.updateUI()
      setContentAreaFilled(false)
      setBorderPainted(false)
      setFocusable(false)
    }
  }
  private val button = object : JButton() {
    override fun updateUI() {
      super.updateUI()
      setLayout(BoxLayout(this, BoxLayout.X_AXIS))
      setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0))
    }
  }
  private val panel = makePanel()

  init {
    val arrowIcon = UIManager.getIcon("Menu.arrowIcon")
    val animIcon = AnimatableIcon(arrowIcon, toggleButton)
    toggleButton.setIcon(animIcon)
    toggleButton.addActionListener { rotate(animIcon) }
    button.add(JLabel(title))
    button.add(Box.createHorizontalGlue())
    button.add(toggleButton)
    button.addActionListener {
      toggleButton.setSelected(!toggleButton.isSelected)
      EventQueue.invokeLater { rotate(animIcon) }
    }
    add(button, BorderLayout.NORTH)
    panel.isVisible = false
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    add(panel)
  }

  private fun rotate(animeIcon: AnimatableIcon) {
    if (toggleButton.isSelected) {
      animeIcon.animateTo(90.0)
      panel.isVisible = true
    } else {
      animeIcon.animateTo(0.0)
      panel.isVisible = false
    }
    revalidate()
    EventQueue.invokeLater { panel.scrollRectToVisible(panel.bounds) }
  }

  abstract fun makePanel(): JPanel

  override fun getPreferredSize(): Dimension {
    val d = button.getPreferredSize()
    if (panel.isVisible) {
      d.height += panel.getPreferredSize().height
    }
    return d
  }

  override fun getMaximumSize(): Dimension {
    val d = getPreferredSize()
    d.width = Short.MAX_VALUE.toInt()
    return d
  }
}

private class AnimatableIcon(
  private val icon: Icon,
  private var parent: Component?,
) : Icon {
  private val timer = Timer(15, null)
  private var currentAngle = 0.0
  private var targetAngle = 0.0

  init {
    this.timer.addActionListener {
      val step = .5
      val diff = targetAngle - currentAngle
      if (abs(diff) < step) {
        currentAngle = targetAngle
        timer.stop()
      } else {
        currentAngle += diff * step
      }
      this.parent?.repaint()
    }
  }

  fun animateTo(angle: Double) {
    this.targetAngle = angle
    if (!timer.isRunning) {
      timer.start()
    }
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    if (this.parent == null) {
      this.parent = c
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val cx = x + icon.iconWidth / 2
    val cy = y + icon.iconHeight / 2
    g2.rotate(Math.toRadians(currentAngle), cx.toDouble(), cy.toDouble())
    icon.paintIcon(c, g2, x, y)
    g2.dispose()
  }

  override fun getIconWidth(): Int = icon.iconWidth

  override fun getIconHeight(): Int = icon.iconHeight
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
