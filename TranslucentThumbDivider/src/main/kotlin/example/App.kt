package example

import java.awt.*
import java.awt.color.ColorSpace
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
// import java.util.Objects
import javax.swing.*
import javax.swing.plaf.LayerUI

class MainPanel : JPanel(BorderLayout()) {
  init {

    val split = JSplitPane()
    split.setContinuousLayout(true)
    split.setResizeWeight(.5)
    split.setDividerSize(0)

    val icon = ImageIcon(javaClass.getResource("test.png"))

    val source = BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB)
    val g = source.createGraphics()
    g.drawImage(icon.getImage(), 0, 0, null)
    g.dispose()
    val colorConvert = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
    val destination = colorConvert.filter(source, null)

    val beforeCanvas = object : JComponent() {
      protected override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), this)
      }
    }
    split.setLeftComponent(beforeCanvas)

    val afterCanvas = object : JComponent() {
      protected override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        val iw = icon.getIconWidth()
        val ih = icon.getIconHeight()
        val pt = getLocation()
        val ins = split.getBorder().getBorderInsets(split)
        g2.translate(-pt.x + ins.left, 0)
        g2.drawImage(destination, 0, 0, iw, ih, this)
        g2.dispose()
      }
    }
    split.setRightComponent(afterCanvas)

    add(JLayer<JSplitPane>(split, DividerLocationDragLayerUI()))
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class DividerLocationDragLayerUI : LayerUI<JSplitPane>() {
  private val startPt = Point()
  private val dc = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  private val wc = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
  private val thumb = Ellipse2D.Double()
  private var dividerLocation: Int = 0
  private var isDragging: Boolean = false
  private var isEnter: Boolean = false

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if ((isEnter || isDragging) && c is JLayer<*>) {
      updateThumbLocation(c.getView(), thumb)
      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setPaint(Color(255, 100, 100, 100))
      g2.fill(thumb)
      g2.dispose()
    }
  }

  protected override fun processMouseEvent(e: MouseEvent, l: JLayer<out JSplitPane>) {
    val splitPane = l.getView()
    when (e.getID()) {
      MouseEvent.MOUSE_ENTERED -> isEnter = true
      MouseEvent.MOUSE_EXITED -> isEnter = false
      MouseEvent.MOUSE_RELEASED -> isDragging = false
      MouseEvent.MOUSE_PRESSED -> {
        val c = e.getComponent()
        if (isDraggableComponent(splitPane, c)) {
          val pt = SwingUtilities.convertPoint(c, e.getPoint(), splitPane)
          isDragging = thumb.contains(pt)
          startPt.setLocation(SwingUtilities.convertPoint(c, e.getPoint(), splitPane))
          dividerLocation = splitPane.getDividerLocation()
        }
      }
      else -> {
      }
    }
    splitPane.repaint()
  }

  protected override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JSplitPane>) {
    val splitPane = l.getView()
    val c = e.getComponent()
    val pt = SwingUtilities.convertPoint(c, e.getPoint(), splitPane)
    if (e.getID() == MouseEvent.MOUSE_MOVED) {
      splitPane.setCursor(if (thumb.contains(e.getPoint())) wc else dc)
    } else if (isDragging && isDraggableComponent(splitPane, c) && e.getID() == MouseEvent.MOUSE_DRAGGED) {
      val delta = if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) pt.x - startPt.x else pt.y - startPt.y
      splitPane.setDividerLocation(Math.max(0, dividerLocation + delta))
    }
  }

  companion object {
    private val R = 25
    private fun isDraggableComponent(splitPane: JSplitPane, c: Component): Boolean {
      return splitPane == c || splitPane == SwingUtilities.getUnwrappedParent(c)
    }
    private fun updateThumbLocation(splitPane: Component, thumb: Ellipse2D) {
      if (splitPane is JSplitPane) {
        val pos = splitPane.getDividerLocation()
        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
          thumb.setFrame((pos - R).toDouble(), (splitPane.getHeight() / 2 - R).toDouble(), (R + R).toDouble(), (R + R).toDouble())
        } else {
          thumb.setFrame((splitPane.getWidth() / 2 - R).toDouble(), (pos - R).toDouble(), (R + R).toDouble(), (R + R).toDouble())
        }
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
      }
      JFrame().apply {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        getContentPane().add(MainPanel())
        pack()
        setLocationRelativeTo(null)
        setVisible(true)
      }
    }
  })
}
