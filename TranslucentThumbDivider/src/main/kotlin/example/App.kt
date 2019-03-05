package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.color.ColorSpace
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import javax.swing.* // ktlint-disable no-wildcard-imports
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
        val iw = icon.getIconWidth()
        val ih = icon.getIconHeight()
        val dim = split.getSize()
        val x = (dim.width - iw) / 2
        val y = (dim.height - ih) / 2
        g.drawImage(icon.getImage(), x, y, iw, ih, this)
      }
    }
    split.setLeftComponent(beforeCanvas)

    val afterCanvas = object : JComponent() {
      protected override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        val iw = destination.getWidth(this)
        val ih = destination.getHeight(this)
        val pt = getLocation()
        val ins = split.getBorder().getBorderInsets(split)
        g2.translate(-pt.x + ins.left, 0)

        val dim = split.getSize()
        val x = (dim.width - iw) / 2
        val y = (dim.height - ih) / 2
        g2.drawImage(destination, x, y, iw, ih, this)
        g2.dispose()
      }
    }
    split.setRightComponent(afterCanvas)

    val layerUI = DividerLocationDragLayerUI()
    val check = JCheckBox("Paint divider")
    check.addActionListener { e ->
      layerUI.setPaintDividerEnabled((e.getSource() as JCheckBox).isSelected())
    }

    add(JLayer<JSplitPane>(split, layerUI))
    add(check, BorderLayout.SOUTH)
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class DividerLocationDragLayerUI : LayerUI<JSplitPane>() {
  private val startPt = Point()
  private val dc = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  private val wc = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
  private val thumb = Ellipse2D.Double()
  private var dividerLocation = 0
  private var isDragging = false
  private var isEnter = false
  private var dividerEnabled = false

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
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
      if (dividerEnabled) {
        paintDivider(g2)
      }
      g2.dispose()
    }
  }

  private fun paintDivider(g2: Graphics2D) {
    g2.setStroke(BasicStroke(5f))
    g2.setPaint(Color.WHITE)
    g2.draw(thumb)

    val cx = thumb.getCenterX()
    val cy = thumb.getCenterY()

    val line = Line2D.Double(cx, 0.0, cx, thumb.getMinY())
    g2.draw(line)

    val v = 8.0
    val mx = cx - thumb.getWidth() / 4.0 + v / 2.0
    val triangle = Path2D.Double()
    triangle.moveTo(mx, cy - v)
    triangle.lineTo(mx - v, cy)
    triangle.lineTo(mx, cy + v)
    triangle.lineTo(mx, cy - v)
    triangle.closePath()
    g2.fill(triangle)

    val at = AffineTransform.getQuadrantRotateInstance(2, cx, cy)
    g2.draw(at.createTransformedShape(line))
    g2.fill(at.createTransformedShape(triangle))
  }

  fun setPaintDividerEnabled(flg: Boolean) {
    this.dividerEnabled = flg
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
    private const val R = 25.0
    private fun isDraggableComponent(splitPane: JSplitPane, c: Component): Boolean {
      return splitPane == c || splitPane == SwingUtilities.getUnwrappedParent(c)
    }

    private fun updateThumbLocation(c: Component, thumb: Ellipse2D) {
      val splitPane = c as? JSplitPane ?: return
      val pos = splitPane.getDividerLocation()
      if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
        thumb.setFrame(pos - R, splitPane.getHeight() / 2.0 - R, R + R, R + R)
      } else {
        thumb.setFrame(splitPane.getWidth() / 2.0 - R, pos - R, R + R, R + R)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
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
}
