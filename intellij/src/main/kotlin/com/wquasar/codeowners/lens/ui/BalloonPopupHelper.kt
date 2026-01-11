package com.wquasar.codeowners.lens.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.inject.Inject
import javax.swing.JComponent

internal class BalloonPopupHelper @Inject constructor() {

    fun createAndShowBalloonPopupAboveComponent(
        actionEvent: AnActionEvent,
        message: String,
        messageType: MessageType = MessageType.INFO,
        duration: Long = 5000,
    ) {
        val component = actionEvent.inputEvent?.component as? JComponent ?: return
        val displayPoint = RelativePoint(component, Point(component.width / 2, 0))
        val balloon = createBalloonPopup(
            message = message,
            messageType = messageType,
            duration = duration,
        )
        balloon.show(displayPoint, Balloon.Position.above)
    }

    private fun createBalloonPopup(message: String, messageType: MessageType, duration: Long) =
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, messageType, null)
            .setFadeoutTime(duration)
            .createBalloon()
}
