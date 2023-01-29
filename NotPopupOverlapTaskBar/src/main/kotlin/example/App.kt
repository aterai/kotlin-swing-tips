package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val list = listOf<Component>(
    JMenuItem("JMenuItem 1"),
    JMenuItem("JMenuItem 2"),
    JMenuItem("JMenuItem 3"),
    JMenuItem("JMenuItem 4"),
    JMenuItem("JMenuItem 5")
  )
  val popup = object : JPopupMenu() {
    override fun show(c: Component, x: Int, y: Int) {
      val popupLocation = getInvokerOrigin(x, y, c.locationOnScreen)
      val scrBounds = getScreenBounds(c, popupLocation)
      val popupSize = preferredSize
      val popupBottomY = popupLocation.y.toLong() + popupSize.height.toLong()
      val p = Point(x, y)
      removeAll()
      if (popupBottomY > scrBounds.y + scrBounds.height) {
        p.translate(-popupSize.width, -popupSize.height)
        for (i in list.indices.reversed()) {
          add(list[i])
        }
      } else {
        list.forEach { add(it) }
      }
      super.show(c, p.x, p.y)
    }
  }
  list.forEach { popup.add(it) }

  return JPanel().also {
    it.componentPopupMenu = popup
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getScreenBounds(c: Component, popupLocation: Point): Rectangle {
  val scrBounds: Rectangle
  val gc = getCurrentGraphicsConfiguration2(c, popupLocation)
  val toolkit = Toolkit.getDefaultToolkit()
  scrBounds = if (gc != null) {
    // If we have GraphicsConfiguration use it to get screen bounds
    gc.bounds
  } else {
    Rectangle(toolkit.screenSize)
  }
  val scrInsets = toolkit.getScreenInsets(gc)
  scrBounds.x += scrInsets.left
  scrBounds.y += scrInsets.top
  scrBounds.width -= scrInsets.left + scrInsets.right
  scrBounds.height -= scrInsets.top + scrInsets.bottom
  return scrBounds
}

// @see JPopupMenu#getCurrentGraphicsConfiguration(Point)
private fun getCurrentGraphicsConfiguration2(c: Component?, p: Point): GraphicsConfiguration? {
  var gc: GraphicsConfiguration? = null
  val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
  val gd = ge.screenDevices
  for (graphicsDevice in gd) {
    if (graphicsDevice.type == GraphicsDevice.TYPE_RASTER_SCREEN) {
      val dgc = graphicsDevice.defaultConfiguration
      if (dgc.bounds.contains(p)) {
        gc = dgc
        break
      }
    }
  }
  // If not found, and we have invoker, ask invoker about his gc
  if (gc == null && c != null) {
    gc = c.graphicsConfiguration
  }
  return gc
}

// @see JPopupMenu#show(Component invoker, int x, int y)
// To avoid integer overflow
private fun getInvokerOrigin(x: Int, y: Int, invokerOrigin: Point): Point {
  var lx = invokerOrigin.x.toLong() + x.toLong()
  var ly = invokerOrigin.y.toLong() + y.toLong()
  if (lx > Int.MAX_VALUE) {
    lx = Int.MAX_VALUE.toLong()
  }
  if (lx < Int.MIN_VALUE) {
    lx = Int.MIN_VALUE.toLong()
  }
  if (ly > Int.MAX_VALUE) {
    ly = Int.MAX_VALUE.toLong()
  }
  if (ly < Int.MIN_VALUE) {
    ly = Int.MIN_VALUE.toLong()
  }
  return Point(lx.toInt(), ly.toInt())
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
