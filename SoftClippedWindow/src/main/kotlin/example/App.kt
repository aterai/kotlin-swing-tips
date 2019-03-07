package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  init {
    val image = try {
      ImageIO.read(javaClass.getResource("test.jpg"))
    } catch (ex: IOException) {
      makeMissingImage()
    }

    val width = image.getWidth()
    val height = image.getHeight()
    val shape = RoundRectangle2D.Float(0f, 0f, width / 2f, height / 2f, 50f, 50f)

    val clippedImage = makeClippedImage(image, shape)

    val button1 = JButton("clipped window")
    button1.addActionListener { e ->
      val b = e.getSource() as? AbstractButton ?: return@addActionListener
      JWindow().also {
        it.getContentPane().add(makePanel(image))
        it.setShape(shape)
        it.pack()
        it.setLocationRelativeTo(b.getRootPane())
        it.setVisible(true)
      }
    }

    val button2 = JButton("soft clipped window")
    button2.addActionListener { e ->
      val b = e.getSource() as? AbstractButton ?: return@addActionListener
      JWindow().also {
        it.getContentPane().add(makePanel(clippedImage))
        it.setBackground(Color(0x0, true))
        it.pack()
        it.setLocationRelativeTo(b.getRootPane())
        it.setVisible(true)
      }
    }

    add(button1)
    add(button2)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makePanel(image: BufferedImage): Component {
    val panel = object : JPanel(BorderLayout()) {
      override fun getPreferredSize() = Dimension(image.getWidth(this) / 2, image.getHeight(this) / 2)

      protected override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.drawImage(image, 0, 0, this)
        g2.dispose()
        super.paintComponent(g)
      }
    }

    val dwl = DragWindowListener()
    panel.addMouseListener(dwl)
    panel.addMouseMotionListener(dwl)

    val close = JButton("close")
    close.addActionListener { e ->
      val c = e.getSource() as Component
      val window = SwingUtilities.getWindowAncestor(c)
      window.dispose()
    }

    val box = Box.createHorizontalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 30))
      it.add(Box.createHorizontalGlue())
      it.add(close)
      // it.setOpaque(false)
    }

    panel.add(box, BorderLayout.SOUTH)
    panel.setOpaque(false)
    return panel
  }

  // @see https://community.oracle.com/blogs/campbell/2006/07/19/java-2d-trickery-soft-clipping
  // campbell: Java 2D Trickery: Soft Clipping Blog | Oracle Community
  private fun makeClippedImage(source: BufferedImage, shape: Shape): BufferedImage {
    val width = source.getWidth()
    val height = source.getHeight()

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    // g2.setComposite(AlphaComposite.Clear)
    // g2.fillRect(0, 0, width, height)

    g2.setComposite(AlphaComposite.Src)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // g2.setColor(Color.WHITE)
    g2.fill(shape)

    g2.setComposite(AlphaComposite.SrcAtop)
    g2.drawImage(source, 0, 0, null)
    g2.dispose()

    return image
  }

  private fun makeMissingImage(): BufferedImage {
    val bi = BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    g2.setPaint(Color.RED)
    g2.fillRect(0, 0, 320, 240)
    g2.dispose()
    return bi
  }
}

internal class DragWindowListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.setLocation(e.getPoint())
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val window = SwingUtilities.getRoot(e.getComponent()) as? Window ?: return
    if (SwingUtilities.isLeftMouseButton(e)) {
      val pt = window.getLocation()
      window.setLocation(pt.x - startPt.x + e.getX(), pt.y - startPt.y + e.getY())
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
