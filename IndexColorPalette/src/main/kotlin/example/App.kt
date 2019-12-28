package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.IndexColorModel
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p = JPanel()
    val label1 = JLabel()
    p.add(label1)
    val label2 = JLabel()
    p.add(label2)

    // val image = try {
    //   ImageIO.read(javaClass.getResource("duke.gif"))
    // } catch (ex: IOException) {
    //   ex.printStackTrace()
    //   makeMissingImage()
    // }
    val image = runCatching { ImageIO.read(javaClass.getResource("duke.gif")) }
      .onFailure { it.printStackTrace() }
      .getOrNull() ?: makeMissingImage()

    label1.setIcon(ImageIcon(image))

    val colorModel = image.getColorModel()
    val indexColorModel = colorModel as? IndexColorModel
    val transIndex = indexColorModel?.getTransparentPixel() ?: -1
    val w = image.getWidth()
    val h = image.getHeight()
    val dataBuffer = image.getRaster().getDataBuffer()
    label2.setIcon(ImageIcon(makeTestImage(dataBuffer, colorModel, w, h, transIndex)))

    val box = JPanel(GridBagLayout())
    if (indexColorModel != null) {
      val palette = object : JList<IndexedColor>(PaletteListModel(indexColorModel)) {
        override fun updateUI() {
          setCellRenderer(null)
          super.updateUI()
          setLayoutOrientation(HORIZONTAL_WRAP)
          setVisibleRowCount(8)
          setFixedCellWidth(CELL_SIZE.width)
          setFixedCellHeight(CELL_SIZE.height)
          getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
          setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
          val renderer = getCellRenderer()
          setCellRenderer { list, value, index, isSelected, cellHasFocus ->
            val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            (c as? JLabel)?.also {
              it.setIcon(ColorIcon(value.color))
              it.setToolTipText("index: ${value.index}")
              it.setBorder(BorderFactory.createLineBorder(when {
                value.isTransparentPixel -> Color.RED
                else -> Color.WHITE
              }))
            }
            return@setCellRenderer c
          }
        }
      }
      box.add(JScrollPane(palette), GridBagConstraints())
    } else {
      box.add(JLabel("No IndexColorModel"), GridBagConstraints())
    }

    add(p, BorderLayout.NORTH)
    add(box)
    setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTestImage(dataBuffer: DataBuffer, colorModel: ColorModel, w: Int, h: Int, transIdx: Int): Image {
    val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until h) {
      for (x in 0 until w) {
        val arrayIndex = x + y * w
        val colorIndex = dataBuffer.getElem(arrayIndex)
        if (transIdx == colorIndex) {
          buf.setRGB(x, y, Color.RED.getRGB())
        } else {
          buf.setRGB(x, y, colorModel.getRGB(colorIndex))
        }
      }
    }
    return buf
  }

  private fun makeMissingImage(): BufferedImage {
    val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
    val w = missingIcon.getIconWidth()
    val h = missingIcon.getIconHeight()
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    return bi
  }

  companion object {
    val CELL_SIZE = Dimension(8, 8)
  }
}

data class IndexedColor(val index: Int, val color: Color, val isTransparentPixel: Boolean)

class PaletteListModel(private val model: IndexColorModel) : AbstractListModel<IndexedColor>() {
  override fun getSize() = model.getMapSize()

  override fun getElementAt(i: Int) = IndexedColor(i, Color(model.getRGB(i)), i == model.getTransparentPixel())
}

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, getIconWidth(), getIconHeight())
    g2.dispose()
  }

  override fun getIconWidth() = MainPanel.CELL_SIZE.width - 2

  override fun getIconHeight() = MainPanel.CELL_SIZE.height - 2
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
