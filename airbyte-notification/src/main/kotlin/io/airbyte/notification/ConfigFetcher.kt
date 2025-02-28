/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.notification

import io.airbyte.api.client.AirbyteApiClient
import io.airbyte.api.client.model.generated.ConnectionIdRequestBody
import io.airbyte.api.client.model.generated.WorkspaceRead
import jakarta.inject.Singleton
import java.util.UUID
import io.airbyte.api.client.model.generated.NotificationType as ApiNotificationType

interface ConfigFetcher<T> {
  fun fetchConfig(connectionId: UUID): T?

  fun notificationType(): NotificationType
}

data class WebhookConfig(
  val webhookUrl: String,
)

@Singleton
class WebhookConfigFetcher(
  private val airbyteApiClient: AirbyteApiClient,
) : ConfigFetcher<WebhookConfig> {
  override fun fetchConfig(connectionId: UUID): WebhookConfig? {
    val workspaceRead: WorkspaceRead =
      ConnectionIdRequestBody(connectionId = connectionId).let {
        airbyteApiClient.workspaceApi.getWorkspaceByConnectionId(it)
      }

    return workspaceRead
      .notifications
      ?.firstOrNull { it.notificationType == ApiNotificationType.SLACK }
      ?.slackConfiguration
      ?.let { WebhookConfig(it.webhook) }
  }

  override fun notificationType(): NotificationType = NotificationType.WEBHOOK
}

data class CustomerIoEmailConfig(
  val to: String,
)
