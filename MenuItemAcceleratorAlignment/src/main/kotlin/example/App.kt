package example

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI
import sun.swing.MenuItemLayoutHelper
import sun.swing.MenuItemLayoutHelper.LayoutResult
import sun.swing.SwingUtilities2
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicMenuItemUI

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  val menu0 = JMenu("Default")
  val menu1 = JMenu("RightAcc")
  menu0.mnemonic = KeyEvent.VK_D
  menu1.mnemonic = KeyEvent.VK_R
  menuBar.add(menu0)
  menuBar.add(menu1)

  val list = mutableListOf<JMenuItem>()
  var menuItem = JMenuItem("mi")
  menuItem.mnemonic = KeyEvent.VK_N
  menuItem.accelerator = KeyStroke.getKeyStroke(
    KeyEvent.VK_N,
    InputEvent.ALT_DOWN_MASK
  )
  list.add(menuItem)
  menuItem = JMenuItem("aaa")
  menuItem.mnemonic = KeyEvent.VK_1
  menuItem.accelerator = KeyStroke.getKeyStroke(
    KeyEvent.VK_ESCAPE,
    InputEvent.ALT_DOWN_MASK
  )
  list.add(menuItem)
  menuItem = JMenuItem("bb")
  menuItem.mnemonic = KeyEvent.VK_2

  val msk2 = InputEvent.ALT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, msk2)
  list.add(menuItem)
  menuItem = JMenuItem("c")
  menuItem.mnemonic = KeyEvent.VK_3

  val msk3 = InputEvent.ALT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, msk3)
  list.add(menuItem)

  for (mi in list) {
    menu0.add(mi)
    menu1.add(makeMenuItem(mi))
  }
  return menuBar
}

private fun makeMenuItem(mi: JMenuItem): JMenuItem {
  val menuItem = object : JMenuItem(mi.text) {
    override fun updateUI() {
      super.updateUI()
      ui = if (ui is WindowsMenuItemUI) {
        RaaWindowsMenuItemUI()
      } else {
        RaaBasicMenuItemUI()
      }
      // XXX: setLocale(Locale.JAPAN)
    }
  }
  menuItem.mnemonic = mi.mnemonic
  menuItem.accelerator = mi.accelerator
  return menuItem
}

private object MenuItemHelper {
  fun paintIcon(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult
  ) {
    lh.icon?.also {
      val menuItem = lh.menuItem
      val model = menuItem.model
      val icon = if (model.isEnabled) {
        if (model.isPressed && model.isArmed) {
          menuItem.pressedIcon ?: menuItem.icon
        } else {
          menuItem.icon
        }
      } else {
        menuItem.disabledIcon
      }
      icon?.paintIcon(menuItem, g, lr.iconRect.x, lr.iconRect.y)
    }
  }

  fun paintCheckIcon(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult,
    c: Color?,
    foreground: Color?
  ) {
    lh.checkIcon?.also {
      val menuItem = lh.menuItem
      val model = menuItem.model
      g.color = when {
        model.isArmed -> foreground
        menuItem is JMenu && model.isSelected -> foreground
        else -> c
      }
      if (lh.useCheckAndArrow()) {
        it.paintIcon(menuItem, g, lr.checkRect.x, lr.checkRect.y)
      }
    }
  }

  @Suppress("LongParameterList")
  fun paintAccText(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult,
    disabledForeground: Color?,
    acceleratorForeground: Color?,
    acceleratorSelectionForeground: Color?
  ) {
    val text = lh.accText
    if (text.isEmpty()) {
      return
    }
    val viewRect = lh.viewRect
    val accRect = lr.accRect
    val ascent = lh.accFontMetrics.ascent
    val menuItem = lh.menuItem
    val model = menuItem.model
    g.font = lh.accFontMetrics.font
    if (model.isEnabled) { // *** paint the accText normally
      if (model.isArmed) {
        g.color = acceleratorSelectionForeground
      } else if (menuItem is JMenu && model.isSelected) {
        g.color = acceleratorSelectionForeground
      } else {
        g.color = acceleratorForeground
      }
      drawString(
        menuItem,
        g,
        text,
        viewRect.x + viewRect.width - menuItem.iconTextGap - accRect.width,
        accRect.y + ascent
      )
    } else { // *** paint the accText disabled
      if (disabledForeground != null) {
        g.color = disabledForeground
        drawString(menuItem, g, text, accRect.x, accRect.y + ascent)
      } else {
        g.color = menuItem.background.brighter()
        drawString(menuItem, g, text, accRect.x, accRect.y + ascent)
        g.color = menuItem.background.darker()
        drawString(menuItem, g, text, accRect.x - 1, accRect.y + lh.fontMetrics.ascent - 1)
      }
    }
  }

