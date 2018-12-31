package example

import java.awt.*
import javax.swing.*

class MainPanel : JPanel(BorderLayout()) {
  init {
    val check = JCheckBox("setXORMode(Color.BLUE)", true)
    check.addActionListener({ repaint() })

    val split = JSplitPane()
    split.setContinuousLayout(true)
    split.setResizeWeight(.5)

    val icon = ImageIcon(javaClass.getResource("test.png"))

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
        if (check.isSelected()) {
          g2.setColor(getBackground())
          g2.setXORMode(Color.BLUE)
        } else {
          g2.setPaintMode()
        }
        val pt = getLocation()
        val ins = split.getBorder().getBorderInsets(split)
        g2.translate(-pt.x + ins.left, 0)
        g2.drawImage(icon.getImage(), 0, 0, iw, ih, this)
        g2.dispose()
      }
    }
    split.setRightComponent(afterCanvas)

    add(split)
    add(check, BorderLayout.SOUTH)
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }
}

fun main() {
  EventQueue.invokeLater({
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
  })
}
