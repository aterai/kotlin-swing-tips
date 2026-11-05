package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs

fun createUI(): Component {
  val accordion = Box.createVerticalBox()
  accordion.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  createExpansionPanelList().forEach {
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

private fun createExpansionPanelList() = listOf(
  ExpansionPanel("System Tasks", createSystemTasksPanel()),
  ExpansionPanel("Other Places", createOtherPlacesPanel()),
  ExpansionPanel("Details", createDetailsPanel()),
)

private fun createSystemTasksPanel() = JPanel(GridLayout(0, 1)).also { p ->
  listOf("1111", "222222")
    .map { title -> JCheckBox(title) }
    .forEach { check -> p.add(check) }
}

private fun createOtherPlacesPanel() = JPanel(GridLayout(0, 1)).also { p ->
  listOf("Desktop", "My Network Places", "My Documents", "Shared Documents")
    .map { title -> JLabel(title) }
    .forEach { label -> p.add(label) }
}

private fun createDetailsPanel() = JPanel(GridLayout(0, 1)).also { p ->
  val bg = ButtonGroup()
  listOf("aaa", "bbb", "ccc", "ddd")
    .map { title -> JRadioButton(title) }
    .forEach { radio ->
      radio.isSelected = p.componentCount == 0
      p.add(radio)
      bg.add(radio)
    }
}

// The content is passed in instead of being created by an abstract factory
// method called from the initializer, which would run before the
// subclass properties are initialized.
private class ExpansionPanel(
  title: String,
  private val contentPanel: JComponent,
) : JPanel(BorderLayout()) {
  private val toggleButton = object : JToggleButton() {
    override fun updateUI() {
      super.updateUI()
      setContentAreaFilled(false)
      setBorderPainted(false)
      setFocusable(false)
    }
  }
  private val titleButton = object : JButton() {
    override fun updateUI() {
      super.updateUI()
      setLayout(BoxLayout(this, BoxLayout.X_AXIS))
      setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0))
    }
  }

  init {
    val arrowIcon = UIManager.getIcon("Menu.arrowIcon")
    val animatedIcon = AnimatableIcon(arrowIcon, toggleButton)
    toggleButton.setIcon(animatedIcon)
    // An ItemListener is also notified by setSelected(), so it handles both buttons.
    toggleButton.addItemListener { updateExpandedState(animatedIcon) }
    titleButton.add(JLabel(title))
    titleButton.add(Box.createHorizontalGlue())
    titleButton.add(toggleButton)
    titleButton.addActionListener { toggleButton.setSelected(!toggleButton.isSelected) }
    add(titleButton, BorderLayout.NORTH)
    contentPanel.isVisible = false
    contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    add(contentPanel)
  }

  private fun updateExpandedState(animatedIcon: AnimatableIcon) {
    val expanded = toggleButton.isSelected
    animatedIcon.animateTo(if (expanded) 90.0 else 0.0)
    contentPanel.isVisible = expanded
    revalidate()
    if (expanded) {
      // The rectangle is relative to contentPanel itself, so bounds cannot be used.
      EventQueue.invokeLater {
        contentPanel.scrollRectToVisible(Rectangle(contentPanel.size))
      }
    }
  }

  override fun getPreferredSize(): Dimension {
    val d = titleButton.getPreferredSize()
    if (contentPanel.isVisible) {
      d.height += contentPanel.getPreferredSize().height
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
  private val parent: Component,
) : Icon {
  private val timer = Timer(15) { updateAngle() }

  // MetalIconFactory.MenuArrowIcon casts the component passed to paintIcon() to JMenuItem.
  private val menuItem = JMenuItem()
  private var currentAngle = 0.0
  private var targetAngle = 0.0

  private fun updateAngle() {
    val diff = targetAngle - currentAngle
    if (abs(diff) < SNAP_THRESHOLD) {
      currentAngle = targetAngle
      timer.stop()
    } else {
      currentAngle += diff * EASING_RATE
    }
    parent.repaint()
  }

  fun animateTo(angle: Double) {
    targetAngle = angle
    if (!timer.isRunning) {
      timer.start()
    }
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    // Use floating point so that an icon with an odd size rotates around its true center.
    val cx = x + icon.iconWidth / 2.0
    val cy = y + icon.iconHeight / 2.0
    g2.rotate(Math.toRadians(currentAngle), cx, cy)
    icon.paintIcon(menuItem, g2, x, y)
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight

  private companion object {
    const val EASING_RATE = .5
    const val SNAP_THRESHOLD = .5
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