  private fun drawString(c: JComponent, g: Graphics, text: String, x: Int, y: Int) {
    SwingUtilities2.drawString(c, g, text, x, y)
    // Java 9: BasicGraphicsUtils.drawString(c, g as Graphics2D, text, x, y)
  }

  fun paintArrowIcon(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult,
    foreground: Color?
  ) {
    lh.arrowIcon?.also { arrowIcon ->
      val menuItem = lh.menuItem
      val model = menuItem.model
      if (model.isArmed || menuItem is JMenu && model.isSelected) {
        g.color = foreground
      }
      if (lh.useCheckAndArrow()) {
        arrowIcon.paintIcon(menuItem, g, lr.arrowRect.x, lr.arrowRect.y)
      }
    }
  }

  fun applyInsets(rect: Rectangle, insets: Insets?) {
    insets?.also { i ->
      rect.x += i.left
      rect.y += i.top
      rect.width -= i.right + rect.x
      rect.height -= i.bottom + rect.y
    }
  }
}

private class RaaWindowsMenuItemUI : WindowsMenuItemUI() {
  override fun paintMenuItem(
    g: Graphics,
    c: JComponent,
    checkIcon: Icon,
    arrowIcon: Icon,
    background: Color,
    foreground: Color,
    defaultTextIconGap: Int
  ) { // // Save original graphics font and color
    val g2 = g.create() as? Graphics2D ?: return
    val mi = c as? JMenuItem ?: return
    g2.font = mi.font
    val viewRect = Rectangle(mi.width, mi.height)
    MenuItemHelper.applyInsets(viewRect, mi.insets)
    val lh = MenuItemLayoutHelper(
      mi, checkIcon, arrowIcon, viewRect, defaultTextIconGap, "+",
      true, mi.font, acceleratorFont,
      MenuItemLayoutHelper.useCheckAndArrow(menuItem), propertyPrefix
    )
    val lr = lh.layoutMenuItem()
    paintBackground(g2, mi, background)
    MenuItemHelper.paintCheckIcon(g2, lh, lr, g.color, foreground)
    MenuItemHelper.paintIcon(g2, lh, lr)
    paintText(g2, lh, lr)
    MenuItemHelper.paintAccText(g2, lh, lr, disabledForeground, acceleratorForeground, acceleratorSelectionForeground)
    MenuItemHelper.paintArrowIcon(g2, lh, lr, foreground)
  }

  private fun paintText(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult
  ) {
    if (lh.text.isNotEmpty()) {
      if (lh.htmlView != null) {
        lh.htmlView.paint(g, lr.textRect)
      } else { // Text isn't HTML
        paintText(g, lh.menuItem, lr.textRect, lh.text)
      }
    }
  }
}

private class RaaBasicMenuItemUI : BasicMenuItemUI() {
  override fun paintMenuItem(
    g: Graphics,
    c: JComponent,
    checkIcon: Icon,
    arrowIcon: Icon,
    background: Color,
    foreground: Color,
    defaultTextIconGap: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val mi = c as? JMenuItem ?: return
    g2.font = mi.font
    val viewRect = Rectangle(mi.width, mi.height)
    MenuItemHelper.applyInsets(viewRect, mi.insets)
    val lh = MenuItemLayoutHelper(
      mi, checkIcon, arrowIcon, viewRect, defaultTextIconGap, "+",
      true, mi.font, acceleratorFont,
      MenuItemLayoutHelper.useCheckAndArrow(menuItem), propertyPrefix
    )
    val lr = lh.layoutMenuItem()
    paintBackground(g2, mi, background)
    MenuItemHelper.paintCheckIcon(g2, lh, lr, g.color, foreground)
    MenuItemHelper.paintIcon(g2, lh, lr)
    paintText(g2, lh, lr)
    MenuItemHelper.paintAccText(g2, lh, lr, disabledForeground, acceleratorForeground, acceleratorSelectionForeground)
    MenuItemHelper.paintArrowIcon(g2, lh, lr, foreground)
  }

  private fun paintText(
    g: Graphics,
    lh: MenuItemLayoutHelper,
    lr: LayoutResult
  ) {
    if (lh.text.isNotEmpty()) {
      if (lh.htmlView != null) {
        lh.htmlView.paint(g, lr.textRect)
      } else { // Text isn't HTML
        paintText(g, lh.menuItem, lr.textRect, lh.text)
      }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
