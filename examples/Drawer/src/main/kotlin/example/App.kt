package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import kotlin.math.min
import kotlin.math.pow

val DRAWER_BG = Color(33, 37, 41)
val HOVER_COLOR = Color(52, 58, 64)
val TEXT_COLOR = Color(248, 249, 250)
const val DRAWER_WIDTH = 200
const val DURATION = 400
private val mainConstants = JPanel()
private val overlay = makeOverlay()
private val drawer = makeDrawer()
private val timer = Timer(10) { animate() }
private var position = DrawerPosition.LEFT
private var startTime = 0L
private var startX = 0
private var targetX = 0
private var isOpen = false

fun makeUI(): Component {
  mainConstants.add(makeControlBox())
  mainConstants.setBackground(Color.GRAY)
  mainConstants.preferredSize = Dimension(320, 240)
  EventQueue.invokeLater {
    initLayeredPane()
    updateLayoutSizes()
  }
  return mainConstants
}

private fun initLayeredPane() {
  val layeredPane = mainConstants.rootPane.getLayeredPane()
  layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER)
  layeredPane.add(drawer, JLayeredPane.MODAL_LAYER)
  layeredPane.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent?) {
      updateLayoutSizes()
    }
  })
}

private fun makeControlBox(): JPanel {
  val btnOpen = JButton("Open Menu")
  btnOpen.addActionListener { toggleDrawer() }
  val rbLeft = JRadioButton("Left", true)
  val rbRight = JRadioButton("Right", false)
  val group = ButtonGroup()
  group.add(rbLeft)
  group.add(rbRight)
  val positionSwitcher = ActionListener {
    position = if (rbLeft.isSelected) DrawerPosition.LEFT else DrawerPosition.RIGHT
    updateLayoutSizes()
  }
  rbLeft.addActionListener(positionSwitcher)
  rbRight.addActionListener(positionSwitcher)
  val controls = JPanel()
  controls.setOpaque(false)
  controls.add(btnOpen)
  controls.add(rbLeft)
  controls.add(rbRight)
  return controls
}

private fun makeOverlay(): JPanel {
  val ov = OverlayPanel()
  ov.isVisible = false
  ov.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (isOpen) {
        toggleDrawer()
      }
    }
  })
  return ov
}

private fun makeDrawer(): JPanel {
  val menuContainer = Box.createVerticalBox()
  menuContainer.setBackground(DRAWER_BG)
  menuContainer.add(NavButton("🏠  Dashboard"))
  menuContainer.add(NavButton("📩  Messages"))
  menuContainer.add(NavButton("📊  Analytics"))
  menuContainer.add(NavButton("⚙️  Settings"))
  menuContainer.add(Box.createVerticalGlue())
  menuContainer.add(NavButton("🚪  Logout"))
  val scroll = JScrollPane(menuContainer)
  scroll.setBorder(BorderFactory.createEmptyBorder())
  scroll.getVerticalScrollBar().setUnitIncrement(16)
  scroll.getViewport().setBackground(DRAWER_BG)
  val dr = JPanel(BorderLayout())
  dr.setBackground(DRAWER_BG)
  dr.add(scroll)
  return dr
}

private fun updateLayoutSizes() {
  val d = mainConstants.size
  val w = d.width
  val h = d.height
  mainConstants.setBounds(0, 0, w, h) // this.setBounds(0, 0, w, h);
  overlay.setBounds(0, 0, w, h)
  drawer.setBounds(if (isOpen) getOpenX(w) else getClosedX(w), 0, DRAWER_WIDTH, h)
  mainConstants.revalidate()
  mainConstants.repaint()
}

private fun getOpenX(w: Int): Int {
  return if (position == DrawerPosition.LEFT) 0 else w - DRAWER_WIDTH
}

private fun getClosedX(w: Int): Int {
  return if (position == DrawerPosition.LEFT) -DRAWER_WIDTH else w
}

private fun toggleDrawer() {
  if (!timer.isRunning) {
    isOpen = !isOpen
    val w = mainConstants.getWidth()
    startX = drawer.getX()
    targetX = if (isOpen) getOpenX(w) else getClosedX(w)
    overlay.isVisible = isOpen
    startTime = System.currentTimeMillis()
    timer.start()
  }
}

private fun animate() {
  val elapsed = System.currentTimeMillis() - startTime
  val progress = min(1.0, elapsed.toDouble() / DURATION)
  val easedProgress = 1 - (1 - progress).pow(3.0)
  val currentX = (startX + (targetX - startX) * easedProgress).toInt()
  drawer.setLocation(currentX, 0)
  val stop = progress >= 1.0
  if (stop) {
    timer.stop()
    if (!isOpen) {
      overlay.isVisible = false
    }
  }
}

private enum class DrawerPosition {
  LEFT,
  RIGHT,
}

private class OverlayPanel : JPanel() {
  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.color = Color(0, 0, 0, 140)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }
}

private class NavButton(text: String) : JButton(text) {
  private var mouseListener: MouseListener? = null

  override fun updateUI() {
    removeMouseListener(mouseListener)
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBorderPainted(false)
    setForeground(TEXT_COLOR)
    setHorizontalAlignment(LEFT)
    setFont(getFont().deriveFont(Font.PLAIN, 14f))
    setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20))
    setCursor(Cursor(Cursor.HAND_CURSOR))
    mouseListener = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent?) {
        setOpaque(true)
        setBackground(HOVER_COLOR)
      }

      override fun mouseExited(e: MouseEvent?) {
        setOpaque(false)
      }

      override fun mousePressed(e: MouseEvent) {
        setBackground(HOVER_COLOR.darker())
      }

      override fun mouseReleased(e: MouseEvent) {
        setBackground(HOVER_COLOR)
      }
    }
    addMouseListener(mouseListener)
  }

  override fun getMaximumSize(): Dimension {
    return Dimension(DRAWER_WIDTH, 50)
  }

  override fun paintComponent(g: Graphics) {
    if (isOpaque) {
      g.color = getBackground()
      g.fillRect(0, 0, getWidth(), getHeight())
    }
    super.paintComponent(g)
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
